package org.jd.infestusfrontier.core.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class PodKindTest {
    @Test
    void aBulkPodHoldsEightSixteenThenThirtyTwoStacks() {
        assertEquals(1, PodKind.BULK.compartments());
        assertEquals(8, PodKind.BULK.totalStacks(0));
        assertEquals(16, PodKind.BULK.totalStacks(1));
        assertEquals(32, PodKind.BULK.totalStacks(2));
    }

    @Test
    void aCrawHoldsFourKindsAtAQuarterOfTheDepth() {
        assertEquals(PodKind.CRAW_COMPARTMENTS, PodKind.CRAW.compartments());
        for (int grade = 0; grade <= 2; grade++) assertEquals(1 << grade, PodKind.CRAW.stacks(grade));
        assertEquals(4, PodKind.CRAW.totalStacks(0));
        assertEquals(8, PodKind.CRAW.totalStacks(1));
        assertEquals(16, PodKind.CRAW.totalStacks(2));
    }

    @Test
    void aCrawIsHalfABulkPodAtEveryGradeAndOneKindIsAQuarterOfIt() {
        for (int grade = 0; grade <= 2; grade++) {
            assertEquals(PodKind.BULK.totalStacks(grade), 2 * PodKind.CRAW.totalStacks(grade));
            assertEquals(PodKind.BULK.stacks(grade), 8 * PodKind.CRAW.stacks(grade));
        }
    }

    @Test
    void kindsAreFoundByTheirSavedName() {
        assertEquals(Optional.of(PodKind.CRAW), PodKind.byId("craw"));
        assertEquals(Optional.of(PodKind.BULK), PodKind.byId("bulk"));
        assertEquals(Optional.empty(), PodKind.byId("larder"));
    }

    @Test
    void anImpossibleGradeIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> PodKind.CRAW.stacks(-1));
    }
}
