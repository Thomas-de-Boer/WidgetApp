package com.example.widgetapp.recyclers;

public class WidgetItem {
    public enum widgetTypes {
        QUOTES,
        IMAGES,
    }
    int widgetId;
    public widgetTypes widgetType;

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
