package com.example.widgetapp.widgets.gen;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.widgetapp.R;

import java.io.File;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GeneratedImageWidgetFactory implements RemoteViewsService.RemoteViewsFactory {
    private static final int EXPECTED_FRAME_COUNT = 120;
    private static final int MAX_CACHED_BITMAPS = 12;
    private final Context context;
    private List<String> files = new ArrayList<>();
    private final Map<Integer, Bitmap> bitmapCache = new LinkedHashMap<>();
    private Bitmap lastDisplayedBitmap;

    public GeneratedImageWidgetFactory(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.generated_flipper_item);
        if (position < 0 || position >= files.size()) {
            return views;
        }
        File file = new File(context.getFilesDir(), "generated_widget/" + files.get(position));
        Bitmap bitmap = bitmapCache.get(position);
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                bitmapCache.put(position, bitmap);
                if (bitmapCache.size() > MAX_CACHED_BITMAPS) {
                    Iterator<Integer> iterator = bitmapCache.keySet().iterator();
                    iterator.next();
                    iterator.remove();
                }
            }
        }
        if (bitmap != null) {
            views.setImageViewBitmap(R.id.generated_flipper_item_image, bitmap);
            views.setTextViewText(R.id.generated_flipper_item_text, "");
            lastDisplayedBitmap = bitmap;
        }
        return views;
    }

    @Override
    public void onCreate() {
        files = findGeneratedFiles();
    }

    @Override
    public void onDataSetChanged() {
        files = findGeneratedFiles();
        bitmapCache.clear();
    }

    @Override
    public RemoteViews getLoadingView() {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.generated_flipper_item);
        if (lastDisplayedBitmap != null && !lastDisplayedBitmap.isRecycled()) {
            views.setImageViewBitmap(R.id.generated_flipper_item_image, lastDisplayedBitmap);
            views.setTextViewText(R.id.generated_flipper_item_text, "");
        }
        return views;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onDestroy() {
        files.clear();
        bitmapCache.clear();
        lastDisplayedBitmap = null;
    }

    private List<String> findGeneratedFiles() {
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
            if (entry.getValue().size() != EXPECTED_FRAME_COUNT) {
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
        result.sort(Comparator.comparingInt(GeneratedImageWidgetFactory::frameNumber));
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
}
