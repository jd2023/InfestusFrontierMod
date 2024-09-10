package org.jd.infestusfrontier.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import org.jd.infestusfrontier.block.InfestusBlocks;
import org.jd.infestusfrontier.item.InfestusItems;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;


public class InfestusRecipeProvider extends RecipeProvider implements IConditionBuilder {


    public InfestusRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        buildItemRecipes(writer);
        buildBlockRecipes(writer);


    }
    protected void buildItemRecipes(Consumer<FinishedRecipe> writer){
        buildShapelessItemRecipes(writer);
        buildShapedItemRecipes(writer);

    }
    protected void buildShapedItemRecipes(Consumer<FinishedRecipe> writer){
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, InfestusItems.FERMENTED_FLESH.get())
                .pattern("   ")
                .pattern("YE ")
                .pattern(" X ")
                .define('X', Items.ROTTEN_FLESH)
                .define('E', Items.SUGAR)
                .define('Y', Items.BROWN_MUSHROOM)
                .unlockedBy(getHasName(InfestusItems.FERMENTED_FLESH.get()), has(InfestusItems.FERMENTED_FLESH.get()))
                .save(writer, "infestusfrontier:fermented_flesh_from_crafting");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, InfestusItems.EYE_OF_CORRUPTION.get())
                .pattern(" X ")
                .pattern("XEX")
                .pattern(" X ")
                .define('X', InfestusItems.FERMENTED_FLESH.get())
                .define('E', Items.ENDER_PEARL)
                .unlockedBy(getHasName(InfestusItems.EYE_OF_CORRUPTION.get()), has(InfestusItems.EYE_OF_CORRUPTION.get()))
                .save(writer, "infestusfrontier:eye_of_swarm_from_crafting");
    }
    protected void buildShapelessItemRecipes(Consumer<FinishedRecipe> writer){
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.ROTTEN_FLESH, 9)
                .requires(InfestusBlocks.ROTTEN_FLESH_BLOCK.get())
                .unlockedBy(getHasName(Items.ROTTEN_FLESH), has(Items.ROTTEN_FLESH))
                .save(writer, "infestusfrontier:rotten_flesh_from_block");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, InfestusItems.FERMENTED_FLESH.get(), 9)
                .requires(InfestusBlocks.FERMENTED_FLESH_BLOCK.get())
                .unlockedBy(getHasName(InfestusItems.FERMENTED_FLESH.get()), has(InfestusItems.FERMENTED_FLESH.get()))
                .save(writer, "infestusfrontier:fermented_flesh_from_block");
    }
    protected void buildBlockRecipes(Consumer<FinishedRecipe> writer){
        buildDecorationBlockRecipes(writer);
        buildCoreBlockRecipes(writer);

    }
    protected void buildCoreBlockRecipes(Consumer<FinishedRecipe> writer){
        //Blocks that have their own class and are made for progression not decor
        //Ingredients Dont Count,    Rotten flesh block...

    }
    protected void buildDecorationBlockRecipes(Consumer<FinishedRecipe> writer){
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, InfestusBlocks.ROTTEN_FLESH_BLOCK.get())
                .pattern("XXX")
                .pattern("XXX")
                .pattern("XXX")
                .define('X', Items.ROTTEN_FLESH)
                .unlockedBy(getHasName(InfestusBlocks.ROTTEN_FLESH_BLOCK.get()), has(InfestusBlocks.ROTTEN_FLESH_BLOCK.get()))
                .save(writer, "infestusfrontier:rotten_flesh_block_from_crafting");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, InfestusBlocks.FERMENTED_FLESH_BLOCK.get())
                .pattern("XXX")
                .pattern("XXX")
                .pattern("XXX")
                .define('X', InfestusItems.FERMENTED_FLESH.get())
                .unlockedBy(getHasName(InfestusBlocks.FERMENTED_FLESH_BLOCK.get()), has(InfestusBlocks.FERMENTED_FLESH_BLOCK.get()))
                .save(writer, "infestusfrontier:fermented_block_from_crafting");
    }



}