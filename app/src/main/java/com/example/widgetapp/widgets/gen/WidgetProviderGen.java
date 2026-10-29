package com.example.widgetapp.widgets.gen;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import com.example.widgetapp.SettingsManager;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class WidgetProviderGen extends AppWidgetProvider {
    private static final String ACTION_ROTATE_GENERATED = "com.example.widgetapp.ROTATE_GENERATED";
    private static final String ACTION_RETRY_GENERATED = "com.example.widgetapp.RETRY_GENERATED";
    // a non-wakeup alarm: it only fires while the phone is awake anyway, so it never wakes it up just for the widget
    private static final long ROTATION_PERIOD_MILLIS = 30 * 60 * 1000L;
    private static final long RETRY_PERIOD_MILLIS = 60 * 1000L;
    private static final AtomicBoolean GENERATION_IN_PROGRESS = new AtomicBoolean();
    private static volatile Runnable settingsRandomizedListener;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        updateWidgets(context, appWidgetManager, true);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_ROTATE_GENERATED.equals(intent.getAction())
                || ACTION_RETRY_GENERATED.equals(intent.getAction())) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            updateWidgets(context, manager, true);
        }
    }

    public static void update(Context context) {
        updateWidgets(context, AppWidgetManager.getInstance(context), true);
    }

    public static void updateWithCurrentSettings(Context context) {
        updateWidgets(context, AppWidgetManager.getInstance(context), false);
    }

    public static void setSettingsRandomizedListener(Runnable listener) {
        settingsRandomizedListener = listener;
    }

    private static void updateWidgets(Context context, AppWidgetManager manager,
                                      boolean randomize) {
        SettingsManager.read(SettingsManager.GENERATED_SCALE, 5f, scale ->
                SettingsManager.read(SettingsManager.GENERATED_OCTAVES, 4, octaves ->
                        SettingsManager.read(SettingsManager.GENERATED_PALETTE, 0, palette ->
                                SettingsManager.read(SettingsManager.GENERATED_ALGORITHM, 0, algorithm ->
                                SettingsManager.read(SettingsManager.GENERATED_SPEED, 1, speed ->
                                SettingsManager.read(SettingsManager.GENERATED_RANDOM, false, randomEnabled ->
                                SettingsManager.read(SettingsManager.GENERATED_IMAGES, null, imageJson -> {
                                            if (isThermalBusy(context) || !GENERATION_IN_PROGRESS.compareAndSet(false, true)) {
                                                scheduleRetry(context);
                                                return;
                                            }
                                            List<String> oldFiles = SettingsManager.deserializeString(imageJson);
                                            if (randomize && randomEnabled) {
                                                ThreadLocalRandom random = ThreadLocalRandom.current();
                                                float randomScale = random.nextInt(1, 13);
                                                int randomOctaves = random.nextInt(1, 7);
                                                int randomSpeed = random.nextInt(1, 6);
                                                int randomPalette = random.nextInt(0, 9);
                                                int randomAlgorithm = random.nextInt(0, 10);
                                                SettingsManager.writeGeneratedSettings(randomScale, randomOctaves, randomSpeed,
                                                        randomPalette, randomAlgorithm, () -> {
                                                            Runnable listener = settingsRandomizedListener;
                                                            if (listener != null) {
                                                                listener.run();
                                                            }
                                                            generate(context, manager, randomScale, randomOctaves,
                                                                    randomPalette, randomAlgorithm, randomSpeed, oldFiles);
                                                        });
                                            } else {
                                                generate(context, manager, scale, octaves, palette, algorithm,
                                                        speed, oldFiles);
                                            }
                                        })))))));
    }

    private static void generate(Context context, AppWidgetManager manager, float scale,
                                 int octaves, int palette, int algorithm, int speed, List<String> oldFiles) {
        long generationSeed = ThreadLocalRandom.current().nextLong();
        GeneratedImageStore.generate(context, scale, octaves, palette, algorithm, speed, generationSeed,
                oldFiles,
                ignored -> {
                    GENERATION_IN_PROGRESS.set(false);
                    updateViews(context, manager);
                });
    }

    // every gen widget shows the same frames, so they are always updated together
    private static void updateViews(Context context, AppWidgetManager manager) {
        int[] appWidgetIds = allWidgetIds(context, manager);
        for (int appWidgetId : appWidgetIds) {
            manager.updateAppWidget(appWidgetId, WidgetHelperGen.makeView(context));
        }
        cancelPerWidgetAlarms(context, appWidgetIds);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(alarmIntent(context, ACTION_RETRY_GENERATED));
        alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                android.os.SystemClock.elapsedRealtime() + ROTATION_PERIOD_MILLIS,
                ROTATION_PERIOD_MILLIS,
                alarmIntent(context, ACTION_ROTATE_GENERATED)
        );
    }

    private static int[] allWidgetIds(Context context, AppWidgetManager manager) {
        return manager.getAppWidgetIds(new ComponentName(context, WidgetProviderGen.class));
    }

    private static boolean isThermalBusy(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false;
        }
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return powerManager != null
                && powerManager.getCurrentThermalStatus() >= PowerManager.THERMAL_STATUS_SEVERE;
    }

    private static void scheduleRetry(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(
                AlarmManager.ELAPSED_REALTIME,
                android.os.SystemClock.elapsedRealtime() + RETRY_PERIOD_MILLIS,
                alarmIntent(context, ACTION_RETRY_GENERATED)
        );
    }

    private static PendingIntent alarmIntent(Context context, String action) {
        Intent intent = new Intent(context, WidgetProviderGen.class).setAction(action);
        return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    // older versions scheduled one alarm per widget, with the widget id as request code
    private static void cancelPerWidgetAlarms(Context context, int[] appWidgetIds) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        for (int appWidgetId : appWidgetIds) {
            for (String action : new String[]{ACTION_ROTATE_GENERATED, ACTION_RETRY_GENERATED}) {
                alarmManager.cancel(PendingIntent.getBroadcast(context, appWidgetId,
                        new Intent(context, WidgetProviderGen.class).setAction(action),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            }
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        cancelPerWidgetAlarms(context, appWidgetIds);
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(alarmIntent(context, ACTION_ROTATE_GENERATED));
        alarmManager.cancel(alarmIntent(context, ACTION_RETRY_GENERATED));
        super.onDisabled(context);
    }
}
