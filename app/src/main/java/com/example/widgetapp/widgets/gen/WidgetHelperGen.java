package com.example.widgetapp.widgets.gen;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.example.widgetapp.MainActivity;
import com.example.widgetapp.R;

public class WidgetHelperGen {

    public static RemoteViews makeView(Context context, int durationSeconds) {
        Intent intent = new Intent(context, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_gen);
        views.setOnClickPendingIntent(R.id.widget_gen, pendingIntent);
        Intent factoryIntent = new Intent(context, GeneratedImageWidgetService.class);
        factoryIntent.setData(Uri.parse(factoryIntent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widget_gen_flipper, factoryIntent);
        views.setEmptyView(R.id.widget_gen_flipper, R.id.widget_gen_empty);

        return views;
    }
}
