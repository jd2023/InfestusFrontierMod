package org.jd.infestusfrontier.processing;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A batch organ as hoppers and pipes see it: the ingredient slots take items and give none back; the output slots after
 * them give items and take none. Every slot still answers what it holds.
 */
record BatchOrganPorts(ItemStackHandler ingredients, ItemStackHandler output) implements IItemHandler {
    @Override
    public int getSlots() {
        return ingredients.getSlots() + output.getSlots();
    }

    private boolean isIngredient(int slot) {
        return slot < ingredients.getSlots();
    }

    private int outputSlot(int slot) {
        return slot - ingredients.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return isIngredient(slot) ? ingredients.getStackInSlot(slot) : output.getStackInSlot(outputSlot(slot));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return isIngredient(slot) ? ingredients.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return isIngredient(slot) ? ItemStack.EMPTY : output.extractItem(outputSlot(slot), amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return isIngredient(slot) ? ingredients.getSlotLimit(slot) : output.getSlotLimit(outputSlot(slot));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isIngredient(slot) && ingredients.isItemValid(slot, stack);
    }
}
