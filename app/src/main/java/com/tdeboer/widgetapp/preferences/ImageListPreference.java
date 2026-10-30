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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tdeboer.widgetapp.R;
import com.tdeboer.widgetapp.SettingsManager;
import com.tdeboer.widgetapp.recyclers.ImageListAdapter;
import com.tdeboer.widgetapp.widgets.image.WidgetProviderImages;
import com.google.gson.Gson;

import java.util.List;

public class ImageListPreference extends Preference {
    private int appWidgetId;

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

    public void setAppWidgetId(int appWidgetId) {
        this.appWidgetId = appWidgetId;
    }

    public void notifyChange() {
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        AppWidgetManager widgetManager = AppWidgetManager.getInstance(getContext());

        RecyclerView images_list_preference_recycler = holder.itemView.findViewById(R.id.images_list_preference_recycler);

        List<String> uploadedFileNames = SettingsManager.deserializeString(SettingsManager.readsyncImages(appWidgetId));

        holder.itemView.findViewById(R.id.images_list_preference_empty)
                .setVisibility(uploadedFileNames.isEmpty() ? View.VISIBLE : View.GONE);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 3);

        images_list_preference_recycler.setLayoutManager(gridLayoutManager);

        images_list_preference_recycler.setAdapter(new ImageListAdapter(getContext(), uploadedFileNames, s ->
                new MaterialAlertDialogBuilder(getContext())
                        .setTitle(R.string.image_delete_confirm_title)
                        .setMessage(R.string.image_delete_confirm_message)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.delete, (dialog, which) -> deleteImage(s, uploadedFileNames, widgetManager))
                        .show()));

    }

    private void deleteImage(String fileName, List<String> uploadedFileNames, AppWidgetManager widgetManager) {
        uploadedFileNames.remove(fileName);

        Gson gson = new Gson();

        SettingsManager.writeWidget(SettingsManager.UPLOADEDIMAGES, appWidgetId, gson.toJson(uploadedFileNames), string -> {
//            delete only after the list is saved, the settings screen expects every listed file to exist
            if (string != null) {
                getContext().deleteFile(fileName);
            }

            Handler mainHandler = new Handler(getContext().getMainLooper());

            Runnable myRunnable = new Runnable() {
                @Override

                public void run() {
                    notifyChanged();

                    WidgetProviderImages.update(getContext(), widgetManager, new int[]{appWidgetId});
                }
            };
            mainHandler.post(myRunnable);
        });
    }
}
