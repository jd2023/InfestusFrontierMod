package org.jd.infestusfrontier.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.jd.infestusfrontier.processing.api.BatchWork;
import org.jd.infestusfrontier.processing.api.PreparationRecipes;
import org.jd.infestusfrontier.processing.api.SharedRecipeCompletionAdmission;
import org.jd.infestusfrontier.storage.api.QuantityStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreparationBatchWorkTest {
    @Test
    void catalogOwnsOnlyMembraneAndPlateWithoutPrematureGraftsOrBoneBlocks() {
        assertEquals(List.of("I002", "I003"), new ArrayList<>(PreparationRecipes.all().keySet()));
        assertEquals(400, PreparationRecipes.recipe("I002").routes().getFirst().workUnits());
        assertEquals(800, PreparationRecipes.recipe("I002").routes().get(1).workUnits());
        assertEquals(Map.of("membrane_sheet", 1), PreparationRecipes.recipe("I002").outputs());
        assertEquals(Map.of("bone_plate", 1), PreparationRecipes.recipe("I003").outputs());
        assertTrue(PreparationRecipes.find("I050").isEmpty());
        assertTrue(PreparationRecipes.recipe("I003").routes().stream()
                .noneMatch(route -> route.itemInputs().containsKey("bone_block")));
    }

    @Test
    void fullRackOutputAndMissingWaterRefuseWithoutChangingInputs() {
        var full = work(store(List.of(
                item("rotten_flesh", 1), item("string", 1), item("membrane_sheet", 64),
                item("filler", 64)), List.of(tank("water", 100))));
        var fullBefore = full.state();
        var fullResult = assertInstanceOf(BatchWork.Refused.class,
                full.start(new BatchWork.StartRequest("I002"), full.revision()));
        assertEquals(BatchWork.StartRefusal.OUTPUT_FULL, fullResult.reason());
        assertEquals(fullBefore, full.state());

        var dry = work(store(List.of(item("rotten_flesh", 1), item("string", 1)),
                List.of(tank("water", 99))));
        var dryBefore = dry.state();
        var dryResult = assertInstanceOf(BatchWork.Refused.class,
                dry.start(new BatchWork.StartRequest("I002"), dry.revision()));
        assertEquals(BatchWork.StartRefusal.INSUFFICIENT_FLUID, dryResult.reason());
        assertEquals(dryBefore, dry.state());
    }

    @Test
    void leatherTakesTwiceAsLongWithoutDoublingTheSheet() {
        var leather = work(store(List.of(item("leather", 1), item("string", 1)),
                List.of(tank("water", 100))));
        assertInstanceOf(BatchWork.Started.class,
                leather.start(new BatchWork.StartRequest("I002"), leather.revision()));
        assertEquals(800, leather.state().activeBatch().requiredWorkUnits());
        assertEquals(BatchWork.Status.WORKING, leather.advance(400).status());
        var completed = leather.advance(400);
        assertEquals(BatchWork.Status.IDLE, completed.status());
        assertEquals(1, completed.quantities().itemCount("membrane_sheet"));
        assertEquals(0, completed.quantities().itemCount("leather"));
        assertEquals(0, completed.quantities().itemCount("string"));
        assertEquals(0, completed.quantities().fluidAmount("water"));
    }

    @Test
    void boneLoomConsumesOneBoneAndFiftyBiomassForOnePlate() {
        var loom = work(store(List.of(item("bone", 1)), List.of(tank("biomass", 50))));
        assertInstanceOf(BatchWork.Started.class,
                loom.start(new BatchWork.StartRequest("I003"), loom.revision()));
        assertEquals(400, loom.state().activeBatch().requiredWorkUnits());
        var completed = loom.advance(400);
        assertEquals(1, completed.quantities().itemCount("bone_plate"));
        assertEquals(0, completed.quantities().itemCount("bone"));
        assertEquals(0, completed.quantities().fluidAmount("biomass"));
        assertEquals(1, completed.history().completedBatches());

        var blockOnly = work(store(List.of(item("bone_block", 1)), List.of(tank("biomass", 450))));
        var before = blockOnly.state();
        var refused = assertInstanceOf(BatchWork.Refused.class,
                blockOnly.start(new BatchWork.StartRequest("I003"), blockOnly.revision()));
        assertEquals(BatchWork.StartRefusal.INSUFFICIENT_ITEM, refused.reason());
        assertEquals(before, blockOnly.state());
    }

    @Test
    void allPreparationOrgansShareTheExistingSixteenCompletionBudget() {
        var tick = new AtomicLong(7);
        BatchWork.CompletionAdmission admission = new SharedRecipeCompletionAdmission(tick::get);
        var organs = new ArrayList<BatchWork>();
        for (int index = 0; index < 17; index++) {
            var work = BatchWork.create(store(List.of(item("bone", 1)), List.of(tank("biomass", 50))),
                    PreparationRecipes.catalog(), admission);
            work.start(new BatchWork.StartRequest("I003"), work.revision());
            organs.add(work);
        }
        organs.forEach(work -> work.advance(400));
        assertEquals(16, organs.stream().filter(work -> work.state().status() == BatchWork.Status.IDLE).count());
        assertEquals(BatchWork.Status.COMPLETION_BLOCKED, organs.getLast().state().status());
        tick.incrementAndGet();
        assertEquals(BatchWork.Status.IDLE, organs.getLast().advance(0).status());
    }

    private static BatchWork work(QuantityStore store) {
        return BatchWork.create(store, PreparationRecipes.catalog(), () -> true);
    }

    private static QuantityStore store(List<QuantityStore.ItemSlot> items, List<QuantityStore.Tank> tanks) {
        var slots = new ArrayList<>(items);
        while (slots.size() < 4) slots.add(QuantityStore.ItemSlot.empty(64));
        var fluids = new ArrayList<>(tanks);
        while (fluids.size() < 1) fluids.add(QuantityStore.Tank.empty(1000));
        return new QuantityStore(slots, fluids);
    }

    private static QuantityStore.ItemSlot item(String resource, int count) {
        return new QuantityStore.ItemSlot(resource, count, 64);
    }

    private static QuantityStore.Tank tank(String resource, int amount) {
        return new QuantityStore.Tank(resource, amount, 1000);
    }
}
