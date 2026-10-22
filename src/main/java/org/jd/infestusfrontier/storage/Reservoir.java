package org.jd.infestusfrontier.storage;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.core.storage.Body;
import org.jd.infestusfrontier.core.storage.Box;

/**
 * The biomass of Reservoir bodies. A body is the cells inside one box that all name that box ({@link Bodies}); each
 * holds its own share, so a broken cell takes exactly its share and the rest stays. Every operation reads the whole
 * box, at most {@value Body#MAX_SIDE}³ cells, and is refused while any of it is unloaded. A body found broken (a cell
 * left while its chunk was away) refuses, and changes nothing, until its cells have fallen apart and formed again on
 * the tick each schedules when it loads.
 */
final class Reservoir {
    /** Part of the body is not loaded, or it is still forming. */
    static final Refusal OUT_OF_REACH = new Refusal("storage.body_out_of_reach");

    /** A body formed or looked at again settles its biomass from the bottom; a cell left alone shows its own share. */
    static final Bodies.Kind<ReservoirCellBlockEntity> KIND = new Bodies.Kind<>() {
        @Override
        public Class<ReservoirCellBlockEntity> type() {
            return ReservoirCellBlockEntity.class;
        }

        @Override
        public void formed(List<ReservoirCellBlockEntity> members, Box body) {
            settle(members, body);
        }

        @Override
        public void alone(ReservoirCellBlockEntity cell) {
            show(cell, cell.body(), fill(new int[] {cell.share().stored().units()}));
        }
    };

    private Reservoir() {}

    private static Optional<List<ReservoirCellBlockEntity>> members(ReservoirCellBlockEntity cell) {
        return Bodies.members(cell, KIND);
    }

    /** The shares of {@code members} as layers, bottom first, in the order {@link Bodies#positions} lists them. */
    private static int[][] layers(List<ReservoirCellBlockEntity> members, Box body) {
        int perLayer = body.sizeX() * body.sizeZ();
        int[][] layers = new int[body.sizeY()][perLayer];
        for (int i = 0; i < members.size(); i++)
            layers[i / perLayer][i % perLayer] = members.get(i).share().stored().units();
        return layers;
    }

    private static BiomassTank total(int[][] layers) {
        int stored = 0;
        int cells = 0;
        for (int[] layer : layers)
            for (int share : layer) {
                stored += share;
                cells++;
            }
        return new BiomassTank(
                Biomass.of(stored), Biomass.ZERO, Biomass.of(cells * ReservoirCellBlockEntity.CAPACITY_BU));
    }

    static BiomassTank tank(ReservoirCellBlockEntity cell) {
        return members(cell).map(members -> total(layers(members, cell.body()))).orElse(cell.share());
    }

    static Outcome<BiomassTank> insert(ReservoirCellBlockEntity cell, Biomass amount, boolean simulate) {
        Optional<List<ReservoirCellBlockEntity>> members = members(cell);
        if (members.isEmpty()) return Outcome.refused(OUT_OF_REACH);
        int[][] layers = layers(members.get(), cell.body());
        if (Body.fill(layers, ReservoirCellBlockEntity.CAPACITY_BU, amount.units()) < amount.units())
            return Outcome.refused(BiomassTank.FULL);
        if (!simulate) apply(members.get(), cell.body(), layers);
        return Outcome.done(total(layers));
    }

    static Outcome<BiomassTank> extract(ReservoirCellBlockEntity cell, Biomass amount, boolean simulate) {
        Optional<List<ReservoirCellBlockEntity>> members = members(cell);
        if (members.isEmpty()) return Outcome.refused(OUT_OF_REACH);
        int[][] layers = layers(members.get(), cell.body());
        if (Body.drain(layers, ReservoirCellBlockEntity.CAPACITY_BU, amount.units()) < amount.units())
            return Outcome.refused(BiomassTank.EMPTY);
        if (!simulate) apply(members.get(), cell.body(), layers);
        return Outcome.done(total(layers));
    }

    /** Writes {@code layers} back into the shares and shows each layer's level on its cells. */
    private static void apply(List<ReservoirCellBlockEntity> members, Box body, int[][] layers) {
        int perLayer = body.sizeX() * body.sizeZ();
        for (int i = 0; i < members.size(); i++) {
            ReservoirCellBlockEntity member = members.get(i);
            int share = layers[i / perLayer][i % perLayer];
            if (share != member.share().stored().units())
                member.setShare(new BiomassTank(Biomass.of(share), Biomass.ZERO, ReservoirCellBlockEntity.CAPACITY));
            show(member, body, fill(layers[i / perLayer]));
        }
    }

    /** The level of a layer in sixteenths: 0 only when empty, 16 only when full. */
    static int fill(int[] layer) {
        long stored = 0;
        for (int share : layer) stored += share;
        long capacity = (long) layer.length * ReservoirCellBlockEntity.CAPACITY_BU;
        if (stored == 0) return 0;
        if (stored >= capacity) return ReservoirCellBlock.FILL_STEPS;
        return (int)
                Math.clamp(stored * ReservoirCellBlock.FILL_STEPS / capacity, 1, ReservoirCellBlock.FILL_STEPS - 1);
    }

    private static void show(ReservoirCellBlockEntity cell, Box body, int fill) {
        Level level = cell.getLevel();
        if (level == null) return;
        BlockState shown = ReservoirCellBlock.shaped(cell.getBlockState(), cell.getBlockPos(), body, fill);
        if (shown != cell.getBlockState()) level.setBlock(cell.getBlockPos(), shown, Block.UPDATE_CLIENTS);
    }

    /** Lays all the biomass of {@code members} in again from the bottom and shows it. */
    private static void settle(List<ReservoirCellBlockEntity> members, Box body) {
        int total = 0;
        for (ReservoirCellBlockEntity member : members)
            total += member.share().stored().units();
        int[][] layers = new int[body.sizeY()][body.sizeX() * body.sizeZ()];
        Body.fill(layers, ReservoirCellBlockEntity.CAPACITY_BU, total);
        apply(members, body, layers);
    }
}
