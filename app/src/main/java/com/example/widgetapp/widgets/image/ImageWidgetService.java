package com.example.widgetapp.widgets.image;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class ImageWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ImageWidgetFactory(getApplicationContext());
    }
}