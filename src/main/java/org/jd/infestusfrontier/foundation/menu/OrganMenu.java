package org.jd.infestusfrontier.foundation.menu;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jd.infestusfrontier.foundation.screen.BiomassView;
import org.jd.infestusfrontier.ui.Sheet;
import org.jetbrains.annotations.Nullable;

/**
 * The server half every organ screen shares. A subclass adds the organ's own slots first and then calls
 * {@link #addPlayer}; quick moves, validity and the synced ints work the same for every organ. Nothing here decides
 * anything about the organ: the block entity does.
 */
public abstract class OrganMenu extends AbstractContainerMenu implements BiomassView {
    private static final int INVENTORY_ROWS = 3;
    private static final int COLUMNS = 9;
    private static final int PITCH = 18;
    private static final int PLAYER_SLOTS = INVENTORY_ROWS * COLUMNS + COLUMNS;

    private final ContainerLevelAccess access;

    @Nullable private final BlockEntity organ;

    private final Block block;
    private final ContainerData data;
    private int organSlots;

    protected OrganMenu(MenuType<?> type, int id, ContainerLevelAccess access, Block block, ContainerData data) {
        super(type, id);
        this.access = access;
        this.organ = access instanceof Live live ? live.organ() : null;
        this.block = block;
        this.data = data;
    }

    /**
     * The organ's position on the server. A menu built on it stays valid only while {@code organ} itself is the live
     * block entity there, not while any block of the same type stands at the position.
     */
    protected static ContainerLevelAccess at(BlockEntity organ) {
        return new Live(organ, ContainerLevelAccess.create(organ.getLevel(), organ.getBlockPos()));
    }

    /** The organ's position on the client, read from the extra data the server sent. */
    protected static ContainerLevelAccess at(Inventory inventory, RegistryFriendlyByteBuf extra) {
        return ContainerLevelAccess.create(inventory.player.level(), extra.readBlockPos());
    }

    /** Synced ints read live on the server; vanilla sends the client the ones that changed. */
    protected static ContainerData reading(IntSupplier... reads) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return reads[index].getAsInt();
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return reads.length;
            }
        };
    }

    /** Adds the player's inventory under the organ's slots on a panel with {@code strips} body strips. Call it last. */
    protected final void addPlayer(Inventory inventory, int strips) {
        organSlots = slots.size();
        for (int row = 0; row < INVENTORY_ROWS; row++)
            for (int col = 0; col < COLUMNS; col++)
                addSlot(new Slot(
                        inventory,
                        col + row * COLUMNS + COLUMNS,
                        Sheet.LEFT_X + col * PITCH,
                        Sheet.inventoryY(strips) + row * PITCH));
        for (int col = 0; col < COLUMNS; col++)
            addSlot(new Slot(inventory, col, Sheet.LEFT_X + col * PITCH, Sheet.hotbarY(strips)));
        addDataSlots(data);
    }

    /** The synced int at {@code index}, as the server last sent it. */
    protected final int synced(int index) {
        return data.get(index);
    }

    protected final ContainerLevelAccess access() {
        return access;
    }

    /**
     * Shift-click: organ slots empty into the inventory; inventory items go to the first organ slot that takes them.
     * An organ slot's {@code onTake} is called with the whole stack that was there, as vanilla does, even when only
     * part of it moved; nothing here counts what it is given.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        int inventoryStart = organSlots;
        int hotbarStart = organSlots + INVENTORY_ROWS * COLUMNS;
        int end = organSlots + PLAYER_SLOTS;
        boolean moved = false;
        if (index < organSlots) moved = moveItemStackTo(stack, inventoryStart, end, true);
        else {
            for (int organ = 0; organ < organSlots && !moved; organ++)
                if (slots.get(organ).mayPlace(stack)) moved = moveItemStackTo(stack, organ, organ + 1, false);
            if (!moved)
                moved = index < hotbarStart
                        ? moveItemStackTo(stack, hotbarStart, end, false)
                        : moveItemStackTo(stack, inventoryStart, hotbarStart, false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (index < organSlots) slot.onTake(player, before); // vanilla's quick move never calls it
        return before;
    }

    /**
     * Valid while the viewer can reach the block and, on the server, the organ the menu was opened on is still the
     * live block entity at its loaded position. A broken organ that was replaced by one of the same type is a
     * different organ. Subclasses add to this and never replace it.
     */
    @Override
    public boolean stillValid(Player player) {
        return (organ == null || isLive(organ)) && stillValid(access, player, block);
    }

    private static boolean isLive(BlockEntity organ) {
        Level level = organ.getLevel();
        BlockPos pos = organ.getBlockPos();
        return !organ.isRemoved()
                && level != null
                && level.hasChunkAt(pos) // never load a chunk to ask
                && level.getBlockEntity(pos) == organ;
    }

    /** A server position that remembers the organ it belongs to. */
    private record Live(BlockEntity organ, ContainerLevelAccess where) implements ContainerLevelAccess {
        @Override
        public <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> action) {
            return where.evaluate(action);
        }
    }
}
