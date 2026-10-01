package org.jd.infestusfrontier.processing.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jd.infestusfrontier.organ.api.OrganHistory;

/** Supplies the finite, owner-defined alternatives that the shared batch engine may reserve. */
@FunctionalInterface
public interface BatchRecipeCatalog {
    List<ResolvedRecipe> alternatives(String recipeId, OrganHistory.Snapshot history);

    record ResolvedRecipe(
            Map<String, Integer> itemInputs,
            Map<String, Integer> fluidInputs,
            Map<String, Integer> outputs,
            Map<String, Integer> returnedContainers,
            int workUnits) {
        public ResolvedRecipe {
            itemInputs = Map.copyOf(Objects.requireNonNull(itemInputs, "itemInputs"));
            fluidInputs = Map.copyOf(Objects.requireNonNull(fluidInputs, "fluidInputs"));
            outputs = Map.copyOf(Objects.requireNonNull(outputs, "outputs"));
            returnedContainers = Map.copyOf(Objects.requireNonNull(returnedContainers, "returnedContainers"));
            if (outputs.isEmpty() || workUnits < 1) throw new IllegalArgumentException("A batch needs output and work");
        }
    }
}
