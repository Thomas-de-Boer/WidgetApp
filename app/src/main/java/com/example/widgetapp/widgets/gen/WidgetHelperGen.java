package com.example.widgetapp.widgets.gen;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.widgetapp.MainActivity;
import com.example.widgetapp.R;

public class WidgetHelperGen {

    public static RemoteViews makeView(Context context) {
        Intent intent = new Intent(context, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_gen);
        views.setOnClickPendingIntent(R.id.widget_gen_image, pendingIntent);

        return views;
    }
}
