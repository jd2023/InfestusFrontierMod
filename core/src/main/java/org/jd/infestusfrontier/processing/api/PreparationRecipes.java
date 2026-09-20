package org.jd.infestusfrontier.processing.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Finite T0 Rack/Loom recipes. Binder-dependent skeletal grafts are deliberately absent. */
public final class PreparationRecipes {
    public static final int TICKS_PER_SECOND = 20;
    private static final Map<String, Recipe> RECIPES = createRecipes();
    private static final BatchRecipeCatalog CATALOG = (recipeId, history) -> find(recipeId)
            .map(recipe -> recipe.routes().stream().map(route -> new BatchRecipeCatalog.ResolvedRecipe(
                    route.itemInputs(), recipe.fluidInputs(), recipe.outputs(), Map.of(), route.workUnits())).toList())
            .orElseGet(List::of);

    private PreparationRecipes() {}

    public static Map<String, Recipe> all() { return RECIPES; }
    public static Optional<Recipe> find(String id) { return Optional.ofNullable(RECIPES.get(id)); }
    public static Recipe recipe(String id) {
        var recipe = RECIPES.get(id);
        if (recipe == null) throw new IllegalArgumentException("Unknown preparation recipe: " + id);
        return recipe;
    }
    public static BatchRecipeCatalog catalog() { return CATALOG; }

    private static Map<String, Recipe> createRecipes() {
        var recipes = new LinkedHashMap<String, Recipe>();
        add(recipes, new Recipe("I002", List.of(
                new Route(Map.of("rotten_flesh", 1, "string", 1), 20 * TICKS_PER_SECOND),
                new Route(Map.of("leather", 1, "string", 1), 40 * TICKS_PER_SECOND)),
                Map.of("water", 100), Map.of("membrane_sheet", 1)));
        add(recipes, new Recipe("I003", List.of(
                new Route(Map.of("bone", 1), 20 * TICKS_PER_SECOND)),
                Map.of("biomass", 50), Map.of("bone_plate", 1)));
        return Collections.unmodifiableMap(recipes);
    }

    private static void add(Map<String, Recipe> recipes, Recipe recipe) {
        if (recipes.putIfAbsent(recipe.catalogId(), recipe) != null) {
            throw new IllegalStateException("Duplicate preparation recipe: " + recipe.catalogId());
        }
    }

    public record Recipe(String catalogId, List<Route> routes,
            Map<String, Integer> fluidInputs, Map<String, Integer> outputs) {
        public Recipe {
            if (catalogId == null || catalogId.isBlank() || routes == null || routes.isEmpty()) {
                throw new IllegalArgumentException("Preparation recipe needs an ID and route");
            }
            routes = List.copyOf(routes);
            fluidInputs = quantities(fluidInputs);
            outputs = quantities(outputs);
            if (outputs.isEmpty()) throw new IllegalArgumentException("Preparation recipe needs output");
        }
    }

    public record Route(Map<String, Integer> itemInputs, int workUnits) {
        public Route {
            itemInputs = quantities(itemInputs);
            if (itemInputs.isEmpty() || workUnits < 1) throw new IllegalArgumentException("Preparation route is empty");
        }
    }

    private static Map<String, Integer> quantities(Map<String, Integer> source) {
        Objects.requireNonNull(source, "quantities");
        var copy = new LinkedHashMap<String, Integer>();
        source.forEach((resource, amount) -> {
            if (resource == null || resource.isBlank() || amount == null || amount < 1) {
                throw new IllegalArgumentException("Quantities require named resources and positive amounts");
            }
            copy.put(resource, amount);
        });
        return Collections.unmodifiableMap(copy);
    }
}
