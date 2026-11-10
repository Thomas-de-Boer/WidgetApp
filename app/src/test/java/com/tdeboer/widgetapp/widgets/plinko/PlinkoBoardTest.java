package com.tdeboer.widgetapp.widgets.plinko;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlinkoBoardTest {
    private static final int MIDDLE = 4;

    // path bits with this many rights land in that slot: 0 rights is the far left slot
    private static int pathWithRights(int rights) {
        return (1 << rights) - 1;
    }

    private static PlinkoBoard landOne(int rights) {
        PlinkoBoard board = new PlinkoBoard(1, 1, 3);
        board.dropWithPath(pathWithRights(rights));
        board.advanceTo(PlinkoBoard.TRAVEL_FRAMES);
        return board;
    }

    // plays until nothing moves, a game that never ends fails the test
    private static void playOut(PlinkoBoard board) {
        int limit = board.frame() + 200_000;
        while (!board.isIdle()) {
            board.step();
            assertTrue("the game should end", board.frame() < limit);
        }
    }

    @Test
    public void middleSlotRemovesTheBall() {
        PlinkoBoard board = landOne(MIDDLE);
        assertEquals(0, board.inPlay());
        assertEquals(0, board.score());
    }

    @Test
    public void sideSlotsTurnTheBallIntoThatManyBalls() {
        assertEquals(3, landOne(0).inPlay());
        assertEquals(2, landOne(1).inPlay());
        assertEquals(2, landOne(2).inPlay());
        assertEquals(1, landOne(3).inPlay());
        assertEquals(1, landOne(5).inPlay());
        assertEquals(2, landOne(6).inPlay());
        assertEquals(2, landOne(7).inPlay());
        assertEquals(3, landOne(8).inPlay());
    }

    @Test
    public void slotGivesItsValueAsPoints() {
        assertEquals(3, landOne(0).score());
        assertEquals(2, landOne(1).score());
        assertEquals(1, landOne(3).score());
    }

    @Test
    public void longerChainsMultiplyThePoints() {
        assertEquals(1, PlinkoBoard.multiplier(1));
        assertEquals(1, PlinkoBoard.multiplier(14));
        assertEquals(2, PlinkoBoard.multiplier(15));
        assertEquals(3, PlinkoBoard.multiplier(30));
        assertEquals(5, PlinkoBoard.multiplier(60));
        assertEquals(10, PlinkoBoard.multiplier(100));
    }

    @Test
    public void onlyFiveBallsPerLevel() {
        PlinkoBoard board = new PlinkoBoard(3, 1, 3);
        for (int i = 0; i < PlinkoBoard.BALLS_PER_LEVEL; i++) {
            assertTrue(board.drop());
        }
        assertFalse(board.drop());
        assertEquals(0, board.ballsLeft());
    }

    @Test
    public void levelIsLostWithoutEnoughPoints() {
        PlinkoBoard board = new PlinkoBoard(1, 3, 3);
        for (int i = 0; i < PlinkoBoard.BALLS_PER_LEVEL; i++) {
            board.dropWithPath(pathWithRights(MIDDLE));
        }
        board.advanceTo(PlinkoBoard.TRAVEL_FRAMES);
        assertEquals(PlinkoBoard.LOST, board.phase());
        assertFalse(board.drop());

        playOut(board);
        assertEquals(3, board.level());
        assertEquals(2, board.lives());
        assertEquals(PlinkoBoard.BALLS_PER_LEVEL, board.ballsLeft());
        assertEquals(0, board.score());
        assertTrue(board.drop());
    }

    @Test
    public void lastLifeStartsAgainAtLevelOne() {
        PlinkoBoard board = new PlinkoBoard(1, 6, 3);
        for (int life = PlinkoBoard.START_LIVES; life > 0; life--) {
            assertEquals(6, board.level());
            assertEquals(life, board.lives());
            for (int i = 0; i < PlinkoBoard.BALLS_PER_LEVEL; i++) {
                board.dropWithPath(pathWithRights(MIDDLE));
            }
            board.advanceTo(board.frame() + PlinkoBoard.TRAVEL_FRAMES);
            assertEquals(life == 1 ? PlinkoBoard.GAME_OVER : PlinkoBoard.LOST, board.phase());
            playOut(board);
        }
        assertEquals(1, board.level());
        assertEquals(PlinkoBoard.START_LIVES, board.lives());
    }

    @Test
    public void levelIsWonWithEnoughPoints() {
        // find a game that reaches the target of level 1
        for (long seed = 1; seed < 500; seed++) {
            PlinkoBoard board = new PlinkoBoard(seed, 1, 3);
            boolean won = false;
            while (board.drop()) {
                // all at once
            }
            int limit = 200_000;
            while (!board.isIdle() && limit-- > 0) {
                board.step();
                won |= board.phase() == PlinkoBoard.WON;
            }
            if (won) {
                assertEquals(2, board.level());
                assertEquals(PlinkoBoard.BALLS_PER_LEVEL, board.ballsLeft());
                assertEquals(0, board.score());
                return;
            }
        }
        throw new AssertionError("no game reached the target of level 1");
    }

    @Test
    public void ballsStayWithinTheLimits() {
        for (long seed = 1; seed <= 200; seed++) {
            PlinkoBoard board = new PlinkoBoard(seed, 1, 3);
            while (board.drop()) {
                // all at once
            }
            while (!board.isIdle()) {
                board.step();
                assertTrue(board.balls().size() <= PlinkoBoard.MAX_ON_SCREEN);
                assertTrue(board.inPlay() <= PlinkoBoard.MAX_IN_PLAY);
            }
        }
    }

    @Test
    public void aCopyPlaysTheSameGame() {
        PlinkoBoard board = new PlinkoBoard(42, 1, 3);
        board.drop();
        board.drop();
        board.advanceTo(20);
        PlinkoBoard copy = board.copy();
        board.advanceTo(400);
        copy.advanceTo(400);
        assertEquals(board.inPlay(), copy.inPlay());
        assertEquals(board.score(), copy.score());
        assertEquals(board.level(), copy.level());
        assertEquals(board.balls().size(), copy.balls().size());
        for (int i = 0; i < board.balls().size(); i++) {
            assertEquals(board.balls().get(i).path, copy.balls().get(i).path);
        }
    }

    @Test
    public void ballEndsInItsSlot() {
        PlinkoBoard board = new PlinkoBoard(1, 1, 3);
        board.dropWithPath(pathWithRights(8));
        board.advanceTo(PlinkoBoard.TRAVEL_FRAMES - 1);
        float[] position = new float[2];
        board.position(board.balls().get(0), position);
        assertEquals(4f, position[0], 0.01f);
        assertTrue(position[1] > PlinkoBoard.ROWS - 1);
    }

    @Test
    public void quickFinishStopsNewBallsAfterOneMoreRound() {
        PlinkoBoard board = new PlinkoBoard(5, 1, 3);
        board.setQuickFinish(true);
        // four balls on +3 give 12 points, enough for level 1. the fifth ball lands after that
        for (int i = 0; i < 4; i++) {
            board.dropWithPath(pathWithRights(0));
        }
        board.advanceTo(10);
        board.dropWithPath(pathWithRights(0));

        board.advanceTo(PlinkoBoard.TRAVEL_FRAMES);
        assertEquals(12, board.score());
        // the balls of the four that reached the points are whole, like the fifth that was already falling
        assertEquals(13, board.inPlay());

        board.advanceTo(PlinkoBoard.TRAVEL_FRAMES + 50);
        assertEquals(15, board.score());
        int spent = 0;
        for (PlinkoBoard.Ball ball : board.balls()) {
            if (ball.spent) {
                spent++;
            }
        }
        // only the three balls of the fifth are spent
        assertEquals(3, spent);
        assertEquals(15, board.inPlay());

        // whole balls make spent balls, and spent balls make nothing: two more rounds at most
        int limit = board.frame() + 3 * PlinkoBoard.TRAVEL_FRAMES + 99 * PlinkoBoard.RELEASE_GAP;
        while (board.phase() == PlinkoBoard.PLAYING) {
            board.step();
            assertTrue(board.frame() < limit);
        }
        assertEquals(PlinkoBoard.WON, board.phase());
        playOut(board);
        assertEquals(2, board.level());

        // the next level starts with whole balls again
        board.dropWithPath(pathWithRights(0));
        assertFalse(board.balls().get(0).spent);
    }

    @Test
    public void targetGrowsEveryLevel() {
        assertEquals(10, PlinkoBoard.target(1));
        for (int level = 1; level < 200; level++) {
            assertTrue(PlinkoBoard.target(level + 1) >= PlinkoBoard.target(level));
        }
        assertTrue(PlinkoBoard.target(10) > PlinkoBoard.target(9));
    }

    // every chain has to end, and levels have to get harder slowly. the numbers are printed for tuning
    @Test
    public void chainsEndAndLevelsGetHarder() {
        int games = 2000;
        double[] passRate = new double[21];
        long chainBalls = 0;
        int chains = 0;
        int longChains = 0;
        for (int level = 1; level <= 20; level++) {
            int won = 0;
            for (int game = 0; game < games; game++) {
                PlinkoBoard board = new PlinkoBoard(level * 100_003L + game * 7919L + 1, level, 3);
                while (board.drop()) {
                    // all at once
                }
                int[] sizes = new int[PlinkoBoard.BALLS_PER_LEVEL];
                int limit = 200_000;
                while (board.phase() == PlinkoBoard.PLAYING) {
                    for (int chain = 0; chain < sizes.length; chain++) {
                        sizes[chain] = board.chainSize(chain);
                    }
                    board.step();
                    assertTrue("the game should end", limit-- > 0);
                }
                for (int size : sizes) {
                    chainBalls += size;
                    chains++;
                    if (size >= 15) {
                        longChains++;
                    }
                }
                if (board.phase() == PlinkoBoard.WON) {
                    won++;
                }
            }
            passRate[level] = won / (double) games;
        }
        double meanChain = chainBalls / (double) chains;
        System.out.println("PLINKO lean " + PlinkoBoard.LEAN + ": pass rate level 1 " + passRate[1]
                + ", 5 " + passRate[5] + ", 10 " + passRate[10] + ", 15 " + passRate[15] + ", 20 " + passRate[20]
                + "; balls per chain " + meanChain + "; chains of 15 or more " + longChains / (double) chains);

        assertTrue(meanChain > 4 && meanChain < 8);
        assertTrue(longChains / (double) chains < 0.15);
        assertTrue(passRate[1] > 0.7 && passRate[1] < 0.95);
        assertTrue(passRate[5] < passRate[1]);
        assertTrue(passRate[10] < passRate[5]);
        assertTrue(passRate[20] < passRate[10]);
        assertTrue(passRate[15] > 0.01);
    }
}