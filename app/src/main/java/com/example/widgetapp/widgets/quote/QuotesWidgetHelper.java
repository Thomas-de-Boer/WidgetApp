package com.example.widgetapp.widgets.quote;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.widgetapp.MainActivity;
import com.example.widgetapp.R;

import java.util.Random;

public class QuotesWidgetHelper {
    public static RemoteViews makeView(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quotes);
        views.setOnClickPendingIntent(R.id.quote, pendingIntent);

        String[] quotes = context.getResources().getStringArray(R.array.quotes);

        String randomQuote = quotes[new Random().nextInt(quotes.length)];

        int[] images = {R.drawable.quote_background1, R.drawable.quote_background2, R.drawable.quote_background3, R.drawable.quote_background4, R.drawable.quote_background5, R.drawable.quote_background6};

        int randomImage = images[new Random().nextInt(images.length)];


        views.setTextViewText(R.id.quote, randomQuote);

        views.setInt(R.id.quote_widget, "setBackgroundResource", randomImage);

        return views;
    }
}
