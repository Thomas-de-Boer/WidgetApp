package com.tdeboer.widgetapp.widgets.image;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.RemoteViews;

import com.tdeboer.widgetapp.MainActivity;
import com.tdeboer.widgetapp.R;
import com.tdeboer.widgetapp.SettingsManager;

import java.util.List;

public class WidgetHelperImages {

    public static final String FILE_PREFIX = "UploadedImages";

    public static List<Bitmap> images;

//    only call this after the list of uploaded images is cleared, the settings screen expects every listed file to exist
    public static void deleteAll(Context context) {
        for (String fileName : context.fileList()) {
            if (fileName.startsWith(FILE_PREFIX)) {
                context.deleteFile(fileName);
            }
        }
    }

    public static void makeView(Context context, WidgetProviderImages.Callback<RemoteViews> callback) {
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
            factoryIntent.setData(Uri.parse(factoryIntent.toUri(Intent.URI_INTENT_SCHEME)));
            views.setRemoteAdapter(R.id.widget_images_flipper, factoryIntent);
            views.setEmptyView(R.id.widget_images_flipper, R.id.widget_images_empty);

            callback.onResult(views);
        });
    }
}
