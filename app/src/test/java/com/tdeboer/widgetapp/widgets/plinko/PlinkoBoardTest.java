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

    // a board without glowing pins, so known paths give known results
    private static PlinkoBoard plainBoard(long seed, int level, int lives) {
        PlinkoBoard board = new PlinkoBoard(seed, level, lives);
        board.clearBonusPins();
        return board;
    }

    private static PlinkoBoard landOne(int rights) {
        PlinkoBoard board = plainBoard(1, 1, 3);
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
        assertEquals(4, landOne(0).inPlay());
        assertEquals(3, landOne(1).inPlay());
        assertEquals(2, landOne(2).inPlay());
        assertEquals(1, landOne(3).inPlay());
        assertEquals(1, landOne(5).inPlay());
        assertEquals(2, landOne(6).inPlay());
        assertEquals(3, landOne(7).inPlay());
        assertEquals(4, landOne(8).inPlay());
    }

    @Test
    public void slotGivesItsValueAsPoints() {
        assertEquals(4, landOne(0).score());
        assertEquals(3, landOne(1).score());
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
        PlinkoBoard board = plainBoard(1, 3, 3);
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
            board.clearBonusPins();
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
        PlinkoBoard board = plainBoard(5, 1, 3);
        board.setQuickFinish(true);
        // three balls on +4 give 12 points, enough for level 1. the fourth ball lands after that
        for (int i = 0; i < 3; i++) {
            board.dropWithPath(pathWithRights(0));
        }
        board.advanceTo(10);
        board.dropWithPath(pathWithRights(0));

        board.advanceTo(PlinkoBoard.TRAVEL_FRAMES);
        assertEquals(12, board.score());
        // the balls of the three that reached the points are whole, like the fourth that was already falling
        assertEquals(13, board.inPlay());

        board.advanceTo(PlinkoBoard.TRAVEL_FRAMES + 50);
        assertEquals(16, board.score());
        int spent = 0;
        for (PlinkoBoard.Ball ball : board.balls()) {
            if (ball.spent) {
                spent++;
            }
        }
        // only the four balls of the fourth are spent
        assertEquals(4, spent);
        assertEquals(16, board.inPlay());

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
        int levels = 0;
        int bonusLevels = 0;
        int bonusBalls = 0;
        for (int level = 1; level <= 20; level++) {
            int won = 0;
            for (int game = 0; game < games; game++) {
                PlinkoBoard board = new PlinkoBoard(level * 100_003L + game * 7919L + 1, level, 3);
                int[] sizes = new int[PlinkoBoard.MAX_CHAINS];
                int limit = 200_000;
                while (board.phase() == PlinkoBoard.PLAYING) {
                    // every ball as soon as it can be dropped, bonus balls too
                    while (board.drop()) {
                        // all at once
                    }
                    for (int chain = 0; chain < sizes.length; chain++) {
                        sizes[chain] = board.chainSize(chain);
                    }
                    board.step();
                    assertTrue("the game should end", limit-- > 0);
                }
                for (int size : sizes) {
                    if (size == 0) {
                        continue;
                    }
                    chainBalls += size;
                    chains++;
                    if (size >= 15) {
                        longChains++;
                    }
                }
                int bonuses = 0;
                for (int pin = 0; pin < PlinkoBoard.BONUS_PINS; pin++) {
                    if (board.bonusTaken(pin)) {
                        bonuses++;
                    }
                }
                bonusLevels += bonuses > 0 ? 1 : 0;
                bonusBalls += bonuses;
                levels++;
                if (board.phase() == PlinkoBoard.WON) {
                    won++;
                }
            }
            passRate[level] = won / (double) games;
        }
        double meanChain = chainBalls / (double) chains;
        System.out.println("PLINKO lean " + PlinkoBoard.LEAN + ": pass rate level 1 " + passRate[1]
                + ", 5 " + passRate[5] + ", 10 " + passRate[10] + ", 15 " + passRate[15] + ", 20 " + passRate[20]
                + "; balls per chain " + meanChain + "; chains of 15 or more " + longChains / (double) chains
                + "; levels with a bonus " + bonusLevels / (double) levels
                + "; bonus balls per level " + bonusBalls / (double) levels);

        assertTrue(passRate[1] > 0.7);
        assertTrue(passRate[5] <= passRate[1]);
        assertTrue(passRate[10] < passRate[5]);
        assertTrue(passRate[20] < passRate[10]);
        assertTrue(passRate[15] > 0.01);
    }

    // a ball makes just under one new ball on average: then every chain ends, but chains can get long
    @Test
    public void aBallMakesJustUnderOneNewBall() {
        double[] chance = {1};
        for (int row = 0; row < PlinkoBoard.ROWS; row++) {
            double[] next = new double[row + 2];
            for (int rights = 0; rights <= row; rights++) {
                int side = 2 * rights - row;
                double right = side < 0 ? 0.5 + PlinkoBoard.LEAN : side > 0 ? 0.5 - PlinkoBoard.LEAN : 0.5;
                next[rights + 1] += chance[rights] * right;
                next[rights] += chance[rights] * (1 - right);
            }
            chance = next;
        }
        double newBalls = 0;
        for (int slot = 0; slot < PlinkoBoard.SLOTS; slot++) {
            newBalls += chance[slot] * Math.max(0, PlinkoBoard.SLOT_VALUES[slot]);
        }
        System.out.println("PLINKO new balls per ball " + newBalls);
        assertTrue(newBalls > 0.98 && newBalls < 0.99);
    }

    // how far a whole game gets, from level 1 until the last life is gone. printed for tuning
    @Test
    public void wholeGamesReachALevel() {
        int games = 400;
        int[] reached = new int[games];
        for (int game = 0; game < games; game++) {
            PlinkoBoard board = new PlinkoBoard(game * 31_337L + 7, 1, PlinkoBoard.START_LIVES);
            int best = 1;
            int limit = 5_000_000;
            while (best < 60) {
                while (board.drop()) {
                    // all at once
                }
                board.step();
                if (board.phase() == PlinkoBoard.GAME_OVER) {
                    break;
                }
                best = Math.max(best, board.level());
                assertTrue("the game should end", limit-- > 0);
            }
            reached[game] = best;
        }
        java.util.Arrays.sort(reached);
        System.out.println("PLINKO highest level of a game: quarter " + reached[games / 4]
                + ", median " + reached[games / 2] + ", three quarters " + reached[games * 3 / 4]);
        assertTrue(reached[games / 2] >= 2);
    }

    @Test
    public void aGlowingPinGivesOneMoreBall() {
        PlinkoBoard board = plainBoard(1, 1, 3);
        // the far left path touches the first pin of every row
        board.setBonusPin(0, 5, 0);
        board.dropWithPath(pathWithRights(0));
        assertEquals(PlinkoBoard.BALLS_PER_LEVEL - 1, board.ballsLeft());
        board.advanceTo(PlinkoBoard.DROP_FRAMES + 5 * PlinkoBoard.ROW_FRAMES);
        assertTrue(board.bonusTaken(0));
        assertEquals(PlinkoBoard.BALLS_PER_LEVEL, board.ballsLeft());

        // the pin is dark now: a second ball over it gives nothing
        board.dropWithPath(pathWithRights(0));
        board.advanceTo(board.frame() + PlinkoBoard.TRAVEL_FRAMES);
        assertEquals(PlinkoBoard.BALLS_PER_LEVEL - 1, board.ballsLeft());
    }

    @Test
    public void aBallThatMissesTheGlowingPinGivesNothing() {
        PlinkoBoard board = plainBoard(1, 1, 3);
        board.setBonusPin(0, 5, 5);
        board.dropWithPath(pathWithRights(0));
        board.advanceTo(PlinkoBoard.TRAVEL_FRAMES);
        assertFalse(board.bonusTaken(0));
        assertEquals(PlinkoBoard.BALLS_PER_LEVEL - 1, board.ballsLeft());
    }

    @Test
    public void aBonusBallKeepsTheLevelGoing() {
        PlinkoBoard board = plainBoard(1, 3, 3);
        board.setBonusPin(0, 4, 4);
        // the middle path touches pin 4 of row 4, and lands in the middle slot
        for (int i = 0; i < PlinkoBoard.BALLS_PER_LEVEL; i++) {
            board.dropWithPath(pathWithRights(MIDDLE));
        }
        board.advanceTo(PlinkoBoard.TRAVEL_FRAMES + 5);
        assertEquals(PlinkoBoard.PLAYING, board.phase());
        assertEquals(1, board.ballsLeft());

        // the bonus ball is the sixth chain
        board.dropWithPath(pathWithRights(MIDDLE));
        assertEquals(1, board.chainSize(PlinkoBoard.BALLS_PER_LEVEL));
        board.advanceTo(board.frame() + PlinkoBoard.TRAVEL_FRAMES);
        assertEquals(PlinkoBoard.LOST, board.phase());
    }

    @Test
    public void eachLevelHasItsOwnGlowingPins() {
        PlinkoBoard board = new PlinkoBoard(7, 1, 3);
        for (int pin = 0; pin < PlinkoBoard.BONUS_PINS; pin++) {
            assertFalse(board.bonusTaken(pin));
            assertTrue(board.bonusRow(pin) >= PlinkoBoard.BONUS_MIN_ROW && board.bonusRow(pin) < PlinkoBoard.ROWS);
            assertTrue(board.bonusIndex(pin) >= 0 && board.bonusIndex(pin) <= board.bonusRow(pin));
        }
        PlinkoBoard same = new PlinkoBoard(7, 1, 3);
        for (int pin = 0; pin < PlinkoBoard.BONUS_PINS; pin++) {
            assertEquals(board.bonusRow(pin), same.bonusRow(pin));
            assertEquals(board.bonusIndex(pin), same.bonusIndex(pin));
        }
    }
}