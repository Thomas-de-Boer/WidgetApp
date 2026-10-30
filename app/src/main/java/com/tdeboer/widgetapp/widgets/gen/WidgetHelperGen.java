package com.tdeboer.widgetapp.widgets.gen;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.tdeboer.widgetapp.MainActivity;
import com.tdeboer.widgetapp.R;

import java.util.List;

public class WidgetHelperGen {

    public static RemoteViews makeView(Context context, int appWidgetId) {
        Intent intent = new Intent(context, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_gen);
        views.setOnClickPendingIntent(R.id.widget_gen, pendingIntent);

        // launchers may apply an update on top of the views they already show, so clear the old frames
        // and set both visibilities every time; otherwise the new frames are appended to the old ones
        // and the previous animation keeps playing in between
        views.removeAllViews(R.id.widget_gen_flipper);
        List<String> frames = GeneratedImageStore.findCurrentFrames(context, appWidgetId);
        views.setViewVisibility(R.id.widget_gen_flipper, frames.isEmpty() ? View.GONE : View.VISIBLE);
        views.setViewVisibility(R.id.widget_gen_empty, frames.isEmpty() ? View.VISIBLE : View.GONE);

        // all frames go to the launcher at once, so it can flip through them without asking the app for each frame
        for (String frame : frames) {
            RemoteViews frameView = new RemoteViews(context.getPackageName(), R.layout.generated_flipper_item);
            frameView.setImageViewUri(R.id.generated_flipper_item_image,
                    GeneratedImageProvider.uriFor(context, frame));
            views.addView(R.id.widget_gen_flipper, frameView);
        }

        return views;
    }
}
