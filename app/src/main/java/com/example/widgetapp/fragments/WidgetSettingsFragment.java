package com.example.widgetapp.fragments;

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

import com.example.widgetapp.R;
import com.example.widgetapp.preferences.ImagePreference;
import com.example.widgetapp.preferences.QuotePreference;
import com.example.widgetapp.recyclers.WidgetItem;

import java.util.HashMap;
import java.util.Map;

public class WidgetSettingsFragment extends Fragment {

    WidgetItem.widgetTypes widgetType;

    FrameLayout fswFragment;

    HashMap<WidgetItem.widgetTypes, PreferenceFragmentCompat> typeToPreference = new HashMap<>();

    public WidgetSettingsFragment(WidgetItem.widgetTypes widgetType, FrameLayout fswFragment) {
        this.widgetType = widgetType;
        this.fswFragment = fswFragment;

        typeToPreference.put(WidgetItem.widgetTypes.QUOTES, new QuotePreference());
        typeToPreference.put(WidgetItem.widgetTypes.IMAGES, new ImagePreference());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings_widgets, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_settings_quotes_settings_container, typeToPreference.get(widgetType))
                .commit();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView imageView = view.findViewById(R.id.fragment_settings_quotes_image);

        imageView.setOnClickListener(v -> {
            fswFragment.setVisibility(View.GONE);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .remove(typeToPreference.get(widgetType))
                    .commit();
        });
    }
}