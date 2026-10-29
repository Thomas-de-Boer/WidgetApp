package com.example.widgetapp.widgets.gen;

// draws one frame of noise into an ARGB pixel array.
// plain java (no android classes), and it reuses its buffers, so rendering many frames in a row stays cheap
final class NoiseRenderer {
    static final int IMAGE_SIZE = 192;
    private static final int LUT_SIZE = 1024;
    private static final float[] POW_LUT = buildPowLut();
    private static final int ANGLE_STEPS = 1024;
    private static final float[] SIN_LUT = new float[ANGLE_STEPS];
    private static final float[] COS_LUT = new float[ANGLE_STEPS];
    // these scale the new algorithms so their contrast roughly matches the value noise ones
    private static final float PERLIN_CONTRAST = 0.75f;
    private static final float CELL_FALLOFF = 0.35f;
    private static final float VEIN_SHARPNESS = 2.5f;

    static {
        for (int i = 0; i < ANGLE_STEPS; i++) {
            SIN_LUT[i] = (float) Math.sin(i * Math.PI * 2 / ANGLE_STEPS);
            COS_LUT[i] = (float) Math.cos(i * Math.PI * 2 / ANGLE_STEPS);
        }
    }
    private static final int[][] PALETTES = {
            {rgb(15, 32, 64), rgb(34, 139, 167), rgb(244, 210, 122), rgb(250, 245, 224)},
            {rgb(32, 20, 55), rgb(92, 45, 145), rgb(229, 91, 143), rgb(255, 194, 112)},
            {rgb(12, 48, 44), rgb(42, 120, 75), rgb(157, 193, 92), rgb(239, 230, 156)},
            {rgb(8, 18, 38), rgb(25, 70, 145), rgb(67, 170, 210), rgb(220, 245, 255)},
            {rgb(48, 8, 18), rgb(135, 24, 42), rgb(238, 87, 49), rgb(255, 211, 105)},
            {rgb(28, 12, 52), rgb(83, 35, 125), rgb(164, 77, 181), rgb(242, 170, 211)},
            {rgb(20, 28, 24), rgb(84, 69, 45), rgb(164, 125, 69), rgb(238, 218, 157)},
            {rgb(24, 12, 8), rgb(91, 46, 24), rgb(188, 103, 43), rgb(255, 205, 108)},
            {rgb(10, 35, 35), rgb(16, 112, 105), rgb(53, 190, 154), rgb(190, 245, 194)}
    };

    private final float[] noise = new float[IMAGE_SIZE * IMAGE_SIZE];
    private final float[] sampleX = new float[IMAGE_SIZE];
    private final float[] sampleY = new float[IMAGE_SIZE];
    private final float[] warpX = new float[IMAGE_SIZE];
    private final float[] warpY = new float[IMAGE_SIZE];
    private final float[] bandSinX = new float[IMAGE_SIZE];
    private final float[] bandCosX = new float[IMAGE_SIZE];
    private final float[] bandSinY = new float[IMAGE_SIZE];
    private final float[] bandCosY = new float[IMAGE_SIZE];
    private final int[] colorLut = new int[LUT_SIZE];
    private final float[] gradientX = new float[4];
    private final float[] gradientY = new float[4];
    private final float[] pointX = new float[9];
    private final float[] pointY = new float[9];
    private final float[] pointShade = new float[9];
    private float morph;
    private int morphPeriod = 1;
    private int cellX;
    private int cellY = Integer.MIN_VALUE;
    private long cellSeed;
    private float topLeft;
    private float topRight;
    private float bottomLeft;
    private float bottomRight;

    NoiseRenderer(int palette) {
        int[] selected = PALETTES[Math.max(0, Math.min(PALETTES.length - 1, palette))];
        for (int i = 0; i < LUT_SIZE; i++) {
            float position = (i / (float) (LUT_SIZE - 1)) * (selected.length - 1);
            int index = Math.min(selected.length - 2, (int) position);
            colorLut[i] = blend(selected[index], selected[index + 1], position - index);
        }
    }

