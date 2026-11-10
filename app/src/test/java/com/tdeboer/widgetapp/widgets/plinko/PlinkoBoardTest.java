package com.tdeboer.widgetapp.widgets.plinko;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlinkoBoardTest {
    // path bits with this many rights land in that slot: 0 rights is the far left slot
    private static int pathWithRights(int rights) {
        return (1 << rights) - 1;
    }

    private static PlinkoBoard landOne(int rights) {
        PlinkoBoard board = new PlinkoBoard(1);
        board.dropWithPath(pathWithRights(rights));
        board.advanceTo(PlinkoBoard.TRAVEL_FRAMES);
        return board;
    }

    @Test
    public void middleSlotRemovesTheBall() {
        PlinkoBoard board = landOne(4);
        assertEquals(0, board.inPlay());
        assertTrue(board.isEmpty());
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
    public void newBallsLeaveTheTopOneByOne() {
        PlinkoBoard board = landOne(0);
        assertEquals(1, board.balls().size());
        board.advanceTo(board.frame() + PlinkoBoard.RELEASE_GAP);
        assertEquals(2, board.balls().size());
    }

    @Test
    public void ballsStayWithinTheLimits() {
        PlinkoBoard board = new PlinkoBoard(7);
        int mostOnScreen = 0;
        for (int i = 0; i < 200; i++) {
            board.drop();
            board.step();
            assertTrue(board.balls().size() <= PlinkoBoard.MAX_ON_SCREEN);
            assertTrue(board.inPlay() <= PlinkoBoard.MAX_IN_PLAY);
            mostOnScreen = Math.max(mostOnScreen, board.balls().size());
        }
        assertEquals(PlinkoBoard.MAX_IN_PLAY, board.peak());
        assertTrue(mostOnScreen >= 25);
    }

    @Test
    public void aCopyPlaysTheSameGame() {
        PlinkoBoard board = new PlinkoBoard(42);
        board.drop();
        board.advanceTo(20);
        PlinkoBoard copy = board.copy();
        board.advanceTo(400);
        copy.advanceTo(400);
        assertEquals(board.inPlay(), copy.inPlay());
        assertEquals(board.peak(), copy.peak());
        assertEquals(board.balls().size(), copy.balls().size());
        for (int i = 0; i < board.balls().size(); i++) {
            assertEquals(board.balls().get(i).path, copy.balls().get(i).path);
        }
    }

    @Test
    public void slotsFollowTheBellCurve() {
        PlinkoBoard board = new PlinkoBoard(99);
        int[] slots = new int[PlinkoBoard.SLOTS];
        int samples = 20000;
        for (int i = 0; i < samples; i++) {
            board.drop();
            slots[board.balls().get(board.balls().size() - 1).slot()]++;
            board.clear();
            board.advanceTo(board.frame() + PlinkoBoard.RELEASE_GAP);
        }
        // 8 rows of left or right: the middle gets 27%, the slots next to it 22%, the far sides 0.4%
        assertEquals(0.273, slots[4] / (double) samples, 0.02);
        assertEquals(0.219, slots[3] / (double) samples, 0.02);
        assertEquals(0.219, slots[5] / (double) samples, 0.02);
        assertEquals(0.004, slots[0] / (double) samples, 0.004);
    }

    @Test
    public void ballEndsInItsSlot() {
        PlinkoBoard board = new PlinkoBoard(1);
        board.dropWithPath(pathWithRights(8));
        board.advanceTo(PlinkoBoard.TRAVEL_FRAMES - 1);
        float[] position = new float[2];
        board.position(board.balls().get(0), position);
        assertEquals(4f, position[0], 0.01f);
        assertTrue(position[1] > PlinkoBoard.ROWS - 1);
    }
}
