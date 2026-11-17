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
import org.jd.infestusfrontier.genetics.api.DnaSampleRecipe;

/**
 * A guide page that shows one DNA sample: the creature and the needle that take it, the sample, and what a DNA Bank's
 * extraction of it adds and costs. Same JSON as Modonomicon's crafting_recipe page: {@code recipe_id_1},
 * {@code title1}, optional {@code text}.
 */
public final class DnaSamplePage extends BookRecipePage<DnaSampleRecipe> {
    public static final ResourceLocation TYPE =
            ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "dna_sample");

    DnaSamplePage(
            BookTextHolder title1,
            ResourceLocation recipeId1,
            BookTextHolder title2,
            ResourceLocation recipeId2,
            BookTextHolder text,
            String anchor,
            BookCondition condition) {
        super(GeneticsModule.DNA_SAMPLE.get(), title1, recipeId1, title2, recipeId2, text, anchor, condition);
    }

    public static DnaSamplePage fromJson(JsonObject json, HolderLookup.Provider provider) {
        DataHolder data = commonFromJson(json, provider);
        String anchor = GsonHelper.getAsString(json, "anchor", "");
        BookCondition condition = json.has("condition")
                ? BookCondition.fromJson(TYPE, json.getAsJsonObject("condition"), provider)
                : new BookNoneCondition();
        return new DnaSamplePage(
                data.title1(), data.recipeId1(), data.title2(), data.recipeId2(), data.text(), anchor, condition);
    }

    public static DnaSamplePage fromNetwork(RegistryFriendlyByteBuf buf) {
        DataHolder data = commonFromNetwork(buf);
        String anchor = buf.readUtf();
        BookCondition condition = BookCondition.fromNetwork(buf);
        return new DnaSamplePage(
                data.title1(), data.recipeId1(), data.title2(), data.recipeId2(), data.text(), anchor, condition);
    }

    @Override
    protected ItemStack getRecipeOutput(Level level, RecipeHolder<DnaSampleRecipe> recipe) {
        return recipe == null ? ItemStack.EMPTY : recipe.value().getResultItem(level.registryAccess());
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }
}
