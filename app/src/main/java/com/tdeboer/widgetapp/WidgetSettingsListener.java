package com.tdeboer.widgetapp;

import com.tdeboer.widgetapp.recyclers.WidgetItem;

public interface WidgetSettingsListener {
    void onWidgetSettingsRequested(WidgetItem.widgetTypes widgetType);
}