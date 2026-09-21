package org.jd.infestusfrontier.integration.modonomicon;

import com.klikli_dev.modonomicon.book.BookTextHolder;
import com.klikli_dev.modonomicon.book.conditions.BookTrueCondition;
import com.klikli_dev.modonomicon.book.page.BookTextPage;
import com.klikli_dev.modonomicon.data.LoaderRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jd.infestusfrontier.processing.PreparationResources;
import org.jd.infestusfrontier.processing.api.PreparationRecipes;

/** Projects Rack/Loom recipes from the processing-owned catalog into guide pages. */
public final class PreparationGuidePages {
    public static void register() {
        LoaderRegistry.registerPageLoader(ResourceLocation.fromNamespaceAndPath("infestusfrontier", "preparation_recipe"),
                (json, registries) -> page(json.get("recipe").getAsString(), json.get("route").getAsInt()),
                BookTextPage::fromNetwork);
    }

    private static BookTextPage page(String catalogId, int routeIndex) {
        var recipe = PreparationRecipes.recipe(catalogId);
        if (routeIndex < 0 || routeIndex >= recipe.routes().size()) throw new IllegalArgumentException("Invalid preparation route");
        var route = recipe.routes().get(routeIndex);
        var text = Component.empty().append(items(route.itemInputs()));
        if (recipe.fluidInputs().containsKey("water")) text.append("\n\n").append(Component.translatable(
                "book.infestusfrontier.recipe.water", recipe.fluidInputs().get("water")));
        if (recipe.fluidInputs().containsKey("biomass")) text.append("\n\n").append(Component.translatable(
                "book.infestusfrontier.recipe.biomass", recipe.fluidInputs().get("biomass")));
        text.append("\n\n").append(Component.translatable("book.infestusfrontier.recipe.output"))
                .append(items(recipe.outputs()));
        text.append("\n\n").append(Component.translatable("book.infestusfrontier.recipe.duration",
                route.workUnits() / PreparationRecipes.TICKS_PER_SECOND));
        var output = recipe.outputs().keySet().iterator().next();
        return new BookTextPage(new BookTextHolder(PreparationResources.item(output).getDescription()),
                new BookTextHolder(text), false, true, "", new BookTrueCondition());
    }

    private static MutableComponent items(java.util.Map<String, Integer> quantities) {
        var text = Component.empty();
        quantities.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(entry -> {
            if (!text.getSiblings().isEmpty()) text.append(" + ");
            text.append(entry.getValue() + " × ").append(PreparationResources.item(entry.getKey()).getDescription());
        });
        return text;
    }

    private PreparationGuidePages() {}
}
