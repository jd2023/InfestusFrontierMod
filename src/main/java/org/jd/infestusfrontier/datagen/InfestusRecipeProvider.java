package org.jd.infestusfrontier.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import org.jd.infestusfrontier.block.InfestusBlocks;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;


public class InfestusRecipeProvider extends RecipeProvider implements IConditionBuilder {


    public InfestusRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, InfestusBlocks.ROTTEN_FLESH_BLOCK.get())
                .pattern("XXX")
                .pattern("XXX")
                .pattern("XXX")
                .define('X', Items.ROTTEN_FLESH)
                .unlockedBy(getHasName(InfestusBlocks.ROTTEN_FLESH_BLOCK.get()), has(InfestusBlocks.ROTTEN_FLESH_BLOCK.get()))
                .save(writer);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.ROTTEN_FLESH, 9)
                .requires(InfestusBlocks.ROTTEN_FLESH_BLOCK.get())
                .unlockedBy(getHasName(Items.ROTTEN_FLESH), has(Items.ROTTEN_FLESH))
                .save(writer);
    }
}