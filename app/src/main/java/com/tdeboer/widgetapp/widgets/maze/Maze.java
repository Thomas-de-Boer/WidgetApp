package com.tdeboer.widgetapp.widgets.maze;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Random;

// a square perfect maze: every style carves a spanning tree, so there is exactly one route between any two cells
public final class Maze {
    public static final int DEFAULT_CELLS = 15;
    public static final int DEFAULT_SPEED = 30;
    public static final int STYLE_WINDING = 0;
    public static final int STYLE_BRANCHY = 1;
    public static final int STYLE_BALANCED = 2;

    public static final int NORTH = 1;
    public static final int EAST = 2;
    public static final int SOUTH = 4;
    public static final int WEST = 8;
    private static final int[] DIRECTIONS = {NORTH, EAST, SOUTH, WEST};

    private final int cells;
    // per cell: the directions that are open, as a combination of the flags above
    private final int[] openings;

    public Maze(int cells, long seed, int style) {
        this.cells = cells;
        openings = new int[cells * cells];
        Random random = new Random(seed);
        switch (style) {
            case STYLE_BRANCHY:
                carveBranchy(random);
                break;
            case STYLE_BALANCED:
                carveBalanced(random);
                break;
            default:
                carveWinding(random);
                break;
        }
    }

    public int cells() {
        return cells;
    }

    public int exit() {
        return openings.length - 1;
    }

    public boolean isOpen(int cell, int direction) {
        return (openings[cell] & direction) != 0;
    }

    // the only route from the top left cell to the bottom right cell, both included
    public int[] solve() {
        int[] cameFrom = new int[openings.length];
        Arrays.fill(cameFrom, -1);
        cameFrom[0] = 0;
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(0);
        while (!queue.isEmpty() && cameFrom[exit()] < 0) {
            int cell = queue.remove();
            for (int direction : DIRECTIONS) {
                if (isOpen(cell, direction) && cameFrom[neighbor(cell, direction)] < 0) {
                    cameFrom[neighbor(cell, direction)] = cell;
                    queue.add(neighbor(cell, direction));
                }
            }
        }

        List<Integer> reversed = new ArrayList<>();
        for (int cell = exit(); cell != 0; cell = cameFrom[cell]) {
            reversed.add(cell);
        }
        int[] route = new int[reversed.size() + 1];
        for (int i = 0; i < reversed.size(); i++) {
            route[route.length - 1 - i] = reversed.get(i);
        }
        return route;
    }

    // the frames needed to walk a route at a fixed speed, so a longer route takes longer instead of going faster
    public static int solveFrameCount(int routeLength, int cellsPerSecond, int flipIntervalMillis) {
        // whole numbers only: with doubles, rounding errors can add a frame to a route that fits exactly
        long routeMillis = (routeLength - 1) * 1000L;
        long millisPerFrameAtOneCell = (long) cellsPerSecond * flipIntervalMillis;
        return (int) ((routeMillis + millisPerFrameAtOneCell - 1) / millisPerFrameAtOneCell) + 1;
    }

    // walks forward until stuck, then backtracks: long corridors with few dead ends
    private void carveWinding(Random random) {
        boolean[] visited = new boolean[openings.length];
        visited[0] = true;
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(0);
        while (!stack.isEmpty()) {
            int cell = stack.peek();
            int direction = randomDirection(cell, visited, false, random);
            if (direction == 0) {
                stack.pop();
                continue;
            }
            int next = neighbor(cell, direction);
            carve(cell, direction);
            visited[next] = true;
            stack.push(next);
        }
    }

    // randomized Prim: grows outward from the visited area, which gives many short dead ends
    private void carveBranchy(Random random) {
        boolean[] visited = new boolean[openings.length];
        boolean[] queued = new boolean[openings.length];
        List<Integer> frontier = new ArrayList<>();
        visited[0] = true;
        addFrontier(0, visited, queued, frontier);
        while (!frontier.isEmpty()) {
            int index = random.nextInt(frontier.size());
            int cell = frontier.get(index);
            frontier.set(index, frontier.get(frontier.size() - 1));
            frontier.remove(frontier.size() - 1);

            carve(cell, randomDirection(cell, visited, true, random));
            visited[cell] = true;
            addFrontier(cell, visited, queued, frontier);
        }
    }

    // Wilson's algorithm: random walks until they hit the maze, every possible maze is equally likely.
    // storing only the last direction left from each cell erases the loops of a walk
    private void carveBalanced(Random random) {
        boolean[] visited = new boolean[openings.length];
        int[] lastDirection = new int[openings.length];
        visited[0] = true;
        for (int start = 1; start < openings.length; start++) {
            int cell = start;
            while (!visited[cell]) {
                int direction;
                do {
                    direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
                } while (!isInside(cell, direction));
                lastDirection[cell] = direction;
                cell = neighbor(cell, direction);
            }
            for (cell = start; !visited[cell]; cell = neighbor(cell, lastDirection[cell])) {
                visited[cell] = true;
                carve(cell, lastDirection[cell]);
            }
        }
    }

    private void addFrontier(int cell, boolean[] visited, boolean[] queued, List<Integer> frontier) {
        for (int direction : DIRECTIONS) {
            if (isInside(cell, direction)) {
                int next = neighbor(cell, direction);
                if (!visited[next] && !queued[next]) {
                    queued[next] = true;
                    frontier.add(next);
                }
            }
        }
    }

    // a random direction whose neighbor has the wanted visited state, or 0 if there is none
    private int randomDirection(int cell, boolean[] visited, boolean wantVisited, Random random) {
        int[] options = new int[DIRECTIONS.length];
        int count = 0;
        for (int direction : DIRECTIONS) {
            if (isInside(cell, direction) && visited[neighbor(cell, direction)] == wantVisited) {
                options[count++] = direction;
            }
        }
        return count == 0 ? 0 : options[random.nextInt(count)];
    }

    private void carve(int cell, int direction) {
        openings[cell] |= direction;
        openings[neighbor(cell, direction)] |= opposite(direction);
    }

    private boolean isInside(int cell, int direction) {
        switch (direction) {
            case NORTH:
                return cell >= cells;
            case SOUTH:
                return cell < openings.length - cells;
            case WEST:
                return cell % cells != 0;
            default:
                return cell % cells != cells - 1;
        }
    }

    private int neighbor(int cell, int direction) {
        switch (direction) {
            case NORTH:
                return cell - cells;
            case SOUTH:
                return cell + cells;
            case WEST:
                return cell - 1;
            default:
                return cell + 1;
        }
    }

    private static int opposite(int direction) {
        switch (direction) {
            case NORTH:
                return SOUTH;
            case SOUTH:
                return NORTH;
            case WEST:
                return EAST;
            default:
                return WEST;
        }
    }
}
