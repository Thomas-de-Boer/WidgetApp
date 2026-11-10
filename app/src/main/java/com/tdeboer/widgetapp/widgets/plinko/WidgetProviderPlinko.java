package com.tdeboer.widgetapp.widgets.plinko;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.widget.RemoteViews;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// the widget can't move balls by itself, so the game is played as a film: the frames are drawn here
// and the launcher flips through them. the film is made in chunks, and every tap makes a new film
// from the frame that is on screen at that moment
public class WidgetProviderPlinko extends AppWidgetProvider {
    static final String ACTION_DROP = "com.tdeboer.widgetapp.PLINKO_DROP";
    private static final String ACTION_CHUNK = "com.tdeboer.widgetapp.PLINKO_CHUNK";
    private static final String EXTRA_RUN = "plinko_run";
    private static final String EXTRA_START = "plinko_start";
    private static final String EXTRA_DUE = "plinko_due";
    // the time the launcher needs to show a new chunk
    private static final long APPLY_MILLIS = 40L;
    private static final long FLIP = PlinkoRenderer.FLIP_INTERVAL_MILLIS;
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    // the running animation per widget; removing an entry cancels the rest of it
    private static final Map<Integer, Run> RUNS = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_RUN = new AtomicInteger();
    // how much earlier than needed the next chunk is asked for, it follows how late the chunks really are
    private static volatile long swapLead = 80L;

    // which chunk the launcher is playing, to know the frame that is on screen
    private static final class Run {
        final int id;
        final int start;
        final long shownAt;
        // 0 as long as nothing of this run is on screen
        final int frames;

        Run(int id, int start, long shownAt, int frames) {
            this.id = id;
            this.start = start;
            this.shownAt = shownAt;
            this.frames = frames;
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        for (int appWidgetId : appWidgetIds) {
            showStill(context, appWidgetId, false);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return;
        }
        if (ACTION_DROP.equals(intent.getAction())) {
            drop(context, manager, appWidgetId);
        } else if (ACTION_CHUNK.equals(intent.getAction())) {
            int run = intent.getIntExtra(EXTRA_RUN, 0);
            if (isActive(appWidgetId, run)) {
                playChunk(context, manager, appWidgetId, run, intent.getIntExtra(EXTRA_START, 0),
                        intent.getLongExtra(EXTRA_DUE, 0L));
            }
        }
    }

    // draws every plinko widget again with the current settings
    public static void update(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        for (int appWidgetId : manager.getAppWidgetIds(new ComponentName(context, WidgetProviderPlinko.class))) {
            showStill(context, appWidgetId, false);
        }
    }

    // puts every plinko widget back at level 1
    public static void resetAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        for (int appWidgetId : manager.getAppWidgetIds(new ComponentName(context, WidgetProviderPlinko.class))) {
            showStill(context, appWidgetId, true);
        }
    }

    public static void update(Context context, int appWidgetId) {
        showStill(context, appWidgetId, false);
    }

    // back to level 1
    public static void resetProgress(Context context, int appWidgetId) {
        showStill(context, appWidgetId, true);
    }

