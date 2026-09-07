package com.example.widgetapp;

import android.content.Context;

import androidx.datastore.guava.GuavaDataStore;
import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesFileSerializer;
import androidx.datastore.preferences.core.PreferencesKeys;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SettingsManager {
//    place where settings are stored
    private static GuavaDataStore<Preferences> dataStore;
//    a thread where search's happen
    private static final Executor executor = Executors.newSingleThreadExecutor();

//    all settings fields
    public static final Preferences.Key<Float> QUOTEANDFACT = PreferencesKeys.floatKey("QUOTEANDFACT");


    private SettingsManager() { }

//    fill the dataStore variable with the actual instance of dataStore with filename: 'settings'
//    also make it run on the thread created earlier
    public static void setInstance(Context context) {
        dataStore = new GuavaDataStore.Builder<>(
                context.getApplicationContext(),
                "settings",
                PreferencesFileSerializer.INSTANCE
        ).setExecutor(executor).build();
    }


//    internal helper method for read and write to reach the datastore
    private static GuavaDataStore<Preferences> getDataStore() {
        return dataStore;
    }

//    write method: write the settings to disc. uses the setting field and the new value
    public static <T> void write(Preferences.Key<T> key, T value) {
        getDataStore().updateDataAsync(preferences -> {
            MutablePreferences mutablePreferences = preferences.toMutablePreferences();
            mutablePreferences.set(key, value);
            return mutablePreferences;
        });
    }

//    callback interface because read method is async
    public interface Callback<T> {
        void onResult(T value);
    }

//    read method: reads the value based on a setting field. gives a defaultValue in case you cant retrieve datastore.
//    uses a callback to make sure the method is async
    public static <T> void read(Preferences.Key<T> key, T defaultValue, Callback<T> callback) {
        ListenableFuture<Preferences> future = getDataStore().getDataAsync();
        Futures.addCallback(future, new FutureCallback<Preferences>() {
            @Override
            public void onSuccess(Preferences preferences) {
                T value = preferences.get(key);
                callback.onResult(value != null ? value : defaultValue);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onResult(defaultValue);
            }
        }, executor);
    }
}
