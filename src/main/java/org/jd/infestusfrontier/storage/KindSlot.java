package org.jd.infestusfrontier.storage;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A view cell of a tree's ledger: one kind with its whole count over every pod that holds it, or the open entry. It
 * shows the whole count, takes at most a stack out at a time and takes in as many as the tree has room for; a kind
 * never swaps with another. The menu keeps drags and hotbar swaps off it, since vanilla's versions of those assume a
 * slot holds one stack.
 */
final class KindSlot extends Slot {
    /** The ledger behind the view: the tree's kinds on the server, plain copies on the client. */
    interface Source {
        /** What cell {@code cell} shows, a copy; empty past the last kind. */
        ItemStack get(int cell);

        /**
         * Sets cell {@code cell}: more of its kind go into the tree, fewer come out, a kind in an open entry goes in
         * where the tree's rules put it. Another kind over a kind is refused.
         */
        void set(int cell, ItemStack stack);

        /** Takes {@code amount} of the kind cell {@code cell} shows; what was taken. */
        ItemStack take(int cell, int amount);

        /** How many more of {@code stack}'s kind the tree has room for. */
        int room(int cell, ItemStack stack);

        boolean active(int cell);
    }

    private final Source source;

    KindSlot(Source source, Container shared, int cell, int x, int y) {
        super(shared, cell, x, y);
        this.source = source;
    }

    @Override
    public ItemStack getItem() {
        return source.get(getContainerSlot());
    }

    @Override
    public void set(ItemStack stack) {
        source.set(getContainerSlot(), stack);
    }

    @Override
    public void setChanged() {}

    /** At most one stack of the kind, however many the entry counts. */
    @Override
    public ItemStack remove(int amount) {
        ItemStack held = getItem();
        int taken = Math.min(Math.min(amount, held.getCount()), held.getMaxStackSize());
        if (taken <= 0) return ItemStack.EMPTY;
        return source.take(getContainerSlot(), taken);
    }

    /**
     * Never: {@link #getItem} is a copy, so an item that edits the clicked stack in place, as a bundle does after
     * asking this, would change only the copy. Plain clicks still take and put through {@link #remove} and
     * {@link #set}.
     */
    @Override
    public boolean allowModification(Player player) {
        return false;
    }

    /** Its own kind, or any kind into the open entry, while the tree has room for it. */
    @Override
    public boolean mayPlace(ItemStack stack) {
        if (!isActive() || stack.isEmpty()) return false;
        ItemStack held = getItem();
        return (held.isEmpty() || ItemStack.isSameItemSameComponents(held, stack))
                && source.room(getContainerSlot(), stack) > 0;
    }

    @Override
    public boolean isActive() {
        return source.active(getContainerSlot());
    }

    @Override
    public boolean mayPickup(Player player) {
        return isActive();
    }

    @Override
    public int getMaxStackSize() {
        return getMaxStackSize(getItem());
    }

    /** What the entry counts of {@code stack}'s kind plus what the tree still takes of it. */
    @Override
    public int getMaxStackSize(ItemStack stack) {
        ItemStack held = getItem();
        int have = ItemStack.isSameItemSameComponents(held, stack) ? held.getCount() : 0;
        return have + source.room(getContainerSlot(), stack);
    }
}
