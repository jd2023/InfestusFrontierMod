package org.jd.infestusfrontier.storage.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * What one Item Capsule carries, as saved and as carried on its item: its compartments, each empty or holding one kind
 * with its count, which may pass a stack, and its grade, the volume it has been grown to. A value: stacks are copied
 * in and out and compared with {@link ItemStack#matches}. Saved as one entry per held compartment; a slot out of
 * range, a slot named twice or a grade past the last does not decode, though any value encodes, so bad state can be
 * kept as it came.
 */
public record CapsuleStock(List<ItemStack> held, int grade) {
    /** The most compartments any capsule carries. */
    public static final int COMPARTMENTS = 4;
    /** Grades 0 to this: a young capsule holds {@link #stacks} stacks of its kind, each grade doubles it. */
    public static final int LAST_GRADE = 2;

    public static final int YOUNG_STACKS = 8;
    public static final CapsuleStock EMPTY = new CapsuleStock(List.of(), 0);

    private record Entry(int slot, ItemStack kind, int count) {}

    private static final Codec<Entry> ENTRY = RecordCodecBuilder.<Entry>create(instance -> instance.group(
                    Codec.intRange(0, COMPARTMENTS - 1).fieldOf("slot").forGetter(Entry::slot),
                    ItemStack.SINGLE_ITEM_CODEC.fieldOf("item").forGetter(Entry::kind),
                    Codec.intRange(1, Integer.MAX_VALUE).fieldOf("count").forGetter(Entry::count))
            .apply(instance, Entry::new));

    private static final Codec<List<ItemStack>> HELD =
            ENTRY.listOf(0, COMPARTMENTS).comapFlatMap(CapsuleStock::decode, CapsuleStock::entries);

    public static final Codec<CapsuleStock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    HELD.optionalFieldOf("held", List.of()).forGetter(CapsuleStock::held),
                    Codec.intRange(0, LAST_GRADE).optionalFieldOf("grade", 0).forGetter(CapsuleStock::grade))
            .apply(instance, CapsuleStock::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CapsuleStock> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(COMPARTMENTS)),
            CapsuleStock::held,
            ByteBufCodecs.VAR_INT,
            CapsuleStock::grade,
            CapsuleStock::new);

    /** Keeps copies, so the value never changes. */
    public CapsuleStock {
        if (held.size() > COMPARTMENTS) throw new IllegalArgumentException(held.size() + " compartments");
        if (grade < 0 || grade > LAST_GRADE) throw new IllegalArgumentException("grade " + grade);
        List<ItemStack> copies = new ArrayList<>(held.size());
        for (ItemStack stack : held) copies.add(stack.copy());
        held = List.copyOf(copies);
    }

    /** Stacks of its kind a compartment of grade {@code grade} holds: 8, 16 then 32. */
    public static int stacks(int grade) {
        return YOUNG_STACKS << grade;
    }

    /** Copies of the compartments, each with its whole count. */
    @Override
    public List<ItemStack> held() {
        List<ItemStack> copies = new ArrayList<>(held.size());
        for (ItemStack stack : held) copies.add(stack.copy());
        return copies;
    }

    /** True while no compartment holds anything; the grade may still be worth carrying. */
    public boolean isEmpty() {
        for (ItemStack stack : held) if (!stack.isEmpty()) return false;
        return true;
    }

    /** True when there is nothing to carry: empty and ungrown. */
    public boolean isBare() {
        return isEmpty() && grade == 0;
    }

    /** The same stock at {@code grade}. */
    public CapsuleStock graded(int grade) {
        return new CapsuleStock(held, grade);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CapsuleStock that) || that.held.size() != held.size() || that.grade != grade)
            return false;
        for (int i = 0; i < held.size(); i++) if (!ItemStack.matches(held.get(i), that.held.get(i))) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = grade;
        for (ItemStack stack : held) hash = 31 * hash + ItemStack.hashItemAndComponents(stack) * 31 + stack.getCount();
        return hash;
    }

    private static DataResult<List<ItemStack>> decode(List<Entry> entries) {
        List<ItemStack> held = new ArrayList<>();
        for (int i = 0; i < COMPARTMENTS; i++) held.add(ItemStack.EMPTY);
        int last = -1;
        for (Entry entry : entries) {
            if (!held.get(entry.slot()).isEmpty()) return DataResult.error(() -> "slot " + entry.slot() + " twice");
            held.set(entry.slot(), entry.kind().copyWithCount(entry.count()));
            last = Math.max(last, entry.slot());
        }
        return DataResult.success(List.copyOf(held.subList(0, last + 1)));
    }

    private static List<Entry> entries(List<ItemStack> held) {
        List<Entry> entries = new ArrayList<>();
        for (int slot = 0; slot < held.size(); slot++) {
            ItemStack stack = held.get(slot);
            if (!stack.isEmpty()) entries.add(new Entry(slot, stack.copyWithCount(1), stack.getCount()));
        }
        return entries;
    }
}
