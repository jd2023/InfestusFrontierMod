package org.jd.infestusfrontier.core.genetics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SynthesisTest {
    /** Slot contents as letters, materials as the letter they need; a slot fits a material holding its letter. */
    private static Optional<List<Integer>> assign(List<String> materials, List<String> slots) {
        return Synthesis.assign(
                materials.size(),
                slots.stream().map(slot -> !slot.isEmpty()).toList(),
                (material, slot) -> slots.get(slot).contains(materials.get(material)));
    }

    @Test
    void materialsMayLieInAnyOrder() {
        assertEquals(Optional.of(List.of(2, 0, 1)), assign(List.of("a", "b", "c"), List.of("b", "c", "a")));
        assertEquals(Optional.of(List.of(1)), assign(List.of("a"), List.of("", "a", "")));
    }

    @Test
    void aSlotServesOneMaterialOnly() {
        assertEquals(Optional.empty(), assign(List.of("a", "a"), List.of("a", "", "")));
        assertEquals(Optional.of(List.of(0, 2)), assign(List.of("a", "a"), List.of("a", "", "a")));
    }

    @Test
    void anEarlyGreedyChoiceIsUndoneWhenItBlocksALaterMaterial() {
        // "ab" fits both materials; "a" fits only the first. The first must leave "ab" to the second.
        assertEquals(Optional.of(List.of(1, 0)), assign(List.of("a", "b"), List.of("ab", "a", "")));
    }

    @Test
    void aStrayItemOrAMissingMaterialMakesNothing() {
        assertEquals(Optional.empty(), assign(List.of("a", "b"), List.of("a", "b", "x")), "a stray item");
        assertEquals(Optional.empty(), assign(List.of("a", "b"), List.of("a", "", "")), "a missing material");
        assertEquals(Optional.empty(), assign(List.of("a", "b"), List.of("a", "x", "")), "the wrong material");
    }

    @Test
    void moreMaterialsOrSlotsThanTheBankHasAreRefused() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Synthesis.assign(Genetics.MATERIAL_SLOTS + 1, List.of(true, true, true), (m, s) -> true));
        assertThrows(
                IllegalArgumentException.class,
                () -> Synthesis.assign(1, List.of(true, false, false, false), (m, s) -> true));
    }
}
