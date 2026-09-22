package com.example.widgetapp.widgets.image;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.widgetapp.MainActivity;
import com.example.widgetapp.R;
import com.example.widgetapp.SettingsManager;

import java.util.List;
import java.util.Set;

public class WidgetHelperImages {

    public static void makeView(Context context, WidgetProviderImages.Callback<RemoteViews> callback) {
        SettingsManager.read(SettingsManager.UPLOADEDIMAGES, null, string -> {
            List<String> uploadedFileNames = SettingsManager.deserializeString(string);


        });


        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_images);
        views.setOnClickPendingIntent(R.id.widget_images, pendingIntent);
    }


}
