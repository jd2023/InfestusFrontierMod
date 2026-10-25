package org.jd.infestusfrontier.storage;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.core.storage.Compartments;

/**
 * A menu slot over one capsule compartment, which may hold more than a stack. It shows the whole count, takes at most
 * a stack out at a time and takes in up to the compartment's limit. The menu keeps drags and hotbar swaps off it,
 * since vanilla's versions of those assume a slot holds one stack.
 */
final class CompartmentSlot extends Slot {
    /** Where the compartments live: the cell on the server, a plain copy on the client. */
    interface Source {
        ItemStack get(int compartment);

        void set(int compartment, ItemStack stack);

        boolean accepts(int compartment, ItemStack stack);

        default boolean active(int compartment) {
            return true;
        }
    }

    private final Source source;

    CompartmentSlot(Source source, Container shared, int compartment, int x, int y) {
        super(shared, compartment, x, y);
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

    @Override
    public ItemStack remove(int amount) {
        ItemStack held = getItem();
        int taken = Math.min(Math.min(amount, held.getCount()), held.getMaxStackSize());
        if (taken <= 0) return ItemStack.EMPTY;
        set(held.copyWithCount(held.getCount() - taken));
        return held.copyWithCount(taken);
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

    @Override
    public boolean mayPlace(ItemStack stack) {
        return isActive() && source.accepts(getContainerSlot(), stack);
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
        return Compartments.limit(64);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Compartments.limit(stack.getMaxStackSize());
    }
}
