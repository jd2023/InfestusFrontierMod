package org.jd.infestusfrontier.storage.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The one rule by which an organ reaches and fills a foreign item handler: found only in loaded chunks, read over a
 * bounded number of slots, and filled by the store's own rules when the colony knows the store. A Mouth and a Palm
 * both use it.
 */
public final class ItemStores {
    /** Slots of a store one insertion or one count touches. */
    public static final int STORE_SLOTS = 64;

    private ItemStores() {}

    /**
     * The item handler of the block at {@code pos} seen from {@code side}, or null when there is none to reach. Nothing
     * is loaded to find out: a position in an unloaded chunk has none, and neither does a double chest whose other
     * half is in an unloaded chunk, because vanilla's chest capability reads that half and would load its chunk.
     */
    public static @Nullable IItemHandler at(Level level, BlockPos pos, @Nullable Direction side) {
        if (!level.hasChunkAt(pos)) return null;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                && !level.hasChunkAt(pos.relative(ChestBlock.getConnectedDirection(state)))) return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
    }

    /**
     * Puts {@code stack} into {@code store} and returns what did not fit; {@code stack} is unchanged. A store the
     * colony knows takes it by its own rules as a whole; one that tells a Mouth's delivery from a hopper's takes it as
     * {@link MouthFed} says; any other takes it slot by slot over its first {@link #STORE_SLOTS} slots.
     */
    public static ItemStack insert(IItemHandler store, ItemStack stack, boolean simulate) {
        if (store instanceof ItemStore known) return known.insert(stack, simulate);
        if (store instanceof MouthFed fed) return fed.deliver(stack, simulate);
        ItemStack rest = stack.copy(); // a handler may keep the stack it is given in a slot
        int slots = Math.min(STORE_SLOTS, store.getSlots());
        for (int slot = 0; slot < slots && !rest.isEmpty(); slot++) rest = store.insertItem(slot, rest, simulate);
        return rest;
    }

    /** A store the colony knows gives its whole count; any other store is counted over its first slots. */
    public static int count(IItemHandler store, ItemStack like) {
        if (store instanceof ItemStore known) return known.count(like);
        int total = 0;
        int slots = Math.min(STORE_SLOTS, store.getSlots());
        for (int slot = 0; slot < slots; slot++) {
            ItemStack held = store.getStackInSlot(slot);
            if (ItemStack.isSameItemSameComponents(held, like)) total += held.getCount();
        }
        return total;
    }
}
