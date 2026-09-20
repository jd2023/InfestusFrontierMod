package org.jd.infestusfrontier.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.jd.infestusfrontier.organ.api.OrganHistory;
import org.jd.infestusfrontier.processing.api.BatchWork;
import org.jd.infestusfrontier.processing.api.CultureBowlRecipes;
import org.jd.infestusfrontier.processing.api.SharedRecipeCompletionAdmission;
import org.jd.infestusfrontier.storage.api.QuantityStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CultureBowlBatchWorkTest {
    @Test
    void bowlCatalogContainsOnlyTheSixAuthorizedRecipes() {
        assertEquals(
                List.of("I000", "I001", "I005", "I007", "I030", "I033"),
                new ArrayList<>(CultureBowlRecipes.all().keySet()));
        assertEquals(1, CultureBowlRecipes.recipe("I000").outputs().get("spore_culture"));
        assertEquals(1, CultureBowlRecipes.recipe("I001").outputs().get("organ_bud"));
        assertEquals(2, CultureBowlRecipes.recipe("I005").outputs().get("elastic_gel"));
        assertEquals(2, CultureBowlRecipes.recipe("I007").outputs().get("nutrient_mash"));
        assertEquals(2, CultureBowlRecipes.recipe("I030").outputs().get("honey_culture"));
        assertEquals(1, CultureBowlRecipes.recipe("I030").returnedContainers().get("glass_bottle"));
        assertEquals(2, CultureBowlRecipes.recipe("I033").outputs().get("rooting_gel"));
        assertTrue(CultureBowlRecipes.find("I004").isEmpty());
        assertTrue(CultureBowlRecipes.find("I006").isEmpty());
        assertTrue(CultureBowlRecipes.find("I029").isEmpty());
    }

    @Test
    void returnedBottleCapacityIsReservedBeforeHoneyInputsChange() {
        var store = store(
                List.of(
                        item("honey_bottle", 1),
                        item("spore_culture", 1),
                        item("honey_culture", 63),
                        item("glass_bottle", 64),
                        item("filler_1", 64),
                        item("filler_2", 64),
                        item("filler_3", 64),
                        item("filler_4", 64),
                        item("filler_5", 64)),
                List.of());
        var work = BatchWork.create(store, () -> true);
        var before = work.state();

        var refused = assertInstanceOf(
                BatchWork.Refused.class,
                work.start(new BatchWork.StartRequest("I030"), work.revision()));

        assertEquals(BatchWork.StartRefusal.RETURNED_CONTAINER_FULL, refused.reason());
        assertEquals(before, work.state());
    }

    @Test
    void insufficientWaterAndStaleRevisionRefuseWithoutReservationsOrConsumption() {
        var store = store(
                List.of(item("slime_ball", 1), item("spore_culture", 1)),
                List.of(tank("water", 49, 1000)));
        var work = BatchWork.create(store, () -> true);
        var before = work.state();

        var insufficient = assertInstanceOf(
                BatchWork.Refused.class,
                work.start(new BatchWork.StartRequest("I005"), work.revision()));
        assertEquals(BatchWork.StartRefusal.INSUFFICIENT_FLUID, insufficient.reason());
        assertEquals(before, work.state());

        var stale = assertInstanceOf(
                BatchWork.Refused.class,
                work.start(new BatchWork.StartRequest("I005"), work.revision() + 1));
        assertEquals(BatchWork.StartRefusal.STALE_REVISION, stale.reason());
        assertEquals(before, work.state());
    }

    @Test
    void fullProductCapacityRefusesBeforeInputsAreReserved() {
        var store = store(
                List.of(
                        item("spore_culture", 1),
                        item("rotten_flesh", 2),
                        item("bone_meal", 1),
                        item("organ_bud", 64),
                        item("filler_1", 64),
                        item("filler_2", 64),
                        item("filler_3", 64),
                        item("filler_4", 64),
                        item("filler_5", 64)),
                List.of());
        var work = BatchWork.create(store, () -> true);
        var before = work.state();

        var refused = assertInstanceOf(
                BatchWork.Refused.class,
                work.start(new BatchWork.StartRequest("I001"), work.revision()));

        assertEquals(BatchWork.StartRefusal.OUTPUT_FULL, refused.reason());
        assertEquals(before, work.state());
    }

    @Test
    void duplicateStartLeavesTheFirstBatchAndAllBalancesUnchanged() {
        var store = store(
                List.of(item("wheat", 1), item("carrot", 1)),
                List.of(tank("water", 100, 1000)));
        var work = BatchWork.create(store, () -> true);

        assertInstanceOf(
                BatchWork.Started.class,
                work.start(new BatchWork.StartRequest("I007"), work.revision()));
        var once = work.state();
        assertEquals(1, once.quantities().itemCount("wheat"));
        assertEquals(100, once.quantities().fluidAmount("water"));
        assertEquals(1, once.quantities().reservations().size());

        var duplicate = assertInstanceOf(
                BatchWork.Refused.class,
                work.start(new BatchWork.StartRequest("I007"), work.revision()));
        assertEquals(BatchWork.StartRefusal.ACTIVE_BATCH, duplicate.reason());
        assertEquals(once, work.state());
    }

    @Test
    void aMidwaySnapshotCompletesExactlyOnceAfterReload() {
        var store = store(
                List.of(item("wheat", 1), item("carrot", 1)),
                List.of(tank("water", 100, 1000)));
        var work = BatchWork.create(store, () -> true);
        work.start(new BatchWork.StartRequest("I007"), work.revision());
        var midway = work.advance(600);
        assertEquals(BatchWork.Status.WORKING, midway.status());
        assertEquals(600, midway.activeBatch().completedWorkUnits());

        var reloaded = BatchWork.restore(midway, () -> true);
        var completed = reloaded.advance(600);
        assertEquals(BatchWork.Status.IDLE, completed.status());
        assertEquals(0, completed.quantities().itemCount("wheat"));
        assertEquals(0, completed.quantities().itemCount("carrot"));
        assertEquals(0, completed.quantities().fluidAmount("water"));
        assertEquals(2, completed.quantities().itemCount("nutrient_mash"));
        assertEquals(1, completed.history().completedBatches());
        assertEquals(completed, reloaded.advance(1200));

        var completedReload = BatchWork.restore(completed, () -> true);
        assertEquals(completed, completedReload.advance(1200));
        assertEquals(1, completedReload.state().history().completedBatches());
    }

    @Test
    void oneSharedQuotaAdmitsOnlySixteenCompletionsAcrossBowls() {
        var tick = new AtomicLong(42);
        BatchWork.CompletionAdmission shared = new SharedRecipeCompletionAdmission(tick::get);
        var bowls = new ArrayList<BatchWork>();
        for (int index = 0; index < 17; index++) {
            var bowl = BatchWork.create(
                    store(
                            List.of(item("red_mushroom", 1), item("wheat_seeds", 1)),
                            List.of(tank("water", 100, 1000))),
                    shared);
            bowl.start(new BatchWork.StartRequest("I000"), bowl.revision());
            bowls.add(bowl);
        }

        for (var bowl : bowls) bowl.advance(1200);

        assertEquals(16, bowls.stream().filter(b -> b.state().status() == BatchWork.Status.IDLE).count());
        var blocked = bowls.get(16).state();
        assertEquals(BatchWork.Status.COMPLETION_BLOCKED, blocked.status());
        assertEquals(0, blocked.history().completedBatches());
        assertEquals(1, blocked.quantities().itemCount("red_mushroom"));

        tick.incrementAndGet();
        var admitted = bowls.get(16).advance(0);
        assertEquals(BatchWork.Status.IDLE, admitted.status());
        assertEquals(1, admitted.history().completedBatches());
    }

    @Test
    void successfulRecipesUseExactInputsOutputsAndReturnedContainer() {
        assertBatch(
                "I000",
                store(
                        List.of(item("brown_mushroom", 1), item("wheat_seeds", 1)),
                        List.of(tank("water", 100, 1000))),
                Map.of("spore_culture", 1));
        assertBatch(
                "I001",
                store(List.of(item("spore_culture", 1), item("rotten_flesh", 2), item("bone_meal", 1)), List.of()),
                Map.of("organ_bud", 1));
        assertBatch(
                "I005",
                store(
                        List.of(item("slime_ball", 1), item("spore_culture", 1)),
                        List.of(tank("water", 50, 1000))),
                Map.of("elastic_gel", 2));
        assertBatch(
                "I007",
                store(
                        List.of(item("wheat", 1), item("carrot", 1)),
                        List.of(tank("water", 100, 1000))),
                Map.of("nutrient_mash", 2));
        assertBatch(
                "I030",
                store(List.of(item("honey_bottle", 1), item("spore_culture", 1)), List.of()),
                Map.of("honey_culture", 2, "glass_bottle", 1));
        assertBatch(
                "I033",
                store(
                        List.of(item("wheat_seeds", 1), item("spore_culture", 1)),
                        List.of(tank("water", 50, 1000))),
                Map.of("rooting_gel", 2));
    }

    @Test
    void earnedChoicesReduceOnlyTheirSelectedBoundedCost() {
        var waterHistory = new OrganHistory.Snapshot(
                32, 32, List.of(OrganHistory.GrowthChoice.WATER_ECONOMY));
        var economical = BatchWork.create(
                store(
                        List.of(item("red_mushroom", 1), item("wheat_seeds", 1)),
                        List.of(tank("water", 90, 1000))),
                waterHistory,
                () -> true);
        assertInstanceOf(
                BatchWork.Started.class,
                economical.start(new BatchWork.StartRequest("I000"), economical.revision()));
        assertEquals(1200, economical.state().activeBatch().requiredWorkUnits());
        assertEquals(90, economical.state().quantities().fluidAmount("water"));
        assertEquals(BatchWork.Status.IDLE, economical.advance(1200).status());

        var speedHistory = new OrganHistory.Snapshot(
                128,
                128,
                List.of(OrganHistory.GrowthChoice.INCUBATION, OrganHistory.GrowthChoice.INCUBATION));
        var faster = BatchWork.create(
                store(
                        List.of(item("red_mushroom", 1), item("wheat_seeds", 1)),
                        List.of(tank("water", 100, 1000))),
                speedHistory,
                () -> true);
        faster.start(new BatchWork.StartRequest("I000"), faster.revision());
        assertEquals(960, faster.state().activeBatch().requiredWorkUnits());
        assertEquals(BatchWork.Status.IDLE, faster.advance(960).status());
        assertEquals(0, faster.state().quantities().fluidAmount("water"));
    }

    @Test
    void cancellationReleasesTheWholeReservationWithoutEarningHistory() {
        var store = store(
                List.of(item("wheat_seeds", 1), item("spore_culture", 1)),
                List.of(tank("water", 50, 1000)));
        var work = BatchWork.create(store, () -> true);
        work.start(new BatchWork.StartRequest("I033"), work.revision());

        assertEquals(BatchWork.CancelResult.CANCELLED, work.cancel(work.revision()));
        assertEquals(BatchWork.Status.IDLE, work.state().status());
        assertEquals(1, work.state().quantities().itemCount("wheat_seeds"));
        assertEquals(1, work.state().quantities().itemCount("spore_culture"));
        assertEquals(50, work.state().quantities().fluidAmount("water"));
        assertEquals(0, work.state().quantities().reservations().size());
        assertEquals(0, work.state().history().completedBatches());
    }

    @Test
    void recoveredCoreCarriesOneHistoryWithItsEarnedChoice() {
        var work = BatchWork.create(
                store(
                        List.of(item("spore_culture", 1), item("rotten_flesh", 2), item("bone_meal", 1)),
                        List.of()),
                new OrganHistory.Snapshot(31, 31, List.of()),
                () -> true);
        work.start(new BatchWork.StartRequest("I001"), work.revision());
        work.advance(1200);
        assertEquals(BatchWork.GrowthResult.CHOSEN, work.choose(
                OrganHistory.GrowthChoice.INCUBATION, work.revision()));

        var recoveredCore = work.snapshot();
        var replaced = BatchWork.restore(recoveredCore, () -> true);

        assertEquals(recoveredCore, replaced.snapshot());
        assertEquals(32, replaced.snapshot().history().completedBatches());
        assertEquals(List.of(OrganHistory.GrowthChoice.INCUBATION), replaced.snapshot().history().choices());
    }

    private static void assertBatch(String recipe, QuantityStore store, Map<String, Integer> outputs) {
        var work = BatchWork.create(store, () -> true);
        assertInstanceOf(
                BatchWork.Started.class,
                work.start(new BatchWork.StartRequest(recipe), work.revision()));
        var completed = work.advance(1200);
        assertEquals(BatchWork.Status.IDLE, completed.status());
        assertEquals(1, completed.history().completedBatches());
        outputs.forEach((item, amount) -> assertEquals(amount, completed.quantities().itemCount(item), item));
        store.snapshot().itemSlots().stream().filter(slot -> !slot.isEmpty()).forEach(slot ->
                assertEquals(0, completed.quantities().itemCount(slot.resource()), "Consumed input: " + slot.resource()));
    }

    private static QuantityStore store(List<QuantityStore.ItemSlot> items, List<QuantityStore.Tank> tanks) {
        var slots = new ArrayList<>(items);
        while (slots.size() < QuantityStore.MAX_ITEM_SLOTS) slots.add(QuantityStore.ItemSlot.empty(64));
        var fluidTanks = new ArrayList<>(tanks);
        while (fluidTanks.size() < QuantityStore.MAX_TANKS) fluidTanks.add(QuantityStore.Tank.empty(1000));
        return new QuantityStore(slots, fluidTanks);
    }

    private static QuantityStore.ItemSlot item(String item, int count) {
        return new QuantityStore.ItemSlot(item, count, 64);
    }

    private static QuantityStore.Tank tank(String fluid, int amount, int capacity) {
        return new QuantityStore.Tank(fluid, amount, capacity);
    }
}