    // morph runs from 0 to morphPeriod over one animation loop. every noise corner steps through morphPeriod
    // random values and then returns to the first one, so the loop is seamless
    void render(float scale, int octaves, int algorithm, long seed,
                float offsetX, float offsetY, float phase, float morph, int morphPeriod, int[] pixels) {
        java.util.Arrays.fill(noise, 0f);
        float totalWeight = 0;
        // the cached corners belong to this frame's morph position, so they can't be reused in the next frame
        cellY = Integer.MIN_VALUE;
        this.morph = morph;
        this.morphPeriod = morphPeriod;

        // the cell algorithms are about 3x as expensive per octave, and above 4 octaves their cells are only
        // a pixel or two wide, which just adds grain, so they stop there to save battery
        int octaveCount = algorithm == 8 || algorithm == 9 ? Math.min(octaves, 4) : octaves;
        for (int octave = 0; octave < octaveCount; octave++) {
            int gridSize = Math.max(2, Math.round(scale * (1 << octave)));
            float weight = 1f / (1 << octave);
            totalWeight += weight;
            long octaveSeed = seed + octave * 7919L;
            float warpAmount = algorithm == 4
                    ? 0.05f + octave * 0.02f
                    : 0.18f + octave * 0.04f;

            // the warp of x only depends on the row and the warp of y only on the column,
            // so the expensive sin/cos is done once per row/column instead of once per pixel
            for (int i = 0; i < IMAGE_SIZE; i++) {
                sampleX[i] = (i / (float) IMAGE_SIZE) * gridSize + offsetX;
                sampleY[i] = (i / (float) IMAGE_SIZE) * gridSize + offsetY;
                warpX[i] = (float) Math.sin(sampleY[i] * 1.35f + phase) * warpAmount;
                warpY[i] = (float) Math.cos(sampleX[i] * 1.15f - phase) * warpAmount;
            }
            if (algorithm == 4) {
                // sin(a(x) + b(y)) = sin(a)cos(b) + cos(a)sin(b), so the bands are separable too
                for (int i = 0; i < IMAGE_SIZE; i++) {
                    double bandX = sampleX[i] * 2.4 + warpY[i] * 0.8 + phase;
                    double bandY = warpX[i] * 2.4 + sampleY[i] * 0.8;
                    bandSinX[i] = (float) Math.sin(bandX);
                    bandCosX[i] = (float) Math.cos(bandX);
                    bandSinY[i] = (float) Math.sin(bandY);
                    bandCosY[i] = (float) Math.cos(bandY);
                }
            }

            for (int y = 0; y < IMAGE_SIZE; y++) {
                float rowWarp = warpX[y];
                float rowSample = sampleY[y];
                int rowStart = y * IMAGE_SIZE;
                for (int x = 0; x < IMAGE_SIZE; x++) {
                    float warpedX = sampleX[x] + rowWarp;
                    float warpedY = rowSample + warpY[x];
                    float value;
                    if (algorithm == 7) {
                        value = perlinNoise(warpedX, warpedY, octaveSeed);
                    } else if (algorithm == 8 || algorithm == 9) {
                        value = cellNoise(warpedX, warpedY, octaveSeed, algorithm == 9);
                    } else {
                        value = valueNoise(warpedX, warpedY, octaveSeed);
                    }
                    switch (algorithm) {
                        case 1:
                            value = 1f - Math.abs(2f * value - 1f);
                            break;
                        case 2:
                            value = Math.abs(2f * value - 1f);
                            break;
                        case 3:
                            value = lookup(POW_LUT, value);
                            break;
                        case 4:
                            float bands = 0.5f + 0.5f * (bandSinX[x] * bandCosY[y] + bandCosX[x] * bandSinY[y]);
                            value = value + (bands - value) * 0.72f;
                            break;
                        case 5:
                            value = 1f - Math.abs(2f * value - 1f);
                            value = value * value * (3f - 2f * value);
                            break;
                        case 6:
                            float island = value * value * (3 - 2 * value);
                            value = island < 0.42f ? island * 0.35f : 0.35f + (island - 0.42f) * 1.12f;
                            break;
                    }
                    noise[rowStart + x] += value * weight;
                }
            }
        }

        float toLut = (LUT_SIZE - 1) / totalWeight;
        for (int i = 0; i < noise.length; i++) {
            int index = (int) (noise[i] * toLut + 0.5f);
            pixels[i] = colorLut[index < 0 ? 0 : Math.min(LUT_SIZE - 1, index)];
        }
    }

