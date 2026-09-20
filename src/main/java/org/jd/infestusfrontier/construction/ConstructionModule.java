package org.jd.infestusfrontier.construction;

import net.neoforged.bus.api.IEventBus;
import org.jd.infestusfrontier.construction.api.BudRecipeRegistrar;

/** Composition boundary for construction content and its startup recipe port. */
public final class ConstructionModule {
    private final BudRecipeRegistry budRecipes = new BudRecipeRegistry();
    private final ConstructionContent content = new ConstructionContent(budRecipes);

    public void register(IEventBus modBus) {
        content.register(modBus);
    }

    public BudRecipeRegistrar budRecipes() {
        return budRecipes;
    }
}
