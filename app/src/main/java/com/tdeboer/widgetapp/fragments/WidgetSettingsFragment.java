package com.tdeboer.widgetapp.fragments;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.tdeboer.widgetapp.R;
import com.tdeboer.widgetapp.SettingsManager;
import com.tdeboer.widgetapp.preferences.GenPreference;
import com.tdeboer.widgetapp.preferences.MazePreference;
import com.tdeboer.widgetapp.preferences.ImagePreference;
import com.tdeboer.widgetapp.preferences.QuotePreference;
import com.tdeboer.widgetapp.recyclers.WidgetCardSize;
import com.tdeboer.widgetapp.recyclers.WidgetItem;

import java.util.HashMap;

public class WidgetSettingsFragment extends Fragment {
    public static final String ARGUMENT_WIDGET_ID = "app_widget_id";
//    same host as WidgetFragment
    private static final int APP_WIDGET_HOST_ID = 67;
    private static final long PREVIEW_DELAY_MILLIS = 1000;

    WidgetItem.widgetTypes widgetType;
    int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    FrameLayout fswFragment;
    OnBackPressedCallback backCallback;
    View previewCard;
    FrameLayout preview;
    AppWidgetHost appWidgetHost;

    private final Handler previewHandler = new Handler(Looper.getMainLooper());
    private final Runnable rebuildPreview = this::showPreview;

    HashMap<WidgetItem.widgetTypes, PreferenceFragmentCompat> typeToPreference = new HashMap<>();

    public WidgetSettingsFragment() {
    }

    public static WidgetSettingsFragment newInstance(WidgetItem.widgetTypes widgetType, int appWidgetId) {
        WidgetSettingsFragment fragment = new WidgetSettingsFragment();
        Bundle arguments = new Bundle();
        arguments.putString("widget_type", widgetType.name());
        arguments.putInt(ARGUMENT_WIDGET_ID, appWidgetId);
        fragment.setArguments(arguments);
        return fragment;
    }

    public WidgetSettingsFragment(WidgetItem.widgetTypes widgetType, FrameLayout fswFragment) {
        this();
        this.widgetType = widgetType;
        this.fswFragment = fswFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            appWidgetId = getArguments().getInt(ARGUMENT_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (widgetType == null && getArguments() != null) {
            String type = getArguments().getString("widget_type");
            if (type != null) {
                widgetType = WidgetItem.widgetTypes.valueOf(type);
            }
        }
        if (widgetType == null) {
            widgetType = WidgetItem.widgetTypes.GEN;
        }
        typeToPreference.put(WidgetItem.widgetTypes.QUOTES, new QuotePreference());
        typeToPreference.put(WidgetItem.widgetTypes.IMAGES, new ImagePreference());
        typeToPreference.put(WidgetItem.widgetTypes.GEN, new GenPreference());
        typeToPreference.put(WidgetItem.widgetTypes.MAZE, new MazePreference());

        Bundle arguments = new Bundle();
        arguments.putInt(ARGUMENT_WIDGET_ID, appWidgetId);
        typeToPreference.get(widgetType).setArguments(arguments);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_widgets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.fragment_settings_quotes_toolbar);
        toolbar.setTitle(WidgetItem.nameFor(widgetType));
        toolbar.setNavigationOnClickListener(v -> close());

        previewCard = view.findViewById(R.id.fragment_settings_quotes_preview_card);
        preview = view.findViewById(R.id.fragment_settings_quotes_preview);
        appWidgetHost = new AppWidgetHost(requireContext().getApplicationContext(), APP_WIDGET_HOST_ID);

        fswFragment = requireActivity().findViewById(R.id.fswFragment);

        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_settings_quotes_settings_container, typeToPreference.get(widgetType))
                .commit();

        OnBackPressedCallback backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                close();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);
        this.backCallback = backCallback;
    }

    @Override
    public void onStart() {
        super.onStart();

        appWidgetHost.startListening();
        showPreview();
//        android only sends widget updates to the launcher that owns the widget, so build the preview again
//        after a save. the delay waits for the last save and gives the widget time to update
        SettingsManager.setChangeListener(this::schedulePreview);
    }

    @Override
    public void onStop() {
        super.onStop();

        stopPreview();
    }

    private void schedulePreview() {
        previewHandler.removeCallbacks(rebuildPreview);
        previewHandler.postDelayed(rebuildPreview, PREVIEW_DELAY_MILLIS);
    }

    private void stopPreview() {
        SettingsManager.setChangeListener(null);
        previewHandler.removeCallbacks(rebuildPreview);
        appWidgetHost.stopListening();
    }

    private void showPreview() {
        Context context = requireContext().getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(appWidgetId);

        if (info == null) {
            previewCard.setVisibility(View.GONE);
            return;
        }

        ViewGroup.LayoutParams params = previewCard.getLayoutParams();
        params.width = WidgetCardSize.width(context, appWidgetManager, appWidgetId, params.height);
        previewCard.setLayoutParams(params);

        View view = appWidgetHost.createView(context, appWidgetId, info);
        view.setPadding(0, 0, 0, 0);

        preview.removeAllViews();
        preview.addView(view);
    }

    private void close() {
//        this fragment stays attached while hidden, so the back gesture has to go to the app again
        backCallback.setEnabled(false);
        stopPreview();
        fswFragment.setVisibility(View.GONE);

        getChildFragmentManager()
                .beginTransaction()
                .remove(typeToPreference.get(widgetType))
                .commit();
    }
}
