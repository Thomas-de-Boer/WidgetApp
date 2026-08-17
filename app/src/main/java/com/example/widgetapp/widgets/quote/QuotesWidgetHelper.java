package com.example.widgetapp.widgets.quote;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.widgetapp.MainActivity;
import com.example.widgetapp.R;

import java.util.Random;

public class QuotesWidgetHelper {
    public static RemoteViews SelectQoute(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quotes);
        views.setOnClickPendingIntent(R.id.widget_text, pendingIntent);

        String[] quotes = context.getResources().getStringArray(R.array.quotes);

        String randomQuote = quotes[new Random().nextInt(quotes.length)];


        views.setTextViewText(R.id.widget_text, randomQuote);

        return views;
    }
}
