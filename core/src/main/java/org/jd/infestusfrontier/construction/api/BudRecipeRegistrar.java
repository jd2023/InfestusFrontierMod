package org.jd.infestusfrontier.construction.api;

/** Startup-only port through which owning features attach their paid bud construction recipe. */
@FunctionalInterface
public interface BudRecipeRegistrar {
    void register(BudConstructionRecipe recipe);
}
