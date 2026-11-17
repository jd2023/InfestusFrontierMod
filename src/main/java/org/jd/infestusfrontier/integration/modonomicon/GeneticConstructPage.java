package org.jd.infestusfrontier.integration.modonomicon;

import com.google.gson.JsonObject;
import com.klikli_dev.modonomicon.book.BookTextHolder;
import com.klikli_dev.modonomicon.book.conditions.BookCondition;
import com.klikli_dev.modonomicon.book.conditions.BookNoneCondition;
import com.klikli_dev.modonomicon.book.page.BookRecipePage;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.genetics.GeneticsModule;
import org.jd.infestusfrontier.genetics.api.GeneticConstructRecipe;

/**
 * A guide page that shows one genetic construct: the genomes it needs, its materials, the construct, and its biomass.
 * Same JSON as Modonomicon's crafting_recipe page: {@code recipe_id_1}, {@code title1}, optional {@code text}.
 */
public final class GeneticConstructPage extends BookRecipePage<GeneticConstructRecipe> {
    public static final ResourceLocation TYPE =
            ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "genetic_construct");

    GeneticConstructPage(
            BookTextHolder title1,
            ResourceLocation recipeId1,
            BookTextHolder title2,
            ResourceLocation recipeId2,
            BookTextHolder text,
            String anchor,
            BookCondition condition) {
        super(GeneticsModule.GENETIC_CONSTRUCT.get(), title1, recipeId1, title2, recipeId2, text, anchor, condition);
    }

    public static GeneticConstructPage fromJson(JsonObject json, HolderLookup.Provider provider) {
        DataHolder data = commonFromJson(json, provider);
        String anchor = GsonHelper.getAsString(json, "anchor", "");
        BookCondition condition = json.has("condition")
                ? BookCondition.fromJson(TYPE, json.getAsJsonObject("condition"), provider)
                : new BookNoneCondition();
        return new GeneticConstructPage(
                data.title1(), data.recipeId1(), data.title2(), data.recipeId2(), data.text(), anchor, condition);
    }

    public static GeneticConstructPage fromNetwork(RegistryFriendlyByteBuf buf) {
        DataHolder data = commonFromNetwork(buf);
        String anchor = buf.readUtf();
        BookCondition condition = BookCondition.fromNetwork(buf);
        return new GeneticConstructPage(
                data.title1(), data.recipeId1(), data.title2(), data.recipeId2(), data.text(), anchor, condition);
    }

    @Override
    protected ItemStack getRecipeOutput(Level level, RecipeHolder<GeneticConstructRecipe> recipe) {
        return recipe == null ? ItemStack.EMPTY : recipe.value().getResultItem(level.registryAccess());
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }
}
