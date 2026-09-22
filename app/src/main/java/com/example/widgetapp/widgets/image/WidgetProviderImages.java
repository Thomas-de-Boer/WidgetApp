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
//
//        SettingsManager.read(SettingsManager.QUOTEANDFACT, 0.5f, value -> {
//
//        }

        for (int appWidgetId: appWidgetIds) {

            WidgetHelperImages.makeView(context, view -> {
                appWidgetManager.updateAppWidget(appWidgetId, view);
            });
        }
    }

    public static void update(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            WidgetHelperImages.makeView(context, view -> {
                manager.updateAppWidget(appWidgetId, view);
            });
        }
    }
}