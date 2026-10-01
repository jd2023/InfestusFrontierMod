package org.jd.infestusfrontier.processing.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jd.infestusfrontier.organ.api.OrganHistory;

/** Resolves each vanilla smelting input into its single Bio-Furnace batch alternative. */
public final class BioFurnaceRecipes {
    public static final int BASE_WORK_UNITS = 320;
    public static final int BASE_BIOMASS = 40;
    public static final String BIOMASS = "biomass";

    private BioFurnaceRecipes() {}

    @FunctionalInterface
    public interface SmeltingLookup {
        Optional<String> resultOf(String inputItemId);
    }

    public static BatchRecipeCatalog catalog(SmeltingLookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        return (inputItemId, history) -> {
            Objects.requireNonNull(inputItemId, "inputItemId");
            Objects.requireNonNull(history, "history");
            var result = Objects.requireNonNull(lookup.resultOf(inputItemId), "smelting result");
            return result.map(output -> new BatchRecipeCatalog.ResolvedRecipe(
                            Map.of(inputItemId, 1),
                            Map.of(BIOMASS, biomass(history)),
                            Map.of(output, 1),
                            Map.of(),
                            workUnits(history)))
                    .map(List::of)
                    .orElseGet(List::of);
        };
    }

    public static int workUnits(OrganHistory.Snapshot history) {
        Objects.requireNonNull(history, "history");
        return reducedByChoices(BASE_WORK_UNITS, history.choiceCount(OrganHistory.GrowthChoice.INCUBATION));
    }

    public static int biomass(OrganHistory.Snapshot history) {
        Objects.requireNonNull(history, "history");
        return reducedByChoices(BASE_BIOMASS, history.choiceCount(OrganHistory.GrowthChoice.WATER_ECONOMY));
    }

    private static int reducedByChoices(int base, int choices) {
        return base * (10 - Math.min(OrganHistory.MAX_CHOICES, choices)) / 10;
    }
}
