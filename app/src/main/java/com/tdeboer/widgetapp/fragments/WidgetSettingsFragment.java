package com.tdeboer.widgetapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import com.tdeboer.widgetapp.R;
import com.tdeboer.widgetapp.preferences.GenPreference;
import com.tdeboer.widgetapp.preferences.MazePreference;
import com.tdeboer.widgetapp.preferences.ImagePreference;
import com.tdeboer.widgetapp.preferences.QuotePreference;
import com.tdeboer.widgetapp.recyclers.WidgetItem;

import java.util.HashMap;
import java.util.Map;

public class WidgetSettingsFragment extends Fragment {

    WidgetItem.widgetTypes widgetType;

    FrameLayout fswFragment;

    HashMap<WidgetItem.widgetTypes, PreferenceFragmentCompat> typeToPreference = new HashMap<>();

    public WidgetSettingsFragment() {
    }

    public static WidgetSettingsFragment newInstance(WidgetItem.widgetTypes widgetType) {
        WidgetSettingsFragment fragment = new WidgetSettingsFragment();
        Bundle arguments = new Bundle();
        arguments.putString("widget_type", widgetType.name());
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

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_widgets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView imageView = view.findViewById(R.id.fragment_settings_quotes_image);
        fswFragment = requireActivity().findViewById(R.id.fswFragment);

        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_settings_quotes_settings_container, typeToPreference.get(widgetType))
                .commit();

        imageView.setOnClickListener(v -> {
            fswFragment.setVisibility(View.GONE);

            getChildFragmentManager()
                    .beginTransaction()
                    .remove(typeToPreference.get(widgetType))
                    .commit();
        });
    }
}