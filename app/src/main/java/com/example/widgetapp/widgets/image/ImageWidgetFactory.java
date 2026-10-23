package com.example.widgetapp.widgets.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.widgetapp.R;
import com.example.widgetapp.SettingsManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ImageWidgetFactory implements RemoteViewsService.RemoteViewsFactory {
    Context context;
    List<String> uploadedFileNames;

    public ImageWidgetFactory(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        if (uploadedFileNames == null || uploadedFileNames.isEmpty())
            return 1;

        return uploadedFileNames.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public RemoteViews getLoadingView() {
        return new RemoteViews(context.getPackageName(), R.layout.flipper_item);
    }

    @Override
    public RemoteViews getViewAt(int position) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.flipper_item);

        if (uploadedFileNames == null || uploadedFileNames.isEmpty())
            return views;

        if (position < 0 || position >= uploadedFileNames.size()) {
            return views;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream boundsStream = context.openFileInput(uploadedFileNames.get(position))) {
            BitmapFactory.decodeStream(boundsStream, null, options);
        } catch (IOException e) {
            return views;
        }

        options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, 320, 320);
        options.inJustDecodeBounds = false;
        Bitmap bitmap;
        try (InputStream imageStream = context.openFileInput(uploadedFileNames.get(position))) {
            bitmap = BitmapFactory.decodeStream(imageStream, null, options);
        } catch (IOException e) {
            return views;
        }
        if (bitmap != null) {
            views.setImageViewBitmap(R.id.flipper_item_image, bitmap);
            views.setTextViewText(R.id.flipper_item_text, "");
        }

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

    private int calculateSampleSize(int width, int height, int targetWidth, int targetHeight) {
        int sampleSize = 1;
        while (width / (sampleSize * 2) >= targetWidth
                || height / (sampleSize * 2) >= targetHeight) {
            sampleSize *= 2;
        }
        return sampleSize;
    }
}