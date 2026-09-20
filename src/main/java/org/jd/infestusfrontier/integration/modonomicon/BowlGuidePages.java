package org.jd.infestusfrontier.integration.modonomicon;

import com.klikli_dev.modonomicon.book.BookTextHolder;
import com.klikli_dev.modonomicon.book.conditions.BookTrueCondition;
import com.klikli_dev.modonomicon.book.page.BookTextPage;
import com.klikli_dev.modonomicon.data.LoaderRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jd.infestusfrontier.processing.BowlResources;
import org.jd.infestusfrontier.processing.api.CultureBowlRecipes;

/** Projects the processing catalog into ordinary synchronized, localized book pages. */
public final class BowlGuidePages {
    public static void register() {
        LoaderRegistry.registerPageLoader(ResourceLocation.fromNamespaceAndPath("infestusfrontier", "bowl_recipe"),
                (json, registries) -> page(json.get("recipe").getAsString()), BookTextPage::fromNetwork);
    }

    private static BookTextPage page(String catalogId) {
        var recipe = CultureBowlRecipes.recipe(catalogId);
        var text = Component.empty();
        for (int i = 0; i < recipe.itemInputAlternatives().size(); i++) {
            if (i > 0) text.append("\n").append(Component.translatable("book.infestusfrontier.recipe.or")).append("\n");
            text.append(items(recipe.itemInputAlternatives().get(i)));
        }
        text.append("\n\n").append(Component.translatable("book.infestusfrontier.recipe.water",
                recipe.fluidInputs().getOrDefault("water", 0)));
        text.append("\n\n").append(Component.translatable("book.infestusfrontier.recipe.output"))
                .append(items(recipe.outputs()));
        if (!recipe.returnedContainers().isEmpty()) text.append("\n").append(
                Component.translatable("book.infestusfrontier.recipe.returned")).append(items(recipe.returnedContainers()));
        text.append("\n\n").append(Component.translatable("book.infestusfrontier.recipe.duration", recipe.baseWorkUnits() / 20));
        text.append("\n").append(Component.translatable("jei.infestusfrontier.biomass", CultureBowlRecipes.BIOMASS_BU));
        var output = recipe.outputs().keySet().iterator().next();
        return new BookTextPage(new BookTextHolder(BowlResources.item(output).getDescription()),
                new BookTextHolder(text), false, true, "", new BookTrueCondition());
    }

    private static MutableComponent items(java.util.Map<String, Integer> quantities) {
        var text = Component.empty();
        quantities.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(entry -> {
            if (!text.getSiblings().isEmpty()) text.append(" + ");
            text.append(entry.getValue() + " × ").append(BowlResources.item(entry.getKey()).getDescription());
        });
        return text;
    }

    private BowlGuidePages() {}
}