    // stops the animation where it is. the balls stay and fall on when a new ball is dropped
    private static void showStill(Context context, int appWidgetId, boolean resetProgress) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        RUNS.remove(appWidgetId);
        PlinkoImageStore.still(context, appWidgetId, resetProgress, still ->
                manager.updateAppWidget(appWidgetId, WidgetHelperPlinko.makeStillView(context, appWidgetId, still)));
    }

    private void drop(Context context, AppWidgetManager manager, int appWidgetId) {
        long now = SystemClock.elapsedRealtime();
        Run old = RUNS.get(appWidgetId);
        boolean playing = old != null && old.frames > 0;
        int nowFrame = playing ? old.start + (int) Math.min(old.frames - 1, (now - old.shownAt) / FLIP) : 0;

        PendingResult result = goAsync();
        PlinkoImageStore.drop(context, appWidgetId, playing, nowFrame, chunk -> {
            // null: no ball left to drop, or an earlier tap already dropped this one too
            if (chunk == null || chunk.finished) {
                result.finish();
                return;
            }
            // from here on this tap owns the animation, the chunks that were on their way are dropped.
            // the old chunk stays on screen until the new one is shown, so its timing is kept
            int run = NEXT_RUN.incrementAndGet();
            Run current = RUNS.get(appWidgetId);
            RUNS.put(appWidgetId, current != null
                    ? new Run(run, current.start, current.shownAt, current.frames)
                    : new Run(run, 0, 0L, 0));

            RemoteViews views = WidgetHelperPlinko.makeAnimatedView(context, appWidgetId, chunk.frames);
            // the new chunk starts a few frames ahead, so it waits until the old one is that far
            long wait = playing
                    ? now + PlinkoImageStore.LEAD_FRAMES * FLIP - APPLY_MILLIS - SystemClock.elapsedRealtime()
                    : 0L;
            HANDLER.postDelayed(() -> show(context, manager, appWidgetId, run, chunk, views, result),
                    Math.max(0L, wait));
        });
    }

    // every chunk is its own broadcast, so a game can take as long as it needs
    private void playChunk(Context context, AppWidgetManager manager, int appWidgetId, int run, int start, long due) {
        PendingResult result = goAsync();
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        // nobody is looking with the screen off, so the game waits for the next tap
        boolean pause = !powerManager.isInteractive();

        PlinkoImageStore.loadChunk(context, appWidgetId, start, pause, () -> isActive(appWidgetId, run), chunk -> {
            Run current = RUNS.get(appWidgetId);
            if (chunk == null || current == null || current.id != run) {
                result.finish();
                return;
            }
            if (chunk.finished) {
                if (RUNS.remove(appWidgetId, current)) {
                    manager.updateAppWidget(appWidgetId,
                            WidgetHelperPlinko.makeStillView(context, appWidgetId, chunk.still));
                }
                result.finish();
                return;
            }
            RemoteViews views = WidgetHelperPlinko.makeAnimatedView(context, appWidgetId, chunk.frames);
            // late chunks are asked for earlier next time, early chunks later
            long late = SystemClock.elapsedRealtime() + APPLY_MILLIS - due;
            swapLead = Math.max(20L, Math.min(400L, swapLead + late / 2));
            show(context, manager, appWidgetId, run, chunk, views, result);
        });
    }

    private static void show(Context context, AppWidgetManager manager, int appWidgetId, int run,
                             PlinkoImageStore.Chunk chunk, RemoteViews views, PendingResult result) {
        Run current = RUNS.get(appWidgetId);
        if (current == null || current.id != run) {
            result.finish();
            return;
        }
        manager.updateAppWidget(appWidgetId, views);
        long shownAt = SystemClock.elapsedRealtime();
        RUNS.replace(appWidgetId, current, new Run(run, chunk.start, shownAt, chunk.frames.size()));

        // android gives the app one broadcast at a time, so a tap would wait for as long as this one is held.
        // the next chunk is asked for with a new broadcast, which also keeps the app awake during a game
        result.finish();

        long playMillis = chunk.length * FLIP;
        HANDLER.postDelayed(() -> {
            if (isActive(appWidgetId, run)) {
                context.sendBroadcast(new Intent(context, WidgetProviderPlinko.class)
                        .setAction(ACTION_CHUNK)
                        .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        .putExtra(EXTRA_RUN, run)
                        .putExtra(EXTRA_START, chunk.start + chunk.length)
                        .putExtra(EXTRA_DUE, shownAt + playMillis));
            }
        }, Math.max(0L, playMillis - swapLead));
    }

    private static boolean isActive(int appWidgetId, int run) {
        Run current = RUNS.get(appWidgetId);
        return current != null && current.id == run;
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RUNS.remove(appWidgetId);
            PlinkoImageStore.deleteForWidget(context, appWidgetId);
        }
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        RUNS.clear();
        PlinkoImageStore.deleteAll(context);
        super.onDisabled(context);
    }
}
