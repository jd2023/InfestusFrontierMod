package org.jd.infestusfrontier.processing;

import java.util.List;
import java.util.Set;

enum PreparationOrgan {
    MEMBRANE_RACK("I002", List.of("I002"), "water", Set.of("membrane_sheet")),
    BONE_LOOM("I003", List.of("I050", "I003"), "biomass", Set.of("bone_plate", "skeletal_graft"));

    final String defaultRecipeId;
    final List<String> recipePriority;
    final String fluid;
    final Set<String> outputs;

    PreparationOrgan(String defaultRecipeId, List<String> recipePriority, String fluid, Set<String> outputs) {
        this.defaultRecipeId = defaultRecipeId;
        this.recipePriority = List.copyOf(recipePriority);
        this.fluid = fluid;
        this.outputs = Set.copyOf(outputs);
    }

    boolean supports(String recipeId) {
        return recipePriority.contains(recipeId);
    }
}
