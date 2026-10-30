package com.tdeboer.widgetapp.preferences;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tdeboer.widgetapp.AppTheme;
import com.tdeboer.widgetapp.DataBackup;
import com.tdeboer.widgetapp.R;
import com.tdeboer.widgetapp.SettingsManager;
import com.tdeboer.widgetapp.widgets.gen.GeneratedImageStore;
import com.tdeboer.widgetapp.widgets.image.WidgetHelperImages;
import com.tdeboer.widgetapp.widgets.image.WidgetProviderImages;
import com.tdeboer.widgetapp.widgets.maze.WidgetProviderMaze;

import java.io.IOException;
import java.time.LocalDate;

public class MainPreference extends PreferenceFragmentCompat {
    private static final String[] BACKUP_TYPES = {"application/zip", "application/x-zip-compressed", "application/octet-stream"};

    private ListPreference appearance;

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/zip"), uri -> {
                if (uri != null) {
                    exportData(uri);
                }
            });

    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    importData(uri);
                }
            });

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.main_preference, rootKey);

        appearance = findPreference("appearance");
        Preference accent = findPreference("accent_color");
        Preference reset = findPreference("reset_data");
        Preference exportData = findPreference("export_data");
        Preference importData = findPreference("import_data");

        assert exportData != null;
        exportData.setOnPreferenceClickListener(preference -> {
            exportLauncher.launch(getString(R.string.backup_file_name, LocalDate.now()));
            return false;
        });

        assert importData != null;
        importData.setOnPreferenceClickListener(preference -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.import_confirm_title)
                    .setMessage(R.string.import_confirm_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.import_confirm_button, (dialog, which) -> importLauncher.launch(BACKUP_TYPES))
                    .show();
            return false;
        });

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

    private void exportData(Uri uri) {
        Context context = requireContext().getApplicationContext();
        Toast.makeText(context, R.string.export_busy, Toast.LENGTH_SHORT).show();

//        a background thread, because the backup waits for the files and settings
        new Thread(() -> {
            try {
                DataBackup.exportTo(context, uri);
                showResult(context, R.string.export_done);
            } catch (IOException | RuntimeException e) {
                Log.w("MainPreference", "Export failed", e);
                showResult(context, R.string.export_failed);
            }
        }).start();
    }

    private void importData(Uri uri) {
        Context context = requireContext().getApplicationContext();
        Toast.makeText(context, R.string.import_busy, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                DataBackup.importFrom(context, uri);
                showResult(context, R.string.import_done);
                runOnUi(() -> {
                    AppTheme.applySavedAppearance();
                    requireActivity().recreate();
                });
            } catch (IOException e) {
                Log.w("MainPreference", "Import failed", e);
                showResult(context, R.string.import_failed);
            }
        }).start();
    }

    private void showResult(Context context, int message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }

    private void resetData() {
        Context context = requireContext().getApplicationContext();

        SettingsManager.removeAllWidgetSettings();
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
