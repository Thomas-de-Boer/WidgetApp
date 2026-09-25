package com.example.widgetapp.widgets.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.Settings;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.widgetapp.R;
import com.example.widgetapp.SettingsManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class ImageWidgetFactory implements RemoteViewsService.RemoteViewsFactory {
    Context context;
    List<Bitmap> images;
    List<String> uploadedFileNames;

    public ImageWidgetFactory(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        if (uploadedFileNames.isEmpty())
            return 1;

        return uploadedFileNames.size();
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public RemoteViews getViewAt(int position) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.flipper_item);

        if (uploadedFileNames.isEmpty())
            return views;

        FileInputStream fileInputStream;
        try {
            fileInputStream = context.openFileInput(uploadedFileNames.get(position));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream);

        views.setImageViewBitmap(R.id.flipper_item_image, bitmap);
        views.setTextViewText(R.id.flipper_item_text, "");

        return views;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public void onCreate() {
        uploadedFileNames = SettingsManager.deserializeString(SettingsManager.readsyncImages());
//        SettingsManager.read(SettingsManager.UPLOADEDIMAGES, null, string -> {
//            uploadedFileNames = SettingsManager.deserializeString(string);
//        });
    }

    @Override
    public void onDataSetChanged() {
        uploadedFileNames = SettingsManager.deserializeString(SettingsManager.readsyncImages());
//        SettingsManager.read(SettingsManager.UPLOADEDIMAGES, null, string -> {
//            uploadedFileNames = SettingsManager.deserializeString(string);
//        });
    }

    @Override
    public void onDestroy() {

    }
}