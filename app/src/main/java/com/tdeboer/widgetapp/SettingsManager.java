package com.tdeboer.widgetapp;

import android.content.Context;
import android.net.Uri;

import androidx.datastore.guava.GuavaDataStore;
import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesFileSerializer;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.preference.Preference;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SettingsManager {
//    place where settings are stored
    private static GuavaDataStore<Preferences> dataStore;
//    told about every save, for screens that show the result of a setting
    private static volatile Runnable changeListener;
//    a thread where search's happen
    private static final Executor executor = Executors.newSingleThreadExecutor();

//    all settings fields
    public static final Preferences.Key<Float> QUOTEANDFACT = PreferencesKeys.floatKey("QUOTEANDFACT");
//    public static final Preferences.Key<Set<String>> UPLOADEDIMAGES = PreferencesKeys.stringSetKey("UPLOADEDIMAGES");
    public static final Preferences.Key<String> UPLOADEDIMAGES = PreferencesKeys.stringKey("UPLOADEDIMAGESSTRING");
    public static final Preferences.Key<Float> GENERATED_SCALE = PreferencesKeys.floatKey("GENERATED_SCALE");
    public static final Preferences.Key<Integer> GENERATED_OCTAVES = PreferencesKeys.intKey("GENERATED_OCTAVES");
    public static final Preferences.Key<Integer> GENERATED_PALETTE = PreferencesKeys.intKey("GENERATED_PALETTE");
    public static final Preferences.Key<Integer> GENERATED_ALGORITHM = PreferencesKeys.intKey("GENERATED_ALGORITHM");
    public static final Preferences.Key<Integer> GENERATED_SPEED = PreferencesKeys.intKey("GENERATED_SPEED");
    public static final Preferences.Key<Long> GENERATED_SEED = PreferencesKeys.longKey("GENERATED_SEED");
    public static final Preferences.Key<String> GENERATED_IMAGES = PreferencesKeys.stringKey("GENERATED_IMAGES");
    public static final Preferences.Key<Boolean> GENERATED_RANDOM = PreferencesKeys.booleanKey("GENERATED_RANDOM");
    public static final Preferences.Key<Boolean> QUOTE_AVOID_REPEATS = PreferencesKeys.booleanKey("QUOTE_AVOID_REPEATS");
    public static final Preferences.Key<String> QUOTE_HISTORY = PreferencesKeys.stringKey("QUOTE_HISTORY");
    public static final Preferences.Key<String> FACT_HISTORY = PreferencesKeys.stringKey("FACT_HISTORY");
    public static final Preferences.Key<Integer> MAZE_CELLS = PreferencesKeys.intKey("MAZE_CELLS");
    public static final Preferences.Key<Integer> MAZE_STYLE = PreferencesKeys.intKey("MAZE_STYLE");
    public static final Preferences.Key<Integer> MAZE_THEME = PreferencesKeys.intKey("MAZE_THEME");
    public static final Preferences.Key<Integer> MAZE_SPEED = PreferencesKeys.intKey("MAZE_SPEED");
    public static final Preferences.Key<Integer> APPEARANCE = PreferencesKeys.intKey("APPEARANCE");
    public static final Preferences.Key<Integer> ACCENT_COLOR = PreferencesKeys.intKey("ACCENT_COLOR");

    private static final String WIDGET_KEY_PREFIX = "WIDGET_";

//    every widget can have its own value for a setting. the settings above are the starting point for widgets
//    without their own value, so existing widgets keep their look and new widgets start from those settings.
//    the name is all that identifies a key, the stored value keeps its own type
    @SuppressWarnings("unchecked")
    public static <T> Preferences.Key<T> widgetKey(Preferences.Key<T> key, int appWidgetId) {
        return (Preferences.Key<T>) (Preferences.Key<?>) PreferencesKeys.stringKey(
                WIDGET_KEY_PREFIX + appWidgetId + "_" + key.getName());
    }

//    every maze widget has its own maze, so these fields exist per widget
    public static Preferences.Key<Long> mazeSeedKey(int appWidgetId) {
        return PreferencesKeys.longKey("MAZE_SEED_" + appWidgetId);
    }

    public static Preferences.Key<Boolean> mazeSolvedKey(int appWidgetId) {
        return PreferencesKeys.booleanKey("MAZE_SOLVED_" + appWidgetId);
    }

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

    public static void setChangeListener(Runnable listener) {
        changeListener = listener;
    }

    private static void notifyChanged() {
        Runnable listener = changeListener;
        if (listener != null) {
            listener.run();
        }
    }

//    write method: write the settings to disc. uses the setting field and the new value
    public static <T> void write(Preferences.Key<T> key, T value, Callback<T> callback) {
        ListenableFuture<Preferences> future = getDataStore().updateDataAsync(preferences -> {
            MutablePreferences mutablePreferences = preferences.toMutablePreferences();
            mutablePreferences.set(key, value);
            return mutablePreferences;
        });
        Futures.addCallback(future, new FutureCallback<Preferences>() {
            @Override
            public void onSuccess(Preferences result) {
                callback.onResult(value);
                notifyChanged();
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onResult(null);
            }
        }, executor);
    }

//    writes all generated widget look settings at once, so nobody reads a half-updated set
    public static void writeGeneratedSettings(int appWidgetId, float scale, int octaves, int speed, int palette,
                                              int algorithm, Runnable callback) {
        ListenableFuture<Preferences> future = getDataStore().updateDataAsync(preferences -> {
            MutablePreferences mutablePreferences = preferences.toMutablePreferences();
            mutablePreferences.set(widgetKey(GENERATED_SCALE, appWidgetId), scale);
            mutablePreferences.set(widgetKey(GENERATED_OCTAVES, appWidgetId), octaves);
            mutablePreferences.set(widgetKey(GENERATED_SPEED, appWidgetId), speed);
            mutablePreferences.set(widgetKey(GENERATED_PALETTE, appWidgetId), palette);
            mutablePreferences.set(widgetKey(GENERATED_ALGORITHM, appWidgetId), algorithm);
            return mutablePreferences;
        });
        Futures.addCallback(future, new FutureCallback<Preferences>() {
            @Override
            public void onSuccess(Preferences result) {
                callback.run();
            }

            @Override
            public void onFailure(Throwable t) {
                callback.run();
            }
        }, executor);
    }

//    resets all maze settings at once, so the widgets are only updated after every setting is gone
    public static void resetMazeSettings(Runnable callback) {
        ListenableFuture<Preferences> future = getDataStore().updateDataAsync(preferences -> {
            MutablePreferences mutablePreferences = preferences.toMutablePreferences();
            mutablePreferences.remove(MAZE_CELLS);
            mutablePreferences.remove(MAZE_STYLE);
            mutablePreferences.remove(MAZE_THEME);
            mutablePreferences.remove(MAZE_SPEED);
            return mutablePreferences;
        });
        Futures.addCallback(future, new FutureCallback<Preferences>() {
            @Override
            public void onSuccess(Preferences result) {
                callback.run();
            }

            @Override
            public void onFailure(Throwable t) {
                callback.run();
            }
        }, executor);
    }

//    removes everything that was saved for one widget
    public static void removeWidgetSettings(int appWidgetId) {
        removeKeysStartingWith(WIDGET_KEY_PREFIX + appWidgetId + "_");
    }

//    removes everything that was saved for any widget
    public static void removeAllWidgetSettings() {
        removeKeysStartingWith(WIDGET_KEY_PREFIX);
    }

    private static void removeKeysStartingWith(String prefix) {
        getDataStore().updateDataAsync(preferences -> {
            MutablePreferences mutablePreferences = preferences.toMutablePreferences();
            for (Preferences.Key<?> key : preferences.asMap().keySet()) {
                if (key.getName().startsWith(prefix)) {
                    mutablePreferences.remove(key);
                }
            }
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

//    the value of this widget, or else the shared value, or else the defaultValue
    public static <T> void readWidget(Preferences.Key<T> key, int appWidgetId, T defaultValue, Callback<T> callback) {
        ListenableFuture<Preferences> future = getDataStore().getDataAsync();
        Futures.addCallback(future, new FutureCallback<Preferences>() {
            @Override
            public void onSuccess(Preferences preferences) {
                T value = widgetValue(preferences, key, appWidgetId);
                callback.onResult(value != null ? value : defaultValue);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onResult(defaultValue);
            }
        }, executor);
    }

    public static <T> void writeWidget(Preferences.Key<T> key, int appWidgetId, T value, Callback<T> callback) {
        write(widgetKey(key, appWidgetId), value, callback);
    }

    public static <T> T readsyncWidget(Preferences.Key<T> key, int appWidgetId) {
        ListenableFuture<Preferences> future = getDataStore().getDataAsync();
        try {
            return widgetValue(future.get(), key, appWidgetId);
        } catch (ExecutionException | InterruptedException e) {
            return null;
        }
    }

    private static <T> T widgetValue(Preferences preferences, Preferences.Key<T> key, int appWidgetId) {
        T value = preferences.get(widgetKey(key, appWidgetId));
        return value != null ? value : preferences.get(key);
    }

    public static String readsyncImages(int appWidgetId) {
        return readsyncWidget(UPLOADEDIMAGES, appWidgetId);
    }

    public static String readsyncGeneratedImages() {
        return readsync(GENERATED_IMAGES);
    }

    public static <T> T readsync(Preferences.Key<T> key) {
        ListenableFuture<Preferences> future = getDataStore().getDataAsync();
        Preferences preferences;
        try {
            preferences = future.get();
        } catch (ExecutionException | InterruptedException e) {
            return null;
        }

        return preferences.get(key);
    }

//    waits until the value is on disc, never call this on the main thread or from a callback
    public static <T> void writesync(Preferences.Key<T> key, T value) {
        ListenableFuture<Preferences> future = getDataStore().updateDataAsync(preferences -> {
            MutablePreferences mutablePreferences = preferences.toMutablePreferences();
            mutablePreferences.set(key, value);
            return mutablePreferences;
        });
        try {
            future.get();
            notifyChanged();
        } catch (ExecutionException | InterruptedException e) {
            // same as readsync: the caller goes on with what it has
        }
    }

    public static List<String> deserializeString(String string) {
        Gson gson = new Gson();

        if (string != null) {

            Type listType = new TypeToken<ArrayList<String>>() {}.getType();
            return gson.fromJson(string, listType);

        } else {
            return new ArrayList<>();
        }
    }

    public static <T> void resetData(Preferences.Key<T> key) {
        getDataStore().updateDataAsync(preferences -> {
            MutablePreferences mutablePreferences = preferences.toMutablePreferences();
            mutablePreferences.set(key, null);
            return mutablePreferences;
        });
    }
}
