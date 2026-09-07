package com.example.widgetapp;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.widgetapp.fragments.InfoFragment;
import com.example.widgetapp.fragments.WidgetSettingsFragment;
import com.example.widgetapp.fragments.SettingsFragment;
import com.example.widgetapp.fragments.WidgetFragment;
import com.example.widgetapp.recyclers.WidgetItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements WidgetSettingsListener {

    private FrameLayout fswFragment;
    private final HashMap<WidgetItem.widgetTypes, Fragment> typeToFragment = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        fswFragment = findViewById(R.id.fswFragment);

//        handleWidgetIntent(getIntent());

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        Fragment widgetFragment = new WidgetFragment();
        Fragment settingsFragment = new SettingsFragment();
        Fragment infoFragment = new InfoFragment();

        setCurrentFragment(widgetFragment);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.widgets) {
                setCurrentFragment(widgetFragment);
            } else if (itemId == R.id.settings) {
                setCurrentFragment(settingsFragment);
            } else if (itemId == R.id.info) {
                setCurrentFragment(infoFragment);
            }
            return true;
        });
    }


    public void onWidgetSettingsRequested(WidgetItem.widgetTypes widgetType) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fswFragment, new WidgetSettingsFragment(widgetType, fswFragment))
                .commit();

        fswFragment.setVisibility(View.VISIBLE);
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit();
    }
}