    private float valueNoise(float x, float y, long seed) {
        int x0 = (int) x;
        if (x < x0) {
            x0--;
        }
        int y0 = (int) y;
        if (y < y0) {
            y0--;
        }
        float tx = x - x0;
        float ty = y - y0;
        tx = tx * tx * (3 - 2 * tx);
        ty = ty * ty * (3 - 2 * ty);
        // neighbouring pixels mostly land in the same noise cell, so only hash the corners when the cell changes
        if (x0 != cellX || y0 != cellY || seed != cellSeed) {
            if (y0 == cellY && x0 == cellX + 1 && seed == cellSeed) {
                topLeft = topRight;
                bottomLeft = bottomRight;
            } else {
                topLeft = corner(x0, y0, seed);
                bottomLeft = corner(x0, y0 + 1, seed);
            }
            topRight = corner(x0 + 1, y0, seed);
            bottomRight = corner(x0 + 1, y0 + 1, seed);
            cellX = x0;
            cellY = y0;
            cellSeed = seed;
        }
        float top = topLeft + (topRight - topLeft) * tx;
        float bottom = bottomLeft + (bottomRight - bottomLeft) * tx;
        return top + (bottom - top) * ty;
    }

    // the value of one noise corner at the current morph position. every corner has its own random head start,
    // so at any moment some parts of the map are changing while others settle; the map keeps evolving
    // without the whole picture blending at once (that made the contrast pulse and new maps seem to pop in)
    private float corner(int x, int y, long seed) {
        if (morphPeriod <= 1) {
            return hash(x, y, seed);
        }
        float time = morph + hash(x, y, seed ^ 0x5DEECE66DL) * morphPeriod;
        int step = (int) time;
        float amount = time - step;
        amount = amount * amount * (3 - 2 * amount);
        float from = hash(x, y, seed + (step % morphPeriod) * 104729L);
        float to = hash(x, y, seed + ((step + 1) % morphPeriod) * 104729L);
        return from + (to - from) * amount;
    }

    // perlin (gradient) noise: every corner has a direction instead of a value, which gives rounder,
    // smoother shapes. morphing slowly turns the directions
    private float perlinNoise(float x, float y, long seed) {
        int x0 = (int) x;
        if (x < x0) {
            x0--;
        }
        int y0 = (int) y;
        if (y < y0) {
            y0--;
        }
        float fx = x - x0;
        float fy = y - y0;
        if (x0 != cellX || y0 != cellY || seed != cellSeed) {
            setGradient(0, x0, y0, seed);
            setGradient(1, x0 + 1, y0, seed);
            setGradient(2, x0, y0 + 1, seed);
            setGradient(3, x0 + 1, y0 + 1, seed);
            cellX = x0;
            cellY = y0;
            cellSeed = seed;
        }
        float topLeft = gradientX[0] * fx + gradientY[0] * fy;
        float topRight = gradientX[1] * (fx - 1) + gradientY[1] * fy;
        float bottomLeft = gradientX[2] * fx + gradientY[2] * (fy - 1);
        float bottomRight = gradientX[3] * (fx - 1) + gradientY[3] * (fy - 1);
        float u = fade(fx);
        float v = fade(fy);
        float top = topLeft + (topRight - topLeft) * u;
        float bottom = bottomLeft + (bottomRight - bottomLeft) * u;
        float value = 0.5f + (top + (bottom - top) * v) * PERLIN_CONTRAST;
        return value < 0f ? 0f : Math.min(1f, value);
    }

    private void setGradient(int index, int x, int y, long seed) {
        int angle = (int) (corner(x, y, seed) * ANGLE_STEPS) & (ANGLE_STEPS - 1);
        gradientX[index] = COS_LUT[angle];
        gradientY[index] = SIN_LUT[angle];
    }

