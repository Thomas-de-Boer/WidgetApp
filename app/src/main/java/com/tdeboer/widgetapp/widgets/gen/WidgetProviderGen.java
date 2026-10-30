package com.tdeboer.widgetapp.widgets.gen;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import com.tdeboer.widgetapp.SettingsManager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class WidgetProviderGen extends AppWidgetProvider {
    private static final String ACTION_ROTATE_GENERATED = "com.tdeboer.widgetapp.ROTATE_GENERATED";
    private static final String ACTION_RETRY_GENERATED = "com.tdeboer.widgetapp.RETRY_GENERATED";
    // a non-wakeup alarm: it only fires while the phone is awake anyway, so it never wakes it up just for the widget
    private static final long ROTATION_PERIOD_MILLIS = 30 * 60 * 1000L;
    private static final long RETRY_PERIOD_MILLIS = 60 * 1000L;
    // the widgets that are being generated, and those that changed meanwhile and need another round
    private static final Set<Integer> GENERATING = ConcurrentHashMap.newKeySet();
    private static final Map<Integer, NextRound> WAITING = new ConcurrentHashMap<>();
    private static volatile Runnable settingsRandomizedListener;

    private static final class NextRound {
        final boolean randomize;
        final Runnable onFinished;

        NextRound(boolean randomize, Runnable onFinished) {
            this.randomize = randomize;
            this.onFinished = onFinished;
        }
    }

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

    public static void update(Context context, int appWidgetId) {
        GenerationJobService.schedule(context, appWidgetId, true);
    }

    public static void updateWithCurrentSettings(Context context, int appWidgetId) {
        GenerationJobService.schedule(context, appWidgetId, false);
    }

    public static void updateAllWithCurrentSettings(Context context) {
        updateWidgets(context, AppWidgetManager.getInstance(context), false);
    }

    // called by the job, onFinished runs when the frames of the widget are ready
    static void generateNow(Context context, int appWidgetId, boolean randomize, Runnable onFinished) {
        updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId, randomize, onFinished);
    }

    public static void setSettingsRandomizedListener(Runnable listener) {
        settingsRandomizedListener = listener;
    }

    private static void updateWidgets(Context context, AppWidgetManager manager, boolean randomize) {
        for (int appWidgetId : allWidgetIds(context, manager)) {
            GenerationJobService.schedule(context, appWidgetId, randomize);
        }
    }

    private static void updateWidget(Context context, AppWidgetManager manager, int appWidgetId, boolean randomize,
                                     Runnable onFinished) {
        SettingsManager.readWidget(SettingsManager.GENERATED_SCALE, appWidgetId, 5f, scale ->
                SettingsManager.readWidget(SettingsManager.GENERATED_OCTAVES, appWidgetId, 4, octaves ->
                        SettingsManager.readWidget(SettingsManager.GENERATED_PALETTE, appWidgetId, 0, palette ->
                                SettingsManager.readWidget(SettingsManager.GENERATED_ALGORITHM, appWidgetId, 0, algorithm ->
                                SettingsManager.readWidget(SettingsManager.GENERATED_SPEED, appWidgetId, 1, speed ->
                                SettingsManager.readWidget(SettingsManager.GENERATED_RANDOM, appWidgetId, false, randomEnabled -> {
                                            if (isThermalBusy(context)) {
                                                scheduleRetry(context);
                                                onFinished.run();
                                                return;
                                            }
                                            if (!GENERATING.add(appWidgetId)) {
                                                // this job stays open, a replaced job can't keep the app awake anymore
                                                NextRound replaced = WAITING.put(appWidgetId, new NextRound(randomize, onFinished));
                                                if (replaced != null) {
                                                    replaced.onFinished.run();
                                                }
                                                return;
                                            }
                                            if (randomize && randomEnabled) {
                                                ThreadLocalRandom random = ThreadLocalRandom.current();
                                                float randomScale = random.nextInt(1, 13);
                                                int randomOctaves = random.nextInt(1, 7);
                                                int randomSpeed = random.nextInt(1, 6);
                                                int randomPalette = random.nextInt(0, 9);
                                                int randomAlgorithm = random.nextInt(0, 10);
                                                SettingsManager.writeGeneratedSettings(appWidgetId, randomScale, randomOctaves,
                                                        randomSpeed, randomPalette, randomAlgorithm, () -> {
                                                            Runnable listener = settingsRandomizedListener;
                                                            if (listener != null) {
                                                                listener.run();
                                                            }
                                                            generate(context, manager, appWidgetId, randomScale, randomOctaves,
                                                                    randomPalette, randomAlgorithm, randomSpeed, onFinished);
                                                        });
                                            } else {
                                                generate(context, manager, appWidgetId, scale, octaves, palette, algorithm,
                                                        speed, onFinished);
                                            }
                                        }))))));
    }

    private static void generate(Context context, AppWidgetManager manager, int appWidgetId, float scale,
                                 int octaves, int palette, int algorithm, int speed, Runnable onFinished) {
        long generationSeed = ThreadLocalRandom.current().nextLong();
        GeneratedImageStore.generate(context, appWidgetId, scale, octaves, palette, algorithm, speed, generationSeed,
                ignored -> {
                    GENERATING.remove(appWidgetId);
                    updateViews(context, manager, appWidgetId);
                    NextRound next = WAITING.remove(appWidgetId);
                    if (next != null) {
                        updateWidget(context, manager, appWidgetId, next.randomize, next.onFinished);
                    }
                    onFinished.run();
                });
    }

    private static void updateViews(Context context, AppWidgetManager manager, int appWidgetId) {
        manager.updateAppWidget(appWidgetId, WidgetHelperGen.makeView(context, appWidgetId));
        cancelPerWidgetAlarms(context, new int[]{appWidgetId});
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
        for (int appWidgetId : appWidgetIds) {
            WAITING.remove(appWidgetId);
            GeneratedImageStore.deleteForWidget(context, appWidgetId);
            SettingsManager.removeWidgetSettings(appWidgetId);
        }
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
