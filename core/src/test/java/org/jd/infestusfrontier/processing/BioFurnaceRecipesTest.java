package org.jd.infestusfrontier.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jd.infestusfrontier.organ.api.OrganHistory;
import org.jd.infestusfrontier.processing.api.BatchRecipeCatalog;
import org.jd.infestusfrontier.processing.api.BioFurnaceRecipes;
import org.junit.jupiter.api.Test;

class BioFurnaceRecipesTest {
    @Test
    void baselineIsSixteenSecondsAndFortyBiomass() {
        var recipe = resolved("minecraft:raw_iron", "minecraft:iron_ingot", history());

        assertEquals(320, recipe.workUnits());
        assertEquals(Map.of("minecraft:raw_iron", 1), recipe.itemInputs());
        assertEquals(Map.of(BioFurnaceRecipes.BIOMASS, 40), recipe.fluidInputs());
        assertEquals(Map.of("minecraft:iron_ingot", 1), recipe.outputs());
        assertEquals(Map.of(), recipe.returnedContainers());
    }

    @Test
    void eachSpeedChoiceRemovesTenPercentUpToThree() {
        assertEquals(320, BioFurnaceRecipes.workUnits(history()));
        assertEquals(288, BioFurnaceRecipes.workUnits(history(OrganHistory.GrowthChoice.INCUBATION)));
        assertEquals(256, BioFurnaceRecipes.workUnits(history(
                OrganHistory.GrowthChoice.INCUBATION, OrganHistory.GrowthChoice.INCUBATION)));
        assertEquals(224, BioFurnaceRecipes.workUnits(history(
                OrganHistory.GrowthChoice.INCUBATION,
                OrganHistory.GrowthChoice.INCUBATION,
                OrganHistory.GrowthChoice.INCUBATION)));
    }

    @Test
    void eachEconomyChoiceRemovesTenPercentUpToThree() {
        assertEquals(40, BioFurnaceRecipes.biomass(history()));
        assertEquals(36, BioFurnaceRecipes.biomass(history(OrganHistory.GrowthChoice.WATER_ECONOMY)));
        assertEquals(32, BioFurnaceRecipes.biomass(history(
                OrganHistory.GrowthChoice.WATER_ECONOMY, OrganHistory.GrowthChoice.WATER_ECONOMY)));
        assertEquals(28, BioFurnaceRecipes.biomass(history(
                OrganHistory.GrowthChoice.WATER_ECONOMY,
                OrganHistory.GrowthChoice.WATER_ECONOMY,
                OrganHistory.GrowthChoice.WATER_ECONOMY)));
    }

    @Test
    void mixedChoicesApplyIndependently() {
        var mixed = history(
                OrganHistory.GrowthChoice.INCUBATION,
                OrganHistory.GrowthChoice.WATER_ECONOMY,
                OrganHistory.GrowthChoice.INCUBATION);
        var recipe = resolved("minecraft:raw_gold", "minecraft:gold_ingot", mixed);

        assertEquals(256, recipe.workUnits());
        assertEquals(Map.of(BioFurnaceRecipes.BIOMASS, 36), recipe.fluidInputs());
    }

    @Test
    void unknownInputHasNoAlternative() {
        var catalog = BioFurnaceRecipes.catalog(input -> Optional.empty());

        assertEquals(List.of(), catalog.alternatives("minecraft:stone", history()));
    }

    @Test
    void outputIsAlwaysExactlyOneResult() {
        var recipe = resolved("minecraft:raw_iron", "minecraft:iron_ingot", history());

        assertEquals(Map.of("minecraft:iron_ingot", 1), recipe.outputs());
    }

    @Test
    void nullLookupOrHistoryIsRejected() {
        assertThrows(NullPointerException.class, () -> BioFurnaceRecipes.catalog(null));
        assertThrows(NullPointerException.class, () -> BioFurnaceRecipes.workUnits(null));
        assertThrows(NullPointerException.class, () -> BioFurnaceRecipes.biomass(null));
        assertThrows(NullPointerException.class,
                () -> BioFurnaceRecipes.catalog(input -> Optional.of("minecraft:iron_ingot"))
                        .alternatives("minecraft:raw_iron", null));
    }

    private static BatchRecipeCatalog.ResolvedRecipe resolved(
            String input, String output, OrganHistory.Snapshot history) {
        return BioFurnaceRecipes.catalog(candidate -> Optional.of(output))
                .alternatives(input, history)
                .getFirst();
    }

    private static OrganHistory.Snapshot history(OrganHistory.GrowthChoice... choices) {
        return new OrganHistory.Snapshot(0, 0, List.of(choices));
    }
}
