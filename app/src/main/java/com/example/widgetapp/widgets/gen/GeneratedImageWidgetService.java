package com.example.widgetapp.widgets.gen;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class GeneratedImageWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new GeneratedImageWidgetFactory(getApplicationContext());
    }
}
