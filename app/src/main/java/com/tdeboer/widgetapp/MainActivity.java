package com.tdeboer.widgetapp;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

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
import com.tdeboer.widgetapp.update.UpdateChecker;
import com.tdeboer.widgetapp.update.UpdateManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements WidgetSettingsListener {

    private FrameLayout fswFragment;
    private View updateBar;
    private TextView updateText;
    private LinearProgressIndicator updateProgress;
    private MaterialButton updateButton;
    private View updateDismiss;
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
        applySystemBarPadding(findViewById(R.id.mainContent), false);
        applySystemBarPadding(fswFragment, true);

        updateBar = findViewById(R.id.updateBar);
        updateText = findViewById(R.id.updateText);
        updateProgress = findViewById(R.id.updateProgress);
        updateButton = findViewById(R.id.updateButton);
        updateDismiss = findViewById(R.id.updateDismiss);
        updateButton.setOnClickListener(v -> UpdateManager.startUpdate(this));
        updateDismiss.setOnClickListener(v -> UpdateManager.dismiss());
        UpdateManager.checkOnce(this);

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


    @Override
    protected void onResume() {
        super.onResume();
        UpdateManager.attach(this, this::showUpdateState);
    }

    @Override
    protected void onPause() {
        UpdateManager.detach();
        super.onPause();
    }

    private void showUpdateState() {
        UpdateChecker.Release release = UpdateManager.getRelease();
        UpdateManager.State state = UpdateManager.getState();
        if (release == null || state == UpdateManager.State.NONE || state == UpdateManager.State.DISMISSED) {
            updateBar.setVisibility(View.GONE);
            return;
        }
        updateBar.setVisibility(View.VISIBLE);

        boolean busy = state == UpdateManager.State.DOWNLOADING || state == UpdateManager.State.INSTALLING;
        updateButton.setVisibility(busy ? View.GONE : View.VISIBLE);
        updateDismiss.setVisibility(busy ? View.GONE : View.VISIBLE);
        updateProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        updateButton.setText(state == UpdateManager.State.FAILED ? R.string.update_retry : R.string.update_button);

        switch (state) {
            case DOWNLOADING:
                updateText.setText(getString(R.string.update_downloading, release.version, UpdateManager.getProgress()));
                updateProgress.setIndeterminate(false);
                updateProgress.setProgressCompat(UpdateManager.getProgress(), true);
                break;
            case INSTALLING:
                updateText.setText(getString(R.string.update_installing, release.version));
                updateProgress.setIndeterminate(true);
                break;
            case NEEDS_PERMISSION:
                updateText.setText(R.string.update_permission);
                break;
            case FAILED:
                String error = UpdateManager.getError();
                updateText.setText(error != null ? error : getString(R.string.update_failed_install));
                break;
            default:
                updateText.setText(getString(R.string.update_available, release.version));
                break;
        }
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