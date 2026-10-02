package com.example.widgetapp.widgets.image;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.widgetapp.MainActivity;
import com.example.widgetapp.R;

public class WidgetProviderImages extends AppWidgetProvider {

    public interface Callback<RemoteViews> {
        void onResult(RemoteViews value);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int appWidgetId: appWidgetIds) {

            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_images_flipper);

            WidgetHelperImages.makeView(context, view -> {
                appWidgetManager.updateAppWidget(appWidgetId, view);
            });
        }
    }

    public static void update(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {

            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_images_flipper);

            WidgetHelperImages.makeView(context, view -> {
                appWidgetManager.updateAppWidget(appWidgetId, view);
            });
        }
    }
}