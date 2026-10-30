package com.tdeboer.widgetapp.widgets.image;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.RemoteViews;

import com.google.gson.Gson;
import com.tdeboer.widgetapp.MainActivity;
import com.tdeboer.widgetapp.R;
import com.tdeboer.widgetapp.SettingsManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WidgetHelperImages {

    public static final String FILE_PREFIX = "UploadedImages";
    private static final Gson GSON = new Gson();

    public static List<Bitmap> images;

//    only call this after the list of uploaded images is cleared, the settings screen expects every listed file to exist
    public static void deleteAll(Context context) {
        for (String fileName : context.fileList()) {
            if (fileName.startsWith(FILE_PREFIX)) {
                context.deleteFile(fileName);
            }
        }
    }

//    the images used to be one list for all widgets. every widget gets its own list now, and its own copies of the
//    files, so deleting an image from one widget can never take it away from another
    public static void splitSharedImages(Context context) {
        new Thread(() -> {
            String shared = SettingsManager.readsync(SettingsManager.UPLOADEDIMAGES);
            List<String> sharedImages = SettingsManager.deserializeString(shared);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = manager.getAppWidgetIds(new ComponentName(context, WidgetProviderImages.class));
            if (sharedImages.isEmpty() || appWidgetIds.length == 0) {
                return;
            }

            boolean filesUsed = false;
            for (int appWidgetId : appWidgetIds) {
                if (SettingsManager.readsync(SettingsManager.widgetKey(SettingsManager.UPLOADEDIMAGES, appWidgetId)) != null) {
                    continue;
                }
                try {
                    List<String> images = filesUsed ? copyImages(context, sharedImages) : sharedImages;
                    SettingsManager.writesync(SettingsManager.widgetKey(SettingsManager.UPLOADEDIMAGES, appWidgetId),
                            GSON.toJson(images));
                    filesUsed = true;
                } catch (IOException e) {
                    // the shared list stays, so the next start tries again
                    return;
                }
            }
            SettingsManager.writesync(SettingsManager.UPLOADEDIMAGES, "[]");
            WidgetProviderImages.update(context, manager, appWidgetIds);
        }).start();
    }

    private static List<String> copyImages(Context context, List<String> fileNames) throws IOException {
        List<String> copies = new ArrayList<>();
        for (String fileName : fileNames) {
            String copy = FILE_PREFIX + UUID.randomUUID();
            try (InputStream input = context.openFileInput(fileName);
                 OutputStream output = context.openFileOutput(copy, Context.MODE_PRIVATE)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            }
            copies.add(copy);
        }
        return copies;
    }

    public static void deleteForWidget(Context context, int appWidgetId) {
        SettingsManager.read(SettingsManager.widgetKey(SettingsManager.UPLOADEDIMAGES, appWidgetId), null, json -> {
            for (String fileName : SettingsManager.deserializeString(json)) {
                context.deleteFile(fileName);
            }
            SettingsManager.removeWidgetSettings(appWidgetId);
        });
    }

    public static void makeView(Context context, int appWidgetId, WidgetProviderImages.Callback<RemoteViews> callback) {
        SettingsManager.read(SettingsManager.UPLOADEDIMAGES, null, string -> {

//            List<String> uploadedFileNames = SettingsManager.deserializeString(string);

//            images = new ArrayList<>();
//
//            assert uploadedFileNames != null;
//
//            for (String uploadedFileName : uploadedFileNames) {
//
//
//                FileInputStream fileInputStream;
//                try {
//                    fileInputStream = context.openFileInput(uploadedFileName);
//                } catch (FileNotFoundException e) {
//                    throw new RuntimeException(e);
//                }
//
//                Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream);
//
//                images.add(bitmap);
//            }

            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_images);
            views.setOnClickPendingIntent(R.id.widget_images, pendingIntent);
            Intent factoryIntent = new Intent(context, ImageWidgetService.class);
            factoryIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
//            the data contains the extra, so android keeps a separate list for every widget
            factoryIntent.setData(Uri.parse(factoryIntent.toUri(Intent.URI_INTENT_SCHEME)));
            views.setRemoteAdapter(R.id.widget_images_flipper, factoryIntent);
            views.setEmptyView(R.id.widget_images_flipper, R.id.widget_images_empty);

            callback.onResult(views);
        });
    }
}
