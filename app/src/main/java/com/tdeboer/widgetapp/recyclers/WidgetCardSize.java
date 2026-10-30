package com.tdeboer.widgetapp.recyclers;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;

// widgets have different shapes, so a card takes its width from the shape the widget has on the homescreen
public final class WidgetCardSize {
    private static final float DEFAULT_RATIO = 1.75f;
    private static final float MIN_RATIO = 0.4f;
    private static final float MAX_RATIO = 2f;

    private WidgetCardSize() { }

    public static int width(Context context, AppWidgetManager appWidgetManager, int appWidgetId, int height) {
        return Math.round(height * ratio(context, appWidgetManager, appWidgetId));
    }

    private static float ratio(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(appWidgetId);
        if (info == null) {
            return DEFAULT_RATIO;
        }

        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

//        a fixed size widget keeps its designed shape, launcher cells are not exactly square
        if (info.resizeMode == AppWidgetProviderInfo.RESIZE_NONE || width == 0 || height == 0) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            width = Math.round(info.minWidth / metrics.density);
            height = Math.round(info.minHeight / metrics.density);
        }
        return Math.max(MIN_RATIO, Math.min(MAX_RATIO, width / (float) height));
    }
}
