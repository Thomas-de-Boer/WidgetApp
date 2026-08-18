package com.example.widgetapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit();
    }
}