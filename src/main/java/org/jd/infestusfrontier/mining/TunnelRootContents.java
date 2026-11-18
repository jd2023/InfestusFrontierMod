package org.jd.infestusfrontier.mining;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.mining.RootMutation;
import org.jd.infestusfrontier.core.mining.RootMutations;
import org.jd.infestusfrontier.core.mining.Steer;
import org.jd.infestusfrontier.core.mining.TunnelProgress;
import org.jd.infestusfrontier.core.mining.Tunnelling;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;

/**
 * Everything a Tunnel Root head owns: its segments, its output, its biomass, its progress (the cells it has laid are
 * its history), the steering of its next step, its permanent mutations and, as a Descending Rootstock, the staircase
 * steps it has advanced, its minehead history. Saved on the block entity, carried on the
 * item when the head is broken, and handed from the old block entity to the new one when the head moves. A head saved
 * before it could mutate reads with none, and one saved before it could descend with no steps advanced. The stacks are
 * copies.
 */
record TunnelRootContents(
        ItemStack segments,
        List<ItemStack> output,
        BiomassTank tank,
        TunnelProgress progress,
        Steer steer,
        RootMutations mutations,
        int descended) {
    static final TunnelRootContents EMPTY = new TunnelRootContents(
            ItemStack.EMPTY,
            List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY),
            BiomassTank.empty(Biomass.of(Tunnelling.BIOMASS_CAPACITY_BU)),
            TunnelProgress.START,
            Steer.STRAIGHT,
            RootMutations.NONE,
            0);

    TunnelRootContents {
        segments = segments.copy();
        List<ItemStack> copies = new ArrayList<>(output.size());
        for (ItemStack stack : output) copies.add(stack.copy());
        output = List.copyOf(copies);
    }

    /** The saved numbers before they are judged; impossible ones are a decode error, never an exception. */
    private record Raw(
            ItemStack segments,
            List<ItemStack> output,
            BiomassTank tank,
            int cells,
            int ticks,
            int steer,
            List<String> mutations,
            int descended) {}

    static final MapCodec<TunnelRootContents> CODEC = RecordCodecBuilder.<Raw>mapCodec(instance -> instance.group(
                            ItemStack.OPTIONAL_CODEC.fieldOf("segments").forGetter(Raw::segments),
                            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("output").forGetter(Raw::output),
                            BiomassCodecs.TANK.fieldOf("tank").forGetter(Raw::tank),
                            Codec.INT.fieldOf("cells").forGetter(Raw::cells),
                            Codec.INT.fieldOf("ticks").forGetter(Raw::ticks),
                            Codec.INT.fieldOf("steer").forGetter(Raw::steer),
                            Codec.STRING
                                    .listOf()
                                    .optionalFieldOf("mutations", List.of())
                                    .forGetter(Raw::mutations),
                            Codec.INT.optionalFieldOf("descended", 0).forGetter(Raw::descended))
                    .apply(instance, Raw::new))
            .flatXmap(TunnelRootContents::read, contents -> DataResult.success(contents.raw()));

    private Raw raw() {
        return new Raw(
                segments,
                output,
                tank,
                progress.cells(),
                progress.ticks(),
                steer.ordinal(),
                mutations.installed().stream().map(RootMutation::id).toList(),
                descended);
    }

    private static DataResult<TunnelRootContents> read(Raw raw) {
        if (raw.cells() < 0 || raw.cells() > Tunnelling.MAX_CELLS)
            return DataResult.error(() -> "cells " + raw.cells());
        if (raw.ticks() < 0 || raw.ticks() > Tunnelling.STEP_TICKS)
            return DataResult.error(() -> "ticks " + raw.ticks());
        Optional<Steer> steer = Steer.at(raw.steer());
        if (steer.isEmpty()) return DataResult.error(() -> "steer " + raw.steer());
        List<RootMutation> mutations = new ArrayList<>(raw.mutations().size());
        for (String id : raw.mutations()) {
            Optional<RootMutation> mutation = RootMutation.byId(id);
            if (mutation.isEmpty()) return DataResult.error(() -> "mutation " + id);
            mutations.add(mutation.get());
        }
        Optional<String> wrong = RootMutations.problem(mutations);
        if (wrong.isPresent()) return DataResult.error(() -> "mutations " + wrong.get());
        TunnelRootContents contents = new TunnelRootContents(
                raw.segments(),
                raw.output(),
                raw.tank(),
                new TunnelProgress(raw.cells(), raw.ticks()),
                steer.get(),
                new RootMutations(mutations),
                raw.descended());
        Optional<String> problem = contents.problem();
        return problem.isPresent() ? DataResult.error(problem::get) : DataResult.success(contents);
    }

    /** Why these contents cannot be a head's, or empty when they can. */
    Optional<String> problem() {
        if (!segments.isEmpty()
                && (!segments.is(MiningModule.ROOT_SEGMENT.get()) || segments.getCount() > Tunnelling.MAX_SEGMENTS))
            return Optional.of("segments " + segments);
        if (output.size() != Tunnelling.OUTPUT_SLOTS) return Optional.of("output slots " + output.size());
        long items = 0;
        for (ItemStack stack : output) {
            if (stack.getCount() > stack.getMaxStackSize()) return Optional.of("output stack " + stack);
            items += stack.getCount();
        }
        if (items > Tunnelling.OUTPUT_ITEMS) return Optional.of("output items " + items);
        if (tank.capacity().units() != Tunnelling.BIOMASS_CAPACITY_BU
                || !tank.reserved().isZero()) return Optional.of("tank " + tank);
        if (descended < 0) return Optional.of("descended " + descended);
        if (descended > 0 && !mutations.rootstock()) return Optional.of("descended without Stairs " + descended);
        return Optional.empty();
    }

    /** Items in the output, in all. */
    int outputItems() {
        int items = 0;
        for (ItemStack stack : output) items += stack.getCount();
        return items;
    }

    /**
     * The output after {@code drops} were added, or empty when they do not all fit: at most one stack in all, and only
     * {@value Tunnelling#OUTPUT_SLOTS} kinds. A cut is taken whole or not at all.
     */
    Optional<List<ItemStack>> outputWith(List<ItemStack> drops) {
        int incoming = 0;
        for (ItemStack drop : drops) incoming = Math.addExact(incoming, drop.getCount());
        if (!Tunnelling.fits(outputItems(), incoming)) return Optional.empty();
        return Cutting.merged(output, drops);
    }
}
