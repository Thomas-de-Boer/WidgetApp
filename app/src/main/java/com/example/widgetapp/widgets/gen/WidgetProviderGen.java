package com.example.widgetapp.widgets.gen;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.os.Build;
import android.os.PowerManager;
import android.widget.RemoteViews;

import com.example.widgetapp.SettingsManager;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class WidgetProviderGen extends AppWidgetProvider {
    private static final String ACTION_ROTATE_GENERATED = "com.example.widgetapp.ROTATE_GENERATED";
    private static final String ACTION_RETRY_GENERATED = "com.example.widgetapp.RETRY_GENERATED";
    private static final long ROTATION_PERIOD_MILLIS = 10 * 60 * 1000L;
    private static final long RETRY_PERIOD_MILLIS = 60 * 1000L;
    private static final AtomicBoolean GENERATION_IN_PROGRESS = new AtomicBoolean();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        updateWidgets(context, appWidgetManager, appWidgetIds, true);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_ROTATE_GENERATED.equals(intent.getAction())
                || ACTION_RETRY_GENERATED.equals(intent.getAction())) {
            int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateWidgets(context, AppWidgetManager.getInstance(context), new int[]{widgetId}, true);
            }
        }
    }

    public static void update(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        updateWidgets(context, manager, appWidgetIds, true);
    }

    public static void updatePlayback(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        updateWidgets(context, manager, appWidgetIds, false);
    }

    private static void updateWidgets(Context context, AppWidgetManager manager, int[] appWidgetIds,
                                      boolean regenerate) {
        SettingsManager.read(SettingsManager.GENERATED_SCALE, 5f, scale ->
                SettingsManager.read(SettingsManager.GENERATED_OCTAVES, 4, octaves ->
                        SettingsManager.read(SettingsManager.GENERATED_PALETTE, 0, palette ->
                                SettingsManager.read(SettingsManager.GENERATED_ALGORITHM, 0, algorithm ->
                                SettingsManager.read(SettingsManager.GENERATED_SPEED, 1, speed ->
                                SettingsManager.read(SettingsManager.GENERATED_DURATION, 10, duration ->
                                SettingsManager.read(SettingsManager.GENERATED_IMAGES, null, imageJson -> {
                                            long generationSeed = ThreadLocalRandom.current().nextLong();
                                            if (regenerate) {
                                                if (isThermalBusy(context) || !GENERATION_IN_PROGRESS.compareAndSet(false, true)) {
                                                    scheduleRetry(context, appWidgetIds);
                                                    return;
                                                }
                                                List<String> oldFiles = SettingsManager.deserializeString(imageJson);
                                                GeneratedImageStore.generate(context, scale, octaves, palette, algorithm, speed, generationSeed,
                                                        oldFiles,
                                                        ignored -> {
                                                            GENERATION_IN_PROGRESS.set(false);
                                                            updateViews(context, manager, appWidgetIds, duration);
                                                        });
                                            } else {
                                                updateViews(context, manager, appWidgetIds, duration);
                                            }
                                        })))))));
    }

    private static void updateViews(Context context, AppWidgetManager manager, int[] appWidgetIds, int duration) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = WidgetHelperGen.makeView(context, duration);
            manager.updateAppWidget(appWidgetId, views);
            manager.notifyAppWidgetViewDataChanged(appWidgetId, com.example.widgetapp.R.id.widget_gen_flipper);
            scheduleRotation(context, appWidgetId);
        }
    }

    private static boolean isThermalBusy(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false;
        }
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return powerManager != null
                && powerManager.getCurrentThermalStatus() >= PowerManager.THERMAL_STATUS_SEVERE;
    }

    private static void scheduleRetry(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Intent intent = new Intent(context, WidgetProviderGen.class)
                    .setAction(ACTION_RETRY_GENERATED)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(
                    AlarmManager.ELAPSED_REALTIME,
                    android.os.SystemClock.elapsedRealtime() + RETRY_PERIOD_MILLIS,
                    pendingIntent
            );
        }
    }

    private static void scheduleRotation(Context context, int appWidgetId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent retryIntent = new Intent(context, WidgetProviderGen.class)
                .setAction(ACTION_RETRY_GENERATED)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent retryPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                retryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(retryPendingIntent);

        Intent intent = new Intent(context, WidgetProviderGen.class)
                .setAction(ACTION_ROTATE_GENERATED)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                android.os.SystemClock.elapsedRealtime() + ROTATION_PERIOD_MILLIS,
                ROTATION_PERIOD_MILLIS,
                pendingIntent
        );
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Intent retryIntent = new Intent(context, WidgetProviderGen.class)
                    .setAction(ACTION_RETRY_GENERATED)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent retryPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    retryIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Intent intent = new Intent(context, WidgetProviderGen.class)
                    .setAction(ACTION_ROTATE_GENERATED);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(retryPendingIntent);
            alarmManager.cancel(pendingIntent);
        }
        super.onDeleted(context, appWidgetIds);
    }
}
