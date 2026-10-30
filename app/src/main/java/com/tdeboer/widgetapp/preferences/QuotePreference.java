package com.tdeboer.widgetapp.preferences;

import android.appwidget.AppWidgetManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import com.tdeboer.widgetapp.R;
import com.tdeboer.widgetapp.SettingsManager;
import com.tdeboer.widgetapp.fragments.WidgetSettingsFragment;
import com.tdeboer.widgetapp.widgets.quote.WidgetProviderQuotes;

public class QuotePreference extends PreferenceFragmentCompat {

    int appWidgetId;
    SeekBarPreference seekBarPreference;
    SwitchPreferenceCompat avoidRepeats;
    AppWidgetManager widgetManager;


    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_quotes, rootKey);

        appWidgetId = requireArguments().getInt(WidgetSettingsFragment.ARGUMENT_WIDGET_ID);
        seekBarPreference = findPreference("quote_and_fact");
        avoidRepeats = findPreference("quote_avoid_repeats");

        widgetManager = AppWidgetManager.getInstance(getContext());

        seekBarPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            int valueInt = (int) newValue;
            SettingsManager.writeWidget(SettingsManager.QUOTEANDFACT, appWidgetId, (float) valueInt / 100, f ->
                    WidgetProviderQuotes.update(getContext(), widgetManager, new int[]{appWidgetId}));
            return true;
        });

        avoidRepeats.setOnPreferenceChangeListener((preference, newValue) -> {
            SettingsManager.writeWidget(SettingsManager.QUOTE_AVOID_REPEATS, appWidgetId, (Boolean) newValue, ignored -> { });
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        SettingsManager.readWidget(SettingsManager.QUOTEANDFACT, appWidgetId, 0.5f, value ->
                runOnUi(() -> seekBarPreference.setValue((int) (value * 100))));
        SettingsManager.readWidget(SettingsManager.QUOTE_AVOID_REPEATS, appWidgetId, false, value ->
                runOnUi(() -> avoidRepeats.setChecked(value)));
    }

    private void runOnUi(Runnable action) {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                if (isAdded()) {
                    action.run();
                }
            });
        }
    }
}
