package com.tdeboer.widgetapp.preferences;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tdeboer.widgetapp.AppTheme;
import com.tdeboer.widgetapp.R;
import com.tdeboer.widgetapp.SettingsManager;
import com.tdeboer.widgetapp.widgets.gen.GeneratedImageStore;
import com.tdeboer.widgetapp.widgets.image.WidgetHelperImages;
import com.tdeboer.widgetapp.widgets.image.WidgetProviderImages;
import com.tdeboer.widgetapp.widgets.maze.WidgetProviderMaze;

public class MainPreference extends PreferenceFragmentCompat {
    private ListPreference appearance;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.main_preference, rootKey);

        appearance = findPreference("appearance");
        Preference accent = findPreference("accent_color");
        Preference reset = findPreference("reset_data");

        assert appearance != null;
        appearance.setOnPreferenceChangeListener((preference, value) -> {
            int chosen = Integer.parseInt((String) value);
            SettingsManager.write(SettingsManager.APPEARANCE, chosen, ignored -> runOnUi(() -> AppTheme.applyAppearance(chosen)));
            return true;
        });

        assert accent != null;
//        the colors can only be generated on android 12 and up, older phones keep the indigo theme
        accent.setVisible(DynamicColors.isDynamicColorAvailable());
        accent.setOnPreferenceClickListener(preference -> {
            AccentColorDialog.show(requireContext(), AppTheme.savedAccent(), color ->
                    SettingsManager.write(SettingsManager.ACCENT_COLOR, color, ignored -> runOnUi(() -> requireActivity().recreate())));
            return false;
        });

        assert reset != null;
        reset.setOnPreferenceClickListener(preference -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.reset_confirm_title)
                    .setMessage(R.string.reset_confirm_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.reset_confirm_button, (dialog, which) -> resetData())
                    .show();

            return false;
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        SettingsManager.read(SettingsManager.APPEARANCE, AppTheme.APPEARANCE_SYSTEM, value ->
                runOnUi(() -> appearance.setValue(String.valueOf(value))));
    }

    private void resetData() {
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
        GeneratedImageStore.deleteAll(context);
        SettingsManager.resetMazeSettings(() -> WidgetProviderMaze.update(context));
        SettingsManager.write(SettingsManager.ACCENT_COLOR, AppTheme.ACCENT_WALLPAPER, accent ->
                SettingsManager.write(SettingsManager.APPEARANCE, AppTheme.APPEARANCE_SYSTEM, ignored -> runOnUi(() -> {
                    AppTheme.applyAppearance(AppTheme.APPEARANCE_SYSTEM);
                    requireActivity().recreate();
                })));
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
