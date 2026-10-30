package com.tdeboer.widgetapp;

import android.app.Activity;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

public final class AppTheme {
    public static final int APPEARANCE_SYSTEM = 0;
    public static final int APPEARANCE_LIGHT = 1;
    public static final int APPEARANCE_DARK = 2;
//    colors from the settings are never fully transparent, so 0 is free to mean: use the wallpaper
    public static final int ACCENT_WALLPAPER = 0;

    private AppTheme() { }

    public static void applyAppearance(int appearance) {
        switch (appearance) {
            case APPEARANCE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case APPEARANCE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static void applySavedAppearance() {
        Integer appearance = SettingsManager.readsync(SettingsManager.APPEARANCE);
        applyAppearance(appearance != null ? appearance : APPEARANCE_SYSTEM);
    }

    public static int savedAccent() {
        Integer accent = SettingsManager.readsync(SettingsManager.ACCENT_COLOR);
        return accent != null ? accent : ACCENT_WALLPAPER;
    }

//    call this before the views of the activity are created
    public static void applyAccent(Activity activity) {
        DynamicColorsOptions.Builder options = new DynamicColorsOptions.Builder();
        int accent = savedAccent();
        if (accent != ACCENT_WALLPAPER) {
            options.setContentBasedSource(accent);
        }
        DynamicColors.applyToActivityIfAvailable(activity, options.build());
    }
}
