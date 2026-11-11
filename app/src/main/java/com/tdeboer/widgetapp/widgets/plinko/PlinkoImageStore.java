package com.tdeboer.widgetapp.widgets.plinko;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.Log;

import androidx.datastore.preferences.core.Preferences;

import com.tdeboer.widgetapp.SettingsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

// keeps the game of every plinko widget and draws its frames.
// all work runs on one thread, so the games are only touched there
final class PlinkoImageStore {
    // a ball that is dropped while others fall starts this many frames later, which is the time drawing takes
    static final int LEAD_FRAMES = 6;
    private static final int KEEP_FRAMES = 120;
    private static final String DIRECTORY = "plinko_widget";
    private static final String TAG = "PlinkoImageStore";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Map<Integer, Game> GAMES = new HashMap<>();
    // taps per widget that wait for this thread. drawing ahead stops for them, so a new ball never waits
    // for frames that the tap is going to replace anyway
    private static final Map<Integer, AtomicInteger> WAITING_TAPS = new ConcurrentHashMap<>();
    // chunks that wait for this thread. drawing ahead stops for them too: with several widgets a chunk
    // that is needed now goes before a chunk that is needed later
    private static final AtomicInteger WAITING_CHUNKS = new AtomicInteger();

    private PlinkoImageStore() {
    }

    // how many frames a chunk plays. the first chunk after a tap is short, so the new ball shows up quickly.
    // the chunks grow step by step: every chunk has to be drawn while the one before it plays
    static int chunkFrames(int index) {
        return index == 0 ? 10 : index == 1 ? 15 : 30;
    }

    // extra frames after a chunk, shown when the next chunk arrives late.
    // without them the launcher starts the chunk again, which looks like a jump back in time
    static int overlapFrames(int index) {
        return index == 0 ? 8 : index == 1 ? 12 : 20;
    }

    private static final class Game {
        // the game at the frame where the part that is on screen starts
        final PlinkoBoard board;
        // the frames of one set belong to one future. a tap changes the future, so it starts a new set
        String set;
        String previousSet;

        Game(int level, int lives) {
            board = new PlinkoBoard(ThreadLocalRandom.current().nextLong(), level, lives);
        }
    }

    static final class Chunk {
        final int start;
        // 0 for the first chunk after a tap
        final int index;
        // the frames to play before the next chunk, the frames after those are the overlap
        final int length;
        final List<String> frames;
        // one image for when nothing moves, null if it could not be written
        final String still;
        // true when there is nothing left to play
        final boolean finished;

        Chunk(int start, int index, int length, List<String> frames, String still, boolean finished) {
            this.start = start;
            this.index = index;
            this.length = length;
            this.frames = frames;
            this.still = still;
            this.finished = finished;
        }
    }

    // a tap on the widget: drops a ball if the level has one left.
    // playing tells if the widget is animating, nowFrame is the frame it shows at this moment.
    // the callback gets null when the tap changes nothing on screen
    static void drop(Context context, int appWidgetId, boolean playing, int nowFrame,
                     SettingsManager.Callback<Chunk> callback) {
        WAITING_TAPS.computeIfAbsent(appWidgetId, id -> new AtomicInteger()).incrementAndGet();
        EXECUTOR.execute(() -> {
            // fast taps are dropped together by the first of them, so the frames are drawn only once
            AtomicInteger waiting = WAITING_TAPS.get(appWidgetId);
            int taps = waiting != null ? waiting.getAndSet(0) : 0;
            if (taps == 0) {
                callback.onResult(null);
                return;
            }
            Game game = game(appWidgetId);
            int start = playing ? Math.max(game.board.frame(), nowFrame + LEAD_FRAMES) : game.board.frame();
            PlinkoBoard board = game.board.copy();
            board.advanceTo(start);
            int dropped = 0;
            for (int tap = 0; tap < taps; tap++) {
                if (board.drop()) {
                    dropped++;
                }
            }
            // without a new ball only a game that stands still has something to show: it goes on
            if (dropped == 0 && (playing || board.isIdle())) {
                callback.onResult(null);
                return;
            }

            game.board.advanceTo(start);
            for (int ball = 0; ball < dropped; ball++) {
                game.board.drop();
            }
            newSet(context, appWidgetId, game);
            saveProgress(context, appWidgetId, game);

            long startTime = SystemClock.elapsedRealtime();
            List<String> frames = renderFrames(context, appWidgetId, game, start, chunkFrames(0) + overlapFrames(0), false);
            Log.d(TAG, "Drew " + frames.size() + " frames in " + (SystemClock.elapsedRealtime() - startTime) + " ms");
            callback.onResult(new Chunk(start, 0, chunkFrames(0), frames, first(frames), frames.isEmpty()));
            // ready before it is needed: drawing is faster than playing
            renderFrames(context, appWidgetId, game, start + chunkFrames(0), chunkFrames(1) + overlapFrames(1), true);
        });
    }

