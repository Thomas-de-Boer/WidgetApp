package com.tdeboer.widgetapp.widgets.maze;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.tdeboer.widgetapp.MainActivity;
import com.tdeboer.widgetapp.R;
import com.tdeboer.widgetapp.SettingsManager;
import com.tdeboer.widgetapp.widgets.gen.GeneratedImageProvider;

import java.util.List;

// reads settings synchronously, so only call this from the MazeImageStore thread
final class WidgetHelperMaze {
    // the last frame is repeated, so the flipper doesn't loop back while the next update is on its way
    private static final int HOLD_FRAMES = 6;

    private WidgetHelperMaze() {
    }

    // one flip of the repeated frames as margin, for a launcher that runs slightly behind
    static long chunkMillis(int frames) {
        return (frames + 1) * (long) MazeRenderer.FLIP_INTERVAL_MILLIS;
    }

    static RemoteViews makeStillView(Context context, int appWidgetId) {
        RemoteViews views = makeBaseView(context, appWidgetId);
        List<String> frames = MazeImageStore.findFrames(context, appWidgetId);
        views.setViewVisibility(R.id.widget_maze_flipper, View.GONE);
        views.setViewVisibility(R.id.widget_maze_image, frames.isEmpty() ? View.GONE : View.VISIBLE);
        views.setViewVisibility(R.id.widget_maze_empty, frames.isEmpty() ? View.VISIBLE : View.GONE);
        if (!frames.isEmpty()) {
            String frame = frames.get(MazeImageStore.isSolved(appWidgetId) ? frames.size() - 1 : 0);
            views.setImageViewUri(R.id.widget_maze_image, GeneratedImageProvider.uriFor(context, frame));
        }
        return views;
    }

    static RemoteViews makeAnimatedView(Context context, int appWidgetId, List<String> frames) {
        RemoteViews views = makeBaseView(context, appWidgetId);
        views.setViewVisibility(R.id.widget_maze_flipper, View.VISIBLE);
        views.setViewVisibility(R.id.widget_maze_image, View.GONE);
        views.setViewVisibility(R.id.widget_maze_empty, View.GONE);
        for (String frame : frames) {
            views.addView(R.id.widget_maze_flipper, makeFrameView(context, frame));
        }
        for (int i = 0; i < HOLD_FRAMES; i++) {
            views.addView(R.id.widget_maze_flipper, makeFrameView(context, frames.get(frames.size() - 1)));
        }
        views.setDisplayedChild(R.id.widget_maze_flipper, 0);
        return views;
    }

    private static RemoteViews makeBaseView(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_maze);

        // launchers may apply an update on top of the views they already show, so clear the old frames
        views.removeAllViews(R.id.widget_maze_flipper);

        Integer savedTheme = SettingsManager.readsyncWidget(SettingsManager.MAZE_THEME, appWidgetId);
        int theme = savedTheme != null ? savedTheme : 0;
        int textColor = MazeRenderer.textColor(theme);
        views.setInt(R.id.widget_maze, "setBackgroundColor", MazeRenderer.backgroundColor(theme));
        views.setTextColor(R.id.widget_maze_new, textColor);
        views.setTextColor(R.id.widget_maze_solve, textColor);
        views.setTextColor(R.id.widget_maze_empty, textColor);

        Intent openApp = new Intent(context, MainActivity.class);
        views.setOnClickPendingIntent(R.id.widget_maze, PendingIntent.getActivity(context, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        views.setOnClickPendingIntent(R.id.widget_maze_new,
                buttonIntent(context, WidgetProviderMaze.ACTION_NEW_MAZE, appWidgetId));
        views.setOnClickPendingIntent(R.id.widget_maze_solve,
                buttonIntent(context, WidgetProviderMaze.ACTION_SOLVE, appWidgetId));
        return views;
    }

    private static RemoteViews makeFrameView(Context context, String frame) {
        RemoteViews frameView = new RemoteViews(context.getPackageName(), R.layout.maze_flipper_item);
        frameView.setImageViewUri(R.id.maze_flipper_item_image, GeneratedImageProvider.uriFor(context, frame));
        return frameView;
    }

    private static PendingIntent buttonIntent(Context context, String action, int appWidgetId) {
        // foreground, because android can hold back a normal broadcast to an idle app for seconds
        Intent intent = new Intent(context, WidgetProviderMaze.class)
                .setAction(action)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
