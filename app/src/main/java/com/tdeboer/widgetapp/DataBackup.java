package com.tdeboer.widgetapp;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;

import androidx.datastore.preferences.core.Preferences;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tdeboer.widgetapp.recyclers.WidgetItem;
import com.tdeboer.widgetapp.widgets.gen.WidgetProviderGen;
import com.tdeboer.widgetapp.widgets.image.WidgetHelperImages;
import com.tdeboer.widgetapp.widgets.image.WidgetProviderImages;
import com.tdeboer.widgetapp.widgets.maze.WidgetProviderMaze;
import com.tdeboer.widgetapp.widgets.quote.WidgetProviderQuotes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

// saves the settings and uploaded images in one zip file. the generated animations and mazes are not in there,
// the widgets make those again.
// widget ids are different on every phone, so widgets are matched by type and position.
// all methods wait for files and settings, so never call them on the main thread
public final class DataBackup {
    private static final int VERSION = 1;
    private static final int NO_WIDGET = -1;
    private static final String MANIFEST = "backup.json";
    private static final String IMAGE_FOLDER = "images/";
    private static final String IMAGE_NAME = WidgetHelperImages.FILE_PREFIX + "[0-9a-f-]{36}";
    private static final long MAX_IMPORT_BYTES = 1024L * 1024L * 1024L;
    private static final Gson GSON = new Gson();

    private static final List<Setting<?>> SETTINGS = Arrays.asList(
            floatRange(SettingsManager.QUOTEANDFACT, 0f, 1f),
            floatRange(SettingsManager.GENERATED_SCALE, 1f, 12f),
            intRange(SettingsManager.GENERATED_OCTAVES, 1, 6),
            intRange(SettingsManager.GENERATED_PALETTE, 0, 8),
            intRange(SettingsManager.GENERATED_ALGORITHM, 0, 9),
            intRange(SettingsManager.GENERATED_SPEED, 1, 5),
            new Setting<>(SettingsManager.GENERATED_RANDOM, JsonElement::getAsBoolean),
            new Setting<>(SettingsManager.QUOTE_AVOID_REPEATS, JsonElement::getAsBoolean),
            new Setting<>(SettingsManager.QUOTE_HISTORY, JsonElement::getAsString),
            new Setting<>(SettingsManager.FACT_HISTORY, JsonElement::getAsString),
            intRange(SettingsManager.MAZE_CELLS, 8, 25),
            intRange(SettingsManager.MAZE_STYLE, 0, 2),
            intRange(SettingsManager.MAZE_THEME, 0, 3),
            intRange(SettingsManager.MAZE_SPEED, 10, 60),
            intRange(SettingsManager.APPEARANCE, AppTheme.APPEARANCE_SYSTEM, AppTheme.APPEARANCE_DARK),
            new Setting<>(SettingsManager.ACCENT_COLOR, JsonElement::getAsInt)
    );

    private static final Map<WidgetItem.widgetTypes, Class<? extends AppWidgetProvider>> PROVIDERS = new LinkedHashMap<>();

    static {
        PROVIDERS.put(WidgetItem.widgetTypes.QUOTES, WidgetProviderQuotes.class);
        PROVIDERS.put(WidgetItem.widgetTypes.IMAGES, WidgetProviderImages.class);
        PROVIDERS.put(WidgetItem.widgetTypes.GEN, WidgetProviderGen.class);
        PROVIDERS.put(WidgetItem.widgetTypes.MAZE, WidgetProviderMaze.class);
    }

    private DataBackup() { }

    private static final class Setting<T> {
        final Preferences.Key<T> key;
        final Function<JsonElement, T> parse;

        Setting(Preferences.Key<T> key, Function<JsonElement, T> parse) {
            this.key = key;
            this.parse = parse;
        }

        void addTo(JsonObject json, int appWidgetId) {
            T value = SettingsManager.readsync(keyFor(key, appWidgetId));
            if (value != null) {
                json.add(key.getName(), GSON.toJsonTree(value));
            }
        }

        // the value is checked here, the write itself runs when the whole file turned out to be fine
        Runnable writeFrom(JsonObject json, int appWidgetId) {
            T value = parse.apply(json.get(key.getName()));
            return () -> SettingsManager.writesync(keyFor(key, appWidgetId), value);
        }
    }

    private static <T> Preferences.Key<T> keyFor(Preferences.Key<T> key, int appWidgetId) {
        return appWidgetId == NO_WIDGET ? key : SettingsManager.widgetKey(key, appWidgetId);
    }

    private static Setting<Integer> intRange(Preferences.Key<Integer> key, int min, int max) {
        return new Setting<>(key, element -> {
            int value = element.getAsInt();
            if (value < min || value > max) {
                throw new IllegalArgumentException(key.getName() + " is out of range");
            }
            return value;
        });
    }