    // the chunk that starts at this frame. with pause, or when nothing moves, the chunk is only a still image.
    // active is asked right before the work: a tap can have replaced this animation in the meantime,
    // and then the callback gets null
    static void loadChunk(Context context, int appWidgetId, int start, int index, boolean pause,
                          BooleanSupplier active,
                          SettingsManager.Callback<Chunk> callback) {
        WAITING_CHUNKS.incrementAndGet();
        EXECUTOR.execute(() -> {
            WAITING_CHUNKS.decrementAndGet();
            Game game = game(appWidgetId);
            if (!active.getAsBoolean() || start < game.board.frame()) {
                callback.onResult(null);
                return;
            }
            game.board.advanceTo(start);
            saveProgress(context, appWidgetId, game);
            deleteFrames(context, game.set, start - KEEP_FRAMES);

            if (pause || game.board.isIdle()) {
                List<String> still = renderFrames(context, appWidgetId, game, start, 1, false);
                callback.onResult(new Chunk(start, index, 0, new ArrayList<>(), first(still), true));
                // the game stands still now, so only the image that is on the widget is still needed
                deleteAllExcept(context, appWidgetId, first(still));
                return;
            }
            List<String> frames = renderFrames(context, appWidgetId, game, start, chunkFrames(index) + overlapFrames(index), false);
            callback.onResult(new Chunk(start, index, chunkFrames(index), frames, first(frames), frames.isEmpty()));
            renderFrames(context, appWidgetId, game, start + chunkFrames(index),
                    chunkFrames(index + 1) + overlapFrames(index + 1), true);
        });
    }

    // draws the game as it is now with the current settings. the callback gets null if that failed.
    // resetProgress starts again at level 1 with all lives. the highest level stays
    static void still(Context context, int appWidgetId, boolean resetProgress, SettingsManager.Callback<String> callback) {
        EXECUTOR.execute(() -> {
            if (resetProgress) {
                SettingsManager.writesync(SettingsManager.plinkoLevelKey(appWidgetId), 1);
                SettingsManager.writesync(SettingsManager.plinkoLivesKey(appWidgetId), PlinkoBoard.START_LIVES);
                GAMES.remove(appWidgetId);
            }
            Game game = game(appWidgetId);
            newSet(context, appWidgetId, game);
            callback.onResult(first(renderFrames(context, appWidgetId, game, game.board.frame(), 1, false)));
        });
    }

    static void deleteForWidget(Context context, int appWidgetId) {
        EXECUTOR.execute(() -> {
            WAITING_TAPS.remove(appWidgetId);
            GAMES.remove(appWidgetId);
            deleteSets(context, appWidgetId, null, null);
            // a record from when it was kept per widget moves to the shared one before it is removed
            int best = readBest();
            if (best > readInt(SettingsManager.PLINKO_BEST, 1)) {
                SettingsManager.writesync(SettingsManager.PLINKO_BEST, best);
            }
            SettingsManager.resetData(SettingsManager.plinkoBestKey(appWidgetId));
            SettingsManager.resetData(SettingsManager.plinkoLevelKey(appWidgetId));
            SettingsManager.resetData(SettingsManager.plinkoLivesKey(appWidgetId));
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

    private static boolean tapWaiting() {
        if (WAITING_CHUNKS.get() > 0) {
            return true;
        }
        for (AtomicInteger taps : WAITING_TAPS.values()) {
            if (taps.get() > 0) {
                return true;
            }
        }
        return false;
    }

    private static Game game(int appWidgetId) {
        Game game = GAMES.get(appWidgetId);
        if (game == null) {
            game = new Game(readInt(SettingsManager.plinkoLevelKey(appWidgetId), 1),
                    readInt(SettingsManager.plinkoLivesKey(appWidgetId), PlinkoBoard.START_LIVES));
            game.set = prefix(appWidgetId) + UUID.randomUUID();
            GAMES.put(appWidgetId, game);
        }
        // read every time, so a change of the setting counts from the next frame that is drawn
        Boolean quickFinish = SettingsManager.readsyncWidget(SettingsManager.PLINKO_QUICK_FINISH, appWidgetId);
        game.board.setQuickFinish(quickFinish == null || quickFinish);
        return game;
    }

    // the launcher can still show frames of the previous set, so only the sets before that are deleted
    private static void newSet(Context context, int appWidgetId, Game game) {
        game.previousSet = game.set;
        game.set = prefix(appWidgetId) + UUID.randomUUID();
        deleteSets(context, appWidgetId, game.set, game.previousSet);
    }

    // the level and lives are saved when they change, so the game goes on there after the app was closed
    private static void saveProgress(Context context, int appWidgetId, Game game) {
        int lives = game.board.lives();
        if (lives != readInt(SettingsManager.plinkoLivesKey(appWidgetId), PlinkoBoard.START_LIVES)) {
            SettingsManager.writesync(SettingsManager.plinkoLivesKey(appWidgetId), lives);
        }
        int level = game.board.level();
        if (level != readInt(SettingsManager.plinkoLevelKey(appWidgetId), 1)) {
            SettingsManager.writesync(SettingsManager.plinkoLevelKey(appWidgetId), level);
        }
        if (level > readBest()) {
            SettingsManager.writesync(SettingsManager.PLINKO_BEST, level);
            // the other plinko widgets show the same record
            WidgetProviderPlinko.redrawOthers(context, appWidgetId);
        }
    }

    // the highest level counts for all plinko widgets
    private static int readBest() {
        return SettingsManager.readsyncPlinkoBest();
    }

    private static int readInt(Preferences.Key<Integer> key, int defaultValue) {
        Integer value = SettingsManager.readsync(key);
        return value != null ? value : defaultValue;
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
        int best = readBest();
        PlinkoRenderer renderer = null;
        Bitmap bitmap = null;
        try {
            for (int frame = start; frame < start + count; frame++) {
                if (ahead && tapWaiting()) {
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

    // deletes every frame of this widget except one. keep can be null
    private static void deleteAllExcept(Context context, int appWidgetId, String keep) {
        File[] files = directory(context).listFiles((dir, name) ->
                name.startsWith(prefix(appWidgetId)) && !name.equals(keep));
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
