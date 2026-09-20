package org.jd.infestusfrontier.construction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.jd.infestusfrontier.construction.api.BudConstructionPort;
import org.jd.infestusfrontier.construction.api.BudConstructionRecipe;
import org.jd.infestusfrontier.construction.api.BudConstructionResult;
import org.jd.infestusfrontier.foundation.TickQuota;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudConstructionPortTest {
    private static final BudConstructionRecipe SAC = new BudConstructionRecipe(
            "digestive_sac", "bowl", "digestive_sac", Map.of("rotten_flesh", 2, "bowl", 1));

    @Test
    void incompleteAndUnknownApplicationsLeaveBudAndStockUnchanged() {
        var port = new BudConstructionPort(List.of(SAC));
        var stock = new MutableStock(Map.of("rotten_flesh", 1, "bowl", 1));
        var site = new MutableSite();
        var admission = new CountingAdmission(true);

        assertEquals(BudConstructionResult.INCOMPLETE, port.apply("bowl", stock, site, admission));
        assertEquals(BudConstructionResult.UNAVAILABLE, port.apply("glass", stock, site, admission));
        assertTrue(site.bud);
        assertEquals(Map.of("rotten_flesh", 1, "bowl", 1), stock.snapshot());
        assertEquals(0, admission.calls);
    }

    @Test
    void aCommittedConstructionCannotSpendTwice() {
        var port = new BudConstructionPort(List.of(SAC));
        var stock = new MutableStock(Map.of("rotten_flesh", 4, "bowl", 2));
        var site = new MutableSite();
        var admission = new CountingAdmission(true);

        assertEquals(BudConstructionResult.SUCCESS, port.apply("bowl", stock, site, admission));
        assertEquals(BudConstructionResult.TARGET_CHANGED, port.apply("bowl", stock, site, admission));
        assertFalse(site.bud);
        assertEquals("digestive_sac", site.output);
        assertEquals(Map.of("rotten_flesh", 2, "bowl", 1), stock.snapshot());
        assertEquals(1, admission.calls);
    }

    @Test
    void exhaustedAdmissionLeavesBudAndIngredientsUntouched() {
        var port = new BudConstructionPort(List.of(SAC));
        var stock = new MutableStock(Map.of("rotten_flesh", 2, "bowl", 1));
        var site = new MutableSite();

        assertEquals(
                BudConstructionResult.ADMISSION_EXHAUSTED,
                port.apply("bowl", stock, site, new CountingAdmission(false)));
        assertTrue(site.bud);
        assertEquals(Map.of("rotten_flesh", 2, "bowl", 1), stock.snapshot());
    }

    @Test
    void recipesRequirePositiveCostsAndUniqueTriggers() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BudConstructionRecipe("bad", "bowl", "bad", Map.of("bowl", 0)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new BudConstructionPort(List.of(
                        SAC,
                        new BudConstructionRecipe("other", "bowl", "other", Map.of("bowl", 1)))));
    }

    @Test
    void sharedQuotaAdmitsOnlySixteenPlacementsInOneTick() {
        var port = new BudConstructionPort(List.of(SAC));
        var quota = new TickQuota(16);
        int successes = 0;
        int refused = 0;
        for (int attempt = 0; attempt < 17; attempt++) {
            var result = port.apply(
                    "bowl",
                    new MutableStock(Map.of("rotten_flesh", 2, "bowl", 1)),
                    new MutableSite(),
                    () -> quota.take(42));
            if (result == BudConstructionResult.SUCCESS) successes++;
            if (result == BudConstructionResult.ADMISSION_EXHAUSTED) refused++;
        }
        assertEquals(16, successes);
        assertEquals(1, refused);
    }

    private static final class MutableStock implements BudConstructionPort.Stock {
        private final Map<String, Integer> counts = new LinkedHashMap<>();

        private MutableStock(Map<String, Integer> initial) {
            counts.putAll(initial);
        }

        @Override
        public int count(String ingredient) {
            return counts.getOrDefault(ingredient, 0);
        }

        @Override
        public void consume(Map<String, Integer> costs) {
            costs.forEach((ingredient, count) -> counts.compute(ingredient, (ignored, old) -> old - count));
        }

        private Map<String, Integer> snapshot() {
            return Map.copyOf(counts);
        }
    }

    private static final class MutableSite implements BudConstructionPort.Site {
        private boolean bud = true;
        private String output;

        @Override
        public boolean isBud() {
            return bud;
        }

        @Override
        public boolean replace(String requestedOutput) {
            if (!bud) return false;
            bud = false;
            output = requestedOutput;
            return true;
        }
    }

    private static final class CountingAdmission implements BudConstructionPort.Admission {
        private final boolean allowed;
        private int calls;

        private CountingAdmission(boolean allowed) {
            this.allowed = allowed;
        }

        @Override
        public boolean take() {
            calls++;
            return allowed;
        }
    }
}
