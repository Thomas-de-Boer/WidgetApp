package com.example.widgetapp.widgets.image;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.AdapterViewFlipper;
import android.widget.RemoteViews;

import com.example.widgetapp.MainActivity;
import com.example.widgetapp.R;
import com.example.widgetapp.SettingsManager;

import java.util.List;

public class WidgetHelperImages {

    public static List<Bitmap> images;

    public static void makeView(Context context, WidgetProviderImages.Callback<RemoteViews> callback) {
        SettingsManager.read(SettingsManager.UPLOADEDIMAGES, null, string -> {

            AdapterViewFlipper adapterViewFlipper;

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
            views.setRemoteAdapter(R.id.widget_images_flipper, factoryIntent);


//            views.setImageViewBitmap(R.id.flipper_item_image, bitmap);

            callback.onResult(views);
        });
    }
}
