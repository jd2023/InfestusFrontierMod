package org.jd.infestusfrontier.organ.api;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jd.infestusfrontier.organ.OrganModule;

/**
 * What an Organ Bud needs to grow into {@code organ}: each material is an ingredient and a count, supplied by hand in
 * any order. Data: {@code recipe/growth/*.json} with {@code "type": "infestusfrontier:organ_growth"}, {@code organ}
 * (the organ's block id; its item has the same id) and {@code materials} (flat sized ingredients such as
 * {@code {"item": "minecraft:bowl", "count": 1}}). The grown organ is the block's default state. Never a crafting
 * recipe: it matches no grid.
 */
public record OrganGrowthRecipe(ResourceLocation organ, List<SizedIngredient> materials)
        implements Recipe<RecipeInput> {
    /** Most distinct materials a growth asks for. */
    public static final int MAX_MATERIALS = 9;
    /** Most items one growth takes in all: what a bud may hold is bounded by it. */
    public static final int MAX_ITEMS = 64;

    public static final MapCodec<OrganGrowthRecipe> CODEC = RecordCodecBuilder.<OrganGrowthRecipe>mapCodec(
                    instance -> instance.group(
                                    ResourceLocation.CODEC.fieldOf("organ").forGetter(OrganGrowthRecipe::organ),
                                    SizedIngredient.FLAT_CODEC
                                            .listOf(1, MAX_MATERIALS)
                                            .fieldOf("materials")
                                            .forGetter(OrganGrowthRecipe::materials))
                            .apply(instance, OrganGrowthRecipe::new))
            .flatXmap(OrganGrowthRecipe::checked, DataResult::success);
    public static final StreamCodec<RegistryFriendlyByteBuf, OrganGrowthRecipe> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            OrganGrowthRecipe::organ,
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()),
            OrganGrowthRecipe::materials,
            OrganGrowthRecipe::new);

    public OrganGrowthRecipe {
        materials = List.copyOf(materials);
    }

    private static DataResult<OrganGrowthRecipe> checked(OrganGrowthRecipe recipe) {
        if (!BuiltInRegistries.BLOCK.containsKey(recipe.organ) || !BuiltInRegistries.ITEM.containsKey(recipe.organ))
            return DataResult.error(() -> "No organ block and item named " + recipe.organ);
        if (recipe.totalItems() > MAX_ITEMS)
            return DataResult.error(() -> "A growth takes at most " + MAX_ITEMS + " items in all");
        return DataResult.success(recipe);
    }

    /** Every growth the loaded recipes name, in a stable order: by organ id. */
    public static List<RecipeHolder<OrganGrowthRecipe>> all(Level level) {
        RecipeManager recipes = level.getRecipeManager();
        return recipes.getAllRecipesFor(OrganModule.ORGAN_GROWTH.get()).stream()
                .sorted(Comparator.comparing(holder -> holder.value().organ()))
                .toList();
    }

    public int totalItems() {
        int total = 0;
        for (SizedIngredient material : materials) total += material.count();
        return total;
    }

    /** The organ's block. */
    public Block block() {
        return BuiltInRegistries.BLOCK.get(organ);
    }

    /** True when some material of this growth takes {@code stack}, whatever the stack's count. */
    public boolean uses(ItemStack stack) {
        return materials.stream().anyMatch(material -> material.ingredient().test(stack));
    }

    /** Never matches a grid: a bud takes its materials by hand. */
    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return getResultItem(registries);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(BuiltInRegistries.ITEM.get(organ));
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return OrganModule.ORGAN_GROWTH_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return OrganModule.ORGAN_GROWTH.get();
    }

    /** Reads and writes the JSON and network forms. */
    public static final class Serializer implements RecipeSerializer<OrganGrowthRecipe> {
        @Override
        public MapCodec<OrganGrowthRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, OrganGrowthRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
