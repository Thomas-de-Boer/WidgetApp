package com.tdeboer.widgetapp.widgets.plinko;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.tdeboer.widgetapp.R;
import com.tdeboer.widgetapp.widgets.gen.GeneratedImageProvider;

import java.util.List;

// reads settings synchronously, so only call this from the PlinkoImageStore thread
final class WidgetHelperPlinko {
    // the last frame is repeated, so the flipper doesn't loop back while the next update is on its way
    private static final int HOLD_FRAMES = 4;

    private WidgetHelperPlinko() {
    }

    // still can be null when the image could not be made
    static RemoteViews makeStillView(Context context, int appWidgetId, String still) {
        RemoteViews views = makeBaseView(context, appWidgetId);
        views.setViewVisibility(R.id.widget_plinko_flipper, View.GONE);
        views.setViewVisibility(R.id.widget_plinko_image, still != null ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.widget_plinko_empty, still != null ? View.GONE : View.VISIBLE);
        if (still != null) {
            views.setImageViewUri(R.id.widget_plinko_image, GeneratedImageProvider.uriFor(context, still));
        }
        return views;
    }

    static RemoteViews makeAnimatedView(Context context, int appWidgetId, List<String> frames) {
        RemoteViews views = makeBaseView(context, appWidgetId);
        views.setViewVisibility(R.id.widget_plinko_flipper, View.VISIBLE);
        views.setViewVisibility(R.id.widget_plinko_image, View.GONE);
        views.setViewVisibility(R.id.widget_plinko_empty, View.GONE);
        for (String frame : frames) {
            views.addView(R.id.widget_plinko_flipper, makeFrameView(context, frame));
        }
        for (int i = 0; i < HOLD_FRAMES; i++) {
            views.addView(R.id.widget_plinko_flipper, makeFrameView(context, frames.get(frames.size() - 1)));
        }
        views.setDisplayedChild(R.id.widget_plinko_flipper, 0);
        return views;
    }

    private static RemoteViews makeBaseView(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_plinko);

        // launchers may apply an update on top of the views they already show, so clear the old frames
        views.removeAllViews(R.id.widget_plinko_flipper);

        int theme = PlinkoImageStore.readTheme(appWidgetId);
        views.setInt(R.id.widget_plinko, "setBackgroundColor", PlinkoRenderer.backgroundColor(theme));
        views.setTextColor(R.id.widget_plinko_empty, PlinkoRenderer.textColor(theme));

        // a tap anywhere on the widget drops a ball.
        // foreground, because android can hold back a normal broadcast to an idle app for seconds
        Intent intent = new Intent(context, WidgetProviderPlinko.class)
                .setAction(WidgetProviderPlinko.ACTION_DROP)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        views.setOnClickPendingIntent(R.id.widget_plinko, PendingIntent.getBroadcast(context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        return views;
    }

    private static RemoteViews makeFrameView(Context context, String frame) {
        RemoteViews frameView = new RemoteViews(context.getPackageName(), R.layout.plinko_flipper_item);
        frameView.setImageViewUri(R.id.plinko_flipper_item_image, GeneratedImageProvider.uriFor(context, frame));
        return frameView;
    }
}