    private static Setting<Float> floatRange(Preferences.Key<Float> key, float min, float max) {
        return new Setting<>(key, element -> {
            float value = element.getAsFloat();
            if (value < min || value > max) {
                throw new IllegalArgumentException(key.getName() + " is out of range");
            }
            return value;
        });
    }

    public static void exportTo(Context context, Uri uri) throws IOException {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("version", VERSION);
        manifest.add("settings", readSettings(NO_WIDGET));
        Set<String> images = new HashSet<>();
        List<String> sharedImages = SettingsManager.deserializeString(SettingsManager.readsync(SettingsManager.UPLOADEDIMAGES));
        manifest.add("sharedImages", GSON.toJsonTree(sharedImages));
        images.addAll(sharedImages);

        JsonArray widgets = new JsonArray();
        for (WidgetItem.widgetTypes type : PROVIDERS.keySet()) {
            for (int appWidgetId : widgetIds(context, type)) {
                JsonObject widget = new JsonObject();
                widget.addProperty("type", type.name());
                widget.add("settings", readSettings(appWidgetId));
                if (type == WidgetItem.widgetTypes.IMAGES) {
                    List<String> widgetImages = SettingsManager.deserializeString(SettingsManager.readsync(
                            SettingsManager.widgetKey(SettingsManager.UPLOADEDIMAGES, appWidgetId)));
                    widget.add("images", GSON.toJsonTree(widgetImages));
                    images.addAll(widgetImages);
                }
                widgets.add(widget);
            }
        }
        manifest.add("widgets", widgets);

        try (OutputStream output = context.getContentResolver().openOutputStream(uri, "wt");
             ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(MANIFEST));
            zip.write(GSON.toJson(manifest).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            for (String image : images) {
                File file = new File(context.getFilesDir(), image);
                if (image.matches(IMAGE_NAME) && file.isFile()) {
                    zip.putNextEntry(new ZipEntry(IMAGE_FOLDER + image));
                    try (InputStream input = new FileInputStream(file)) {
                        copy(input, zip, Long.MAX_VALUE);
                    }
                    zip.closeEntry();
                }
            }
        }
    }

    private static JsonObject readSettings(int appWidgetId) {
        JsonObject settings = new JsonObject();
        for (Setting<?> setting : SETTINGS) {
            setting.addTo(settings, appWidgetId);
        }
        return settings;
    }

    // nothing of the current data changes until the whole file has been read and checked, so a broken file does no harm
    public static void importFrom(Context context, Uri uri) throws IOException {
        File folder = new File(context.getCacheDir(), "import_" + UUID.randomUUID());
        if (!folder.mkdirs()) {
            throw new IOException("Could not create the import folder");
        }
        try {
            JsonObject manifest = unzip(context, uri, folder);
            if (manifest == null || manifest.get("version").getAsInt() != VERSION) {
                throw new IOException("This is not a backup of this app");
            }
            apply(context, manifest, folder);
        } catch (RuntimeException e) {
            throw new IOException("The backup could not be read", e);
        } finally {
            deleteFolder(folder);
        }
    }

