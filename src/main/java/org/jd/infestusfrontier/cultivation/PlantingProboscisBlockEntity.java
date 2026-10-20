package org.jd.infestusfrontier.cultivation;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.cultivation.api.PlantingStock;
import org.jetbrains.annotations.Nullable;

/**
 * Plants one seed a pulse into the first empty bed of its plot, from its own slot first, then from any item handler
 * beside it that gives planting stock, a Seed Pouch above its reserve for one. {@value #COST} BU a plant; a failed
 * placement costs nothing and takes nothing.
 */
final class PlantingProboscisBlockEntity extends PlotOrganBlockEntity {
    PlantingProboscisBlockEntity(BlockPos pos, BlockState state) {
        super(CultivationModule.PLANTING_PROBOSCIS_ENTITY.get(), pos, state, "Planting Proboscis", 1);
    }

    @Override
    protected boolean slotTakes(int slot, ItemStack stack) {
        return PlantingStock.is(stack);
    }

    @Override
    boolean takesByHand(ItemStack stack) {
        return PlantingStock.is(stack);
    }

    @Override
    boolean insertByHand(ItemStack stack) {
        if (!items.insertItem(0, stack.copyWithCount(1), false).isEmpty()) return false;
        stack.shrink(1);
        return true;
    }

    @Override
    protected boolean act(ServerLevel level) {
        Optional<BlockPos> bed = emptyBed(level);
        if (bed.isEmpty()) return false;
        BlockPos site = bed.get().above();
        if (plantFrom(level, items, site)) return true;
        for (Direction side : Direction.values()) {
            BlockPos next = worldPosition.relative(side);
            if (!level.hasChunkAt(next)) continue;
            IItemHandler source = level.getCapability(Capabilities.ItemHandler.BLOCK, next, side.getOpposite());
            if (source != null && plantFrom(level, source, site)) return true;
        }
        return false;
    }

    /** The first bed of the plot with nothing on it. */
    private Optional<BlockPos> emptyBed(ServerLevel level) {
        for (BlockPos cell : Plot.ground(worldPosition)) {
            if (!level.hasChunkAt(cell)) continue;
            if (level.getBlockState(cell).is(CultivationModule.CULTIVATION_TISSUE.get())
                    && level.getBlockState(cell.above()).isAir()) return Optional.of(cell);
        }
        return Optional.empty();
    }

    /** Takes one seed {@code source} gives and plants it at {@code site} when the plant can stand there. */
    private boolean plantFrom(ServerLevel level, IItemHandler source, BlockPos site) {
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack seed = source.extractItem(slot, 1, true);
            if (seed.isEmpty() || !PlantingStock.is(seed)) continue;
            BlockState plant = PlantingStock.plantOf(seed);
            if (!plant.canSurvive(level, site)) continue;
            if (source.extractItem(slot, 1, false).isEmpty()) continue;
            pay();
            level.setBlock(site, plant, Block.UPDATE_ALL);
            level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, site, 5);
            return true;
        }
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.infestusfrontier.planting_proboscis");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PlotOrganMenu(CultivationModule.PLANTING_PROBOSCIS_MENU.get(), id, inventory, this, false);
    }
}
