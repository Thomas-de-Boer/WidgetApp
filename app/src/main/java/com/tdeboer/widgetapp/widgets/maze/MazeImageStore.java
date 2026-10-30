package com.tdeboer.widgetapp.widgets.maze;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;

import androidx.datastore.preferences.core.Preferences;

import com.tdeboer.widgetapp.SettingsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

// all work runs on one thread, so a tap on solve simply waits for the maze that is still being rendered
final class MazeImageStore {
    // a long route doesn't fit in the launcher's memory at once, so it is played in chunks of this many frames
    private static final int CHUNK_FRAMES = 75;
    private static final String DIRECTORY = "maze_widget";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private MazeImageStore() {
    }

    static final class Chunk {
        final List<String> frames;
        final boolean last;

        Chunk(List<String> frames, boolean last) {
            this.frames = frames;
            this.last = last;
        }
    }

    static void run(Runnable task) {
        EXECUTOR.execute(task);
    }

    // onReady runs as soon as the still images exist, the first chunk is rendered right after it
    static void newMaze(Context context, int appWidgetId, Runnable onReady) {
        EXECUTOR.execute(() -> {
            long seed = ThreadLocalRandom.current().nextLong();
            SettingsManager.writesync(SettingsManager.mazeSeedKey(appWidgetId), seed);
            SettingsManager.writesync(SettingsManager.mazeSolvedKey(appWidgetId), false);
            render(context, appWidgetId, seed, onReady);
        });
    }

    // draws the same maze again with the current settings
    static void redraw(Context context, int appWidgetId, Runnable onReady) {
        EXECUTOR.execute(() -> render(context, appWidgetId, readSeed(appWidgetId), onReady));
    }

    // the callback gets an empty chunk if the frames could not be written
    static void loadChunk(Context context, int appWidgetId, int chunk, SettingsManager.Callback<Chunk> callback) {
        EXECUTOR.execute(() -> {
            if (chunk == 0) {
                SettingsManager.writesync(SettingsManager.mazeSolvedKey(appWidgetId), true);
            }
            callback.onResult(renderChunk(context, appWidgetId, chunk));
            // ready before it is needed: rendering is faster than playing
            renderChunk(context, appWidgetId, chunk + 1);
        });
    }

    static boolean isSolved(int appWidgetId) {
        return Boolean.TRUE.equals(SettingsManager.readsync(SettingsManager.mazeSolvedKey(appWidgetId)));
    }

    static void deleteForWidget(Context context, int appWidgetId) {
        EXECUTOR.execute(() -> {
            deleteSets(context, appWidgetId, null);
            SettingsManager.resetData(SettingsManager.mazeSeedKey(appWidgetId));
            SettingsManager.resetData(SettingsManager.mazeSolvedKey(appWidgetId));
        });
    }

    static void deleteAll(Context context) {
        EXECUTOR.execute(() -> {
            File[] files = directory(context).listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        });
    }

    // the frames of the widget's maze that exist so far, in playback order.
    // the first is the empty maze and the last is the solved maze
    static List<String> findFrames(Context context, int appWidgetId) {
        File[] files = directory(context).listFiles((dir, name) ->
                name.startsWith(prefix(appWidgetId)) && name.endsWith(".png"));
        List<String> result = new ArrayList<>();
        if (files == null) {
            return result;
        }

        String newestSet = null;
        long newestTime = Long.MIN_VALUE;
        for (File file : files) {
            if (file.lastModified() > newestTime) {
                newestSet = setName(file.getName());
                newestTime = file.lastModified();
            }
        }
        for (File file : files) {
            if (setName(file.getName()).equals(newestSet)) {
                result.add(file.getName());
            }
        }
        result.sort(Comparator.comparingInt(MazeImageStore::frameNumber));
        return result;
    }

    private static void render(Context context, int appWidgetId, long seed, Runnable onReady) {
        String setName = prefix(appWidgetId) + UUID.randomUUID();
        MazeRenderer renderer = makeRenderer(seed);
        boolean written = directory(context).exists() || directory(context).mkdirs();
        if (written) {
            written = writeFrames(context, renderer, setName, new int[]{0, renderer.frameCount() - 1});
        }
        if (written) {
            deleteSets(context, appWidgetId, setName);
        }
        onReady.run();
        if (written) {
            writeFrames(context, renderer, setName, chunkFrames(renderer, 0));
        }
        renderer.recycle();
    }

