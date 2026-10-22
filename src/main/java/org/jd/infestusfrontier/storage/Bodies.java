package org.jd.infestusfrontier.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jd.infestusfrontier.core.storage.Body;
import org.jd.infestusfrontier.core.storage.Box;

/**
 * The life of storage bodies in a level, for every kind of cell that forms one ({@link Body}): a cell looks for a
 * body on a scheduled tick, joins every touching body it makes a full box with, and a body that loses a cell, or is
 * found broken, falls apart into cells that look again. Every step reads at most one box, {@value Body#MAX_SIDE}³
 * cells, and never an unloaded one. What a body holds, and how it shows, belongs to its {@link Kind}.
 */
final class Bodies {
    /** Ticks from a placement, a load or a break to a cell's look for a body. */
    static final int FORM_TICKS = 1;

    /** A body as its two corners, min x y z then max x y z. */
    static final Codec<Box> BOX = Codec.INT
            .listOf(6, 6)
            .comapFlatMap(
                    Bodies::box,
                    box -> List.of(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()));

    /** A block entity that is one cell of a body. */
    interface Cell {
        Box body();

        void setBody(Box body);

        /** False while the cell keeps unreadable saved data; such a cell joins no body. */
        boolean isReadable();
    }

    /** What one kind of body does when its cells change. */
    interface Kind<C extends BlockEntity & Cell> {
        Class<C> type();

        /** {@code members}, every cell of {@code body} bottom layer first, have just formed it or looked at it again. */
        void formed(List<C> members, Box body);

        /** {@code cell} was left on its own by a body that fell apart. */
        void alone(C cell);
    }

    private Bodies() {}

    private static DataResult<Box> box(List<Integer> corners) {
        if (corners.get(0) > corners.get(3) || corners.get(1) > corners.get(4) || corners.get(2) > corners.get(5))
            return DataResult.error(() -> "Inverted body " + corners);
        return DataResult.success(new Box(
                corners.get(0), corners.get(1), corners.get(2), corners.get(3), corners.get(4), corners.get(5)));
    }

    static Box single(BlockPos pos) {
        return Box.of(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * True when {@code box} could be the body of a cell at {@code pos}. A body saved somewhere else, as a structure
     * moved elsewhere, is no body: the cell is alone and forms again.
     */
    static boolean holds(Box box, BlockPos pos) {
        return box.fits(Body.MAX_SIDE) && box.contains(pos.getX(), pos.getY(), pos.getZ());
    }

    /** Every position of {@code box}, layer by layer from the bottom. */
    static List<BlockPos> positions(Box box) {
        List<BlockPos> positions = new ArrayList<>(box.volume());
        for (int y = box.minY(); y <= box.maxY(); y++)
            for (int x = box.minX(); x <= box.maxX(); x++)
                for (int z = box.minZ(); z <= box.maxZ(); z++) positions.add(new BlockPos(x, y, z));
        return positions;
    }

    /** The cells of {@code cell}'s body in {@link #positions} order, or empty while any is out of reach. */
    static <C extends BlockEntity & Cell> Optional<List<C>> members(C cell, Kind<C> kind) {
        Box body = cell.body();
        if (body.single()) return Optional.of(List.of(cell));
        Level level = cell.getLevel();
        if (level == null) return Optional.empty();
        List<C> members = new ArrayList<>(body.volume());
        for (BlockPos pos : positions(body)) {
            if (!level.hasChunkAt(pos)) return Optional.empty();
            BlockEntity found = level.getBlockEntity(pos);
            if (!kind.type().isInstance(found)) return Optional.empty();
            C member = kind.type().cast(found);
            if (!member.isReadable() || !member.body().equals(body)) return Optional.empty();
            members.add(member);
        }
        return Optional.of(members);
    }

    /** The box {@link Body#grow} reads: the body of a loaded, readable cell of this kind at a position. */
    private static <C extends BlockEntity & Cell> Body.Cells cells(Level level, Kind<C> kind) {
        return (x, y, z) -> {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.hasChunkAt(pos)) return Optional.empty();
            BlockEntity found = level.getBlockEntity(pos);
            if (!kind.type().isInstance(found)) return Optional.empty();
            C cell = kind.type().cast(found);
            return cell.isReadable() ? Optional.of(cell.body()) : Optional.empty();
        };
    }

    /**
     * The cell's scheduled look at its body: a broken body falls apart, then the body joins every touching body it
     * makes a full box with, one at a time, until none fits. Each join reads at most the joined box. Last the kind
     * sees the body as it stands, so a cell placed with contents on its item, or a body set by a command, is right.
     */
    static <C extends BlockEntity & Cell> void form(C cell, Kind<C> kind) {
        Level level = cell.getLevel();
        if (level == null || level.isClientSide || !cell.isReadable()) return;
        if (!cell.body().single()) {
            for (BlockPos pos : positions(cell.body())) if (!level.hasChunkAt(pos)) return;
            if (members(cell, kind).isEmpty()) {
                fallApart(level, cell.body(), kind);
                return;
            }
        }
        Body.Cells cells = cells(level, kind);
        boolean grew = true;
        while (grew) {
            grew = false;
            for (BlockPos pos : around(cell.body())) {
                Optional<Box> touching = cells.at(pos.getX(), pos.getY(), pos.getZ());
                if (touching.isEmpty()) continue;
                Optional<Box> union = Body.grow(cell.body(), touching.get(), cells, Body.MAX_SIDE);
                if (union.isPresent()) {
                    join(level, union.get(), kind);
                    grew = true;
                    break;
                }
            }
        }
        members(cell, kind).ifPresent(members -> kind.formed(members, cell.body()));
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

    /** Makes every cell inside {@code union} one body; {@link Body#grow} has read them all. */
    private static <C extends BlockEntity & Cell> void join(Level level, Box union, Kind<C> kind) {
        List<C> members = new ArrayList<>(union.volume());
        for (BlockPos pos : positions(union)) {
            C member = kind.type().cast(level.getBlockEntity(pos));
            members.add(member);
            member.setBody(union);
        }
        kind.formed(members, union);
    }

    /** Every loaded cell still naming {@code body} stands alone with its contents and looks for a body next tick. */
    private static <C extends BlockEntity & Cell> void fallApart(Level level, Box body, Kind<C> kind) {
        for (BlockPos pos : positions(body)) {
            if (!level.hasChunkAt(pos)) continue;
            BlockEntity found = level.getBlockEntity(pos);
            if (!kind.type().isInstance(found)) continue;
            C member = kind.type().cast(found);
            if (!member.body().equals(body)) continue;
            member.setBody(single(pos));
            kind.alone(member);
            level.scheduleTick(pos, member.getBlockState().getBlock(), FORM_TICKS);
        }
    }

    /** {@code cell} is leaving: the rest of its body falls apart, each keeping its contents. */
    static <C extends BlockEntity & Cell> void leave(C cell, Kind<C> kind) {
        Level level = cell.getLevel();
        if (level == null || level.isClientSide || cell.body().single()) return;
        Box body = cell.body();
        cell.setBody(single(cell.getBlockPos()));
        fallApart(level, body, kind);
    }
}
