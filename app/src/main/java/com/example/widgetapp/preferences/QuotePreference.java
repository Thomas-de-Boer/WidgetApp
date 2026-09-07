package com.example.widgetapp.preferences;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.example.widgetapp.R;
import com.example.widgetapp.SettingsManager;

public class QuotePreference extends PreferenceFragmentCompat {

    SeekBarPreference seekBarPreference;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_quotes, rootKey);

         seekBarPreference = findPreference("quote_and_fact");

        SettingsManager.read(SettingsManager.QUOTEANDFACT, 0f, value -> {
            assert seekBarPreference != null;
            seekBarPreference.setValue((int) (value * 100));
        });
    }


    @Override
    public void onStop() {
        super.onStop();

        SettingsManager.write(SettingsManager.QUOTEANDFACT, (float) seekBarPreference.getValue() / 100);
    }
}
