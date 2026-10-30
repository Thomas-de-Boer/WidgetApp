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

            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_images_flipper);

            WidgetHelperImages.makeView(context, appWidgetId, view -> {
                appWidgetManager.updateAppWidget(appWidgetId, view);
            });
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
