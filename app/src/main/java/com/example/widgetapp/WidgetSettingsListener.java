package com.example.widgetapp;

import com.example.widgetapp.recyclers.WidgetItem;

public interface WidgetSettingsListener {
    void onWidgetSettingsRequested(WidgetItem.widgetTypes widgetType);
}