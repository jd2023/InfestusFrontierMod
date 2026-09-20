package org.jd.infestusfrontier.construction.api;

import java.util.Map;

/** Immutable recipe exposed to construction-owned platform composition. */
public record BudConstructionRecipe(String id, String trigger, String output, Map<String, Integer> costs) {
    public BudConstructionRecipe {
        requireName(id, "id");
        requireName(trigger, "trigger");
        requireName(output, "output");
        costs = Map.copyOf(costs);
        if (costs.isEmpty()) throw new IllegalArgumentException("Construction costs must not be empty");
        costs.forEach((ingredient, count) -> {
            requireName(ingredient, "ingredient");
            if (count == null || count < 1) {
                throw new IllegalArgumentException("Construction costs must be positive");
            }
        });
        if (!costs.containsKey(trigger)) {
            throw new IllegalArgumentException("Construction trigger must be one of its paid ingredients");
        }
    }

    private static void requireName(String value, String label) {
        if (value == null || value.isBlank() || value.length() > 256) {
            throw new IllegalArgumentException("Construction " + label + " must contain 1..256 characters");
        }
    }
}
