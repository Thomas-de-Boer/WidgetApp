package com.tdeboer.widgetapp.widgets.plinko;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

// the game itself, without any drawing. time moves in frames of the animation.
// a ball bounces down 8 rows of pins and lands in one of 9 slots. the middle slot removes the ball,
// the other slots turn it into that many new balls, which drop from the top again.
// every level gives a few balls to drop and a number of points to reach.
// the same seed and the same drops always give the same game, so a part can be played again exactly
public final class PlinkoBoard {
    public static final int ROWS = 8;
    public static final int SLOTS = ROWS + 1;
    static final int[] SLOT_VALUES = {3, 2, 2, 1, -1, 1, 2, 2, 3};
    static final int BALLS_PER_LEVEL = 5;
    static final int MAX_ON_SCREEN = 30;
    static final int MAX_IN_PLAY = 99;
    // how much more often a ball bounces towards the middle than away from it.
    // with this a ball makes a bit less than one new ball on average, so every chain ends.
    // 0.06 gives chains of 6 balls on average, and 1 in 10 chains gets to 15 balls
    static final float LEAN = 0.06f;

    static final int PLAYING = 0;
    static final int WON = 1;
    static final int LOST = 2;
    // the last life is gone: the game starts again at level 1
    static final int GAME_OVER = 3;
    static final int START_LIVES = 3;
    private static final int SPENT = 8;

    // how long each part of the fall takes, in frames
    static final int DROP_FRAMES = 8;
    static final int ROW_FRAMES = 6;
    static final int FALL_FRAMES = 7;
    static final int TRAVEL_FRAMES = DROP_FRAMES + (ROWS - 1) * ROW_FRAMES + FALL_FRAMES;
    // new balls leave the top one by one, so they don't all fall as one
    static final int RELEASE_GAP = 2;
    // how long the message at the end of a level stays
    static final int BANNER_FRAMES = 40;
    static final int POPUP_FRAMES = 16;

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
        // the dropped ball this one comes from, 0 for the first drop of the level
        final int chain;
        // a spent ball still gives points, but makes no new balls
        final boolean spent;

        Ball(int start, int path, float jitter, int chain, boolean spent) {
            this.start = start;
            this.path = path;
            this.jitter = jitter;
            this.chain = chain;
            this.spent = spent;
        }

