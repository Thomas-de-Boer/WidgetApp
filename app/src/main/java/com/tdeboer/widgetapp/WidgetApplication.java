package com.tdeboer.widgetapp;

import android.app.Application;

import com.tdeboer.widgetapp.widgets.image.WidgetHelperImages;

public class WidgetApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        SettingsManager.setInstance(this);
        AppTheme.applySavedAppearance();
        WidgetHelperImages.splitSharedImages(this);
    }
}
