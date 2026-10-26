package org.jd.infestusfrontier.storage;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.storage.api.ItemStore;

/**
 * The item handler of a pod or a stem cell: the whole tree's store, found again on every call, so a pipe that keeps
 * the handler never holds a stale tree. A slot takes only what {@link CapsuleTree#insert(int, ItemStack, boolean)}
 * allows and gives at most a stack at a time. While the pod is unreadable it has no slots; a tree with a chunk out
 * of reach serves the pods still in reach and is read again on every call until the chunk is back.
 */
final class CapsuleItems implements IItemHandler, ItemStore {
    private final Supplier<Optional<CapsuleTree>> store;

    CapsuleItems(Supplier<Optional<CapsuleTree>> store) {
        this.store = store;
    }

    private Optional<CapsuleTree> tree() {
        return store.get();
    }

    @Override
    public int getSlots() {
        return tree().map(CapsuleTree::slots).orElse(0);
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return tree().map(tree -> tree.stack(slot)).orElse(ItemStack.EMPTY);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return tree().map(tree -> tree.insert(slot, stack, simulate)).orElse(stack);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return tree().map(tree -> tree.extract(slot, amount, simulate)).orElse(ItemStack.EMPTY);
    }

    @Override
    public int getSlotLimit(int slot) {
        return tree().map(tree -> tree.limit(slot)).orElse(0);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return tree().map(tree -> tree.fits(slot, stack)).orElse(false);
    }

    @Override
    public int count(ItemStack like) {
        return tree().map(tree -> tree.count(like)).orElse(0);
    }

    @Override
    public ItemStack insert(ItemStack stack, boolean simulate) {
        return tree().map(tree -> tree.insert(stack, simulate)).orElse(stack);
    }

    @Override
    public long stored() {
        return tree().map(CapsuleTree::stored).orElse(0L);
    }

    @Override
    public long capacity() {
        return tree().map(CapsuleTree::capacity).orElse(0L);
    }
}
