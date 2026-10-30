package com.tdeboer.widgetapp.preferences;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.tdeboer.widgetapp.R;
import com.tdeboer.widgetapp.SettingsManager;
import com.tdeboer.widgetapp.fragments.WidgetSettingsFragment;
import com.tdeboer.widgetapp.widgets.maze.Maze;
import com.tdeboer.widgetapp.widgets.maze.WidgetProviderMaze;

public class MazePreference extends PreferenceFragmentCompat {
    private int appWidgetId;
    private SeekBarPreference cells;
    private SeekBarPreference speed;
    private ListPreference style;
    private ListPreference theme;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_maze, rootKey);

        appWidgetId = requireArguments().getInt(WidgetSettingsFragment.ARGUMENT_WIDGET_ID);
        cells = findPreference("maze_cells");
        speed = findPreference("maze_speed");
        style = findPreference("maze_style");
        theme = findPreference("maze_theme");
        // the writes finish on another thread, the screen can already be closed by then
        Context context = requireContext().getApplicationContext();

        if (cells != null) {
            cells.setOnPreferenceChangeListener((preference, value) -> {
                SettingsManager.writeWidget(SettingsManager.MAZE_CELLS, appWidgetId, (Integer) value,
                        ignored -> WidgetProviderMaze.update(context, appWidgetId));
                return true;
            });
        }
        if (speed != null) {
            speed.setOnPreferenceChangeListener((preference, value) -> {
                SettingsManager.writeWidget(SettingsManager.MAZE_SPEED, appWidgetId, (Integer) value,
                        ignored -> WidgetProviderMaze.redraw(context, appWidgetId));
                return true;
            });
        }
        if (style != null) {
            style.setOnPreferenceChangeListener((preference, value) -> {
                SettingsManager.writeWidget(SettingsManager.MAZE_STYLE, appWidgetId, Integer.parseInt((String) value),
                        ignored -> WidgetProviderMaze.update(context, appWidgetId));
                return true;
            });
        }
        if (theme != null) {
            theme.setOnPreferenceChangeListener((preference, value) -> {
                SettingsManager.writeWidget(SettingsManager.MAZE_THEME, appWidgetId, Integer.parseInt((String) value),
                        ignored -> WidgetProviderMaze.redraw(context, appWidgetId));
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadValues();
    }

    private void loadValues() {
        SettingsManager.readWidget(SettingsManager.MAZE_CELLS, appWidgetId, Maze.DEFAULT_CELLS, value -> runOnUi(() -> {
            if (cells != null) {
                cells.setValue(value);
            }
        }));
        SettingsManager.readWidget(SettingsManager.MAZE_SPEED, appWidgetId, Maze.DEFAULT_SPEED, value -> runOnUi(() -> {
            if (speed != null) {
                speed.setValue(value);
            }
        }));
        SettingsManager.readWidget(SettingsManager.MAZE_STYLE, appWidgetId, Maze.STYLE_WINDING, value -> runOnUi(() -> {
            if (style != null) {
                style.setValue(String.valueOf(value));
            }
        }));
        SettingsManager.readWidget(SettingsManager.MAZE_THEME, appWidgetId, 0, value -> runOnUi(() -> {
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
