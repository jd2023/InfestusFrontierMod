package org.jd.infestusfrontier.construction;

import org.jd.infestusfrontier.construction.api.SeedPouch;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SeedPouchTest {
    @Test
    void retainsOnePlantingStockAndBoundsTypesAndCounts() {
        var pouch = new SeedPouch();
        assertEquals(64, pouch.insert("minecraft:wheat_seeds", 80).accepted());
        assertEquals(63, pouch.takeSurplus("minecraft:wheat_seeds", 64));
        assertEquals(1, pouch.count("minecraft:wheat_seeds"));

        pouch.insert("minecraft:carrot", 1);
        pouch.insert("minecraft:potato", 1);
        pouch.insert("minecraft:oak_sapling", 1);
        assertEquals(SeedPouch.InsertStatus.TYPE_CAPACITY,
                pouch.insert("minecraft:beetroot_seeds", 1).status());
        assertEquals(4, pouch.snapshot().size());
    }

    @Test
    void rejectedInsertAndSurplusRequestAreConservative() {
        var pouch = new SeedPouch();
        assertEquals(SeedPouch.InsertStatus.INVALID,
                pouch.insert("", 1).status());
        pouch.insert("minecraft:wheat_seeds", 2);
        assertEquals(1, pouch.takeSurplus("minecraft:wheat_seeds", 20));
        assertEquals(0, pouch.takeSurplus("minecraft:wheat_seeds", 20));
        assertEquals(1, pouch.count("minecraft:wheat_seeds"));
    }
}