    // perlin's fade curve, smoother than smoothstep so the grid lines don't show
    private static float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    // cellular (worley) noise: every cell has one point, and the value depends on the distance to the
    // nearest points. cells shows the cells themselves, veins shows the borders between them.
    // morphing slowly moves the points around inside their cells
    private float cellNoise(float x, float y, long seed, boolean veins) {
        int x0 = (int) x;
        if (x < x0) {
            x0--;
        }
        int y0 = (int) y;
        if (y < y0) {
            y0--;
        }
        if (x0 != cellX || y0 != cellY || seed != cellSeed) {
            // moving one cell to the right keeps 6 of the 9 surrounding points, so only the new column is computed
            boolean shift = y0 == cellY && x0 == cellX + 1 && seed == cellSeed;
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 3; i++) {
                    int index = j * 3 + i;
                    if (shift && i < 2) {
                        pointX[index] = pointX[index + 1];
                        pointY[index] = pointY[index + 1];
                        pointShade[index] = pointShade[index + 1];
                    } else {
                        setCellPoint(index, x0 + i - 1, y0 + j - 1, seed, veins);
                    }
                }
            }
            cellX = x0;
            cellY = y0;
            cellSeed = seed;
        }
        float nearest = Float.MAX_VALUE;
        float secondNearest = Float.MAX_VALUE;
        int nearestIndex = 0;
        for (int i = 0; i < 9; i++) {
            float dx = pointX[i] - x;
            float dy = pointY[i] - y;
            float distance = dx * dx + dy * dy;
            if (distance < nearest) {
                secondNearest = nearest;
                nearest = distance;
                nearestIndex = i;
            } else if (distance < secondNearest) {
                secondNearest = distance;
            }
        }
        nearest = (float) Math.sqrt(nearest);
        float value = veins
                ? 1f - ((float) Math.sqrt(secondNearest) - nearest) * VEIN_SHARPNESS
                // every cell has its own shade, a bit darker towards its edge
                : pointShade[nearestIndex] - nearest * CELL_FALLOFF;
        return value < 0f ? 0f : Math.min(1f, value);
    }

    private void setCellPoint(int index, int cx, int cy, long seed, boolean veins) {
        pointX[index] = cx + 0.1f + 0.8f * corner(cx, cy, seed);
        pointY[index] = cy + 0.1f + 0.8f * corner(cx, cy, seed ^ 0x9E3779B97F4A7C15L);
        pointShade[index] = veins ? 0f : 0.15f + corner(cx, cy, seed ^ 0x2545F4914F6CDD1DL);
    }

    private static float hash(int x, int y, long seed) {
        long value = seed + x * 374761393L + y * 668265263L;
        value = (value ^ (value >> 13)) * 1274126177L;
        value ^= value >> 16;
        return (value & 0x7fffffff) / (float) Integer.MAX_VALUE;
    }

    private static float lookup(float[] lut, float value) {
        float position = Math.max(0f, Math.min(1f, value)) * (lut.length - 1);
        int index = Math.min(lut.length - 2, (int) position);
        return lut[index] + (lut[index + 1] - lut[index]) * (position - index);
    }

    private static float[] buildPowLut() {
        float[] lut = new float[LUT_SIZE];
        for (int i = 0; i < LUT_SIZE; i++) {
            lut[i] = (float) Math.pow(i / (float) (LUT_SIZE - 1), 2.2f);
        }
        return lut;
    }

    private static int rgb(int red, int green, int blue) {
        return 0xff000000 | (red << 16) | (green << 8) | blue;
    }

    private static int blend(int first, int second, float amount) {
        int red = (int) (((first >> 16) & 0xff) + (((second >> 16) & 0xff) - ((first >> 16) & 0xff)) * amount);
        int green = (int) (((first >> 8) & 0xff) + (((second >> 8) & 0xff) - ((first >> 8) & 0xff)) * amount);
        int blue = (int) ((first & 0xff) + ((second & 0xff) - (first & 0xff)) * amount);
        return rgb(red, green, blue);
    }
}
