package org.jd.infestusfrontier.mining;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Fallable;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.PlanShape;
import org.jd.infestusfrontier.core.mining.PlanStep;
import org.jd.infestusfrontier.core.mining.Rootstocking;
import org.jd.infestusfrontier.core.mining.Survey;
import org.jd.infestusfrontier.core.mining.Tunnelling;

/**
 * A Descending Rootstock's minehead, read from the blocks around the head each time it is asked; nothing about the
 * assembly is saved, so it is rebuilt by placing its organs again. Seen from a head facing {@code F}:
 *
 * <ul>
 *   <li>the Surveyed Tissue marker is in front, one lower, flush with the head's foundation, and faces {@code F};
 *   <li>the Structure Grower stands on the marker, right in front of the head;
 *   <li>the Fracture Jaw stands right behind the head and faces it, with its Work Bed behind it;
 *   <li>Collection Cilia stand beside the bed, or behind it, and face it.
 * </ul>
 *
 * <p>Each organ keeps doing its own work, with its own supplies, rate and limits: the jaw cuts and pays, the bed holds,
 * the cilia export and the grower builds. The Rootstock only orders which block the jaw cuts next, and judges the
 * ground of the next step before it does. With an organ missing the minehead pauses and names it; nothing else does
 * that organ's work.
 */
final class Minehead {
    /** Blocks around the head whose chunks the parts are in: the bed is two behind and its cilia one further. */
    private static final int PARTS_REACH = 3;
    /** Blocks around each block of the next step whose chunks must be loaded: its neighbours and theirs. */
    private static final int STEP_REACH = 2;

    /** The installed organs and the marker's plan. */
    record Parts(
            SurveyedTissueBlockEntity marker,
            Survey survey,
            StructureGrowerBlockEntity grower,
            FractureJawBlockEntity jaw,
            WorkBedBlockEntity bed,
            CollectionCiliaBlockEntity cilia) {}

    /** What the Rootstock orders now: the next block to cut, or none while the step is clear for the grower. */
    record Order(Parts parts, Optional<BlockPos> cut) {}

    private Minehead() {}

    /** Where the marker of a minehead whose head stands at {@code head} facing {@code facing} lies. */
    static BlockPos marker(BlockPos head, Direction facing) {
        return head.relative(facing).below();
    }

    /** True when a marker lies where a minehead's would. Reads only a loaded position. */
    static boolean faces(Level level, BlockPos head, Direction facing) {
        BlockPos marker = marker(head, facing);
        return level.hasChunkAt(marker) && level.getBlockEntity(marker) instanceof SurveyedTissueBlockEntity;
    }

    /** The Rootstock whose minehead uses {@code marker}, when one stands behind its grower facing the same way. */
    static Optional<TunnelRootBlockEntity> rootstockOf(Level level, SurveyedTissueBlockEntity marker) {
        Direction facing = marker.facing();
        BlockPos head = marker.getBlockPos().above().relative(facing.getOpposite());
        if (!level.hasChunkAt(head)
                || !(level.getBlockEntity(head) instanceof TunnelRootBlockEntity rootstock)
                || !rootstock.rootstock()
                || rootstock.getBlockState().getValue(TunnelRootBlock.FACING) != facing) return Optional.empty();
        return Optional.of(rootstock);
    }

