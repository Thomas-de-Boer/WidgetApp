package com.example.widgetapp.preferences;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.example.widgetapp.R;
import com.example.widgetapp.SettingsManager;
import com.example.widgetapp.widgets.quote.QuotesWidgetHelper;
import com.example.widgetapp.widgets.quote.WidgetProviderQuotes;

import java.util.Objects;

public class QuotePreference extends PreferenceFragmentCompat {

    SeekBarPreference seekBarPreference;
    AppWidgetManager widgetManager;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_quotes, rootKey);

        seekBarPreference = findPreference("quote_and_fact");

        SettingsManager.read(SettingsManager.QUOTEANDFACT, 0f, value -> {
            assert seekBarPreference != null;
            seekBarPreference.setValue((int) (value * 100));
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SettingsManager.write(SettingsManager.QUOTEANDFACT, (float) seekBarPreference.getValue() / 100);

        widgetManager = AppWidgetManager.getInstance(getContext());

        int[] appWidgetIds = widgetManager.getAppWidgetIds(new ComponentName(requireContext(), WidgetProviderQuotes.class));


        WidgetProviderQuotes.update(getContext(), widgetManager, appWidgetIds);
    }
}
