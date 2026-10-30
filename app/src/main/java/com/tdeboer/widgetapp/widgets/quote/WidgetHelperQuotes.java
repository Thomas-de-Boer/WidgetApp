package com.tdeboer.widgetapp.widgets.quote;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

import com.tdeboer.widgetapp.MainActivity;
import com.tdeboer.widgetapp.R;
import com.tdeboer.widgetapp.SettingsManager;
import com.tdeboer.widgetapp.widgets.gen.GeneratedImageStore;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class WidgetHelperQuotes {
    private static final Gson GSON = new Gson();
    private static final Random RANDOM = new Random();

    public static void makeView(Context context, WidgetProviderQuotes.Callback<RemoteViews> callback) {
        SettingsManager.read(SettingsManager.QUOTEANDFACT, 0.5f, weight -> {
            SettingsManager.read(SettingsManager.QUOTE_AVOID_REPEATS, false, avoidRepeats -> {
                SettingsManager.read(SettingsManager.QUOTE_HISTORY, "[]", quoteHistoryJson -> {
                    SettingsManager.read(SettingsManager.FACT_HISTORY, "[]", factHistoryJson -> {
                        makeViewWithContent(context, callback, weight, avoidRepeats,
                                SettingsManager.deserializeString(quoteHistoryJson),
                                SettingsManager.deserializeString(factHistoryJson));
                    });
                });
            });
        });
    }

    private static void makeViewWithContent(Context context, WidgetProviderQuotes.Callback<RemoteViews> callback,
                                             float weight, boolean avoidRepeats, List<String> quoteHistory,
                                             List<String> factHistory) {
        String[] quotes = context.getResources().getStringArray(R.array.quotes);
        String[] facts = context.getResources().getStringArray(R.array.fun_facts);
        boolean fact = weight >= 1f || (weight > 0f && RANDOM.nextFloat() <= weight);
        String[] source = fact ? facts : quotes;
        List<String> history = fact ? factHistory : quoteHistory;
        String chosenText = chooseItem(source, history, avoidRepeats);

        if (avoidRepeats) {
            history.add(chosenText);
            SettingsManager.write(
                    fact ? SettingsManager.FACT_HISTORY : SettingsManager.QUOTE_HISTORY,
                    GSON.toJson(history),
                    ignored -> applyView(context, callback, chosenText, fact)
            );
        } else {
            applyView(context, callback, chosenText, fact);
        }
    }

    private static String chooseItem(String[] source, List<String> history, boolean avoidRepeats) {
        List<String> candidates = new ArrayList<>(Arrays.asList(source));
        if (avoidRepeats) {
            candidates.removeAll(history);
            if (candidates.isEmpty()) {
                history.clear();
                candidates.addAll(Arrays.asList(source));
            }
        }
        return candidates.get(RANDOM.nextInt(candidates.size()));
    }

    private static void applyView(Context context, WidgetProviderQuotes.Callback<RemoteViews> callback,
                                  String chosenText, boolean fact) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_MUTABLE
        );
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quotes);
        views.setOnClickPendingIntent(R.id.quote_widget, pendingIntent);
        views.setOnClickPendingIntent(R.id.quote, pendingIntent);
        views.setTextViewText(R.id.quote, chosenText);

        if (fact) {
            int algorithm = RANDOM.nextInt(10);
            int palette = RANDOM.nextInt(9);
            GeneratedImageStore.generateSingleBackground(
                    context, palette, algorithm, RANDOM.nextLong(), bitmap -> {
                        if (bitmap != null) {
                            views.setImageViewBitmap(R.id.quote_noise_background, bitmap);
                            views.setViewVisibility(R.id.quote_noise_background, android.view.View.VISIBLE);
                        }
                        callback.onResult(views);
                    }
            );
            return;
        }

        int[] images = {
                R.drawable.quote_background1, R.drawable.quote_background2,
                R.drawable.quote_background3, R.drawable.quote_background4,
                R.drawable.quote_background5, R.drawable.quote_background6,
                R.drawable.quote_background7, R.drawable.quote_background8
        };
        views.setViewVisibility(R.id.quote_noise_background, android.view.View.GONE);
        views.setInt(R.id.quote_widget, "setBackgroundResource", images[RANDOM.nextInt(images.length)]);
        callback.onResult(views);
    }
}
