package com.tdeboer.widgetapp.recyclers;

import com.tdeboer.widgetapp.R;

public class WidgetItem {
    public enum widgetTypes {
        QUOTES,
        IMAGES,
        GEN,
        MAZE
    }
    int widgetId;
    public widgetTypes widgetType;

    public static int nameFor(widgetTypes widgetType) {
        switch (widgetType) {
            case QUOTES:
                return R.string.quote_widget_name;
            case IMAGES:
                return R.string.image_widget_name;
            case MAZE:
                return R.string.maze_widget_name;
            default:
                return R.string.gen_widget_name;
        }
    }

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
