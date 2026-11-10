package com.tdeboer.widgetapp.widgets.plinko;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// the game itself, without any drawing. time moves in frames of the animation.
// a ball bounces down 8 rows of pins and lands in one of 9 slots. the middle slot removes the ball,
// the other slots turn it into that many new balls, which drop from the top again.
// the same seed and the same drops always give the same game, so a part can be played again exactly
public final class PlinkoBoard {
    public static final int ROWS = 8;
    public static final int SLOTS = ROWS + 1;
    static final int[] SLOT_VALUES = {3, 2, 2, 1, -1, 1, 2, 2, 3};
    static final int MAX_ON_SCREEN = 30;
    static final int MAX_IN_PLAY = 99;

    // how long each part of the fall takes, in frames
    static final int DROP_FRAMES = 8;
    static final int ROW_FRAMES = 6;
    static final int FALL_FRAMES = 7;
    static final int TRAVEL_FRAMES = DROP_FRAMES + (ROWS - 1) * ROW_FRAMES + FALL_FRAMES;
    // new balls leave the top one by one, so they don't all fall as one
    static final int RELEASE_GAP = 2;

    // positions are in board units: one unit between two pins next to each other, and one unit between rows.
    // x = 0 is the middle, y = 0 is the top pin row
    static final float DROP_Y = -1.8f;
    static final float REST_ABOVE_PIN = 0.35f;
    static final float SLOT_Y = 8.55f;
    private static final float BOUNCE = 0.28f;
    private static final float DROP_SPREAD = 0.3f;

    static final class Ball {
        final int start;
        // bit n is the direction at pin row n: 0 is left, 1 is right
        final int path;
        final float jitter;

        Ball(int start, int path, float jitter) {
            this.start = start;
            this.path = path;
            this.jitter = jitter;
        }

        int slot() {
            return Integer.bitCount(path & ((1 << ROWS) - 1));
        }
    }

    private long random;
    private int frame;
    private final List<Ball> balls;
    private int waiting;
    private int lastRelease;
    private int peak;
    private final int[] lastHit;

    public PlinkoBoard(long seed) {
        random = seed == 0 ? 0x9E3779B97F4A7C15L : seed;
        balls = new ArrayList<>();
        lastRelease = -RELEASE_GAP;
        lastHit = new int[SLOTS];
        java.util.Arrays.fill(lastHit, Integer.MIN_VALUE / 2);
    }

    private PlinkoBoard(PlinkoBoard other) {
        random = other.random;
        frame = other.frame;
        balls = new ArrayList<>(other.balls);
        waiting = other.waiting;
        lastRelease = other.lastRelease;
        peak = other.peak;
        lastHit = other.lastHit.clone();
    }

    public PlinkoBoard copy() {
        return new PlinkoBoard(this);
    }

    public int frame() {
        return frame;
    }

    List<Ball> balls() {
        return balls;
    }

    // the balls that fall and the balls that still wait at the top
    public int inPlay() {
        return balls.size() + waiting;
    }

    // the most balls in play at once during this game
    public int peak() {
        return peak;
    }

    public boolean isEmpty() {
        return inPlay() == 0;
    }

    // frames since a ball landed in this slot, for a short flash
    int framesSinceHit(int slot) {
        return frame - lastHit[slot];
    }

    // the player drops a ball
    public void drop() {
        add(1);
        release();
        peak = Math.max(peak, inPlay());
    }

    // the best of this game starts again from the balls that are in play now
    public void resetPeak() {
        peak = inPlay();
    }

    // only for tests: removes every ball
    void clear() {
        balls.clear();
        waiting = 0;
    }

    // only for tests: a ball with a known path
    void dropWithPath(int path) {
        balls.add(new Ball(frame, path, 0f));
        peak = Math.max(peak, inPlay());
    }

    public void step() {
        frame++;
        Iterator<Ball> iterator = balls.iterator();
        while (iterator.hasNext()) {
            Ball ball = iterator.next();
            if (frame - ball.start >= TRAVEL_FRAMES) {
                iterator.remove();
                int slot = ball.slot();
                lastHit[slot] = frame;
                int value = SLOT_VALUES[slot];
                if (value > 0) {
                    add(value);
                }
            }
        }
        release();
        peak = Math.max(peak, inPlay());
    }

    public void advanceTo(int targetFrame) {
        while (frame < targetFrame) {
            step();
        }
    }

    // balls above the limit are lost
    private void add(int count) {
        waiting += Math.max(0, Math.min(count, MAX_IN_PLAY - inPlay()));
    }

    private void release() {
        if (waiting > 0 && balls.size() < MAX_ON_SCREEN && frame - lastRelease >= RELEASE_GAP) {
            float jitter = (nextFloat() * 2f - 1f) * DROP_SPREAD;
            balls.add(new Ball(frame, (int) (nextLong() & ((1 << ROWS) - 1)), jitter));
            waiting--;
            lastRelease = frame;
        }
    }

    // where the ball is in board units at the current frame, written into out[0] (x) and out[1] (y)
    void position(Ball ball, float[] out) {
        int time = frame - ball.start;
        if (time < DROP_FRAMES) {
            float s = time / (float) DROP_FRAMES;
            float restY = -REST_ABOVE_PIN;
            out[0] = ball.jitter * (1f - s);
            out[1] = DROP_Y + (restY - DROP_Y) * s * s;
            return;
        }

        time -= DROP_FRAMES;
        int hop = Math.min(time / ROW_FRAMES, ROWS - 1);
        float s = (time - hop * ROW_FRAMES) / (float) (hop == ROWS - 1 ? FALL_FRAMES : ROW_FRAMES);
        s = Math.min(1f, s);
        int rights = Integer.bitCount(ball.path & ((1 << hop) - 1));
        int bit = (ball.path >> hop) & 1;
        float x0 = rights - hop / 2f;
        float x1 = rights + bit - (hop + 1) / 2f;
        float y0 = hop - REST_ABOVE_PIN;

        if (hop < ROWS - 1) {
            float y1 = hop + 1 - REST_ABOVE_PIN;
            out[0] = x0 + (x1 - x0) * s;
            out[1] = y0 + (y1 - y0) * s * s - BOUNCE * 4f * s * (1f - s);
        } else {
            // off the last pin and down into the slot
            out[0] = x0 + (x1 - x0) * Math.min(1f, s * 2f);
            out[1] = y0 + (SLOT_Y - y0) * s * s - BOUNCE * 2f * s * (1f - s);
        }
    }

    // xorshift: small and easy to copy, so a copy of the board continues with the same numbers
    private long nextLong() {
        random ^= random << 13;
        random ^= random >>> 7;
        random ^= random << 17;
        return random;
    }

    private float nextFloat() {
        return (nextLong() >>> 40) / (float) (1 << 24);
    }
}
