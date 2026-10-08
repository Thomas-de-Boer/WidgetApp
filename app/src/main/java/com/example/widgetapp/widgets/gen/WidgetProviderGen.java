package com.example.widgetapp.widgets.gen;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import com.example.widgetapp.widgets.quote.WidgetHelperQuotes;

public class WidgetProviderGen extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int appWidgetId: appWidgetIds) {

            RemoteViews views = WidgetHelperGen.makeView(context);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
