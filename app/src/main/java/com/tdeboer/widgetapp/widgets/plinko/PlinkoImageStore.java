package com.tdeboer.widgetapp.widgets.plinko;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.Log;

import com.tdeboer.widgetapp.SettingsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

// keeps the game of every plinko widget and draws its frames.
// all work runs on one thread, so the games are only touched there
final class PlinkoImageStore {
    // the first part after a tap is short, so the new ball shows up quickly
    static final int FIRST_CHUNK_FRAMES = 15;
    static final int CHUNK_FRAMES = 30;
    // extra frames after a chunk, shown when the next chunk arrives a little late
    static final int OVERLAP_FRAMES = 8;
    // a ball that is dropped while others fall starts this many frames later, which is the time drawing takes
    static final int LEAD_FRAMES = 12;
    private static final int KEEP_FRAMES = 2 * (CHUNK_FRAMES + OVERLAP_FRAMES);
    private static final String DIRECTORY = "plinko_widget";
    private static final String TAG = "PlinkoImageStore";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Map<Integer, Game> GAMES = new HashMap<>();
    // taps that wait for this thread. drawing ahead stops for them, so a new ball never waits for frames
    // that the tap is going to replace anyway
    private static final AtomicInteger WAITING_DROPS = new AtomicInteger();

    private PlinkoImageStore() {
    }

    private static final class Game {
        // the game at the frame where the part that is on screen starts
        PlinkoBoard board = new PlinkoBoard(ThreadLocalRandom.current().nextLong());
        // the frames of one set belong to one future. a tap changes the future, so it starts a new set
        String set;
        String previousSet;
    }

    static final class Chunk {
        final int start;
        // the frames to play before the next chunk, the frames after those are the overlap
        final int length;
        final List<String> frames;
        // one image for when nothing moves, null if it could not be written
        final String still;
        // true when there is nothing left to play
        final boolean finished;

        Chunk(int start, int length, List<String> frames, String still, boolean finished) {
            this.start = start;
            this.length = length;
            this.frames = frames;
            this.still = still;
            this.finished = finished;
        }
    }

    // drops a ball. playing tells if the widget is animating, nowFrame is the frame it shows at this moment
    static void drop(Context context, int appWidgetId, boolean playing, int nowFrame,
                     SettingsManager.Callback<Chunk> callback) {
        WAITING_DROPS.incrementAndGet();
        EXECUTOR.execute(() -> {
            WAITING_DROPS.decrementAndGet();
            Game game = game(appWidgetId);
            int start = playing ? Math.max(game.board.frame(), nowFrame + LEAD_FRAMES) : game.board.frame();
            game.board.advanceTo(start);
            game.board.drop();
            newSet(context, appWidgetId, game);
            saveBest(appWidgetId, game);

            long startTime = SystemClock.elapsedRealtime();
            List<String> frames = renderFrames(context, appWidgetId, game, start, FIRST_CHUNK_FRAMES + OVERLAP_FRAMES, false);
            Log.d(TAG, "Drew " + frames.size() + " frames in " + (SystemClock.elapsedRealtime() - startTime) + " ms");
            callback.onResult(new Chunk(start, FIRST_CHUNK_FRAMES, frames, first(frames), frames.isEmpty()));
            // ready before it is needed: drawing is faster than playing
            renderFrames(context, appWidgetId, game, start + FIRST_CHUNK_FRAMES, CHUNK_FRAMES + OVERLAP_FRAMES, true);
        });
    }

    // the chunk that starts at this frame. with pause, or without balls, the chunk is only a still image
    static void loadChunk(Context context, int appWidgetId, int start, boolean pause,
                          SettingsManager.Callback<Chunk> callback) {
        EXECUTOR.execute(() -> {
            Game game = game(appWidgetId);
            game.board.advanceTo(start);
            saveBest(appWidgetId, game);
            deleteFrames(context, game.set, start - KEEP_FRAMES);

            if (pause || game.board.isEmpty()) {
                List<String> still = renderFrames(context, appWidgetId, game, game.board.frame(), 1, false);
                callback.onResult(new Chunk(start, 0, new ArrayList<>(), first(still), true));
                return;
            }
            List<String> frames = renderFrames(context, appWidgetId, game, start, CHUNK_FRAMES + OVERLAP_FRAMES, false);
            callback.onResult(new Chunk(start, CHUNK_FRAMES, frames, first(frames), frames.isEmpty()));
            renderFrames(context, appWidgetId, game, start + CHUNK_FRAMES, CHUNK_FRAMES + OVERLAP_FRAMES, true);
        });
    }

    // draws the game as it is now with the current settings. the callback gets null if that failed
    static void still(Context context, int appWidgetId, boolean resetBest, SettingsManager.Callback<String> callback) {
        EXECUTOR.execute(() -> {
            Game game = game(appWidgetId);
            if (resetBest) {
                SettingsManager.writesync(SettingsManager.plinkoBestKey(appWidgetId), 0);
                game.board.resetPeak();
            }
            newSet(context, appWidgetId, game);
            callback.onResult(first(renderFrames(context, appWidgetId, game, game.board.frame(), 1, false)));
        });
    }

