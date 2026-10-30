package com.tdeboer.widgetapp.widgets.gen;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import com.tdeboer.widgetapp.SettingsManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GeneratedImageStore {
    private static final int IMAGE_SIZE = NoiseRenderer.IMAGE_SIZE;
    public static final int FRAME_COUNT = 180;
    // per speed setting (1-5): how many different noise maps one loop morphs through,
    // and how many times the warp flows around in that loop (whole numbers, so the loop stays seamless)
    private static final int[] MORPH_COUNT = {2, 3, 4, 5, 6};
    private static final int[] WARP_TURNS = {1, 1, 2, 2, 3};
    private static final Gson GSON = new Gson();
    private static final ExecutorService GENERATION_EXECUTOR = Executors.newSingleThreadExecutor();

    private GeneratedImageStore() {
    }

    public static void generate(Context context, float scale, int octaves, int palette, int algorithm, int speed, long seed,
                                List<String> oldFiles,
                                SettingsManager.Callback<List<String>> callback) {
        GENERATION_EXECUTOR.execute(() -> generateInternal(
                context, scale, octaves, palette, algorithm, speed, seed, oldFiles, callback
        ));
    }

    public static void generateSingleBackground(Context context, int palette, int algorithm, long seed,
                                                SettingsManager.Callback<Bitmap> callback) {
        GENERATION_EXECUTOR.execute(() -> {
            Random random = new Random(seed);
            int[] pixels = new int[IMAGE_SIZE * IMAGE_SIZE];
            new NoiseRenderer(palette).render(5f, 4, algorithm, seed,
                    random.nextFloat() * 10000f, random.nextFloat() * 10000f, 0f, 0f, 1, pixels);
            callback.onResult(Bitmap.createBitmap(pixels, IMAGE_SIZE, IMAGE_SIZE, Bitmap.Config.ARGB_8888));
        });
    }

    private static void generateInternal(Context context, float scale, int octaves, int palette, int algorithm,
                                         int speed, long seed, List<String> oldFiles,
                                         SettingsManager.Callback<List<String>> callback) {
        File directory = new File(context.getFilesDir(), "generated_widget");
        if (!directory.exists() && !directory.mkdirs()) {
            callback.onResult(new ArrayList<>());
            return;
        }

        String generationId = UUID.randomUUID().toString();
        String generatedPrefix = "generated_" + generationId;
        String stagingPrefix = "staging_" + generationId;
        List<String> generated = new ArrayList<>();
        List<String> staged = new ArrayList<>();
        long startTime = SystemClock.elapsedRealtime();
        long startCpuTime = SystemClock.currentThreadTimeMillis();
        NoiseRenderer renderer = new NoiseRenderer(palette);
        int[] pixels = new int[IMAGE_SIZE * IMAGE_SIZE];
        Bitmap bitmap = Bitmap.createBitmap(IMAGE_SIZE, IMAGE_SIZE, Bitmap.Config.ARGB_8888);
        try {
            Random random = new Random(seed);
            float offsetX = random.nextFloat() * 10000;
            float offsetY = random.nextFloat() * 10000;
            int speedIndex = Math.max(1, Math.min(5, speed)) - 1;
            int morphCount = MORPH_COUNT[speedIndex];
            int warpTurns = WARP_TURNS[speedIndex];
            for (int frame = 0; frame < FRAME_COUNT; frame++) {
                String stagedName = stagingPrefix + "_" + frame + ".jpg";
                String generatedName = generatedPrefix + "_" + frame + ".jpg";
                File file = new File(directory, stagedName);
                float progress = frame / (float) FRAME_COUNT;
                float phase = progress * warpTurns * (float) (Math.PI * 2);
                renderer.render(scale, octaves, algorithm, seed, offsetX, offsetY,
                        phase, progress * morphCount, morphCount, pixels);
                bitmap.setPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE);
                try (FileOutputStream output = new FileOutputStream(file)) {
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)) {
                        throw new IOException("Could not compress generated image");
                    }
                }
                staged.add(stagedName);
                generated.add(generatedName);
            }
            for (int frame = 0; frame < FRAME_COUNT; frame++) {
                File stagedFile = new File(directory, staged.get(frame));
                File generatedFile = new File(directory, generated.get(frame));
                if (!stagedFile.renameTo(generatedFile)) {
                    throw new IOException("Could not commit generated image");
                }
            }
        } catch (IOException e) {
            deleteFiles(context, staged);
            deleteFiles(context, generated);
            callback.onResult(new ArrayList<>());
            return;
        } finally {
            bitmap.recycle();
        }
        Log.i("GeneratedImageStore", "Generated " + FRAME_COUNT + " frames in "
                + (SystemClock.elapsedRealtime() - startTime) + " ms (cpu "
                + (SystemClock.currentThreadTimeMillis() - startCpuTime) + " ms)");

        SettingsManager.write(SettingsManager.GENERATED_IMAGES, GSON.toJson(generated), ignored -> {
            deleteFilesExcept(context, generated);
            callback.onResult(generated);
        });
    }

    public static void deleteAll(Context context) {
        File directory = new File(context.getFilesDir(), "generated_widget");
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    file.delete();
                }
            }
        }
        SettingsManager.resetData(SettingsManager.GENERATED_IMAGES);
    }

    public static void reset(Context context, Runnable callback) {
        GENERATION_EXECUTOR.execute(() -> {
            File directory = new File(context.getFilesDir(), "generated_widget");
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && !file.delete()) {
                        // A later cleanup pass will remove files still being released by the launcher.
                    }
                }
            }
            SettingsManager.write(SettingsManager.GENERATED_IMAGES, "[]", ignored -> callback.run());
        });
    }

    // the frames of the newest complete animation, in playback order
    public static List<String> findCurrentFrames(Context context) {
        File directory = new File(context.getFilesDir(), "generated_widget");
        File[] imageFiles = directory.listFiles((dir, name) ->
                name.startsWith("generated_") && name.endsWith(".jpg"));
        if (imageFiles == null) {
            return new ArrayList<>();
        }

        Map<String, List<File>> filesBySet = new HashMap<>();
        for (File imageFile : imageFiles) {
            int frameSeparator = imageFile.getName().lastIndexOf('_');
            if (frameSeparator < 0) {
                continue;
            }
            String setName = imageFile.getName().substring(0, frameSeparator);
            filesBySet.computeIfAbsent(setName, ignored -> new ArrayList<>()).add(imageFile);
        }

        String newestSet = null;
        long newestTime = Long.MIN_VALUE;
        for (Map.Entry<String, List<File>> entry : filesBySet.entrySet()) {
            if (entry.getValue().size() != FRAME_COUNT) {
                continue;
            }
            long setTime = 0;
            for (File file : entry.getValue()) {
                setTime = Math.max(setTime, file.lastModified());
            }
            if (setTime > newestTime) {
                newestSet = entry.getKey();
                newestTime = setTime;
            }
        }

        List<String> result = new ArrayList<>();
        if (newestSet == null) {
            return result;
        }
        for (File imageFile : imageFiles) {
            if (imageFile.getName().startsWith(newestSet + "_")) {
                result.add(imageFile.getName());
            }
        }
        result.sort(Comparator.comparingInt(GeneratedImageStore::frameNumber));
        return result;
    }

    private static int frameNumber(String fileName) {
        int separator = fileName.lastIndexOf('_');
        int extension = fileName.lastIndexOf('.');
        if (separator < 0 || extension <= separator) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(fileName.substring(separator + 1, extension));
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private static void deleteFiles(Context context, List<String> files) {
        for (String file : files) {
            new File(context.getFilesDir(), "generated_widget/" + file).delete();
        }
    }

    private static void deleteFilesExcept(Context context, List<String> keepFiles) {
        File directory = new File(context.getFilesDir(), "generated_widget");
        File[] files = directory.listFiles((dir, name) ->
                (name.startsWith("generated_") || name.startsWith("staging_"))
                        && name.endsWith(".jpg"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!keepFiles.contains(file.getName())) {
                file.delete();
            }
        }
    }
}
