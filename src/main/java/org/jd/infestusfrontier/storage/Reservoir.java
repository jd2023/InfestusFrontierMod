package org.jd.infestusfrontier.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
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
 * The bodies of Reservoir cells in a level. A body is the cells inside one box that all name that box; each holds its
 * own share, so a broken cell takes exactly its share and the rest stays. Every operation reads the whole box, at most
 * {@value Body#MAX_SIDE}³ cells, and is refused while any of it is unloaded. A body found broken (a cell left while
 * its chunk was away) refuses until its cells have fallen apart and formed again on their next tick.
 */
final class Reservoir {
    /** Part of the body is not loaded, or it is still forming. */
    static final Refusal OUT_OF_REACH = new Refusal("storage.body_out_of_reach");

    private Reservoir() {}

    /** The cells of {@code cell}'s body, lowest layer first, or empty while any is out of reach. */
    private static Optional<List<ReservoirCellBlockEntity>> members(ReservoirCellBlockEntity cell) {
        Box body = cell.body();
        if (body.single()) return Optional.of(List.of(cell));
        Level level = cell.getLevel();
        if (level == null) return Optional.empty();
        List<ReservoirCellBlockEntity> members = new ArrayList<>(body.volume());
        for (BlockPos pos : positions(body)) {
            if (!level.hasChunkAt(pos)) return Optional.empty();
            if (!(level.getBlockEntity(pos) instanceof ReservoirCellBlockEntity member)
                    || !member.isReadable()
                    || !member.body().equals(body)) {
                if (!level.isClientSide)
                    level.scheduleTick(
                            cell.getBlockPos(), cell.getBlockState().getBlock(), ReservoirCellBlock.FORM_TICKS);
                return Optional.empty();
            }
            members.add(member);
        }
        return Optional.of(members);
    }

    /** Every position of {@code box}, layer by layer from the bottom. */
    private static List<BlockPos> positions(Box box) {
        List<BlockPos> positions = new ArrayList<>(box.volume());
        for (int y = box.minY(); y <= box.maxY(); y++)
            for (int x = box.minX(); x <= box.maxX(); x++)
                for (int z = box.minZ(); z <= box.maxZ(); z++) positions.add(new BlockPos(x, y, z));
        return positions;
    }

    /** The shares of {@code members} as layers, bottom first, in the order {@link #positions} lists them. */
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

    /** The box {@link Body#grow} reads: the body of a loaded, readable cell at a position. */
    private static Body.Cells cells(Level level) {
        return (x, y, z) -> {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.hasChunkAt(pos)) return Optional.empty();
            return level.getBlockEntity(pos) instanceof ReservoirCellBlockEntity cell && cell.isReadable()
                    ? Optional.of(cell.body())
                    : Optional.empty();
        };
    }

    /**
     * The cell's scheduled look at its body: a broken body falls apart, then the body joins every touching body it
     * makes a full box with, one at a time, until none fits. Each join reads at most the joined box. Last the body
     * settles, its biomass laid in from the bottom and shown, so a cell placed with biomass on its item, or a body set
     * by a command, is right.
     */
    static void form(ReservoirCellBlockEntity cell) {
        Level level = cell.getLevel();
        if (level == null || level.isClientSide || !cell.isReadable()) return;
        if (!cell.body().single()) {
            for (BlockPos pos : positions(cell.body())) if (!level.hasChunkAt(pos)) return;
            if (members(cell).isEmpty()) {
                fallApart(level, cell.body());
                return;
            }
        }
        Body.Cells cells = cells(level);
        boolean grew = true;
        while (grew) {
            grew = false;
            for (BlockPos pos : around(cell.body())) {
                Optional<Box> touching = cells.at(pos.getX(), pos.getY(), pos.getZ());
                if (touching.isEmpty()) continue;
                Optional<Box> union = Body.grow(cell.body(), touching.get(), cells, Body.MAX_SIDE);
                if (union.isPresent()) {
                    join(level, union.get());
                    grew = true;
                    break;
                }
            }
        }
        members(cell).ifPresent(members -> settle(members, cell.body()));
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

    /** The positions that touch a face of {@code box} from outside. */
    private static List<BlockPos> around(Box box) {
        List<BlockPos> around = new ArrayList<>();
        for (int y = box.minY(); y <= box.maxY(); y++)
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                around.add(new BlockPos(box.minX() - 1, y, z));
                around.add(new BlockPos(box.maxX() + 1, y, z));
            }
        for (int x = box.minX(); x <= box.maxX(); x++)
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                around.add(new BlockPos(x, box.minY() - 1, z));
                around.add(new BlockPos(x, box.maxY() + 1, z));
            }
        for (int x = box.minX(); x <= box.maxX(); x++)
            for (int y = box.minY(); y <= box.maxY(); y++) {
                around.add(new BlockPos(x, y, box.minZ() - 1));
                around.add(new BlockPos(x, y, box.maxZ() + 1));
            }
        return around;
    }

    /** Makes every cell inside {@code union} one body and lays all their biomass in from the bottom. */
    private static void join(Level level, Box union) {
        List<ReservoirCellBlockEntity> members = new ArrayList<>(union.volume());
        for (BlockPos pos : positions(union)) {
            ReservoirCellBlockEntity member = (ReservoirCellBlockEntity) level.getBlockEntity(pos);
            members.add(member);
            member.setBody(union);
        }
        settle(members, union);
    }

    /** Every loaded cell still naming {@code body} stands alone with its share and looks for a body next tick. */
    private static void fallApart(Level level, Box body) {
        for (BlockPos pos : positions(body)) {
            if (!level.hasChunkAt(pos)
                    || !(level.getBlockEntity(pos) instanceof ReservoirCellBlockEntity member)
                    || !member.body().equals(body)) continue;
            Box alone = ReservoirCellBlockEntity.single(pos);
            member.setBody(alone);
            show(member, alone, fill(new int[] {member.share().stored().units()}));
            level.scheduleTick(pos, member.getBlockState().getBlock(), ReservoirCellBlock.FORM_TICKS);
        }
    }

    /** {@code cell} is leaving: the rest of its body falls apart, each keeping its share. */
    static void leave(ReservoirCellBlockEntity cell) {
        Level level = cell.getLevel();
        if (level == null || level.isClientSide || cell.body().single()) return;
        Box body = cell.body();
        cell.setBody(ReservoirCellBlockEntity.single(cell.getBlockPos()));
        fallApart(level, body);
    }
}
