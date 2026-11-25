package com.tdeboer.widgetapp.widgets.image;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

import com.tdeboer.widgetapp.R;

public class WidgetProviderImages extends AppWidgetProvider {

    public interface Callback<RemoteViews> {
        void onResult(RemoteViews value);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

//        a new widget takes the images that had no widget yet, for example after an import
        WidgetHelperImages.splitSharedImages(context);
        update(context, appWidgetManager, appWidgetIds);
    }

    public static void update(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {

//            first the view, so the widget has a list that can be told its images changed
            appWidgetManager.updateAppWidget(appWidgetId, WidgetHelperImages.makeView(context, appWidgetId));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_images_flipper);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            WidgetHelperImages.deleteForWidget(context, appWidgetId);
        }
        super.onDeleted(context, appWidgetIds);
    }
}
