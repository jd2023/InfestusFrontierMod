package org.jd.infestusfrontier.storage;

import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.storage.api.ItemStore;

/**
 * A capsule cell's item handler: the whole body's store, found again on every call, so a pipe that keeps the handler
 * never holds a stale body. A slot takes only what {@link CapsuleBody#insert(int, ItemStack, boolean)} allows and
 * gives at most a stack at a time. While part of the body is out of reach, or the cell is unreadable, it has no slots.
 */
final class CapsuleItems implements IItemHandler, ItemStore {
    private final ItemCapsuleBlockEntity cell;

    CapsuleItems(ItemCapsuleBlockEntity cell) {
        this.cell = cell;
    }

    private Optional<CapsuleBody> body() {
        return Capsules.of(cell);
    }

    @Override
    public int getSlots() {
        return body().map(CapsuleBody::slots).orElse(0);
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return body().map(body -> body.stack(slot)).orElse(ItemStack.EMPTY);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return body().map(body -> body.insert(slot, stack, simulate)).orElse(stack);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return body().map(body -> body.extract(slot, amount, simulate)).orElse(ItemStack.EMPTY);
    }

    @Override
    public int getSlotLimit(int slot) {
        return body().map(body -> body.limit(slot)).orElse(0);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return body().map(body -> body.fits(slot, stack)).orElse(false);
    }

    @Override
    public int count(ItemStack like) {
        return body().map(body -> body.count(like)).orElse(0);
    }

    @Override
    public ItemStack insert(ItemStack stack, boolean simulate) {
        return body().map(body -> body.insert(stack, simulate)).orElse(stack);
    }

    @Override
    public long stored() {
        return body().map(CapsuleBody::stored).orElse(0L);
    }

    @Override
    public long capacity() {
        return body().map(CapsuleBody::capacity).orElse(0L);
    }
}
