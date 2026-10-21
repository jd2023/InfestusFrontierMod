package org.jd.infestusfrontier.storage;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.organ.OrganModule;

/**
 * The Reservoir cell's shapeless recipe: the cell takes the biomass of the Bladder it grows from. It refuses an
 * ingredient whose saved data cannot be read, or a balance larger than a cell holds, so no biomass is lost in the grid.
 */
public final class ReservoirCellRecipe extends ShapelessRecipe {
    ReservoirCellRecipe(ShapelessRecipe recipe) {
        super(
                recipe.getGroup(),
                recipe.category(),
                recipe.getResultItem(RegistryAccess.EMPTY),
                recipe.getIngredients());
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!super.matches(input, level)) return false;
        for (ItemStack stack : input.items()) {
            if (stack.has(OrganModule.UNREADABLE_SAVE.get())) return false;
            BiomassTank carried = stack.get(StorageModule.TANK.get());
            if (carried != null && carried.stored().units() > ReservoirCellBlockEntity.CAPACITY_BU) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);
        for (ItemStack stack : input.items()) {
            BiomassTank carried = stack.get(StorageModule.TANK.get());
            if (carried == null || carried.stored().isZero()) continue;
            result.set(
                    StorageModule.TANK.get(),
                    new BiomassTank(carried.stored(), Biomass.ZERO, ReservoirCellBlockEntity.CAPACITY));
        }
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return StorageModule.RESERVOIR_CELL_RECIPE.get();
    }

    static final class Serializer implements RecipeSerializer<ReservoirCellRecipe> {
        @SuppressWarnings("unchecked")
        private static final MapCodec<ReservoirCellRecipe> CODEC = ((MapCodec<ShapelessRecipe>)
                        RecipeSerializer.SHAPELESS_RECIPE.codec())
                .xmap(ReservoirCellRecipe::new, recipe -> recipe);

        private static final StreamCodec<RegistryFriendlyByteBuf, ReservoirCellRecipe> STREAM_CODEC =
                RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(ReservoirCellRecipe::new, recipe -> recipe);

        @Override
        public MapCodec<ReservoirCellRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ReservoirCellRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