    private static Chunk renderChunk(Context context, int appWidgetId, int chunk) {
        long seed = readSeed(appWidgetId);
        MazeRenderer renderer = makeRenderer(seed);
        try {
            int[] numbers = chunkFrames(renderer, chunk);
            List<String> frames = findFrames(context, appWidgetId);
            String setName = frames.isEmpty() ? prefix(appWidgetId) + UUID.randomUUID() : setName(frames.get(0));
            if (!directory(context).exists() && !directory(context).mkdirs()) {
                return new Chunk(new ArrayList<>(), true);
            }
            if (!writeFrames(context, renderer, setName, numbers)) {
                return new Chunk(new ArrayList<>(), true);
            }

            List<String> names = new ArrayList<>();
            for (int number : numbers) {
                names.add(setName + "_" + number + ".png");
            }
            boolean last = numbers.length == 0 || numbers[numbers.length - 1] == renderer.frameCount() - 1;
            return new Chunk(names, last);
        } finally {
            renderer.recycle();
        }
    }

    private static int[] chunkFrames(MazeRenderer renderer, int chunk) {
        int start = chunk * CHUNK_FRAMES;
        int end = Math.min(start + CHUNK_FRAMES, renderer.frameCount());
        int[] numbers = new int[Math.max(0, end - start)];
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = start + i;
        }
        return numbers;
    }

    private static MazeRenderer makeRenderer(long seed) {
        int cells = readInt(SettingsManager.MAZE_CELLS, Maze.DEFAULT_CELLS);
        int style = readInt(SettingsManager.MAZE_STYLE, Maze.STYLE_WINDING);
        int theme = readInt(SettingsManager.MAZE_THEME, 0);
        int speed = readInt(SettingsManager.MAZE_SPEED, Maze.DEFAULT_SPEED);
        return new MazeRenderer(new Maze(cells, seed, style), theme, speed);
    }

    private static long readSeed(int appWidgetId) {
        Long seed = SettingsManager.readsync(SettingsManager.mazeSeedKey(appWidgetId));
        if (seed == null) {
            seed = ThreadLocalRandom.current().nextLong();
            SettingsManager.writesync(SettingsManager.mazeSeedKey(appWidgetId), seed);
        }
        return seed;
    }

    // frames that already exist are skipped, so a replay doesn't render anything again
    private static boolean writeFrames(Context context, MazeRenderer renderer, String setName, int[] numbers) {
        File directory = directory(context);
        Bitmap bitmap = Bitmap.createBitmap(renderer.imageSize(), renderer.imageSize(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        try {
            for (int number : numbers) {
                String name = setName + "_" + number + ".png";
                if (!new File(directory, name).exists()) {
                    renderer.drawFrame(number, canvas);
                    writeFrame(directory, name, bitmap);
                }
            }
            return true;
        } catch (IOException e) {
            Log.w("MazeImageStore", "Could not write maze frame", e);
            return false;
        } finally {
            bitmap.recycle();
        }
    }

    // written under a temporary name first, so the launcher never reads a half written file
    private static void writeFrame(File directory, String name, Bitmap bitmap) throws IOException {
        File temporary = new File(directory, name + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temporary)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw new IOException("Could not compress maze image");
            }
        }
        if (!temporary.renameTo(new File(directory, name))) {
            throw new IOException("Could not commit maze image");
        }
    }

    // keepSet can be null to delete everything of this widget
    private static void deleteSets(Context context, int appWidgetId, String keepSet) {
        File[] files = directory(context).listFiles((dir, name) ->
                name.startsWith(prefix(appWidgetId)) && (keepSet == null || !name.startsWith(keepSet + "_")));
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    private static int readInt(Preferences.Key<Integer> key, int defaultValue) {
        Integer value = SettingsManager.readsync(key);
        return value != null ? value : defaultValue;
    }

    private static File directory(Context context) {
        return new File(context.getFilesDir(), DIRECTORY);
    }

    private static String prefix(int appWidgetId) {
        return "maze_" + appWidgetId + "_";
    }

    private static String setName(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf('_'));
    }

    private static int frameNumber(String fileName) {
        return Integer.parseInt(fileName.substring(fileName.lastIndexOf('_') + 1, fileName.lastIndexOf('.')));
    }
}
