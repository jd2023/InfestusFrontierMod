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
import org.jd.infestusfrontier.core.storage.Compartments;

/**
 * The four compartments of one Item Capsule cell, as saved and as carried on its item: each empty or holding one kind
 * with its count, which may pass a stack. A value: stacks are copied in and out and compared with
 * {@link ItemStack#matches}. Saved as one entry per held compartment; a slot out of range, a slot named twice or a
 * count past the kind's compartment limit does not decode, though any value encodes, so bad state can be kept as it
 * came.
 */
public record CapsuleStock(List<ItemStack> held) {
    public static final int COMPARTMENTS = 4;
    public static final CapsuleStock EMPTY = new CapsuleStock(List.of());

    private record Entry(int slot, ItemStack kind, int count) {}

    private static final Codec<Entry> ENTRY = RecordCodecBuilder.<Entry>create(instance -> instance.group(
                    Codec.intRange(0, COMPARTMENTS - 1).fieldOf("slot").forGetter(Entry::slot),
                    ItemStack.SINGLE_ITEM_CODEC.fieldOf("item").forGetter(Entry::kind),
                    Codec.intRange(1, Integer.MAX_VALUE).fieldOf("count").forGetter(Entry::count))
            .apply(instance, Entry::new));

    public static final Codec<CapsuleStock> CODEC =
            ENTRY.listOf(0, COMPARTMENTS).comapFlatMap(CapsuleStock::decode, CapsuleStock::entries);

    public static final StreamCodec<RegistryFriendlyByteBuf, CapsuleStock> STREAM_CODEC =
            ItemStack.OPTIONAL_STREAM_CODEC
                    .apply(ByteBufCodecs.list(COMPARTMENTS))
                    .map(CapsuleStock::new, CapsuleStock::held);

    /** Pads to {@value #COMPARTMENTS} compartments and keeps copies, so the value never changes. */
    public CapsuleStock {
        if (held.size() > COMPARTMENTS) throw new IllegalArgumentException(held.size() + " compartments");
        List<ItemStack> copies = new ArrayList<>(COMPARTMENTS);
        for (int i = 0; i < COMPARTMENTS; i++)
            copies.add(i < held.size() ? held.get(i).copy() : ItemStack.EMPTY);
        held = List.copyOf(copies);
    }

    /** Copies of the compartments, each with its whole count. */
    @Override
    public List<ItemStack> held() {
        List<ItemStack> copies = new ArrayList<>(COMPARTMENTS);
        for (ItemStack stack : held) copies.add(stack.copy());
        return copies;
    }

    public boolean isEmpty() {
        for (ItemStack stack : held) if (!stack.isEmpty()) return false;
        return true;
    }

    private static DataResult<CapsuleStock> decode(List<Entry> entries) {
        ItemStack[] held = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
        for (Entry entry : entries) {
            if (!held[entry.slot()].isEmpty()) return DataResult.error(() -> "Compartment " + entry.slot() + " twice");
            if (entry.count() > Compartments.limit(entry.kind().getMaxStackSize()))
                return DataResult.error(() -> entry.count() + " " + entry.kind() + " is past a compartment");
            held[entry.slot()] = entry.kind().copyWithCount(entry.count());
        }
        return DataResult.success(new CapsuleStock(List.of(held)));
    }

    private List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        for (int slot = 0; slot < COMPARTMENTS; slot++) {
            ItemStack stack = held.get(slot);
            if (!stack.isEmpty()) entries.add(new Entry(slot, stack.copyWithCount(1), stack.getCount()));
        }
        return entries;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CapsuleStock stock && ItemStack.listMatches(held, stock.held);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashStackList(held);
    }
}
