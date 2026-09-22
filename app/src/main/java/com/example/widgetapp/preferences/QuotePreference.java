package com.example.widgetapp.preferences;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.example.widgetapp.R;
import com.example.widgetapp.SettingsManager;
import com.example.widgetapp.widgets.quote.WidgetProviderQuotes;

public class QuotePreference extends PreferenceFragmentCompat {

    SeekBarPreference seekBarPreference;
    AppWidgetManager widgetManager;


    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_quotes, rootKey);

        seekBarPreference = findPreference("quote_and_fact");

        widgetManager = AppWidgetManager.getInstance(getContext());

        SettingsManager.read(SettingsManager.QUOTEANDFACT, 0.5f, value -> {
            assert seekBarPreference != null;
            seekBarPreference.setValue((int) (value * 100));
        });

        seekBarPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            int valueInt = (int) newValue;
            SettingsManager.write(SettingsManager.QUOTEANDFACT, (float) valueInt / 100, f -> {
                int[] appWidgetIds = widgetManager.getAppWidgetIds(new ComponentName(requireContext(), WidgetProviderQuotes.class));

                WidgetProviderQuotes.update(getContext(), widgetManager, appWidgetIds);
            });
            return true;
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

//        SettingsManager.write(SettingsManager.QUOTEANDFACT, (float) seekBarPreference.getValue() / 100);
//
//        widgetManager = AppWidgetManager.getInstance(getContext());
//
//        int[] appWidgetIds = widgetManager.getAppWidgetIds(new ComponentName(requireContext(), WidgetProviderQuotes.class));
//
//
//        WidgetProviderQuotes.update(getContext(), widgetManager, appWidgetIds);
    }
}
