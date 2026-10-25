package org.jd.infestusfrontier.storage;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jd.infestusfrontier.core.storage.Compartments;

/**
 * The one store of a standing Item Capsule body: the compartments of its cells in body order, bottom layer first,
 * {@value ItemCapsuleBlockEntity#COMPARTMENTS} a cell. Its cells share it while the body stands; a change of the body,
 * a cell leaving or unloading, or a cell loading new state closes it, and a closed store holds no slots. Callers reach
 * it through {@link Capsules#of}, which uses it only while {@link #isUsable}. Items go by
 * {@link Compartments}: a kind fills its own compartments before it opens an empty one. What a kind's compartments
 * hold is read once per change of the store, so a hopper trying every slot reads the body once.
 */
final class CapsuleBody {
    private final List<ItemCapsuleBlockEntity> members;
    private final Level level;
    /** The chunks the body spans, at most four, since no side is longer than four cells. */
    private final long[] chunks;

    private boolean open = true;
    private int version;
    private int readVersion = -1;
    private ItemStack readKind = ItemStack.EMPTY;
    private int[] read = new int[0];

    CapsuleBody(Level level, List<ItemCapsuleBlockEntity> members) {
        this.level = level;
        this.members = List.copyOf(members);
        this.chunks = members.stream()
                .mapToLong(member -> ChunkPos.asLong(member.getBlockPos()))
                .distinct()
                .toArray();
    }

    /**
     * True while the store may be used: no cell has left, unloaded or loaded new state, and every chunk of the body is
     * still in reach. A chunk leaves reach before its cells unload, so the chunks are checked on every use.
     */
    boolean isUsable() {
        if (!open) return false;
        for (long chunk : chunks) if (!level.hasChunk(ChunkPos.getX(chunk), ChunkPos.getZ(chunk))) return false;
        return true;
    }

    void close() {
        open = false;
    }

    /** A cell's compartments changed. */
    void touch() {
        version++;
    }

    int cells() {
        return members.size();
    }

    int slots() {
        return open ? members.size() * ItemCapsuleBlockEntity.COMPARTMENTS : 0;
    }

    private boolean holds(int slot) {
        return slot >= 0 && slot < slots();
    }

    private ItemCapsuleBlockEntity cell(int slot) {
        return members.get(slot / ItemCapsuleBlockEntity.COMPARTMENTS);
    }

    private static int compartment(int slot) {
        return slot % ItemCapsuleBlockEntity.COMPARTMENTS;
    }

    /** A hand edits one of the body's compartments under the cell's existing admission rules. */
    boolean accepts(int slot, ItemStack stack) {
        return isUsable() && holds(slot) && cell(slot).accepts(compartment(slot), stack);
    }

    void put(int slot, ItemStack stack) {
        if (!isUsable() || !holds(slot)) return;
        if (!stack.isEmpty()
                && (!accepts(slot, stack) || stack.getCount() > Compartments.limit(stack.getMaxStackSize()))) return;
        cell(slot).put(compartment(slot), stack);
    }

    /** What slot {@code slot} holds, with its whole count; a copy. */
    ItemStack stack(int slot) {
        return holds(slot) ? cell(slot).held(compartment(slot)) : ItemStack.EMPTY;
    }

    /** How many of its kind a slot holds at most: the limit of what it holds, or of the largest stack when empty. */
    int limit(int slot) {
        ItemStack held = stack(slot);
        return Compartments.limit(held.isEmpty() ? Item.ABSOLUTE_MAX_STACK_SIZE : held.getMaxStackSize());
    }

    /** Whether a slot could ever take {@code stack}: a kind the capsule takes, into its own or an empty slot. */
    boolean fits(int slot, ItemStack stack) {
        if (!holds(slot) || !Capsules.takes(stack)) return false;
        ItemCapsuleBlockEntity cell = cell(slot);
        return cell.count(compartment(slot)) == 0 || cell.holds(compartment(slot), stack);
    }

    /** Every slot read for {@code kind}, as {@link Compartments} reads a store: other kind, empty or a count. */
    private int[] counts(ItemStack kind) {
        if (readVersion == version && ItemStack.isSameItemSameComponents(readKind, kind)) return read;
        int[] counts = new int[slots()];
        for (int slot = 0; slot < counts.length; slot++) {
            ItemCapsuleBlockEntity cell = cell(slot);
            int count = cell.count(compartment(slot));
            counts[slot] = count == 0 ? 0 : cell.holds(compartment(slot), kind) ? count : Compartments.OTHER;
        }
        readVersion = version;
        readKind = kind.copyWithCount(1);
        read = counts;
        return counts;
    }

    /** Puts what slot {@code slot} may take on its own of {@code stack}; returns the rest. */
    ItemStack insert(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !fits(slot, stack)) return stack;
        int room = Compartments.room(counts(stack), Compartments.limit(stack.getMaxStackSize()), slot);
        int moved = Math.min(room, stack.getCount());
        if (moved == 0) return stack;
        if (!simulate) cell(slot).add(compartment(slot), stack, moved);
        return stack.copyWithCount(stack.getCount() - moved);
    }

    /** Puts {@code stack} where the body's rules put it: its kind's compartments, then the first empty one. */
    ItemStack insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slots() == 0 || !Capsules.takes(stack)) return stack;
        int[] plan = Compartments.plan(counts(stack), Compartments.limit(stack.getMaxStackSize()), stack.getCount());
        int moved = 0;
        for (int slot = 0; slot < plan.length; slot++) {
            if (plan[slot] == 0) continue;
            moved += plan[slot];
            if (!simulate) cell(slot).add(compartment(slot), stack, plan[slot]);
        }
        return stack.copyWithCount(stack.getCount() - moved);
    }

    /** Takes up to {@code amount} from a slot, never more than one stack of its kind. */
    ItemStack extract(int slot, int amount, boolean simulate) {
        if (amount <= 0 || !holds(slot)) return ItemStack.EMPTY;
        ItemCapsuleBlockEntity cell = cell(slot);
        ItemStack held = cell.held(compartment(slot));
        int taken = Math.min(Math.min(amount, held.getCount()), held.getMaxStackSize());
        if (taken == 0) return ItemStack.EMPTY;
        if (!simulate) cell.take(compartment(slot), taken);
        return held.copyWithCount(taken);
    }

    int count(ItemStack like) {
        if (like.isEmpty()) return 0;
        int total = 0;
        for (int count : counts(like)) if (count > 0) total += count;
        return total;
    }

    long stored() {
        long total = 0;
        for (int slot = 0; slot < slots(); slot++) total += cell(slot).count(compartment(slot));
        return total;
    }

    /** Items the body holds at most with the kinds it holds; an empty compartment counts as one of a 64 stack kind. */
    long capacity() {
        long total = 0;
        for (int slot = 0; slot < slots(); slot++) {
            ItemStack held = stack(slot);
            total += Compartments.limit(held.isEmpty() ? 64 : held.getMaxStackSize());
        }
        return total;
    }

    /** Compartments that hold something. */
    int used() {
        int used = 0;
        for (int slot = 0; slot < slots(); slot++) if (cell(slot).count(compartment(slot)) > 0) used++;
        return used;
    }
}
