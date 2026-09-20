package org.jd.infestusfrontier.construction.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves one explicit bud use and owns its no-partial-payment semantics.
 * Callers provide server-thread adapters for inventory, target replacement and shared admission.
 */
public final class BudConstructionPort {
    private final Map<String, BudConstructionRecipe> recipesByTrigger;

    public BudConstructionPort(List<BudConstructionRecipe> recipes) {
        var indexed = new LinkedHashMap<String, BudConstructionRecipe>();
        for (var recipe : List.copyOf(recipes)) {
            if (indexed.putIfAbsent(recipe.trigger(), recipe) != null) {
                throw new IllegalArgumentException("Construction recipe triggers must be unique: " + recipe.trigger());
            }
        }
        recipesByTrigger = Map.copyOf(indexed);
    }

    public boolean supports(String heldIngredient) {
        return recipesByTrigger.containsKey(heldIngredient);
    }

    public BudConstructionResult apply(String heldIngredient, Stock stock, Site site, Admission admission) {
        var recipe = recipesByTrigger.get(heldIngredient);
        if (recipe == null) return BudConstructionResult.UNAVAILABLE;
        if (!site.isBud()) return BudConstructionResult.TARGET_CHANGED;
        if (!hasAll(stock, recipe.costs())) return BudConstructionResult.INCOMPLETE;
        if (!admission.take()) return BudConstructionResult.ADMISSION_EXHAUSTED;
        if (!site.replace(recipe.output())) return BudConstructionResult.TARGET_CHANGED;
        stock.consume(recipe.costs());
        return BudConstructionResult.SUCCESS;
    }

    private static boolean hasAll(Stock stock, Map<String, Integer> costs) {
        for (var cost : costs.entrySet()) {
            if (stock.count(cost.getKey()) < cost.getValue()) return false;
        }
        return true;
    }

    public interface Stock {
        int count(String ingredient);
        void consume(Map<String, Integer> costs);
    }

    public interface Site {
        boolean isBud();
        boolean replace(String output);
    }

    @FunctionalInterface
    public interface Admission {
        boolean take();
    }
}
