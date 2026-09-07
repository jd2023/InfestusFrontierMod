package org.jd.infestusfrontier.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.block.entity.CorruptionCoreBlockEntity;

import javax.annotation.Nullable;

public class CorruptionCoreBiomassConversionRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final int output;
    private final NonNullList<Ingredient> ingredients;
    public CorruptionCoreBiomassConversionRecipe(ResourceLocation id, int output, NonNullList<Ingredient> ingredients){
        this.id = id;
        this.output = output;
        this.ingredients = ingredients;
    }
    @Override
    public boolean matches(SimpleContainer pContainer, Level pLevel) {
        if (pLevel.isClientSide()) {
            return false;
        }

        return ingredients.get(0).test(pContainer.getItem(0));
    }

    @Override
    public ItemStack assemble(SimpleContainer pContainer) {
        return null;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem() {
        return null;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }
    public int getBiomassValue() {
        return output;
    }
    public static class Type implements RecipeType<CorruptionCoreBiomassConversionRecipe> {
        private Type() { }
        public static final Type INSTANCE = new Type();
        public static final String ID = "corruption_core_biomass";
    }


    public static class Serializer implements RecipeSerializer<CorruptionCoreBiomassConversionRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID =
                new ResourceLocation(InfestusFrontier.MODID, "corruption_core_biomass");

        @Override
        public CorruptionCoreBiomassConversionRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
            int output = GsonHelper.getAsInt(pSerializedRecipe, "output");

            JsonArray ingredients = GsonHelper.getAsJsonArray(pSerializedRecipe, "ingredients");
            NonNullList<Ingredient> inputs = NonNullList.withSize(1, Ingredient.EMPTY);

            for (int i = 0; i < inputs.size(); i++) {
                inputs.set(i, Ingredient.fromJson(ingredients.get(i)));
            }

            return new CorruptionCoreBiomassConversionRecipe(pRecipeId, output, inputs);
        }

        @Override
        public @Nullable CorruptionCoreBiomassConversionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            NonNullList<Ingredient> inputs = NonNullList.withSize(buf.readInt(), Ingredient.EMPTY);

            for (int i = 0; i < inputs.size(); i++) {
                inputs.set(i, Ingredient.fromNetwork(buf));
            }

            int output = buf.readInt();
            return new CorruptionCoreBiomassConversionRecipe(id, output, inputs);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, CorruptionCoreBiomassConversionRecipe recipe) {
            buf.writeInt(recipe.getIngredients().size());

            for (Ingredient ing : recipe.getIngredients()) {
                ing.toNetwork(buf);
            }
            buf.writeInt(recipe.getBiomassValue());
        }
    }
}

