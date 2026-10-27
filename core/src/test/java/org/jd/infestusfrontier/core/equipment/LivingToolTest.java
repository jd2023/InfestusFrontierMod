package org.jd.infestusfrontier.core.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LivingToolTest {
    @Test
    void mendingIsBoundedByWearReserveAndRate() {
        assertEquals(0, LivingTool.mend(0, 40), "a whole tool mends nothing");
        assertEquals(0, LivingTool.mend(10, 0), "an empty reserve mends nothing");
        assertEquals(3, LivingTool.mend(3, 40), "only the wear there is");
        assertEquals(2, LivingTool.mend(10, 2), "only the biomass there is");
        assertEquals(LivingTool.MEND_PER_SECOND, LivingTool.mend(100, 100), "four points a second at most");
        assertEquals(0, LivingTool.mend(-5, 40), "no negative wear");
    }

    @Test
    void aContractileToolBreaksWhatItsReservePaysFor() {
        assertEquals(0, LivingTool.spreads(0));
        assertEquals(0, LivingTool.spreads(1), "two BU a block");
        assertEquals(1, LivingTool.spreads(2));
        assertEquals(5, LivingTool.spreads(10));
        assertEquals(LivingTool.SPREAD, LivingTool.spreads(16), "the eight blocks round the one mined");
        assertEquals(LivingTool.SPREAD, LivingTool.spreads(40), "never more than the eight");
    }

    @Test
    void aGraspingFangGripsWhileItCanPay() {
        assertEquals(false, LivingTool.grips(0));
        assertEquals(false, LivingTool.grips(1));
        assertEquals(true, LivingTool.grips(2));
        assertEquals(true, LivingTool.grips(40));
    }

    @Test
    void aLivingToolOutlastsADormantOne() {
        assertEquals(96, LivingTool.DORMANT_USES);
        assertEquals(256, LivingTool.LIVING_USES);
    }
}
