package org.jd.infestusfrontier.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.datagen.InfestusBlockTagGenerator;
import org.jetbrains.annotations.Nullable;

public class MutationByMutationPoolRecipe implements Recipe<SimpleContainer> {
    private final NonNullList<Ingredient> inputItems;
    private final ItemStack output;
    private final ResourceLocation id;

    public MutationByMutationPoolRecipe(NonNullList<Ingredient> inputItems, ItemStack output, ResourceLocation id) {
        this.inputItems = inputItems;
        this.output = output;
        this.id = id;
    }


    @Override
    public boolean matches(SimpleContainer container, Level level) {
        if (level.isClientSide()){
            return false;
        }
        int count=0;
        for (int i=0; i<inputItems.size()-1;i++){
            if (inputItems.get(i).test(container.getItem(i))){
                count++;
            }
        }
        return count==7;
    }

    @Override
    public ItemStack assemble(SimpleContainer p_44001_, RegistryAccess p_267165_) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess p_267052_) {
        return output.copy();
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
    public static class Type implements RecipeType<MutationByMutationPoolRecipe>{
        public static final Type INSTANCE = new Type();
        public static final String ID="mutation_by_mutation_pool";
    }
    public static class Serializer implements RecipeSerializer<MutationByMutationPoolRecipe>{
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID= new ResourceLocation(InfestusFrontier.MODID, "mutation_by_mutation_pool");


        @Override
        public MutationByMutationPoolRecipe fromJson(ResourceLocation id, JsonObject recipe) {
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(recipe, "output"));
            JsonArray ingredients=GsonHelper.getAsJsonArray(recipe, "ingredients");
            NonNullList<Ingredient> inputs = NonNullList.withSize(7, Ingredient.EMPTY);
            for (int i=0; i<ingredients.size(); i++){
                inputs.set(i, Ingredient.fromJson(ingredients.get(i)));
            }
            return new MutationByMutationPoolRecipe(inputs, output, id);
        }

        @Override
        public @Nullable MutationByMutationPoolRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            NonNullList<Ingredient> inputs = NonNullList.withSize(buf.readInt(), Ingredient.EMPTY);
            for (int i =0; i<inputs.size(); i++){
                inputs.set(i, Ingredient.fromNetwork(buf));
            }
            ItemStack output=buf.readItem();
            return new MutationByMutationPoolRecipe(inputs, output, id);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, MutationByMutationPoolRecipe recipe) {
            buf.writeInt(recipe.inputItems.size());
            for (Ingredient in : recipe.getIngredients()) {
                in.toNetwork(buf);
            }
            buf.writeItemStack(recipe.getResultItem(null), false);
        }
    }
}
