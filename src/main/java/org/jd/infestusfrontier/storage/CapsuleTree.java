package org.jd.infestusfrontier.storage;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jd.infestusfrontier.core.storage.Compartments;
import org.jd.infestusfrontier.storage.api.CapsuleStock;

/**
 * The one store of a standing Capsule Stem: the compartments of the pods its root reaches, in the order they grew,
 * {@value ItemCapsuleBlockEntity#COMPARTMENTS} a pod; or of one pod standing on its own. Its pods share it while the
 * tree stands; a pod or stem cell leaving, unloading or loading new state closes it, and a closed store holds no
 * slots. Callers reach it through {@link Stems#of}, which uses it only while {@link #isUsable}. Items go by
 * {@link Compartments}: a kind fills its own compartments before it opens an empty one, each compartment holding as
 * many stacks as its pod's grade allows. What a kind's compartments hold is read once per change of the store, so a
 * hopper trying every slot reads the tree once.
 */
final class CapsuleTree {
    private final List<ItemCapsuleBlockEntity> members;
    private final Level level;
    /** The chunks the tree's plan spans, members or not: a planned pod coming back into reach reopens the store. */
    private final long[] chunks;

    private boolean open = true;
    private final boolean whole;
    private int version;
    private int readVersion = -1;
    private ItemStack readKind = ItemStack.EMPTY;
    private int[] read = new int[0];

    CapsuleTree(Level level, List<ItemCapsuleBlockEntity> members, List<BlockPos> span) {
        this.level = level;
        this.members = List.copyOf(members);
        this.chunks = span.stream()
                .mapToLong(ChunkPos::asLong)
                .distinct()
                .filter(chunk -> level.hasChunk(ChunkPos.getX(chunk), ChunkPos.getZ(chunk)))
                .toArray();
        this.whole = chunks.length
                == span.stream().mapToLong(ChunkPos::asLong).distinct().count();
        for (ItemCapsuleBlockEntity member : members) member.attach(this);
    }

    /**
     * True while the store may be used: no pod has left, unloaded or loaded new state, and every chunk the plan spans
     * is in reach as it was when the store was made. A chunk leaves reach before its pods unload, so the chunks are
     * checked on every use; a chunk out of reach when the store was made is not watched, so that store is read again
     * on every use until the chunk is back.
     */
    boolean isUsable() {
        if (!open) return false;
        for (long chunk : chunks) if (!level.hasChunk(ChunkPos.getX(chunk), ChunkPos.getZ(chunk))) return false;
        return true;
    }

    /** True when every chunk of the span was in reach when the store was made. */
    boolean whole() {
        return whole;
    }

    void close() {
        open = false;
    }

    /** A pod's compartments changed. */
    void touch() {
        version++;
    }

    int pods() {
        return members.size();
    }

    int slots() {
        return open ? members.size() * ItemCapsuleBlockEntity.COMPARTMENTS : 0;
    }

    /** The first slot of {@code pod}'s compartments, or -1 when the pod is not of this tree. */
    int slotOf(ItemCapsuleBlockEntity pod) {
        int index = members.indexOf(pod);
        return index < 0 ? -1 : index * ItemCapsuleBlockEntity.COMPARTMENTS;
    }

    private boolean holds(int slot) {
        return slot >= 0 && slot < slots();
    }

    private ItemCapsuleBlockEntity pod(int slot) {
        return members.get(slot / ItemCapsuleBlockEntity.COMPARTMENTS);
    }

    private static int compartment(int slot) {
        return slot % ItemCapsuleBlockEntity.COMPARTMENTS;
    }

    /** A hand edits one of the tree's compartments under the pod's existing admission rules. */
    boolean accepts(int slot, ItemStack stack) {
        return isUsable() && holds(slot) && pod(slot).accepts(compartment(slot), stack);
    }

    void put(int slot, ItemStack stack) {
        if (!isUsable() || !holds(slot)) return;
        if (!stack.isEmpty() && (!accepts(slot, stack) || stack.getCount() > limit(slot, stack.getMaxStackSize())))
            return;
        pod(slot).put(compartment(slot), stack);
    }

    /** What slot {@code slot} holds, with its whole count; a copy. */
    ItemStack stack(int slot) {
        return holds(slot) ? pod(slot).held(compartment(slot)) : ItemStack.EMPTY;
    }

