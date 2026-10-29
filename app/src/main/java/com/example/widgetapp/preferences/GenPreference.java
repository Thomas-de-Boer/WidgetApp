package com.example.widgetapp.preferences;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.example.widgetapp.R;
import com.example.widgetapp.SettingsManager;
import com.example.widgetapp.widgets.gen.WidgetProviderGen;
import com.example.widgetapp.widgets.gen.GeneratedImageStore;

public class GenPreference extends PreferenceFragmentCompat {
    private SeekBarPreference scale;
    private SeekBarPreference octaves;
    private SeekBarPreference speed;
    private ListPreference palette;
    private ListPreference algorithm;
    private SwitchPreferenceCompat random;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_gen, rootKey);

        scale = findPreference("generated_scale");
        octaves = findPreference("generated_octaves");
        speed = findPreference("generated_speed");
        palette = findPreference("generated_palette");
        algorithm = findPreference("generated_algorithm");
        random = findPreference("generated_random");
        Preference resetGenerated = findPreference("generated_reset");

        if (random != null) {
            random.setOnPreferenceChangeListener((preference, value) -> {
                boolean enabled = (Boolean) value;
                SettingsManager.write(SettingsManager.GENERATED_RANDOM, enabled, ignored -> {
                    if (enabled) {
                        refreshWidgets();
                    }
                });
                return true;
            });
        }
        if (scale != null) {
            scale.setOnPreferenceChangeListener((preference, value) -> {
                SettingsManager.write(SettingsManager.GENERATED_SCALE, ((Integer) value).floatValue(),
                        ignored -> refreshWidgetsWithCurrentSettings());
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
                SettingsManager.write(SettingsManager.GENERATED_OCTAVES, (Integer) value,
                        ignored -> refreshWidgetsWithCurrentSettings());
                return true;
            });
        }
        if (speed != null) {
            speed.setOnPreferenceChangeListener((preference, value) -> {
                SettingsManager.write(SettingsManager.GENERATED_SPEED, (Integer) value,
                        ignored -> refreshWidgetsWithCurrentSettings());
                return true;
            });
        }
        if (palette != null) {
            palette.setOnPreferenceChangeListener((preference, value) -> {
                SettingsManager.write(SettingsManager.GENERATED_PALETTE, Integer.parseInt((String) value),
                        ignored -> refreshWidgetsWithCurrentSettings());
                return true;
            });
        }
        if (algorithm != null) {
            algorithm.setOnPreferenceChangeListener((preference, value) -> {
                SettingsManager.write(SettingsManager.GENERATED_ALGORITHM,
                        Integer.parseInt((String) value), ignored -> refreshWidgetsWithCurrentSettings());
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // the widget can change the settings itself when random settings is on, so keep the screen in sync
        WidgetProviderGen.setSettingsRandomizedListener(this::loadValues);
        loadValues();
    }

    @Override
    public void onPause() {
        WidgetProviderGen.setSettingsRandomizedListener(null);
        super.onPause();
    }

    private void loadValues() {
        SettingsManager.read(SettingsManager.GENERATED_SCALE, 5f, value -> runOnUi(() -> {
            if (scale != null) {
                scale.setValue(Math.round(value));
            }
        }));
        SettingsManager.read(SettingsManager.GENERATED_OCTAVES, 4, value -> runOnUi(() -> {
            if (octaves != null) {
                octaves.setValue(value);
            }
        }));
        SettingsManager.read(SettingsManager.GENERATED_SPEED, 1, value -> runOnUi(() -> {
            if (speed != null) {
                speed.setValue(value);
            }
        }));
        SettingsManager.read(SettingsManager.GENERATED_PALETTE, 0, value -> runOnUi(() -> {
            if (palette != null) {
                palette.setValue(String.valueOf(value));
            }
        }));
        SettingsManager.read(SettingsManager.GENERATED_ALGORITHM, 0, value -> runOnUi(() -> {
            if (algorithm != null) {
                algorithm.setValue(String.valueOf(value));
            }
        }));
        SettingsManager.read(SettingsManager.GENERATED_RANDOM, false, value -> runOnUi(() -> {
            if (random != null) {
                random.setChecked(value);
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

    private void refreshWidgets() {
        WidgetProviderGen.update(requireContext());
    }

    private void refreshWidgetsWithCurrentSettings() {
        WidgetProviderGen.updateWithCurrentSettings(requireContext());
    }
}
