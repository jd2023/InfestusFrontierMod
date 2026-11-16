package org.jd.infestusfrontier.core.mining;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LeachingTest {
    @Test
    void aLayerIsASevenWideDiscOfAtMostThirtySevenColumns() {
        assertEquals(Leaching.MAX_BLOCKS_PER_LAYER, Leaching.DISC.size());
        int minX = 0, maxX = 0, minZ = 0, maxZ = 0;
        for (Leaching.Cell cell : Leaching.DISC) {
            minX = Math.min(minX, cell.dx());
            maxX = Math.max(maxX, cell.dx());
            minZ = Math.min(minZ, cell.dz());
            maxZ = Math.max(maxZ, cell.dz());
        }
        assertEquals(7, maxX - minX + 1);
        assertEquals(7, maxZ - minZ + 1);
    }

    @Test
    void theDiscIsRoundNotASquare() {
        assertTrue(Leaching.DISC.contains(new Leaching.Cell(0, 0)));
        assertTrue(Leaching.DISC.contains(new Leaching.Cell(3, 1)));
        assertTrue(Leaching.DISC.contains(new Leaching.Cell(-1, -3)));
        assertFalse(Leaching.DISC.contains(new Leaching.Cell(3, 3)), "a corner of the 7x7 square is out");
        assertFalse(Leaching.DISC.contains(new Leaching.Cell(2, 3)));
        assertFalse(Leaching.DISC.contains(new Leaching.Cell(4, 0)));
    }

    @Test
    void groundAtOrBelowStoneHardnessIsSoftEnough() {
        assertTrue(Leaching.softEnough(0.5F));
        assertTrue(Leaching.softEnough(1.5F), "stone is soft enough");
        assertFalse(Leaching.softEnough(3.0F), "deepslate is not");
        assertFalse(Leaching.softEnough(-1.0F), "bedrock is not");
    }
}
