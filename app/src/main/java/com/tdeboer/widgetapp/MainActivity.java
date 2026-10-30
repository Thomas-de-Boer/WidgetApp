package com.tdeboer.widgetapp;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.tdeboer.widgetapp.fragments.InfoFragment;
import com.tdeboer.widgetapp.fragments.WidgetSettingsFragment;
import com.tdeboer.widgetapp.fragments.SettingsFragment;
import com.tdeboer.widgetapp.fragments.WidgetFragment;
import com.tdeboer.widgetapp.recyclers.WidgetItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements WidgetSettingsListener {

    private FrameLayout fswFragment;
    private final HashMap<WidgetItem.widgetTypes, Fragment> typeToFragment = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppTheme.applyAccent(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        fswFragment = findViewById(R.id.fswFragment);

//        handleWidgetIntent(getIntent());

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

//        the app draws behind the system bars, so keep the content out from under them
        applySystemBarPadding(findViewById(R.id.flFragment), false);
        applySystemBarPadding(fswFragment, true);

        Fragment widgetFragment = new WidgetFragment();
        Fragment settingsFragment = new SettingsFragment();
        Fragment infoFragment = new InfoFragment();

        if (savedInstanceState == null) {
            setCurrentFragment(widgetFragment);

        }

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


    public void onWidgetSettingsRequested(WidgetItem.widgetTypes widgetType, int appWidgetId) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fswFragment, WidgetSettingsFragment.newInstance(widgetType, appWidgetId))
                .commit();

        fswFragment.setVisibility(View.VISIBLE);
    }

    private void applySystemBarPadding(View view, boolean includeBottom) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, includeBottom ? bars.bottom : 0);
            return windowInsets;
        });
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit();
    }
}