    /**
     * What the Rootstock at {@code head} facing {@code facing} orders now, or why it orders nothing: the plan, the
     * parts, a finished section, loaded chunks and the ground of the next step, in that order. Reads loaded positions
     * only; changes nothing.
     */
    static Outcome<Order> order(ServerLevel level, BlockPos head, Direction facing) {
        if (!Cutting.loaded(level, head, PARTS_REACH)) return Outcome.refused(Rootstocking.UNLOADED);
        if (!TunnelRootBlockEntity.rooted(level, head)) return Outcome.refused(Tunnelling.UNROOTED);
        Outcome<Parts> found = parts(level, head, facing);
        if (found instanceof Outcome.Refused<Parts>(Refusal why)) return Outcome.refused(why);
        Parts parts = ((Outcome.Done<Parts>) found).value();
        if (parts.survey().finished()) return Outcome.refused(Rootstocking.FINISHED);
        PlanStep step = parts.survey().next();
        List<BlockPos> cuts =
                Rootstocking.cutOrder(step).stream().map(parts.marker()::at).toList();
        for (BlockPos cell : cuts)
            if (!Cutting.loaded(level, cell, STEP_REACH)) return Outcome.refused(Rootstocking.UNLOADED);
        for (BlockPos cell : cuts) if (fluidAt(level, cell)) return Outcome.refused(Rootstocking.FLUID);
        if (level.getBlockState(parts.marker().at(Rootstocking.overhead(step))).getBlock() instanceof Fallable)
            return Outcome.refused(Rootstocking.FALLING);
        BlockPos ground = parts.marker().at(Rootstocking.ground(step));
        if (!level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP))
            return Outcome.refused(Rootstocking.CAVITY);
        Optional<BlockPos> cut =
                cuts.stream().filter(cell -> !level.getBlockState(cell).isAir()).findFirst();
        return Outcome.done(new Order(parts, cut));
    }

    /**
     * The reason the minehead shows on its screen, or empty while its organs work. A finished section still shows
     * the cilia's stop while they cannot clear the last cuts from the bed. Reads; changes nothing.
     */
    static Optional<Refusal> status(ServerLevel level, BlockPos head, Direction facing) {
        Outcome<Order> order = order(level, head, facing);
        if (order instanceof Outcome.Refused<Order>(Refusal why)) {
            if (why.equals(Rootstocking.FINISHED)
                    && parts(level, head, facing) instanceof Outcome.Done<Parts>(Parts parts)
                    && parts.cilia().stop().isPresent()) return parts.cilia().stop();
            return Optional.of(why);
        }
        Order now = ((Outcome.Done<Order>) order).value();
        return Rootstocking.shown(
                Optional.empty(),
                now.cut().isPresent(),
                now.parts().jaw().stop(),
                now.parts().grower().stop(),
                now.parts().cilia().stop());
    }

    /** The marker's plan and the installed organs, or the first that is missing. */
    private static Outcome<Parts> parts(ServerLevel level, BlockPos head, Direction facing) {
        if (!(level.getBlockEntity(marker(head, facing)) instanceof SurveyedTissueBlockEntity marker))
            return Outcome.refused(Rootstocking.NO_PLAN);
        if (marker.facing() != facing) return Outcome.refused(Rootstocking.MARKER_TURNED);
        Optional<Survey> survey = marker.survey();
        if (survey.isEmpty()) return Outcome.refused(Rootstocking.NO_PLAN);
        if (!survey.get().approved()) return Outcome.refused(Rootstocking.NOT_APPROVED);
        if (survey.get().plan().shape() != PlanShape.STAIRCASE) return Outcome.refused(Rootstocking.NOT_STAIRCASE);
        if (!(level.getBlockEntity(head.relative(facing)) instanceof StructureGrowerBlockEntity grower)
                || grower.unreadable()) return Outcome.refused(Rootstocking.NO_GROWER);
        BlockPos jawAt = head.relative(facing.getOpposite());
        if (!(level.getBlockEntity(jawAt) instanceof FractureJawBlockEntity jaw)
                || jaw.unreadable()
                || jaw.getBlockState().getValue(FacingOrganBlock.FACING) != facing)
            return Outcome.refused(Rootstocking.NO_JAW);
        BlockPos bedAt = jawAt.relative(facing.getOpposite());
        if (!(level.getBlockEntity(bedAt) instanceof WorkBedBlockEntity bed) || bed.unreadable())
            return Outcome.refused(Rootstocking.NO_BED);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (side == facing) continue; // the jaw's place
            if (level.getBlockEntity(bedAt.relative(side)) instanceof CollectionCiliaBlockEntity cilia
                    && !cilia.unreadable()
                    && cilia.getBlockState().getValue(FacingOrganBlock.FACING) == side.getOpposite())
                return Outcome.done(new Parts(marker, survey.get(), grower, jaw, bed, cilia));
        }
        return Outcome.refused(Rootstocking.NO_CILIA);
    }

    /** True when fluid stands in {@code cell} or next to it, where it would flow into the cut. */
    private static boolean fluidAt(ServerLevel level, BlockPos cell) {
        if (!level.getFluidState(cell).isEmpty()) return true;
        for (Direction side : Direction.values())
            if (!level.getFluidState(cell.relative(side)).isEmpty()) return true;
        return false;
    }
}
