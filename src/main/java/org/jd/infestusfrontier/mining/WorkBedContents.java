package org.jd.infestusfrontier.mining;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.core.mining.Counts;
import org.jd.infestusfrontier.core.mining.Fracturing;

/**
 * Everything a Work Bed owns: the cut material it holds and its history, the items it has received in all. Saved on
 * the block entity and carried on the item when the bed is broken. The stacks are copies.
 */
record WorkBedContents(List<ItemStack> slots, int received) {
    static final WorkBedContents EMPTY =
            new WorkBedContents(Collections.nCopies(Fracturing.BED_SLOTS, ItemStack.EMPTY), 0);

    WorkBedContents {
        List<ItemStack> copies = new ArrayList<>(slots.size());
        for (ItemStack stack : slots) copies.add(stack.copy());
        slots = List.copyOf(copies);
    }

    private static final MapCodec<WorkBedContents> RAW = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ItemStack.OPTIONAL_CODEC.listOf().fieldOf("slots").forGetter(WorkBedContents::slots),
                    Codec.INT.fieldOf("received").forGetter(WorkBedContents::received))
            .apply(instance, WorkBedContents::new));

    /** Impossible saved contents are a decode error, never an exception. */
    static final MapCodec<WorkBedContents> CODEC = RAW.flatXmap(
            contents -> contents.problem()
                    .<DataResult<WorkBedContents>>map(problem -> DataResult.error(() -> problem))
                    .orElseGet(() -> DataResult.success(contents)),
            DataResult::success);

    /** Why these contents cannot be a bed's, or empty when they can. */
    Optional<String> problem() {
        if (slots.size() != Fracturing.BED_SLOTS) return Optional.of("bed slots " + slots.size());
        for (ItemStack stack : slots)
            if (stack.getCount() > stack.getMaxStackSize()) return Optional.of("bed stack " + stack);
        if (received < 0) return Optional.of("received " + received);
        return Optional.empty();
    }

    /** The contents after {@code drops} were added, or empty when they do not all fit. A cut is taken whole or not. */
    Optional<WorkBedContents> with(List<ItemStack> drops) {
        int incoming = 0;
        for (ItemStack drop : drops) incoming = Math.addExact(incoming, drop.getCount());
        int count = incoming;
        return Cutting.merged(slots, drops).map(after -> new WorkBedContents(after, Counts.more(received, count)));
    }
}
