package com.example.widgetapp.widgets.gen;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.example.widgetapp.SettingsManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GeneratedImageStore {
    private static final int IMAGE_SIZE = 192;
    private static final int FRAME_COUNT = 120;
    private static final Gson GSON = new Gson();
    private static final ExecutorService GENERATION_EXECUTOR = Executors.newSingleThreadExecutor();

    private GeneratedImageStore() {
    }

    public static void generate(Context context, float scale, int octaves, int palette, int algorithm, int speed, long seed,
                                List<String> oldFiles,
                                SettingsManager.Callback<List<String>> callback) {
        GENERATION_EXECUTOR.execute(() -> generateInternal(
                context, scale, octaves, palette, algorithm, speed, seed, oldFiles, callback
        ));
    }

    private static void generateInternal(Context context, float scale, int octaves, int palette, int algorithm,
                                         int speed, long seed, List<String> oldFiles,
                                         SettingsManager.Callback<List<String>> callback) {
        File directory = new File(context.getFilesDir(), "generated_widget");
        if (!directory.exists() && !directory.mkdirs()) {
            callback.onResult(new ArrayList<>());
            return;
        }

        String generationId = UUID.randomUUID().toString();
        String generatedPrefix = "generated_" + generationId;
        String stagingPrefix = "staging_" + generationId;
        List<String> generated = new ArrayList<>();
        List<String> staged = new ArrayList<>();
        try {
            Random random = new Random(seed);
            float baseOffsetX = random.nextFloat() * 10000;
            float baseOffsetY = random.nextFloat() * 10000;
            float normalizedSpeed = Math.max(1, Math.min(5, speed)) - 1;
            float speedCurve = normalizedSpeed * normalizedSpeed;
            float motionRadiusX = 0.02f + speedCurve * 0.75f;
            float motionRadiusY = 0.015f + speedCurve * 0.6f;
            float phaseAmplitude = 0.15f + speedCurve * 3f;
            for (int frame = 0; frame < FRAME_COUNT; frame++) {
                String stagedName = stagingPrefix + "_" + frame + ".jpg";
                String generatedName = generatedPrefix + "_" + frame + ".jpg";
                File file = new File(directory, stagedName);
                float motion = (frame / (float) FRAME_COUNT) * (float) (Math.PI * 2);
                float phase = (float) Math.sin(motion) * phaseAmplitude;
                float[][] noise = createNoise(scale, octaves, algorithm, seed,
                        baseOffsetX + (float) Math.sin(motion) * motionRadiusX,
                        baseOffsetY + (float) Math.cos(motion) * motionRadiusY,
                        phase);
                Bitmap bitmap = render(noise, palette);
                try (FileOutputStream output = new FileOutputStream(file)) {
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)) {
                        throw new IOException("Could not compress generated image");
                    }
                } finally {
                    bitmap.recycle();
                }
                staged.add(stagedName);
                generated.add(generatedName);
            }
            for (int frame = 0; frame < FRAME_COUNT; frame++) {
                File stagedFile = new File(directory, staged.get(frame));
                File generatedFile = new File(directory, generated.get(frame));
                if (!stagedFile.renameTo(generatedFile)) {
                    throw new IOException("Could not commit generated image");
                }
            }
        } catch (IOException e) {
            deleteFiles(context, staged);
            deleteFiles(context, generated);
            callback.onResult(new ArrayList<>());
            return;
        }

        SettingsManager.write(SettingsManager.GENERATED_IMAGES, GSON.toJson(generated), ignored -> {
            deleteFilesExcept(context, generated);
            callback.onResult(generated);
        });
    }

    public static List<String> readFiles() {
        String value = SettingsManager.readsyncGeneratedImages();
        if (value == null) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<ArrayList<String>>() { }.getType();
        List<String> files = GSON.fromJson(value, type);
        return files == null ? new ArrayList<>() : files;
    }

    public static void deleteAll(Context context) {
        File directory = new File(context.getFilesDir(), "generated_widget");
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    file.delete();
                }
            }
        }
        SettingsManager.resetData(SettingsManager.GENERATED_IMAGES);
    }

    public static void reset(Context context, Runnable callback) {
        GENERATION_EXECUTOR.execute(() -> {
            File directory = new File(context.getFilesDir(), "generated_widget");
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && !file.delete()) {
                        // A later cleanup pass will remove files still being released by the launcher.
                    }
                }
            }
            SettingsManager.write(SettingsManager.GENERATED_IMAGES, "[]", ignored -> callback.run());
        });
    }

    private static void deleteFiles(Context context, List<String> files) {
        for (String file : files) {
            new File(context.getFilesDir(), "generated_widget/" + file).delete();
        }
    }

    private static void deleteFilesExcept(Context context, List<String> keepFiles) {
        File directory = new File(context.getFilesDir(), "generated_widget");
        File[] files = directory.listFiles((dir, name) ->
                (name.startsWith("generated_") || name.startsWith("staging_"))
                        && name.endsWith(".jpg"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!keepFiles.contains(file.getName())) {
                file.delete();
            }
        }
    }

    private static float[][] createNoise(float scale, int octaves, int algorithm, long seed,
                                         float offsetX, float offsetY, float phase) {
        float[][] result = new float[IMAGE_SIZE][IMAGE_SIZE];
        float totalWeight = 0;

        for (int octave = 0; octave < octaves; octave++) {
            int gridSize = Math.max(2, Math.round(scale * (1 << octave)));
            float weight = 1f / (1 << octave);
            totalWeight += weight;
            for (int y = 0; y < IMAGE_SIZE; y++) {
                for (int x = 0; x < IMAGE_SIZE; x++) {
                    float sampleX = (x / (float) IMAGE_SIZE) * gridSize + offsetX;
                    float sampleY = (y / (float) IMAGE_SIZE) * gridSize + offsetY;
                    float warpAmount = algorithm == 4
                            ? 0.05f + octave * 0.02f
                            : 0.18f + octave * 0.04f;
                    float warpedX = sampleX
                            + (float) Math.sin(sampleY * 1.35f + phase) * warpAmount;
                    float warpedY = sampleY
                            + (float) Math.cos(sampleX * 1.15f - phase) * warpAmount;
                    float value = valueNoise(warpedX, warpedY, seed + octave * 7919L);
                    switch (algorithm) {
                        case 1:
                            value = 1f - Math.abs(2f * value - 1f);
                            break;
                        case 2:
                            value = Math.abs(2f * value - 1f);
                            break;
                        case 3:
                            value = (float) Math.pow(value, 2.2f);
                            break;
                        case 4:
                            float bands = 0.5f + 0.5f * (float) Math.sin(
                                    warpedX * 2.4f + warpedY * 0.8f + phase
                            );
                            value = lerp(value, bands, 0.72f);
                            break;
                        case 5:
                            value = 1f - Math.abs(2f * value - 1f);
                            value = value * value * (3f - 2f * value);
                            break;
                        case 6:
                            float island = smooth(value);
                            value = island < 0.42f ? island * 0.35f : 0.35f + (island - 0.42f) * 1.12f;
                            break;
                    }
                    result[x][y] += value * weight;
                }
            }
        }

        for (int y = 0; y < IMAGE_SIZE; y++) {
            for (int x = 0; x < IMAGE_SIZE; x++) {
                result[x][y] /= totalWeight;
            }
        }
        return result;
    }

    private static float valueNoise(float x, float y, long seed) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        float tx = smooth(x - x0);
        float ty = smooth(y - y0);
        float top = lerp(hash(x0, y0, seed), hash(x0 + 1, y0, seed), tx);
        float bottom = lerp(hash(x0, y0 + 1, seed), hash(x0 + 1, y0 + 1, seed), tx);
        return lerp(top, bottom, ty);
    }

    private static float hash(int x, int y, long seed) {
        long value = seed + x * 374761393L + y * 668265263L;
        value = (value ^ (value >> 13)) * 1274126177L;
        value ^= value >> 16;
        return (value & 0x7fffffff) / (float) Integer.MAX_VALUE;
    }

    private static float smooth(float value) {
        return value * value * (3 - 2 * value);
    }

    private static float lerp(float first, float second, float amount) {
        return first + (second - first) * amount;
    }

    private static Bitmap render(float[][] noise, int palette) {
        Bitmap bitmap = Bitmap.createBitmap(IMAGE_SIZE, IMAGE_SIZE, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < IMAGE_SIZE; y++) {
            for (int x = 0; x < IMAGE_SIZE; x++) {
                float value = Math.max(0, Math.min(1, noise[x][y]));
                bitmap.setPixel(x, y, colorFor(value, palette));
            }
        }
        return bitmap;
    }

    private static int colorFor(float value, int palette) {
        int[][] colors = {
                {Color.rgb(15, 32, 64), Color.rgb(34, 139, 167), Color.rgb(244, 210, 122), Color.rgb(250, 245, 224)},
                {Color.rgb(32, 20, 55), Color.rgb(92, 45, 145), Color.rgb(229, 91, 143), Color.rgb(255, 194, 112)},
                {Color.rgb(12, 48, 44), Color.rgb(42, 120, 75), Color.rgb(157, 193, 92), Color.rgb(239, 230, 156)},
                {Color.rgb(8, 18, 38), Color.rgb(25, 70, 145), Color.rgb(67, 170, 210), Color.rgb(220, 245, 255)},
                {Color.rgb(48, 8, 18), Color.rgb(135, 24, 42), Color.rgb(238, 87, 49), Color.rgb(255, 211, 105)},
                {Color.rgb(28, 12, 52), Color.rgb(83, 35, 125), Color.rgb(164, 77, 181), Color.rgb(242, 170, 211)},
                {Color.rgb(20, 28, 24), Color.rgb(84, 69, 45), Color.rgb(164, 125, 69), Color.rgb(238, 218, 157)},
                {Color.rgb(24, 12, 8), Color.rgb(91, 46, 24), Color.rgb(188, 103, 43), Color.rgb(255, 205, 108)},
                {Color.rgb(10, 35, 35), Color.rgb(16, 112, 105), Color.rgb(53, 190, 154), Color.rgb(190, 245, 194)}
        };
        int[] selected = colors[Math.max(0, Math.min(colors.length - 1, palette))];
        float position = value * (selected.length - 1);
        int index = Math.min(selected.length - 2, (int) position);
        return blend(selected[index], selected[index + 1], position - index);
    }

    private static int blend(int first, int second, float amount) {
        int red = (int) (Color.red(first) + (Color.red(second) - Color.red(first)) * amount);
        int green = (int) (Color.green(first) + (Color.green(second) - Color.green(first)) * amount);
        int blue = (int) (Color.blue(first) + (Color.blue(second) - Color.blue(first)) * amount);
        return Color.rgb(red, green, blue);
    }
}
