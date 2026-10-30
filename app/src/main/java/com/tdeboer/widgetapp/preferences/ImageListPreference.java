package com.tdeboer.widgetapp.preferences;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tdeboer.widgetapp.R;
import com.tdeboer.widgetapp.SettingsManager;
import com.tdeboer.widgetapp.recyclers.ImageListAdapter;
import com.tdeboer.widgetapp.widgets.image.WidgetProviderImages;
import com.google.gson.Gson;

import java.util.List;

public class ImageListPreference extends Preference {
    public ImageListPreference(@NonNull Context context) {
        this(context, null);
    }

    public ImageListPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ImageListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setLayoutResource(R.layout.images_list_preference);
    }

    public void notifyChange() {
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        AppWidgetManager widgetManager = AppWidgetManager.getInstance(getContext());

        RecyclerView images_list_preference_recycler = holder.itemView.findViewById(R.id.images_list_preference_recycler);

        List<String> uploadedFileNames = SettingsManager.deserializeString(SettingsManager.readsyncImages());

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 3);

        images_list_preference_recycler.setLayoutManager(gridLayoutManager);

        images_list_preference_recycler.setAdapter(new ImageListAdapter(getContext(), uploadedFileNames, s -> {
            uploadedFileNames.remove(s);

            Gson gson = new Gson();

            SettingsManager.write(SettingsManager.UPLOADEDIMAGES, gson.toJson(uploadedFileNames), string -> {
//                delete only after the list is saved, the settings screen expects every listed file to exist
                if (string != null) {
                    getContext().deleteFile(s);
                }

                Handler mainHandler = new Handler(getContext().getMainLooper());

                Runnable myRunnable = new Runnable() {
                    @Override

                    public void run() {
                        notifyChanged();

                        int[] appWidgetIds = widgetManager.getAppWidgetIds(new ComponentName(getContext(), WidgetProviderImages.class));

                        WidgetProviderImages.update(getContext(), widgetManager, appWidgetIds);
                    }
                };
                mainHandler.post(myRunnable);
            });
        }));

    }
}
