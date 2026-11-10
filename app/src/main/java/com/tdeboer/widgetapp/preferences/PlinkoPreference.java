package com.tdeboer.widgetapp.preferences;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.tdeboer.widgetapp.R;
import com.tdeboer.widgetapp.SettingsManager;
import com.tdeboer.widgetapp.fragments.WidgetSettingsFragment;
import com.tdeboer.widgetapp.widgets.plinko.WidgetProviderPlinko;

public class PlinkoPreference extends PreferenceFragmentCompat {
    private int appWidgetId;
    private ListPreference theme;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_plinko, rootKey);

        appWidgetId = requireArguments().getInt(WidgetSettingsFragment.ARGUMENT_WIDGET_ID);
        theme = findPreference("plinko_theme");
        Preference resetProgress = findPreference("plinko_reset_progress");
        // the writes finish on another thread, the screen can already be closed by then
        Context context = requireContext().getApplicationContext();

        if (theme != null) {
            theme.setOnPreferenceChangeListener((preference, value) -> {
                SettingsManager.writeWidget(SettingsManager.PLINKO_THEME, appWidgetId, Integer.parseInt((String) value),
                        ignored -> WidgetProviderPlinko.update(context, appWidgetId));
                return true;
            });
        }
        if (resetProgress != null) {
            resetProgress.setOnPreferenceClickListener(preference -> {
                WidgetProviderPlinko.resetProgress(context, appWidgetId);
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SettingsManager.readWidget(SettingsManager.PLINKO_THEME, appWidgetId, 0, value -> runOnUi(() -> {
            if (theme != null) {
                theme.setValue(String.valueOf(value));
            }
        }));
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
