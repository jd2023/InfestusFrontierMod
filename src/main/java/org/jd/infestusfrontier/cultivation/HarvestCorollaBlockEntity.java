package org.jd.infestusfrontier.cultivation;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Cuts one ripe crop a pulse out of its plot into its four output slots, its vanilla drops and nothing else, for
 * {@value #COST} BU. When the drops do not all fit the crop stays. It never replants. Mouths, hoppers and veins take
 * the output; nothing goes in by hand.
 */
final class HarvestCorollaBlockEntity extends PlotOrganBlockEntity {
    static final int OUTPUT_SLOTS = 4;

    HarvestCorollaBlockEntity(BlockPos pos, BlockState state) {
        super(CultivationModule.HARVEST_COROLLA_ENTITY.get(), pos, state, "Harvest Corolla", OUTPUT_SLOTS);
    }

    @Override
    protected boolean slotTakes(int slot, ItemStack stack) {
        return true;
    }

    /** The output as hoppers and mouths see it: it gives, it takes nothing. */
    IItemHandler output() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return OUTPUT_SLOTS;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return items.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return items.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return false;
            }
        };
    }

    @Override
    protected boolean act(ServerLevel level) {
        for (BlockPos cell : Plot.ground(worldPosition)) {
            BlockPos site = cell.above();
            if (!level.hasChunkAt(site)) continue;
            BlockState crop = level.getBlockState(site);
            if (!(crop.getBlock() instanceof CropBlock block) || !block.isMaxAge(crop)) continue;
            List<ItemStack> drops = Block.getDrops(crop, level, site, null);
            if (!fits(drops)) continue;
            pay();
            for (ItemStack drop : drops) ItemHandlerHelper.insertItemStacked(items, drop, false);
            level.destroyBlock(site, false);
            return true;
        }
        return false;
    }

    /** Whether all of {@code drops} fit the output, judged on a copy. */
    private boolean fits(List<ItemStack> drops) {
        ItemStackHandler copy = new ItemStackHandler(OUTPUT_SLOTS);
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++)
            copy.setStackInSlot(slot, items.getStackInSlot(slot).copy());
        for (ItemStack drop : drops)
            if (!ItemHandlerHelper.insertItemStacked(copy, drop.copy(), false).isEmpty()) return false;
        return true;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.infestusfrontier.harvest_corolla");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PlotOrganMenu(CultivationModule.HARVEST_COROLLA_MENU.get(), id, inventory, this, true);
    }
}
