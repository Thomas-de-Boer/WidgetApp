package com.example.widgetapp.preferences;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.example.widgetapp.R;
import com.example.widgetapp.SettingsManager;
import com.example.widgetapp.widgets.gen.WidgetProviderGen;
import com.example.widgetapp.widgets.gen.GeneratedImageStore;

public class GenPreference extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_gen, rootKey);

        SeekBarPreference scale = findPreference("generated_scale");
        SeekBarPreference octaves = findPreference("generated_octaves");
        SeekBarPreference speed = findPreference("generated_speed");
        SeekBarPreference duration = findPreference("generated_duration");
        ListPreference palette = findPreference("generated_palette");
        ListPreference algorithm = findPreference("generated_algorithm");
        Preference resetGenerated = findPreference("generated_reset");

        SettingsManager.read(SettingsManager.GENERATED_SCALE, 5f, value -> {
            if (scale != null) {
                scale.setValue(Math.round(value));
            }
        });
        SettingsManager.read(SettingsManager.GENERATED_OCTAVES, 4, value -> {
            if (octaves != null) {
                octaves.setValue(value);
            }
        });
        SettingsManager.read(SettingsManager.GENERATED_SPEED, 1, value -> {
            if (speed != null) {
                speed.setValue(value);
            }
        });
        SettingsManager.read(SettingsManager.GENERATED_DURATION, 10, value -> {
            if (duration != null) {
                duration.setValue(value);
            }
        });
        SettingsManager.read(SettingsManager.GENERATED_PALETTE, 0, value -> {
            if (palette != null) {
                palette.setValue(String.valueOf(value));
            }
        });
        SettingsManager.read(SettingsManager.GENERATED_ALGORITHM, 0, value -> {
            if (algorithm != null) {
                algorithm.setValue(String.valueOf(value));
            }
        });

        if (scale != null) {
            scale.setOnPreferenceChangeListener((preference, value) -> {
                SettingsManager.write(SettingsManager.GENERATED_SCALE, ((Integer) value).floatValue(), ignored -> refreshWidgets());
                return true;
            });
        }
        if (resetGenerated != null) {
            resetGenerated.setOnPreferenceClickListener(preference -> {
                GeneratedImageStore.reset(requireContext(), this::refreshWidgets);
                return true;
            });
        }
        if (octaves != null) {
            octaves.setOnPreferenceChangeListener((preference, value) -> {
                SettingsManager.write(SettingsManager.GENERATED_OCTAVES, (Integer) value, ignored -> refreshWidgets());
                return true;
            });
        }
        if (speed != null) {
            speed.setOnPreferenceChangeListener((preference, value) -> {
                SettingsManager.write(SettingsManager.GENERATED_SPEED, (Integer) value, ignored -> refreshWidgets());
                return true;
            });
        }
        if (duration != null) {
            duration.setOnPreferenceChangeListener((preference, value) -> {
                SettingsManager.write(SettingsManager.GENERATED_DURATION, (Integer) value,
                        ignored -> refreshWidgetPlayback());
                return true;
            });
        }
        if (palette != null) {
            palette.setOnPreferenceChangeListener((preference, value) -> {
                SettingsManager.write(SettingsManager.GENERATED_PALETTE, Integer.parseInt((String) value), ignored -> refreshWidgets());
                return true;
            });
        }
        if (algorithm != null) {
            algorithm.setOnPreferenceChangeListener((preference, value) -> {
                SettingsManager.write(SettingsManager.GENERATED_ALGORITHM,
                        Integer.parseInt((String) value), ignored -> refreshWidgets());
                return true;
            });
        }
    }

    private void refreshWidgets() {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(requireContext());
        int[] widgetIds = widgetManager.getAppWidgetIds(
                new ComponentName(requireContext(), WidgetProviderGen.class)
        );
        WidgetProviderGen.update(requireContext(), widgetManager, widgetIds);
    }

    private void refreshWidgetPlayback() {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(requireContext());
        int[] widgetIds = widgetManager.getAppWidgetIds(
                new ComponentName(requireContext(), WidgetProviderGen.class)
        );
        WidgetProviderGen.updatePlayback(requireContext(), widgetManager, widgetIds);
    }
}
