package com.tdeboer.widgetapp.preferences;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.tdeboer.widgetapp.MainActivity;
import com.tdeboer.widgetapp.R;
import com.tdeboer.widgetapp.SettingsManager;
import com.tdeboer.widgetapp.widgets.gen.GeneratedImageStore;
import com.tdeboer.widgetapp.widgets.image.WidgetHelperImages;
import com.tdeboer.widgetapp.widgets.image.WidgetProviderImages;
import com.tdeboer.widgetapp.widgets.maze.WidgetProviderMaze;

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

            Context context = requireContext().getApplicationContext();

            SettingsManager.resetData(SettingsManager.QUOTEANDFACT);
            SettingsManager.write(SettingsManager.UPLOADEDIMAGES, "[]", string -> {
                if (string != null) {
                    WidgetHelperImages.deleteAll(context);

                    AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
                    int[] appWidgetIds = widgetManager.getAppWidgetIds(new ComponentName(context, WidgetProviderImages.class));
                    WidgetProviderImages.update(context, widgetManager, appWidgetIds);
                }
            });
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
            SettingsManager.resetMazeSettings(() -> WidgetProviderMaze.update(context));

            return false;
        });
    }
}
