package org.jd.infestusfrontier.testmod.discovery.client;

import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** Loaded only by the optional JEI profile. */
@JeiPlugin
public final class JeiDiscoveryScenario implements IModPlugin {
    private static IJeiRuntime runtime;
    private static boolean opened;
    private static boolean pointerMoved;
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.parse("infestusfrontier_client:discovery"); }
    @Override public void onRuntimeAvailable(IJeiRuntime value) { runtime = value; }

    static boolean prepare(Minecraft game) {
        if (runtime == null) return false;
        var type = runtime.getRecipeManager().getRecipeType(ResourceLocation.parse("infestusfrontier:culture_bowl")).orElseThrow();
        long expected = org.jd.infestusfrontier.processing.api.CultureBowlRecipes.all().values().stream()
                .mapToLong(recipe -> recipe.itemInputAlternatives().size()).sum();
        if (runtime.getRecipeManager().createRecipeLookup(type).get().count() != expected)
            throw new IllegalStateException("JEI must expose every Bowl alternative");
        verifyIngredients(type);
        if (opened) {
            if (game.screen == null || !game.screen.getClass().getName().contains("RecipesGui"))
                throw new IllegalStateException("JEI recipe browser did not open");
            if (!pointerMoved) {
                long window = game.getWindow().getWindow();
                var callback = org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback(window, null);
                if (callback == null) throw new IllegalStateException("Missing client cursor callback");
                try { callback.invoke(window, 8, 8); }
                finally { org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback(window, callback); }
                pointerMoved = true;
                return false;
            }
            return game.mouseHandler.xpos() < 20 && game.mouseHandler.ypos() < 20;
        }
        runtime.getRecipesGui().showTypes(List.of(type));
        opened = true;
        return false;
    }
    private static <T> void verifyIngredients(mezz.jei.api.recipe.RecipeType<T> type) {
        var manager = runtime.getRecipeManager();
        var category = manager.getRecipeCategory(type);
        for (var display : manager.createRecipeLookup(type).get().toList()) {
            String path = category.getRegistryName(display).getPath();
            String variant = path.substring(path.lastIndexOf('/') + 1);
            var parts = variant.split("_");
            var recipe = org.jd.infestusfrontier.processing.api.CultureBowlRecipes.recipe(parts[0].toUpperCase(java.util.Locale.ROOT));
            var ingredients = manager.getRecipeIngredients(category, display);
            checkItems(ingredients.getIngredients(mezz.jei.api.recipe.RecipeIngredientRole.INPUT),
                    recipe.itemInputAlternatives().get(Integer.parseInt(parts[1])));
            var outputs = new java.util.HashMap<>(recipe.outputs());
            recipe.returnedContainers().forEach((key, amount) -> outputs.merge(key, amount, Integer::sum));
            checkItems(ingredients.getIngredients(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT), outputs);
            int water = 0;
            for (var ingredient : ingredients.getIngredients(mezz.jei.api.recipe.RecipeIngredientRole.INPUT)) {
                if (ingredient.getIngredient() instanceof net.neoforged.neoforge.fluids.FluidStack fluid) {
                    if (!fluid.is(net.minecraft.world.level.material.Fluids.WATER)) throw new IllegalStateException("Wrong JEI fluid");
                    water += fluid.getAmount();
                }
            }
            if (water != recipe.fluidInputs().getOrDefault("water", 0)) throw new IllegalStateException("Wrong JEI water amount");
        }
    }

    private static void checkItems(java.util.List<mezz.jei.api.ingredients.ITypedIngredient<?>> ingredients,
            java.util.Map<String, Integer> quantities) {
        var expected = new java.util.HashMap<net.minecraft.world.item.Item, Integer>();
        quantities.forEach((key, amount) -> expected.put(org.jd.infestusfrontier.processing.BowlResources.item(key), amount));
        var actual = new java.util.HashMap<net.minecraft.world.item.Item, Integer>();
        ingredients.forEach(ingredient -> ingredient.getItemStack().ifPresent(stack -> actual.merge(stack.getItem(), stack.getCount(), Integer::sum)));
        if (!actual.equals(expected)) throw new IllegalStateException("JEI inputs or outputs disagree with processing: " + actual);
    }

}