    static void deleteForWidget(Context context, int appWidgetId) {
        EXECUTOR.execute(() -> {
            GAMES.remove(appWidgetId);
            deleteSets(context, appWidgetId, null, null);
            SettingsManager.resetData(SettingsManager.plinkoBestKey(appWidgetId));
            SettingsManager.removeWidgetSettings(appWidgetId);
        });
    }

    static void deleteAll(Context context) {
        EXECUTOR.execute(() -> {
            GAMES.clear();
            File[] files = directory(context).listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        });
    }

    private static Game game(int appWidgetId) {
        Game game = GAMES.get(appWidgetId);
        if (game == null) {
            game = new Game();
            game.set = prefix(appWidgetId) + UUID.randomUUID();
            GAMES.put(appWidgetId, game);
        }
        return game;
    }

    // the launcher can still show frames of the previous set, so only the sets before that are deleted
    private static void newSet(Context context, int appWidgetId, Game game) {
        game.previousSet = game.set;
        game.set = prefix(appWidgetId) + UUID.randomUUID();
        deleteSets(context, appWidgetId, game.set, game.previousSet);
    }

    private static void saveBest(int appWidgetId, Game game) {
        if (game.board.peak() > readBest(appWidgetId)) {
            SettingsManager.writesync(SettingsManager.plinkoBestKey(appWidgetId), game.board.peak());
        }
    }

    private static int readBest(int appWidgetId) {
        Integer best = SettingsManager.readsync(SettingsManager.plinkoBestKey(appWidgetId));
        return best != null ? best : 0;
    }

    static int readTheme(int appWidgetId) {
        Integer theme = SettingsManager.readsyncWidget(SettingsManager.PLINKO_THEME, appWidgetId);
        return theme != null ? theme : 0;
    }

    // the names of the frames from start on. frames that already exist are not drawn again.
    // the list is empty if the frames could not be written. ahead is for frames that are not needed yet
    private static List<String> renderFrames(Context context, int appWidgetId, Game game, int start, int count,
                                             boolean ahead) {
        List<String> names = new ArrayList<>();
        File directory = directory(context);
        if (!directory.exists() && !directory.mkdirs()) {
            return names;
        }

        PlinkoBoard board = game.board.copy();
        board.advanceTo(start);
        int best = readBest(appWidgetId);
        PlinkoRenderer renderer = null;
        Bitmap bitmap = null;
        try {
            for (int frame = start; frame < start + count; frame++) {
                if (ahead && WAITING_DROPS.get() > 0) {
                    break;
                }
                String name = game.set + "_" + frame + ".png";
                if (!new File(directory, name).exists()) {
                    if (renderer == null) {
                        renderer = new PlinkoRenderer(readTheme(appWidgetId));
                        bitmap = Bitmap.createBitmap(PlinkoRenderer.WIDTH, PlinkoRenderer.HEIGHT, Bitmap.Config.ARGB_8888);
                    }
                    renderer.drawFrame(board, best, new Canvas(bitmap));
                    writeFrame(directory, name, bitmap);
                }
                names.add(name);
                board.step();
            }
            return names;
        } catch (IOException e) {
            Log.w(TAG, "Could not write plinko frame", e);
            return new ArrayList<>();
        } finally {
            if (renderer != null) {
                renderer.recycle();
                bitmap.recycle();
            }
        }
    }

    // written under a temporary name first, so the launcher never reads a half written file
    private static void writeFrame(File directory, String name, Bitmap bitmap) throws IOException {
        File temporary = new File(directory, name + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temporary)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw new IOException("Could not compress plinko image");
            }
        }
        if (!temporary.renameTo(new File(directory, name))) {
            throw new IOException("Could not commit plinko image");
        }
    }

    private static String first(List<String> names) {
        return names.isEmpty() ? null : names.get(0);
    }

    // deletes the frames of this set that were played a while ago
    private static void deleteFrames(Context context, String set, int beforeFrame) {
        File[] files = directory(context).listFiles((dir, name) ->
                name.startsWith(set + "_") && name.endsWith(".png") && frameNumber(name) < beforeFrame);
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    // the sets to keep can be null
    private static void deleteSets(Context context, int appWidgetId, String keepSet, String alsoKeepSet) {
        File[] files = directory(context).listFiles((dir, name) ->
                name.startsWith(prefix(appWidgetId))
                        && (keepSet == null || !name.startsWith(keepSet + "_"))
                        && (alsoKeepSet == null || !name.startsWith(alsoKeepSet + "_")));
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    private static File directory(Context context) {
        return new File(context.getFilesDir(), DIRECTORY);
    }

    private static String prefix(int appWidgetId) {
        return "plinko_" + appWidgetId + "_";
    }

    private static int frameNumber(String fileName) {
        return Integer.parseInt(fileName.substring(fileName.lastIndexOf('_') + 1, fileName.lastIndexOf('.')));
    }
}
