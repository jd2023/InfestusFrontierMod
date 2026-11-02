package org.jd.infestusfrontier.core.mineral;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class GrindingTest {
    @Test
    void everyMetalGivesFourDustAndFourResidualUnitsPerRawItem() {
        for (Metal metal : Metal.values()) {
            for (long raw : new long[] {0, 1, 7, 64, 4096}) {
                Grinding.Ground ground = Grinding.grind(raw);
                assertEquals(4 * raw, ground.dust(), metal.id());
                assertEquals(raw, ground.tailings(), metal.id());
                assertEquals(4 * raw, ground.residualUnits(), metal.id());
                assertEquals(raw, ground.residue(), metal.id());
                assertEquals(8 * raw, ground.recovered(), "every unit of metal is accounted: dust or residual");
            }
        }
    }

    @Test
    void anUnwashedRawItemMakesExactlyOneIngotAndNoDustIsLostOrInvented() {
        for (long raw = 0; raw < 200; raw++) {
            Grinding.Smelt smelt = Grinding.smelt(Grinding.grind(raw).dust());
            assertEquals(raw, smelt.ingots());
            assertEquals(0, smelt.leftover());
        }
    }

    @Test
    void partialDustWaitsAndSurplusStaysUsable() {
        assertEquals(new Grinding.Smelt(0, 3), Grinding.smelt(3));
        assertEquals(new Grinding.Smelt(1, 1), Grinding.smelt(5));
        assertEquals(new Grinding.Smelt(2, 3), Grinding.smelt(11));
        for (long dust = 0; dust < 100; dust++) {
            Grinding.Smelt smelt = Grinding.smelt(dust);
            assertEquals(dust, smelt.ingots() * 4 + smelt.leftover());
        }
    }

    @Test
    void checkedMathThrowsInsteadOfWrapping() {
        assertThrows(ArithmeticException.class, () -> Grinding.grind(Long.MAX_VALUE / 2));
        assertThrows(IllegalArgumentException.class, () -> Grinding.grind(-1));
        assertThrows(IllegalArgumentException.class, () -> Grinding.smelt(-1));
    }

    @Test
    void aParcelHoldsOneToFourUnitsOfOneMetal() {
        assertEquals(4, Parcel.ground(Metal.GOLD).units());
        assertEquals(Metal.COPPER, new Parcel(Metal.COPPER, 1).metal());
        assertThrows(IllegalArgumentException.class, () -> new Parcel(Metal.IRON, 0));
        assertThrows(IllegalArgumentException.class, () -> new Parcel(Metal.IRON, 5));
        assertThrows(IllegalArgumentException.class, () -> new Parcel(null, 4));
    }

    @Test
    void metalsAreFoundByTheirIds() {
        for (Metal metal : Metal.values()) assertEquals(Optional.of(metal), Metal.of(metal.id()));
        assertEquals(Optional.empty(), Metal.of("diamond"));
        assertEquals(Optional.empty(), Metal.of("netherite"));
    }
}
