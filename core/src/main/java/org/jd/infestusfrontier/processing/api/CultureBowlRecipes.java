package org.jd.infestusfrontier.processing.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** The finite M0 Culture Bowl recipe catalog. Binder-dependent recipes are deliberately absent. */
public final class CultureBowlRecipes {
    public static final int BASE_WORK_UNITS = 20 * 60;
    public static final int BIOMASS_BU = 0;
    private static final Map<String, Recipe> RECIPES = createRecipes();
    private static final BatchRecipeCatalog CATALOG = (recipeId, history) -> find(recipeId)
            .map(recipe -> recipe.itemInputAlternatives().stream().map(inputs ->
                    new BatchRecipeCatalog.ResolvedRecipe(
                            inputs, adjustedFluids(recipe.fluidInputs(), history), recipe.outputs(),
                            recipe.returnedContainers(), adjustedWork(recipe.baseWorkUnits(), history))).toList())
            .orElseGet(List::of);

    private CultureBowlRecipes() {}

    public static Map<String, Recipe> all() {
        return RECIPES;
    }

    public static Optional<Recipe> find(String catalogId) {
        return Optional.ofNullable(RECIPES.get(catalogId));
    }

    public static Recipe recipe(String catalogId) {
        var recipe = RECIPES.get(catalogId);
        if (recipe == null) throw new IllegalArgumentException("Unknown Culture Bowl recipe: " + catalogId);
        return recipe;
    }

    public static BatchRecipeCatalog catalog() { return CATALOG; }

    private static Map<String, Integer> adjustedFluids(Map<String, Integer> base, org.jd.infestusfrontier.organ.api.OrganHistory.Snapshot history) {
        int choices = history.choiceCount(org.jd.infestusfrontier.organ.api.OrganHistory.GrowthChoice.WATER_ECONOMY);
        if (choices == 0 || !base.containsKey("water")) return base;
        var adjusted = new LinkedHashMap<>(base);
        adjusted.put("water", economy(base.get("water"), choices));
        return Map.copyOf(adjusted);
    }

    private static int adjustedWork(int base, org.jd.infestusfrontier.organ.api.OrganHistory.Snapshot history) {
        return economy(base, history.choiceCount(org.jd.infestusfrontier.organ.api.OrganHistory.GrowthChoice.INCUBATION));
    }

    private static int economy(int base, int choices) {
        return Math.max(1, Math.multiplyExact(base, 10 - choices) / 10);
    }

    private static Map<String, Recipe> createRecipes() {
        var recipes = new LinkedHashMap<String, Recipe>();
        add(recipes, new Recipe(
                "I000",
                List.of(
                        Map.of("red_mushroom", 1, "wheat_seeds", 1),
                        Map.of("brown_mushroom", 1, "wheat_seeds", 1)),
                Map.of("water", 100),
                Map.of("spore_culture", 1),
                Map.of(),
                BASE_WORK_UNITS));
        add(recipes, new Recipe(
                "I001",
                List.of(Map.of("spore_culture", 1, "rotten_flesh", 2, "bone_meal", 1)),
                Map.of(),
                Map.of("organ_bud", 1),
                Map.of(),
                BASE_WORK_UNITS));
        add(recipes, new Recipe(
                "I005",
                List.of(Map.of("slime_ball", 1, "spore_culture", 1)),
                Map.of("water", 50),
                Map.of("elastic_gel", 2),
                Map.of(),
                BASE_WORK_UNITS));
        add(recipes, new Recipe(
                "I007",
                List.of(Map.of("wheat", 1, "carrot", 1)),
                Map.of("water", 100),
                Map.of("nutrient_mash", 2),
                Map.of(),
                BASE_WORK_UNITS));
        add(recipes, new Recipe(
                "I030",
                List.of(Map.of("honey_bottle", 1, "spore_culture", 1)),
                Map.of(),
                Map.of("honey_culture", 2),
                Map.of("glass_bottle", 1),
                BASE_WORK_UNITS));
        add(recipes, new Recipe(
                "I033",
                List.of(Map.of("wheat_seeds", 1, "spore_culture", 1)),
                Map.of("water", 50),
                Map.of("rooting_gel", 2),
                Map.of(),
                BASE_WORK_UNITS));
        return Collections.unmodifiableMap(recipes);
    }

    private static void add(Map<String, Recipe> recipes, Recipe recipe) {
        if (recipes.putIfAbsent(recipe.catalogId(), recipe) != null) {
            throw new IllegalStateException("Duplicate Culture Bowl recipe: " + recipe.catalogId());
        }
    }

    public record Recipe(
            String catalogId,
            List<Map<String, Integer>> itemInputAlternatives,
            Map<String, Integer> fluidInputs,
            Map<String, Integer> outputs,
            Map<String, Integer> returnedContainers,
            int baseWorkUnits) {
        public Recipe {
            if (catalogId == null || catalogId.isBlank()) throw new IllegalArgumentException("Catalog ID is required");
            Objects.requireNonNull(itemInputAlternatives, "itemInputAlternatives");
            if (itemInputAlternatives.isEmpty()) throw new IllegalArgumentException("At least one input is required");
            itemInputAlternatives = itemInputAlternatives.stream().map(CultureBowlRecipes::quantities).toList();
            fluidInputs = quantities(fluidInputs);
            outputs = quantities(outputs);
            returnedContainers = quantities(returnedContainers);
            if (outputs.isEmpty()) throw new IllegalArgumentException("At least one output is required");
            if (baseWorkUnits < 1) throw new IllegalArgumentException("Work units must be positive");
        }
    }

    private static Map<String, Integer> quantities(Map<String, Integer> source) {
        Objects.requireNonNull(source, "quantities");
        var copy = new LinkedHashMap<String, Integer>();
        for (var entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue() < 1) {
                throw new IllegalArgumentException("Quantities require named resources and positive amounts");
            }
            copy.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(copy);
    }
}
