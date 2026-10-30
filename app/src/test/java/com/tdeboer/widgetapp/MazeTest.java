package com.tdeboer.widgetapp;

import com.tdeboer.widgetapp.widgets.maze.Maze;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.Assert.*;

public class MazeTest {
    private static final int[] SIZES = {8, 15, 25};
    private static final int[] STYLES = {Maze.STYLE_WINDING, Maze.STYLE_BRANCHY, Maze.STYLE_BALANCED};
    private static final int SEEDS = 200;

    @Test
    public void everyMazeIsPerfect() {
        forEveryMaze((maze, label) -> {
            int cellCount = maze.cells() * maze.cells();
            // a connected graph with exactly cells - 1 edges is a tree, so it has no loops
            assertEquals(label, cellCount - 1, countPassages(maze));
            assertEquals(label, cellCount, countReachable(maze));
        });
    }

    @Test
    public void wallsMatchFromBothSides() {
        forEveryMaze((maze, label) -> {
            int cells = maze.cells();
            for (int cell = 0; cell < cells * cells; cell++) {
                if (cell % cells != cells - 1) {
                    assertEquals(label, maze.isOpen(cell, Maze.EAST), maze.isOpen(cell + 1, Maze.WEST));
                }
                if (cell < cells * cells - cells) {
                    assertEquals(label, maze.isOpen(cell, Maze.SOUTH), maze.isOpen(cell + cells, Maze.NORTH));
                }
            }
        });
    }

    @Test
    public void solutionGoesFromStartToExitThroughOpenPassages() {
        forEveryMaze((maze, label) -> {
            int[] route = maze.solve();
            assertEquals(label, 0, route[0]);
            assertEquals(label, maze.exit(), route[route.length - 1]);
            for (int step = 1; step < route.length; step++) {
                assertTrue(label, isConnected(maze, route[step - 1], route[step]));
            }
        });
    }

    @Test
    public void sameSeedGivesSameMaze() {
        for (int style : STYLES) {
            Maze first = new Maze(15, 1234L, style);
            Maze second = new Maze(15, 1234L, style);
            assertArrayEquals(first.solve(), second.solve());
            assertEquals(describeWalls(first), describeWalls(second));
        }
    }

    @Test
    public void differentSeedsGiveDifferentMazes() {
        for (int style : STYLES) {
            assertNotEquals(describeWalls(new Maze(15, 1L, style)), describeWalls(new Maze(15, 2L, style)));
        }
    }

    @Test
    public void solverSpeedIsTheSameForEveryMazeSize() {
        int flipIntervalMillis = 40;
        for (int cellsPerSecond = 10; cellsPerSecond <= 60; cellsPerSecond += 5) {
            int speed = cellsPerSecond;
            forEveryMaze((maze, label) -> {
                int routeCells = maze.solve().length - 1;
                int frames = Maze.solveFrameCount(routeCells + 1, speed, flipIntervalMillis);
                double seconds = (frames - 1) * flipIntervalMillis / 1000.0;
                // rounding up to whole frames can only add less than one frame
                assertEquals(label, routeCells / (double) speed, seconds, flipIntervalMillis / 1000.0);
            });
        }
    }

    private interface MazeCheck {
        void check(Maze maze, String label);
    }

    private static void forEveryMaze(MazeCheck check) {
        for (int size : SIZES) {
            for (int style : STYLES) {
                for (long seed = 0; seed < SEEDS; seed++) {
                    check.check(new Maze(size, seed, style), "size " + size + " style " + style + " seed " + seed);
                }
            }
        }
    }

    private static int countPassages(Maze maze) {
        int passages = 0;
        for (int cell = 0; cell < maze.cells() * maze.cells(); cell++) {
            passages += maze.isOpen(cell, Maze.EAST) ? 1 : 0;
            passages += maze.isOpen(cell, Maze.SOUTH) ? 1 : 0;
        }
        return passages;
    }

    private static int countReachable(Maze maze) {
        int cells = maze.cells();
        boolean[] seen = new boolean[cells * cells];
        Deque<Integer> stack = new ArrayDeque<>();
        seen[0] = true;
        stack.push(0);
        int reachable = 0;
        while (!stack.isEmpty()) {
            int cell = stack.pop();
            reachable++;
            int[][] moves = {{Maze.NORTH, -cells}, {Maze.SOUTH, cells}, {Maze.WEST, -1}, {Maze.EAST, 1}};
            for (int[] move : moves) {
                if (maze.isOpen(cell, move[0]) && !seen[cell + move[1]]) {
                    seen[cell + move[1]] = true;
                    stack.push(cell + move[1]);
                }
            }
        }
        return reachable;
    }

    private static boolean isConnected(Maze maze, int from, int to) {
        int cells = maze.cells();
        if (to == from - cells) {
            return maze.isOpen(from, Maze.NORTH);
        }
        if (to == from + cells) {
            return maze.isOpen(from, Maze.SOUTH);
        }
        if (to == from - 1) {
            return from % cells != 0 && maze.isOpen(from, Maze.WEST);
        }
        return to == from + 1 && from % cells != cells - 1 && maze.isOpen(from, Maze.EAST);
    }

    private static String describeWalls(Maze maze) {
        StringBuilder walls = new StringBuilder();
        for (int cell = 0; cell < maze.cells() * maze.cells(); cell++) {
            walls.append(maze.isOpen(cell, Maze.EAST) ? '1' : '0');
            walls.append(maze.isOpen(cell, Maze.SOUTH) ? '1' : '0');
        }
        return walls.toString();
    }
}
