package com.example.widgetapp.widgets.image;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.widgetapp.MainActivity;
import com.example.widgetapp.R;
import com.example.widgetapp.SettingsManager;
import com.example.widgetapp.widgets.quote.QuotesWidgetHelper;

public class WidgetProviderImages extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
//
//        SettingsManager.read(SettingsManager.QUOTEANDFACT, 0.5f, value -> {
//
//        }

        for (int appWidgetId: appWidgetIds) {
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_images);
            views.setOnClickPendingIntent(R.id.widget_images, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    public static void update(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_images);
            views.setOnClickPendingIntent(R.id.widget_images, pendingIntent);

            manager.updateAppWidget(appWidgetId, views);
        }
    }
}