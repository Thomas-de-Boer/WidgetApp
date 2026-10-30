package com.tdeboer.widgetapp.widgets.gen;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;

// serves the generated and maze frames read-only, so the launcher can load and animate them by itself
public class GeneratedImageProvider extends ContentProvider {
    private static final String GENERATED_FILE = "generated_\\d+_[A-Za-z0-9-]+_\\d+\\.jpg";
    private static final String MAZE_FILE = "maze_\\d+_[A-Za-z0-9-]+_\\d+\\.png";

    public static Uri uriFor(Context context, String fileName) {
        return new Uri.Builder()
                .scheme("content")
                .authority(context.getPackageName() + ".generated")
                .appendPath(fileName)
                .build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new FileNotFoundException("Read only");
        }
        String name = uri.getLastPathSegment();
        if (uri.getPathSegments().size() != 1 || name == null) {
            throw new FileNotFoundException("Unknown file");
        }
        String directory;
        if (name.matches(GENERATED_FILE)) {
            directory = "generated_widget";
        } else if (name.matches(MAZE_FILE)) {
            directory = "maze_widget";
        } else {
            throw new FileNotFoundException("Unknown file");
        }
        File file = new File(new File(getContext().getFilesDir(), directory), name);
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        String name = uri.getLastPathSegment();
        return name != null && name.endsWith(".png") ? "image/png" : "image/jpeg";
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        return 0;
    }
}
