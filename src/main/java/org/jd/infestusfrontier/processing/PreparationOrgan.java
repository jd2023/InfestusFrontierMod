package org.jd.infestusfrontier.processing;

enum PreparationOrgan {
    MEMBRANE_RACK("I002", "water", "membrane_sheet"),
    BONE_LOOM("I003", "biomass", "bone_plate");

    final String recipeId;
    final String fluid;
    final String output;

    PreparationOrgan(String recipeId, String fluid, String output) {
        this.recipeId = recipeId;
        this.fluid = fluid;
        this.output = output;
    }
}
