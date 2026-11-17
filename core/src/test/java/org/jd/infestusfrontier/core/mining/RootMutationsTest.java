package org.jd.infestusfrontier.core.mining;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RootMutationsTest {
    @Test
    void aPlainHeadTakesStairsAndBecomesARootstock() {
        assertFalse(RootMutations.NONE.rootstock());
        assertEquals(Optional.empty(), RootMutations.NONE.refusal(RootMutation.STAIRS));
        RootMutations rootstock = RootMutations.NONE.with(RootMutation.STAIRS);
        assertTrue(rootstock.rootstock());
        assertTrue(rootstock.has(RootMutation.STAIRS));
        assertFalse(rootstock.has(RootMutation.WIDER_SECTION));
    }

    @Test
    void wideningNeedsARootstock() {
        assertEquals(Optional.of(Tunnelling.NOT_ROOTSTOCK), RootMutations.NONE.refusal(RootMutation.WIDER_SECTION));
        assertThrows(IllegalStateException.class, () -> RootMutations.NONE.with(RootMutation.WIDER_SECTION));
        RootMutations wide = RootMutations.NONE.with(RootMutation.STAIRS).with(RootMutation.WIDER_SECTION);
        assertEquals(List.of(RootMutation.STAIRS, RootMutation.WIDER_SECTION), wide.installed());
    }

    @Test
    void eachMutationIsInstalledOnceAndNeverRemoved() {
        RootMutations rootstock = RootMutations.NONE.with(RootMutation.STAIRS);
        assertEquals(Optional.of(Tunnelling.MUTATION_INSTALLED), rootstock.refusal(RootMutation.STAIRS));
        assertThrows(IllegalStateException.class, () -> rootstock.with(RootMutation.STAIRS));
        RootMutations wide = rootstock.with(RootMutation.WIDER_SECTION);
        for (RootMutation mutation : RootMutation.values())
            assertEquals(Optional.of(Tunnelling.MUTATION_INSTALLED), wide.refusal(mutation), mutation.id());
    }

    @Test
    void impossibleSavedMutationsDoNotConstruct() {
        assertTrue(RootMutations.problem(List.of(RootMutation.WIDER_SECTION)).isPresent());
        assertTrue(RootMutations.problem(List.of(RootMutation.STAIRS, RootMutation.STAIRS))
                .isPresent());
        assertTrue(RootMutations.problem(List.of(RootMutation.WIDER_SECTION, RootMutation.STAIRS))
                .isPresent());
        assertThrows(IllegalArgumentException.class, () -> new RootMutations(List.of(RootMutation.WIDER_SECTION)));
    }

    @Test
    void theSyncedBitsNameTheSameMutations() {
        RootMutations wide = RootMutations.NONE.with(RootMutation.STAIRS).with(RootMutation.WIDER_SECTION);
        assertEquals(wide.installed(), RootMutations.fromBits(wide.bits()));
        assertEquals(List.of(), RootMutations.fromBits(RootMutations.NONE.bits()));
        assertEquals(List.of(RootMutation.STAIRS), RootMutations.fromBits(1 | 1 << 20));
    }

    @Test
    void savedNamesReadBackAndUnknownNamesDoNot() {
        for (RootMutation mutation : RootMutation.values())
            assertEquals(Optional.of(mutation), RootMutation.byId(mutation.id()));
        assertEquals(Optional.empty(), RootMutation.byId("ore_sense"));
    }

    @Test
    void wideningCostsMoreThanStairsAndBothFitTheHeadsTank() {
        assertTrue(RootMutation.WIDER_SECTION.biomassUnits() > RootMutation.STAIRS.biomassUnits());
        for (RootMutation mutation : RootMutation.values())
            assertTrue(mutation.biomassUnits() > 0 && mutation.biomassUnits() <= Tunnelling.BIOMASS_CAPACITY_BU);
    }
}