        int slot() {
            return Integer.bitCount(path & ((1 << ROWS) - 1));
        }
    }

    // the points that float up from a slot
    static final class Popup {
        final int slot;
        final int points;
        final int start;

        Popup(int slot, int points, int start) {
            this.slot = slot;
            this.points = points;
            this.start = start;
        }
    }

    private long random;
    private int frame;
    private final List<Ball> balls;
    // the chains of the balls that wait at the top, with SPENT added for a spent ball
    private final ArrayDeque<Integer> waiting;
    private final List<Popup> popups;
    private int lastRelease;
    private final int[] lastHit;
    private int lastEvent;

    private int level;
    private int ballsLeft;
    private int score;
    // how many balls every chain of this level has made so far
    private final int[] chainSizes;
    private int lives;
    // with a quick finish the level is wrapped up once the points are reached: the balls that fall
    // at that moment still make new balls, and those new balls are spent
    private boolean quickFinish;
    private boolean finishing;
    private int phase;
    private int phaseEnd;

    public PlinkoBoard(long seed, int level, int lives) {
        random = seed == 0 ? 0x9E3779B97F4A7C15L : seed;
        balls = new ArrayList<>();
        waiting = new ArrayDeque<>();
        popups = new ArrayList<>();
        lastRelease = -RELEASE_GAP;
        lastHit = new int[SLOTS];
        Arrays.fill(lastHit, -1000);
        lastEvent = -1000;
        this.level = Math.max(1, level);
        this.lives = Math.max(1, Math.min(START_LIVES, lives));
        ballsLeft = BALLS_PER_LEVEL;
        chainSizes = new int[BALLS_PER_LEVEL];
    }

    private PlinkoBoard(PlinkoBoard other) {
        random = other.random;
        frame = other.frame;
        balls = new ArrayList<>(other.balls);
        waiting = new ArrayDeque<>(other.waiting);
        popups = new ArrayList<>(other.popups);
        lastRelease = other.lastRelease;
        lastHit = other.lastHit.clone();
        lastEvent = other.lastEvent;
        level = other.level;
        ballsLeft = other.ballsLeft;
        score = other.score;
        chainSizes = other.chainSizes.clone();
        lives = other.lives;
        quickFinish = other.quickFinish;
        finishing = other.finishing;
        phase = other.phase;
        phaseEnd = other.phaseEnd;
    }

    public PlinkoBoard copy() {
        return new PlinkoBoard(this);
    }

    // the points a level asks for: a fifth more every level.
    // most games give few points and a few give very many, so only a target that grows this fast gets harder
    static int target(int level) {
        double points = Math.min(1e9, 10 * Math.pow(1.2, level - 1));
        return (int) (points < 20 ? Math.round(points) : Math.round(points / 5) * 5);
    }

    // a chain that keeps going is worth more points per ball. the first step is reached by 1 in 10 chains,
    // the last by 1 in 600
    static int multiplier(int chainSize) {
        if (chainSize >= 100) {
            return 10;
        } else if (chainSize >= 60) {
            return 5;
        } else if (chainSize >= 30) {
            return 3;
        } else if (chainSize >= 15) {
            return 2;
        }
        return 1;
    }

    public void setQuickFinish(boolean quickFinish) {
        this.quickFinish = quickFinish;
    }

    public int frame() {
        return frame;
    }

    List<Ball> balls() {
        return balls;
    }

    List<Popup> popups() {
        return popups;
    }

    public int level() {
        return level;
    }

    // a lost level costs a life
    public int lives() {
        return lives;
    }

    int ballsLeft() {
        return ballsLeft;
    }

    int score() {
        return score;
    }

    int target() {
        return target(level);
    }

    // how many balls this chain has made so far, the dropped ball included
    int chainSize(int chain) {
        return chainSizes[chain];
    }

    // PLAYING, or WON, LOST or GAME_OVER while the message at the end of a level shows
    int phase() {
        return phase;
    }

    // the balls that fall and the balls that still wait at the top
    public int inPlay() {
        return balls.size() + waiting.size();
    }

    // the highest multiplier of the chains that still have balls
    int liveMultiplier() {
        int best = 1;
        for (Ball ball : balls) {
            best = Math.max(best, multiplier(chainSizes[ball.chain]));
        }
        for (int ball : waiting) {
            best = Math.max(best, multiplier(chainSizes[ball % SPENT]));
        }
        return best;
    }

    // true when nothing moves anymore, so one still image is enough
    public boolean isIdle() {
        return phase == PLAYING && inPlay() == 0 && frame - lastEvent >= POPUP_FRAMES;
    }

    // frames since a ball landed in this slot, for a short flash
    int framesSinceHit(int slot) {
        return frame - lastHit[slot];
    }

    // the player drops a ball. false when that is not possible now
    public boolean drop() {
        if (phase != PLAYING || ballsLeft == 0) {
            return false;
        }
        waiting.add(takeBall() + (finishing ? SPENT : 0));
        release();
        return true;
    }

    // only for tests: drops a ball with a known path
    void dropWithPath(int path) {
        balls.add(new Ball(frame, path, 0f, takeBall(), finishing));
    }

    // the chain that the next dropped ball starts
    private int takeBall() {
        int chain = BALLS_PER_LEVEL - ballsLeft;
        ballsLeft--;
        chainSizes[chain] = 1;
        lastEvent = frame;
        return chain;
    }

    public void step() {
        frame++;
        List<Ball> landed = null;
        Iterator<Ball> iterator = balls.iterator();
        while (iterator.hasNext()) {
            Ball ball = iterator.next();
            if (frame - ball.start >= TRAVEL_FRAMES) {
                iterator.remove();
                if (landed == null) {
                    landed = new ArrayList<>();
                }
                landed.add(ball);
            }
        }
        if (landed != null) {
            for (Ball ball : landed) {
                land(ball);
            }
        }
        popups.removeIf(popup -> frame - popup.start >= POPUP_FRAMES);
        release();
        updatePhase();
    }

    public void advanceTo(int targetFrame) {
        while (frame < targetFrame) {
            step();
        }
    }

    private void land(Ball ball) {
        int slot = ball.slot();
        lastHit[slot] = frame;
        lastEvent = frame;
        int value = SLOT_VALUES[slot];
        if (value > 0) {
            int points = value * multiplier(chainSizes[ball.chain]);
            score += points;
            popups.add(new Popup(slot, points, frame));
            // balls above the limit are lost
            int added = ball.spent ? 0 : Math.max(0, Math.min(value, MAX_IN_PLAY - inPlay()));
            chainSizes[ball.chain] += added;
            for (int i = 0; i < added; i++) {
                waiting.add(ball.chain + (finishing ? SPENT : 0));
            }
            // the ball that reaches the points still makes whole balls, everything after it is spent
            if (quickFinish && score >= target()) {
                finishing = true;
            }
        }
    }

    // a level ends when all balls are gone: won with enough points, lost without balls to drop
    private void updatePhase() {
        if (phase == PLAYING) {
            boolean started = ballsLeft < BALLS_PER_LEVEL;
            if (started && inPlay() == 0 && (score >= target() || ballsLeft == 0)) {
                if (score >= target()) {
                    phase = WON;
                } else {
                    lives--;
                    phase = lives == 0 ? GAME_OVER : LOST;
                }
                phaseEnd = frame + BANNER_FRAMES;
            }
        } else if (frame >= phaseEnd) {
            if (phase == WON) {
                level++;
            } else if (phase == GAME_OVER) {
                level = 1;
                lives = START_LIVES;
            }
            phase = PLAYING;
            ballsLeft = BALLS_PER_LEVEL;
            score = 0;
            Arrays.fill(chainSizes, 0);
            finishing = false;
            lastEvent = frame - POPUP_FRAMES;
        }
    }

    private void release() {
        if (!waiting.isEmpty() && balls.size() < MAX_ON_SCREEN && frame - lastRelease >= RELEASE_GAP) {
            float jitter = (nextFloat() * 2f - 1f) * DROP_SPREAD;
            int next = waiting.remove();
            balls.add(new Ball(frame, nextPath(), jitter, next % SPENT, next >= SPENT));
            lastRelease = frame;
        }
    }

    // left or right at every row, with a small lean towards the middle
    private int nextPath() {
        int path = 0;
        int rights = 0;
        for (int row = 0; row < ROWS; row++) {
            // twice the distance from the middle: negative is left of it
            int side = 2 * rights - row;
            float chanceRight = side < 0 ? 0.5f + LEAN : side > 0 ? 0.5f - LEAN : 0.5f;
            if (nextFloat() < chanceRight) {
                path |= 1 << row;
                rights++;
            }
        }
        return path;
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
