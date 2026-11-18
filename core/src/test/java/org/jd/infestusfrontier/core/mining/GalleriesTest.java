package org.jd.infestusfrontier.core.mining;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.junit.jupiter.api.Test;

class GalleriesTest {
    @Test
    void aJunctionJoinsThreeRoutesNeverBackTheWayItsRootCame() {
        for (int arrived = 0; arrived < 4; arrived++) {
            List<Integer> sides = Galleries.sides(arrived);
            assertEquals(Galleries.ROUTES_PER_JUNCTION, sides.size());
            assertFalse(sides.contains(Math.floorMod(arrived + 2, 4)), "not back up the root");
            assertEquals(List.of((arrived + 3) % 4, arrived, (arrived + 1) % 4), sides, "left, ahead, right");
        }
    }

    @Test
    void facesAreGivenCutsInTurn() {
        boolean[] both = {true, true};
        assertTrue(Galleries.admitted(0, 0, both), "the face whose turn it is");
        assertFalse(Galleries.admitted(1, 0, both), "a face waits while the other can cut");
        assertEquals(1, Galleries.after(0, 2));
        assertEquals(0, Galleries.after(1, 2), "and round again");
        int[] given = new int[2];
        int turn = 0;
        for (int ask = 0; ask < 40; ask++) {
            int asking = ask % 3 == 0 ? 1 : 0; // the first face asks twice as often
            if (!Galleries.admitted(turn, asking, both)) continue;
            given[asking]++;
            turn = Galleries.after(asking, 2);
        }
        assertTrue(Math.abs(given[0] - given[1]) <= 1, "neither face gets ahead: " + given[0] + ", " + given[1]);
    }

    @Test
    void aFaceThatCannotCutDoesNotHoldTheOthersUp() {
        boolean[] secondStopped = {true, false, true};
        assertTrue(Galleries.admitted(1, 2, secondStopped), "a stopped face's turn passes on");
        assertFalse(Galleries.admitted(0, 2, secondStopped), "a ready face ahead still goes first");
        boolean[] alone = {false, false, false, false};
        for (int turn = 0; turn < 4; turn++)
            for (int asking = 0; asking < 4; asking++)
                assertTrue(Galleries.admitted(turn, asking, alone), "with no other face ready, any face cuts");
        assertThrows(IllegalArgumentException.class, () -> Galleries.admitted(0, 4, alone), "only its faces");
        assertThrows(IllegalArgumentException.class, () -> Galleries.after(2, 2));
    }

    @Test
    void theLimitsAreFourFacesAndThreeJawsForTheWiderSection() {
        assertEquals(4, Galleries.MAX_FACES);
        assertEquals(3, Galleries.WIDE_JAWS);
        assertTrue(
                Galleries.MAX_ROUTES >= Galleries.ROUTES_PER_JUNCTION * Galleries.MAX_FACES,
                "a minehead can find every face its junctions open");
        assertEquals(
                Galleries.STOPS.size(),
                Galleries.STOPS.stream().map(Refusal::key).distinct().count());
        assertTrue(Galleries.STOPS.stream().noneMatch(Rootstocking.STOPS::contains));
    }
}
