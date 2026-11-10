package com.tdeboer.widgetapp.widgets.plinko;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

// draws one frame of the board: the pins and slots never change, so they are drawn once and copied
final class PlinkoRenderer {
    // has to match android:flipInterval of widget_plinko_flipper
    static final int FLIP_INTERVAL_MILLIS = 40;
    static final int WIDTH = 640;
    static final int HEIGHT = 480;

    private static final float UNIT_X = WIDTH / 9.5f;
    private static final float TOP = 26f;
    private static final float SLOT_TOP_Y = 8.05f;
    private static final float SLOT_BOTTOM_Y = 9.45f;
    private static final float UNIT_Y = (HEIGHT - 8f - TOP) / (SLOT_BOTTOM_Y - PlinkoBoard.DROP_Y);
    private static final float PIN_RADIUS = 5f;
    private static final float BALL_RADIUS = 9f;
    private static final float SLOT_GAP = 3f;
    private static final int FLASH_FRAMES = 6;

    // per theme: background, pins, ball, text, minus slot, plus slots
    private static final int[][] THEMES = {
            {0xFF10141F, 0xFF8C96AD, 0xFFFFC857, 0xFFDCE3F2, 0xFFEF5350, 0xFF66BB6A},
            {0xFFF7F1E3, 0xFF8A8275, 0xFF2D6CDF, 0xFF2F2A25, 0xFFD1495B, 0xFF3C8D5A},
            {0xFF16261D, 0xFF6F8F6A, 0xFFFFC857, 0xFFB7D8A8, 0xFFE4572E, 0xFF7BD389},
            {0xFF2B1433, 0xFFB58AA5, 0xFFFFD166, 0xFFFFC3A0, 0xFFFF6F91, 0xFF4DD0E1},
    };

    private final int[] colors;
    private final Bitmap base;
    private final Paint ballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path star = new Path();
    private final float[] position = new float[2];

    PlinkoRenderer(int theme) {
        colors = THEMES[clamp(theme)];
        ballPaint.setColor(colors[2]);

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(24f);
        labelPaint.setTypeface(Typeface.DEFAULT_BOLD);

        numberPaint.setColor(colors[3]);
        numberPaint.setTextSize(30f);
        numberPaint.setTypeface(Typeface.DEFAULT_BOLD);

        // a star with five points, 11 pixels from the middle to a point
        for (int point = 0; point < 10; point++) {
            double angle = Math.PI * point / 5 - Math.PI / 2;
            float radius = point % 2 == 0 ? 11f : 4.6f;
            float x = (float) (Math.cos(angle) * radius);
            float y = (float) (Math.sin(angle) * radius);
            if (point == 0) {
                star.moveTo(x, y);
            } else {
                star.lineTo(x, y);
            }
        }
        star.close();

        base = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        drawBase(new Canvas(base));
    }

    static int backgroundColor(int theme) {
        return THEMES[clamp(theme)][0];
    }

    static int textColor(int theme) {
        return THEMES[clamp(theme)][3];
    }

    private static int clamp(int theme) {
        return Math.max(0, Math.min(THEMES.length - 1, theme));
    }

    // best is the highest number of balls this widget ever had in play
    void drawFrame(PlinkoBoard board, int best, Canvas canvas) {
        canvas.drawBitmap(base, 0f, 0f, null);

        for (int slot = 0; slot < PlinkoBoard.SLOTS; slot++) {
            int since = board.framesSinceHit(slot);
            if (since >= 0 && since < FLASH_FRAMES) {
                drawSlot(canvas, slot, 1f - since / (float) FLASH_FRAMES);
            }
        }

        for (PlinkoBoard.Ball ball : board.balls()) {
            board.position(ball, position);
            canvas.drawCircle(pixelX(position[0]), pixelY(position[1]), BALL_RADIUS, ballPaint);
        }

        // left: the balls in play, right: the best
        canvas.drawCircle(24f, 30f, BALL_RADIUS, ballPaint);
        numberPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(String.valueOf(board.inPlay()), 42f, 41f, numberPaint);

        String bestText = String.valueOf(Math.max(best, board.peak()));
        numberPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(bestText, WIDTH - 16f, 41f, numberPaint);
        float starX = WIDTH - 16f - numberPaint.measureText(bestText) - 18f;
        canvas.save();
        canvas.translate(starX, 30f);
        canvas.drawPath(star, ballPaint);
        canvas.restore();
    }

    void recycle() {
        base.recycle();
    }

    private void drawBase(Canvas canvas) {
        canvas.drawColor(colors[0]);

        Paint pin = new Paint(Paint.ANTI_ALIAS_FLAG);
        pin.setColor(colors[1]);
        for (int row = 0; row < PlinkoBoard.ROWS; row++) {
            for (int index = 0; index <= row; index++) {
                canvas.drawCircle(pixelX(index - row / 2f), pixelY(row), PIN_RADIUS, pin);
            }
        }

        for (int slot = 0; slot < PlinkoBoard.SLOTS; slot++) {
            drawSlot(canvas, slot, 0f);
        }
    }

    // flash is 0 for a resting slot and 1 right after a ball landed in it
    private void drawSlot(Canvas canvas, int slot, float flash) {
        int value = PlinkoBoard.SLOT_VALUES[slot];
        // higher slots are brighter
        int color = value < 0 ? colors[4] : blend(colors[0], colors[5], 0.4f + 0.2f * value);
        float left = pixelX(slot - 4.5f) + SLOT_GAP;
        float right = pixelX(slot - 3.5f) - SLOT_GAP;
        float top = pixelY(SLOT_TOP_Y);
        float bottom = pixelY(SLOT_BOTTOM_Y);

        slotPaint.setColor(blend(colors[0], color, 0.25f + 0.75f * flash));
        canvas.drawRoundRect(left, top, right, bottom, 8f, 8f, slotPaint);

        // the label keeps the full color, so it stays readable on the pale slots
        labelPaint.setColor(blend(value < 0 ? colors[4] : colors[5], colors[0], flash));
        String label = value > 0 ? "+" + value : String.valueOf(value);
        float middle = (top + bottom) / 2f - (labelPaint.ascent() + labelPaint.descent()) / 2f;
        canvas.drawText(label, (left + right) / 2f, middle, labelPaint);
    }

    private static float pixelX(float x) {
        return WIDTH / 2f + x * UNIT_X;
    }

    private static float pixelY(float y) {
        return TOP + (y - PlinkoBoard.DROP_Y) * UNIT_Y;
    }

    private static int blend(int from, int to, float amount) {
        float t = Math.max(0f, Math.min(1f, amount));
        return Color.rgb(
                Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * t),
                Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * t),
                Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t));
    }
}
