package com.tdeboer.widgetapp;

import android.app.Application;

public class WidgetApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        SettingsManager.setInstance(this);
    }
}
