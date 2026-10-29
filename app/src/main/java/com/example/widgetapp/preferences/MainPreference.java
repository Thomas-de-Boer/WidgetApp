package com.example.widgetapp.preferences;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.widgetapp.MainActivity;
import com.example.widgetapp.R;
import com.example.widgetapp.SettingsManager;
import com.example.widgetapp.widgets.gen.GeneratedImageStore;

import java.util.Objects;

public class MainPreference extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.main_preference, rootKey);

        Preference lightDark = findPreference("light_dark");
        Preference reset = findPreference("reset_data");

        assert lightDark != null;
        lightDark.setOnPreferenceClickListener(preference -> {

            int nightModeFlags = requireContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            switch (nightModeFlags) {
                case Configuration.UI_MODE_NIGHT_YES:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

                    break;

                case Configuration.UI_MODE_NIGHT_NO:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    break;
            }
            return false;
        });

        assert reset != null;
        reset.setOnPreferenceClickListener(preference -> {

            SettingsManager.resetData(SettingsManager.QUOTEANDFACT);
            SettingsManager.resetData(SettingsManager.UPLOADEDIMAGES);
            SettingsManager.resetData(SettingsManager.GENERATED_SCALE);
            SettingsManager.resetData(SettingsManager.GENERATED_OCTAVES);
            SettingsManager.resetData(SettingsManager.GENERATED_PALETTE);
            SettingsManager.resetData(SettingsManager.GENERATED_ALGORITHM);
            SettingsManager.resetData(SettingsManager.GENERATED_SPEED);
            SettingsManager.resetData(SettingsManager.GENERATED_SEED);
            SettingsManager.resetData(SettingsManager.GENERATED_RANDOM);
            SettingsManager.resetData(SettingsManager.QUOTE_AVOID_REPEATS);
            SettingsManager.resetData(SettingsManager.QUOTE_HISTORY);
            SettingsManager.resetData(SettingsManager.FACT_HISTORY);
            GeneratedImageStore.deleteAll(requireContext());

            return false;
        });
    }
}
