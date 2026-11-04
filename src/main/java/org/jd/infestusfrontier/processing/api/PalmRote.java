package org.jd.infestusfrontier.processing.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;

/**
 * The saved and carried state of a Shaping Palm that has Rote (BLOCK_CATALOG T1-41, stage 3): its one {@link Pattern},
 * the {@link Cycle} under way, the four-stack tray the products wait in, its biomass, and the cycles it has completed.
 * A value: stacks are copied in and out, and two states are equal when every stack matches item for item and component
 * for component. A Palm without Rote has none.
 *
 * <p>The tray is one result slot (index 0) and three remainder slots.
 */
public record PalmRote(
        Optional<Pattern> pattern,
        Optional<Cycle> cycle,
        List<ItemStack> tray,
        BiomassTank biomass,
        int cycles,
        boolean restockOwed) {
    /** Loaded ticks a cycle takes. */
    public static final int TICKS = 40;
    /** Biomass a cycle spends when it starts. */
    public static final Biomass COST = Biomass.of(10);
    /** The Palm's biomass capacity. */
    public static final Biomass CAPACITY = Biomass.of(1000);
    /** Tray slots: the result, then the remainders. */
    public static final int TRAY = 4;
    /** Kinds of remainder a pattern may leave; one tray slot each. */
    public static final int REMAINDER_SLOTS = TRAY - 1;

    public PalmRote {
        if (tray.size() > TRAY) throw new IllegalArgumentException(tray.size() + " tray stacks");
        if (cycles < 0) throw new IllegalArgumentException("Negative cycles");
        List<ItemStack> padded = new ArrayList<>(TRAY);
        for (ItemStack stack : tray) padded.add(stack.copy());
        while (padded.size() < TRAY) padded.add(ItemStack.EMPTY);
        tray = List.copyOf(padded);
    }

    /** A Palm that has just been given Rote: nothing selected, nothing under way, no biomass. */
    public static PalmRote fresh() {
        return new PalmRote(Optional.empty(), Optional.empty(), List.of(), BiomassTank.empty(CAPACITY), 0, false);
    }

    @Override
    public List<ItemStack> tray() {
        return tray.stream().map(ItemStack::copy).toList();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PalmRote that
                && pattern.equals(that.pattern)
                && cycle.equals(that.cycle)
                && ItemStack.listMatches(tray, that.tray)
                && biomass.equals(that.biomass)
                && cycles == that.cycles
                && restockOwed == that.restockOwed;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(pattern, cycle, ItemStack.hashStackList(tray), biomass, cycles, restockOwed);
    }

    /**
     * What Select recorded: the nine kinds of the grid (one item each, empty where the cell was empty), the id of the
     * recipe it resolved to, and the result it assembled.
     */
    public record Pattern(List<ItemStack> kinds, ResourceLocation recipe, ItemStack result) {
        public Pattern {
            if (kinds.size() != PalmContents.CELLS) throw new IllegalArgumentException(kinds.size() + " pattern cells");
            kinds = kinds.stream()
                    .map(kind -> kind.isEmpty() ? ItemStack.EMPTY : kind.copyWithCount(1))
                    .toList();
            result = result.copy();
        }

        @Override
        public List<ItemStack> kinds() {
            return kinds.stream().map(ItemStack::copy).toList();
        }

        /** A fresh copy of the kind cell {@code cell} holds; empty where the pattern leaves the cell empty. */
        public ItemStack kind(int cell) {
            return kinds.get(cell).copy();
        }

        @Override
        public ItemStack result() {
            return result.copy();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Pattern that
                    && ItemStack.listMatches(kinds, that.kinds)
                    && recipe.equals(that.recipe)
                    && ItemStack.matches(result, that.result);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(
                    ItemStack.hashStackList(kinds), recipe, ItemStack.hashItemAndComponents(result));
        }
    }

    /**
     * A cycle under way: ticks done, and the products it delivers when it finishes. Its inputs are already spent.
     * {@code supplied} is true when one of them was an item the Palm's Mouth fetched from the network.
     */
    public record Cycle(int done, ItemStack result, List<ItemStack> remainders, boolean supplied) {
        /** A cycle on the hand's own ingredients. */
        public Cycle(int done, ItemStack result, List<ItemStack> remainders) {
            this(done, result, remainders, false);
        }

        public Cycle {
            if (done < 0 || done > TICKS) throw new IllegalArgumentException("Progress " + done);
            if (remainders.size() > REMAINDER_SLOTS)
                throw new IllegalArgumentException(remainders.size() + " remainders");
            result = result.copy();
            remainders = remainders.stream().map(ItemStack::copy).toList();
        }

        @Override
        public ItemStack result() {
            return result.copy();
        }

        @Override
        public List<ItemStack> remainders() {
            return remainders.stream().map(ItemStack::copy).toList();
        }

        public boolean isDone() {
            return done == TICKS;
        }

        /** One loaded tick of work. A finished cycle stays finished. */
        public Cycle advance() {
            return isDone() ? this : new Cycle(done + 1, result, remainders, supplied);
        }

        public int percent() {
            return 100 * done / TICKS;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Cycle that
                    && done == that.done
                    && ItemStack.matches(result, that.result)
                    && ItemStack.listMatches(remainders, that.remainders)
                    && supplied == that.supplied;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(
                    done, ItemStack.hashItemAndComponents(result), ItemStack.hashStackList(remainders), supplied);
        }
    }

    public static final Codec<Pattern> PATTERN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ItemStack.OPTIONAL_CODEC
                            .listOf(PalmContents.CELLS, PalmContents.CELLS)
                            .fieldOf("kinds")
                            .forGetter(Pattern::kinds),
                    ResourceLocation.CODEC.fieldOf("recipe").forGetter(Pattern::recipe),
                    ItemStack.CODEC.fieldOf("result").forGetter(Pattern::result))
            .apply(instance, Pattern::new));

    public static final Codec<Cycle> CYCLE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(0, TICKS).fieldOf("done").forGetter(Cycle::done),
                    ItemStack.CODEC.fieldOf("result").forGetter(Cycle::result),
                    ItemStack.CODEC
                            .listOf(0, REMAINDER_SLOTS)
                            .fieldOf("remainders")
                            .forGetter(Cycle::remainders),
                    Codec.BOOL.optionalFieldOf("supplied", false).forGetter(Cycle::supplied))
            .apply(instance, Cycle::new));

    /** Saved as a compound; the tray keeps its empty stacks so each product stays in its slot. */
    public static final Codec<PalmRote> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    PATTERN_CODEC.optionalFieldOf("pattern").forGetter(PalmRote::pattern),
                    CYCLE_CODEC.optionalFieldOf("cycle").forGetter(PalmRote::cycle),
                    ItemStack.OPTIONAL_CODEC.listOf(0, TRAY).fieldOf("tray").forGetter(PalmRote::tray),
                    BiomassCodecs.TANK.fieldOf("biomass").forGetter(PalmRote::biomass),
                    Codec.intRange(0, Integer.MAX_VALUE)
                            .optionalFieldOf("cycles", 0)
                            .forGetter(PalmRote::cycles),
                    Codec.BOOL.optionalFieldOf("restock_owed", false).forGetter(PalmRote::restockOwed))
            .apply(instance, PalmRote::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PalmRote> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);
}