    /** The most of a kind stacking to {@code maxStackSize} slot {@code slot} holds; 0 for no slot. */
    int limit(int slot, int maxStackSize) {
        if (!holds(slot)) return 0;
        return Compartments.limit(CapsuleStock.stacks(pod(slot).grade()), maxStackSize);
    }

    /** How many of its kind a slot holds at most: the limit of what it holds, or of the largest stack when empty. */
    int limit(int slot) {
        if (!holds(slot)) return 0;
        ItemStack held = stack(slot);
        return limit(slot, held.isEmpty() ? Item.ABSOLUTE_MAX_STACK_SIZE : held.getMaxStackSize());
    }

    /** Whether a slot could ever take {@code stack}: a kind a capsule takes, into its own or an empty slot. */
    boolean fits(int slot, ItemStack stack) {
        if (!holds(slot) || !Stems.takes(stack)) return false;
        ItemCapsuleBlockEntity pod = pod(slot);
        return pod.count(compartment(slot)) == 0 || pod.holds(compartment(slot), stack);
    }

    /** Every slot read for {@code kind}, as {@link Compartments} reads a store: other kind, empty or a count. */
    private int[] counts(ItemStack kind) {
        if (readVersion == version && ItemStack.isSameItemSameComponents(readKind, kind)) return read;
        int[] counts = new int[slots()];
        for (int slot = 0; slot < counts.length; slot++) {
            ItemCapsuleBlockEntity pod = pod(slot);
            int count = pod.count(compartment(slot));
            counts[slot] = count == 0 ? 0 : pod.holds(compartment(slot), kind) ? count : Compartments.OTHER;
        }
        readVersion = version;
        readKind = kind.copyWithCount(1);
        read = counts;
        return counts;
    }

    private int[] limits(ItemStack kind) {
        int[] limits = new int[slots()];
        for (int slot = 0; slot < limits.length; slot++) limits[slot] = limit(slot, kind.getMaxStackSize());
        return limits;
    }

    /** Puts what slot {@code slot} may take on its own of {@code stack}; returns the rest. */
    ItemStack insert(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !fits(slot, stack)) return stack;
        int room = Compartments.room(counts(stack), limits(stack), slot);
        int moved = Math.min(room, stack.getCount());
        if (moved == 0) return stack;
        if (!simulate) pod(slot).add(compartment(slot), stack, moved);
        return stack.copyWithCount(stack.getCount() - moved);
    }

    /** Puts {@code stack} where the tree's rules put it: its kind's compartments, then the first empty one. */
    ItemStack insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slots() == 0 || !Stems.takes(stack)) return stack;
        int[] plan = Compartments.plan(counts(stack), limits(stack), stack.getCount());
        int moved = 0;
        for (int slot = 0; slot < plan.length; slot++) {
            if (plan[slot] == 0) continue;
            moved += plan[slot];
            if (!simulate) pod(slot).add(compartment(slot), stack, plan[slot]);
        }
        return stack.copyWithCount(stack.getCount() - moved);
    }

    /** Takes up to {@code amount} from a slot, never more than one stack of its kind. */
    ItemStack extract(int slot, int amount, boolean simulate) {
        if (amount <= 0 || !holds(slot)) return ItemStack.EMPTY;
        ItemCapsuleBlockEntity pod = pod(slot);
        ItemStack held = pod.held(compartment(slot));
        int taken = Math.min(Math.min(amount, held.getCount()), held.getMaxStackSize());
        if (taken == 0) return ItemStack.EMPTY;
        if (!simulate) pod.take(compartment(slot), taken);
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
        for (int slot = 0; slot < slots(); slot++) total += pod(slot).count(compartment(slot));
        return total;
    }

    /** Items the tree holds at most with the kinds it holds; an empty compartment counts as one of a 64 stack kind. */
    long capacity() {
        long total = 0;
        for (int slot = 0; slot < slots(); slot++) {
            ItemStack held = stack(slot);
            total += limit(slot, held.isEmpty() ? 64 : held.getMaxStackSize());
        }
        return total;
    }

    /** Compartments that hold something. */
    int used() {
        int used = 0;
        for (int slot = 0; slot < slots(); slot++) if (pod(slot).count(compartment(slot)) > 0) used++;
        return used;
    }
}
