package com.tdeboer.widgetapp.widgets.maze;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

final class MazeRenderer {
    // has to match android:flipInterval of widget_maze_flipper
    static final int FLIP_INTERVAL_MILLIS = 40;
    private static final int TARGET_SIZE = 600;
    private static final int MIN_WALL_WIDTH = 3;
    private static final int MAX_WALL_WIDTH = 8;

    // per theme: background, walls, path, start, exit
    private static final int[][] THEMES = {
            {0xFF10141F, 0xFFDCE3F2, 0xFF4FC3F7, 0xFF66BB6A, 0xFFEF5350},
            {0xFFF7F1E3, 0xFF2F2A25, 0xFFD1495B, 0xFF3C8D5A, 0xFF2D6CDF},
            {0xFF16261D, 0xFFB7D8A8, 0xFFFFC857, 0xFF7BD389, 0xFFE4572E},
            {0xFF2B1433, 0xFFFFC3A0, 0xFFFF6F91, 0xFFFFD166, 0xFF9B5DE5},
    };

    private final Maze maze;
    private final int[] colors;
    private final int cellSize;
    private final int wallWidth;
    private final float origin;
    private final int imageSize;
    private final int[] route;
    private final int frameCount;
    private final Paint pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap base;

    MazeRenderer(Maze maze, int theme, int cellsPerSecond) {
        this.maze = maze;
        colors = THEMES[Math.max(0, Math.min(THEMES.length - 1, theme))];
        // whole pixels per cell, so all walls have the same sharp width
        cellSize = TARGET_SIZE / maze.cells();
        wallWidth = Math.max(MIN_WALL_WIDTH, Math.min(MAX_WALL_WIDTH, cellSize / 6));
        origin = wallWidth / 2f;
        imageSize = cellSize * maze.cells() + wallWidth;
        route = maze.solve();
        frameCount = Maze.solveFrameCount(route.length, cellsPerSecond, FLIP_INTERVAL_MILLIS);

        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);
        pathPaint.setStrokeWidth(Math.max(2f, cellSize / 3f));
        pathPaint.setColor(colors[2]);
        dotPaint.setColor(colors[2]);

        base = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888);
        drawBase(new Canvas(base));
    }

    int imageSize() {
        return imageSize;
    }

    int frameCount() {
        return frameCount;
    }

    static int backgroundColor(int theme) {
        return THEMES[Math.max(0, Math.min(THEMES.length - 1, theme))][0];
    }

    static int textColor(int theme) {
        return THEMES[Math.max(0, Math.min(THEMES.length - 1, theme))][1];
    }

    // frame 0 is the empty maze, the last frame has the whole route
    void drawFrame(int frame, Canvas canvas) {
        canvas.drawBitmap(base, 0f, 0f, null);
        if (frame == 0) {
            return;
        }

        float progress = frame / (float) (frameCount - 1) * (route.length - 1);
        int fullSteps = Math.min((int) progress, route.length - 2);
        float partialStep = progress - fullSteps;

        Path path = new Path();
        path.moveTo(centerX(route[0]), centerY(route[0]));
        for (int step = 1; step <= fullSteps; step++) {
            path.lineTo(centerX(route[step]), centerY(route[step]));
        }
        int from = route[fullSteps];
        int to = route[fullSteps + 1];
        float tipX = centerX(from) + (centerX(to) - centerX(from)) * partialStep;
        float tipY = centerY(from) + (centerY(to) - centerY(from)) * partialStep;
        path.lineTo(tipX, tipY);

        canvas.drawPath(path, pathPaint);
        canvas.drawCircle(tipX, tipY, pathPaint.getStrokeWidth() * 0.8f, dotPaint);
    }

    void recycle() {
        base.recycle();
    }

    private void drawBase(Canvas canvas) {
        canvas.drawColor(colors[0]);

        Paint marker = new Paint(Paint.ANTI_ALIAS_FLAG);
        float markerRadius = cellSize * 0.3f;
        marker.setColor(colors[3]);
        canvas.drawCircle(centerX(0), centerY(0), markerRadius, marker);
        marker.setColor(colors[4]);
        canvas.drawCircle(centerX(maze.exit()), centerY(maze.exit()), markerRadius, marker);

        Paint wall = new Paint();
        wall.setColor(colors[1]);
        wall.setStyle(Paint.Style.STROKE);
        wall.setStrokeWidth(wallWidth);
        wall.setStrokeCap(Paint.Cap.SQUARE);

        int cells = maze.cells();
        float far = origin + cells * cellSize;
        canvas.drawRect(origin, origin, far, far, wall);
        for (int cell = 0; cell < cells * cells; cell++) {
            float right = origin + (cell % cells + 1) * cellSize;
            float bottom = origin + (cell / cells + 1) * cellSize;
            if (cell % cells != cells - 1 && !maze.isOpen(cell, Maze.EAST)) {
                canvas.drawLine(right, bottom - cellSize, right, bottom, wall);
            }
            if (cell / cells != cells - 1 && !maze.isOpen(cell, Maze.SOUTH)) {
                canvas.drawLine(right - cellSize, bottom, right, bottom, wall);
            }
        }
    }

    private float centerX(int cell) {
        return origin + (cell % maze.cells() + 0.5f) * cellSize;
    }

    private float centerY(int cell) {
        return origin + (cell / maze.cells() + 0.5f) * cellSize;
    }
}
