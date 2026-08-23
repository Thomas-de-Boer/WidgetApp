package com.example.widgetapp;

public class WidgetItem {
    public enum widgetTypes {
        QUOTES,
        IMAGES,
        IMAGES_SMALL
    }
    int widgetId;
    widgetTypes widgetType;

    public WidgetItem(int widgetId, widgetTypes widgetType) {
        this.widgetId = widgetId;
        this.widgetType = widgetType;
    }

    public int get_id() {
        return widgetId;
    }

    public widgetTypes get_type() {
        return widgetType;
    }
}