    // only the expected files are unpacked, and never with a path taken from the zip
    private static JsonObject unzip(Context context, Uri uri, File folder) throws IOException {
        JsonObject manifest = null;
        long remaining = MAX_IMPORT_BYTES;
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.equals(MANIFEST)) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    remaining -= copy(zip, bytes, remaining);
                    manifest = JsonParser.parseString(bytes.toString("UTF-8")).getAsJsonObject();
                } else if (name.startsWith(IMAGE_FOLDER) && name.substring(IMAGE_FOLDER.length()).matches(IMAGE_NAME)) {
                    File file = new File(folder, name.substring(IMAGE_FOLDER.length()));
                    try (OutputStream output = new FileOutputStream(file)) {
                        remaining -= copy(zip, output, remaining);
                    }
                }
            }
        }
        return manifest;
    }

    private static void apply(Context context, JsonObject manifest, File folder) throws IOException {
        // first everything that can still fail, the current data is untouched during this part
        List<Runnable> writes = new ArrayList<>(parseSettings(manifest.getAsJsonObject("settings"), NO_WIDGET));
        Map<Integer, List<String>> widgetImages = new HashMap<>();
        List<String> sharedImages = names(manifest.getAsJsonArray("sharedImages"));

        Map<WidgetItem.widgetTypes, Integer> positions = new HashMap<>();
        Map<WidgetItem.widgetTypes, int[]> targets = new HashMap<>();
        for (WidgetItem.widgetTypes type : PROVIDERS.keySet()) {
            targets.put(type, widgetIds(context, type));
            positions.put(type, 0);
        }

        for (JsonElement element : manifest.getAsJsonArray("widgets")) {
            JsonObject widget = element.getAsJsonObject();
            WidgetItem.widgetTypes type = WidgetItem.widgetTypes.valueOf(widget.get("type").getAsString());
            int position = positions.get(type);
            positions.put(type, position + 1);
            int[] ids = targets.get(type);
            JsonObject settings = widget.getAsJsonObject("settings");
            List<String> images = widget.has("images") ? names(widget.getAsJsonArray("images")) : new ArrayList<>();

            if (position < ids.length) {
                writes.addAll(parseSettings(settings, ids[position]));
                if (type == WidgetItem.widgetTypes.IMAGES) {
                    widgetImages.put(ids[position], images);
                }
            } else {
                // no widget for this one yet: the first becomes the starting point for new widgets,
                // and the images wait for the next image widget
                if (position == 0) {
                    writes.addAll(parseSettings(settings, NO_WIDGET));
                }
                sharedImages.addAll(images);
            }
        }

        Map<String, String> renamed = moveImages(context, folder);
        for (int appWidgetId : targets.get(WidgetItem.widgetTypes.IMAGES)) {
            widgetImages.putIfAbsent(appWidgetId, new ArrayList<>());
        }

        // from here on the file is known to be fine, so the old data can be replaced
        SettingsManager.removeAllWidgetSettings();
        for (Setting<?> setting : SETTINGS) {
            SettingsManager.resetData(setting.key);
        }
        for (Runnable write : writes) {
            write.run();
        }
        for (Map.Entry<Integer, List<String>> entry : widgetImages.entrySet()) {
            SettingsManager.writesync(SettingsManager.widgetKey(SettingsManager.UPLOADEDIMAGES, entry.getKey()),
                    GSON.toJson(renamedList(entry.getValue(), renamed)));
        }
        SettingsManager.writesync(SettingsManager.UPLOADEDIMAGES, GSON.toJson(renamedList(sharedImages, renamed)));

        for (String fileName : context.fileList()) {
            if (fileName.startsWith(WidgetHelperImages.FILE_PREFIX) && !renamed.containsValue(fileName)) {
                context.deleteFile(fileName);
            }
        }
        updateWidgets(context);
    }

    private static List<Runnable> parseSettings(JsonObject settings, int appWidgetId) {
        List<Runnable> writes = new ArrayList<>();
        for (Setting<?> setting : SETTINGS) {
            if (settings.has(setting.key.getName())) {
                writes.add(setting.writeFrom(settings, appWidgetId));
            }
        }
        return writes;
    }

    private static List<String> names(JsonArray array) {
        List<String> names = new ArrayList<>();
        for (JsonElement element : array) {
            names.add(element.getAsString());
        }
        return names;
    }

    // new names, because a backup of this same phone has the same names as the files that are still in use
    private static Map<String, String> moveImages(Context context, File folder) throws IOException {
        Map<String, String> renamed = new HashMap<>();
        File[] files = folder.listFiles();
        if (files == null) {
            return renamed;
        }
        for (File file : files) {
            String newName = WidgetHelperImages.FILE_PREFIX + UUID.randomUUID();
            try (InputStream input = new FileInputStream(file);
                 OutputStream output = context.openFileOutput(newName, Context.MODE_PRIVATE)) {
                copy(input, output, Long.MAX_VALUE);
            }
            renamed.put(file.getName(), newName);
        }
        return renamed;
    }

    // images that are listed but missing from the zip are left out
    private static List<String> renamedList(List<String> names, Map<String, String> renamed) {
        List<String> result = new ArrayList<>();
        for (String name : names) {
            if (renamed.containsKey(name)) {
                result.add(renamed.get(name));
            }
        }
        return result;
    }

    private static void updateWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        WidgetProviderQuotes.update(context, manager, widgetIds(context, WidgetItem.widgetTypes.QUOTES));
        WidgetProviderImages.update(context, manager, widgetIds(context, WidgetItem.widgetTypes.IMAGES));
        WidgetProviderGen.updateAllWithCurrentSettings(context);
        WidgetProviderMaze.update(context);
    }

    private static int[] widgetIds(Context context, WidgetItem.widgetTypes type) {
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, PROVIDERS.get(type)));
        Arrays.sort(ids);
        return ids;
    }

    private static long copy(InputStream input, OutputStream output, long limit) throws IOException {
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > limit) {
                throw new IOException("The backup is too large");
            }
            output.write(buffer, 0, read);
        }
        return total;
    }

    private static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        folder.delete();
    }
}
