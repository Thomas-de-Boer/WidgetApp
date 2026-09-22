package com.example.widgetapp.widgets.quote;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

public class WidgetProviderQuotes extends AppWidgetProvider {

    public interface Callback<RemoteViews> {
        void onResult(RemoteViews value);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int appWidgetId: appWidgetIds) {

            WidgetHelperQuotes.makeView(context, views -> {
                appWidgetManager.updateAppWidget(appWidgetId, views);
            });

        }
    }

    public static void update(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            WidgetHelperQuotes.makeView(context, views -> {
                manager.updateAppWidget(appWidgetId, views);
            });
        }
    }
}