package com.example.widgetapp.widgets.quote;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.widgetapp.MainActivity;
import com.example.widgetapp.R;
import com.example.widgetapp.SettingsManager;

import java.util.Arrays;
import java.util.Random;

import javax.security.auth.callback.Callback;

public class QuotesWidgetHelper {
    static float weight;

    public static void makeView(Context context, WidgetProviderQuotes.Callback<RemoteViews> callback) {

        SettingsManager.read(SettingsManager.QUOTEANDFACT, 0.5f, value -> {
            weight = value;

            Intent intent = new Intent(context, MainActivity.class);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quotes);
            views.setOnClickPendingIntent(R.id.quote_widget, pendingIntent);
            views.setOnClickPendingIntent(R.id.quote, pendingIntent);

//        get all quotes from quotes.xml and select a random one
            String[] quotes = context.getResources().getStringArray(R.array.quotes);
            String randomQuote = quotes[new Random().nextInt(quotes.length)];

//        get all facts from quotes.xml and select a random one
            String[] facts = context.getResources().getStringArray(R.array.fun_facts);
            String randomFact = facts[new Random().nextInt(facts.length)];


//        get all images and select a random one
            int[] images = {R.drawable.quote_background1, R.drawable.quote_background2, R.drawable.quote_background3, R.drawable.quote_background4, R.drawable.quote_background5, R.drawable.quote_background6, R.drawable.quote_background7, R.drawable.quote_background8};
            int randomImage = images[new Random().nextInt(images.length)];

//        choose and set the chosen text in the text element
            String chosenText = chooseQuoteOrFact(randomQuote, randomFact, weight);
            views.setTextViewText(R.id.quote, chosenText);

//        set random background image if it's a quote and set custom background if fact
            if (Arrays.asList(quotes).contains(chosenText)) { views.setInt(R.id.quote_widget, "setBackgroundResource", randomImage); }
            else { views.setInt(R.id.quote_widget, "setBackgroundResource", R.color.background_dark); }

            callback.onResult(views);
        });

//        Intent intent = new Intent(context, MainActivity.class);
//
//        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE);
//
//        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quotes);
//        views.setOnClickPendingIntent(R.id.quote_widget, pendingIntent);
//        views.setOnClickPendingIntent(R.id.quote, pendingIntent);
//
////        get all quotes from quotes.xml and select a random one
//        String[] quotes = context.getResources().getStringArray(R.array.quotes);
//        String randomQuote = quotes[new Random().nextInt(quotes.length)];
//
////        get all facts from quotes.xml and select a random one
//        String[] facts = context.getResources().getStringArray(R.array.fun_facts);
//        String randomFact = facts[new Random().nextInt(facts.length)];
//
//
////        get all images and select a random one
//        int[] images = {R.drawable.quote_background1, R.drawable.quote_background2, R.drawable.quote_background3, R.drawable.quote_background4, R.drawable.quote_background5, R.drawable.quote_background6, R.drawable.quote_background7, R.drawable.quote_background8};
//        int randomImage = images[new Random().nextInt(images.length)];
//
////        choose and set the chosen text in the text element
//        String chosenText = chooseQuoteOrFact(randomQuote, randomFact, weight);
//        views.setTextViewText(R.id.quote, chosenText);
//
////        set random background image if it's a quote and set custom background if fact
//        if (Arrays.asList(quotes).contains(chosenText)) { views.setInt(R.id.quote_widget, "setBackgroundResource", randomImage); }
//        else { views.setInt(R.id.quote_widget, "setBackgroundResource", R.color.background_dark); }

//        return views;
    }

    public static String chooseQuoteOrFact(String randomQuote, String randomFact, float weight) {
//        choose a fact or a quote based on the weight. if weight is 0 it's a quote, if weight is 1 it's a fact
//        if weight is in between it wil be chosen randomly with the weight
//        for example: weight is 0.8 there is an 80% chance for a fact and a 20% chance for a quote
        String chosenText;

        double randomNum = Math.random();

        if (weight == 0) { chosenText = randomQuote; }
        else if (weight == 1) { chosenText = randomFact; }
        else if (randomNum <= weight) { chosenText = randomFact; }
        else if (randomNum > weight) { chosenText = randomQuote; }
        else { chosenText = randomFact; }

        return chosenText;
    }
}
