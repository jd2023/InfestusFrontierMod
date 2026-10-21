package org.jd.infestusfrontier.core.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BodyTest {
    private final Map<String, Box> world = new HashMap<>();

    private void cell(int x, int y, int z, Box box) {
        world.put(x + "," + y + "," + z, box);
    }

    private void single(int x, int y, int z) {
        cell(x, y, z, Box.of(x, y, z));
    }

    private Optional<Box> at(int x, int y, int z) {
        return Optional.ofNullable(world.get(x + "," + y + "," + z));
    }

    @Test
    void twoSinglesFormARow() {
        single(0, 0, 0);
        single(1, 0, 0);
        assertEquals(Optional.of(new Box(0, 0, 0, 1, 0, 0)), Body.grow(Box.of(0, 0, 0), Box.of(1, 0, 0), this::at, 4));
    }

    @Test
    void aCellThatLeavesAGapWaitsOutside() {
        Box row = new Box(0, 0, 0, 1, 0, 0);
        cell(0, 0, 0, row);
        cell(1, 0, 0, row);
        single(0, 0, 1); // the start of a second row
        assertTrue(Body.grow(row, Box.of(0, 0, 1), this::at, 4).isEmpty(), "(1,0,1) is missing");
        single(1, 0, 1);
        assertEquals(Optional.of(new Box(0, 0, 0, 1, 0, 1)), Body.grow(row, Box.of(1, 0, 1), this::at, 4));
    }

    @Test
    void aBodyNeverTakesPartOfAnother() {
        Box a = new Box(0, 0, 0, 1, 0, 0);
        Box b = new Box(2, 0, 0, 2, 0, 1);
        cell(0, 0, 0, a);
        cell(1, 0, 0, a);
        cell(2, 0, 0, b);
        cell(2, 0, 1, b);
        assertTrue(Body.grow(a, Box.of(2, 0, 0), this::at, 4).isEmpty(), "b reaches outside the union");
    }

    @Test
    void aBodyStopsAtTheLongestSide() {
        for (int x = 0; x < 5; x++) single(x, 0, 0);
        Box four = new Box(0, 0, 0, 3, 0, 0);
        assertTrue(Body.grow(four, Box.of(4, 0, 0), this::at, 4).isEmpty());
    }

    @Test
    void theBottomLayerFillsFirstAndEvenly() {
        int[][] stored = {{0, 0}, {0, 0}};
        assertEquals(5000, Body.fill(stored, 4000, 5000));
        assertArrayEquals(new int[] {2500, 2500}, stored[0]);
        assertArrayEquals(new int[] {0, 0}, stored[1]);
        assertEquals(11000, Body.fill(stored, 4000, 20000), "only what fits");
        assertArrayEquals(new int[] {4000, 4000}, stored[0]);
        assertArrayEquals(new int[] {4000, 4000}, stored[1]);
    }

    @Test
    void theTopLayerDrainsFirst() {
        int[][] stored = {{4000, 4000}, {1000, 1001}};
        assertEquals(3000, Body.drain(stored, 4000, 3000));
        assertArrayEquals(new int[] {0, 0}, stored[1]);
        assertArrayEquals(new int[] {3500, 3501}, stored[0]);
        int total = 0;
        for (int[] layer : stored) for (int c : layer) total += c;
        assertEquals(7001, total);
    }

    @Test
    void smallStepsKeepALayerLevel() {
        int[][] stored = {{0, 0, 0}};
        for (int i = 0; i < 10; i++) Body.fill(stored, 4000, 1);
        assertArrayEquals(new int[] {4, 3, 3}, stored[0]);
        for (int i = 0; i < 4; i++) Body.drain(stored, 4000, 1);
        assertArrayEquals(new int[] {2, 2, 2}, stored[0]);
    }

    @Test
    void aLowCellCatchesUpFirst() {
        int[][] stored = {{100, 0}};
        assertEquals(150, Body.fill(stored, 4000, 150));
        assertArrayEquals(new int[] {125, 125}, stored[0]);
        assertEquals(60, Body.drain(stored, 4000, 60));
        assertArrayEquals(new int[] {95, 95}, stored[0]);
    }

    @Test
    void anOddAmountStillGoesInWhole() {
        int[][] stored = {{0, 0, 0}};
        assertEquals(10, Body.fill(stored, 4000, 10));
        assertEquals(10, stored[0][0] + stored[0][1] + stored[0][2]);
    }
}
