package com.tdeboer.widgetapp.widgets.maze;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WidgetProviderMaze extends AppWidgetProvider {
    static final String ACTION_NEW_MAZE = "com.tdeboer.widgetapp.NEW_MAZE";
    static final String ACTION_SOLVE = "com.tdeboer.widgetapp.SOLVE_MAZE";
    private static final String ACTION_SOLVE_CHUNK = "com.tdeboer.widgetapp.SOLVE_MAZE_CHUNK";
    private static final String ACTION_ROTATE_MAZE = "com.tdeboer.widgetapp.ROTATE_MAZE";
    private static final String EXTRA_SOLVE = "maze_solve";
    private static final String EXTRA_CHUNK = "maze_chunk";
    // a non-wakeup alarm: it only fires while the phone is awake anyway, so it never wakes it up just for the widget
    private static final long ROTATION_PERIOD_MILLIS = 30 * 60 * 1000L;
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    // the running animation per widget; removing an entry cancels the rest of it
    private static final Map<Integer, Integer> ACTIVE_SOLVES = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_SOLVE = new AtomicInteger();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int appWidgetId : appWidgetIds) {
            newMaze(context, appWidgetManager, appWidgetId);
        }
        // setting it again here restores the alarm after a reboot, which clears all alarms
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + ROTATION_PERIOD_MILLIS,
                ROTATION_PERIOD_MILLIS,
                rotateIntent(context)
        );
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (ACTION_ROTATE_MAZE.equals(intent.getAction())) {
            update(context);
        } else if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return;
        } else if (ACTION_NEW_MAZE.equals(intent.getAction())) {
            newMaze(context, manager, appWidgetId);
        } else if (ACTION_SOLVE.equals(intent.getAction())) {
            solve(context, manager, appWidgetId);
        } else if (ACTION_SOLVE_CHUNK.equals(intent.getAction())) {
            int solve = intent.getIntExtra(EXTRA_SOLVE, 0);
            if (isActive(appWidgetId, solve)) {
                playChunk(context, manager, appWidgetId, solve, intent.getIntExtra(EXTRA_CHUNK, 0));
            }
        }
    }

    // makes a new maze for every maze widget
    public static void update(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        for (int appWidgetId : allWidgetIds(context, manager)) {
            newMaze(context, manager, appWidgetId);
        }
    }

    // keeps the current mazes and draws them again with the current theme and speed
    public static void redraw(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        for (int appWidgetId : allWidgetIds(context, manager)) {
            ACTIVE_SOLVES.remove(appWidgetId);
            MazeImageStore.redraw(context, appWidgetId, () -> showStill(context, manager, appWidgetId));
        }
    }

    private static void newMaze(Context context, AppWidgetManager manager, int appWidgetId) {
        ACTIVE_SOLVES.remove(appWidgetId);
        MazeImageStore.newMaze(context, appWidgetId, () -> showStill(context, manager, appWidgetId));
    }

    private static void showStill(Context context, AppWidgetManager manager, int appWidgetId) {
        manager.updateAppWidget(appWidgetId, WidgetHelperMaze.makeStillView(context, appWidgetId));
    }

    private void solve(Context context, AppWidgetManager manager, int appWidgetId) {
        int solve = NEXT_SOLVE.incrementAndGet();
        if (ACTIVE_SOLVES.putIfAbsent(appWidgetId, solve) == null) {
            playChunk(context, manager, appWidgetId, solve, 0);
        }
    }

    // goAsync keeps the app alive for about 10 seconds, so every chunk is its own broadcast.
    // that way a long route can take as long as it needs
    private void playChunk(Context context, AppWidgetManager manager, int appWidgetId, int solve, int chunkIndex) {
        PendingResult result = goAsync();
        MazeImageStore.loadChunk(context, appWidgetId, chunkIndex, chunk -> {
            if (!isActive(appWidgetId, solve) || chunk.frames.isEmpty()) {
                ACTIVE_SOLVES.remove(appWidgetId, solve);
                result.finish();
                return;
            }
            manager.updateAppWidget(appWidgetId,
                    WidgetHelperMaze.makeAnimatedView(context, appWidgetId, chunk.frames));
            HANDLER.postDelayed(() -> {
                if (chunk.last) {
                    finishSolve(context, manager, appWidgetId, solve, result);
                } else {
                    if (isActive(appWidgetId, solve)) {
                        context.sendBroadcast(nextChunkIntent(context, appWidgetId, solve, chunkIndex + 1));
                    }
                    result.finish();
                }
            }, WidgetHelperMaze.chunkMillis(chunk.frames.size()));
        });
    }

    // the flipper loops forever, so replace it with the still solved image when the path is complete
    private static void finishSolve(Context context, AppWidgetManager manager, int appWidgetId, int solve,
                                    PendingResult result) {
        MazeImageStore.run(() -> {
            try {
                if (ACTIVE_SOLVES.remove(appWidgetId, solve)) {
                    showStill(context, manager, appWidgetId);
                }
            } finally {
                result.finish();
            }
        });
    }

    private static boolean isActive(int appWidgetId, int solve) {
        Integer active = ACTIVE_SOLVES.get(appWidgetId);
        return active != null && active == solve;
    }

    private static Intent nextChunkIntent(Context context, int appWidgetId, int solve, int chunkIndex) {
        return new Intent(context, WidgetProviderMaze.class)
                .setAction(ACTION_SOLVE_CHUNK)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                .putExtra(EXTRA_SOLVE, solve)
                .putExtra(EXTRA_CHUNK, chunkIndex);
    }

    private static int[] allWidgetIds(Context context, AppWidgetManager manager) {
        return manager.getAppWidgetIds(new ComponentName(context, WidgetProviderMaze.class));
    }

    private static PendingIntent rotateIntent(Context context) {
        Intent intent = new Intent(context, WidgetProviderMaze.class).setAction(ACTION_ROTATE_MAZE);
        return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            ACTIVE_SOLVES.remove(appWidgetId);
            MazeImageStore.deleteForWidget(context, appWidgetId);
        }
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(rotateIntent(context));
        MazeImageStore.deleteAll(context);
        super.onDisabled(context);
    }
}
