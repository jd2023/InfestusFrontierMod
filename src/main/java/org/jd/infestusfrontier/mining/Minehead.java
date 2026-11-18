package org.jd.infestusfrontier.mining;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Fallable;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.Fracturing;
import org.jd.infestusfrontier.core.mining.Galleries;
import org.jd.infestusfrontier.core.mining.Offset;
import org.jd.infestusfrontier.core.mining.PlanShape;
import org.jd.infestusfrontier.core.mining.PlanStep;
import org.jd.infestusfrontier.core.mining.RootMutation;
import org.jd.infestusfrontier.core.mining.RootMutations;
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
 *
 * <p>Once the staircase's landing is built the minehead finds its gallery faces ({@link Galleries}) by walking its root
 * junctions from the landing, and reads each face from the blocks around its marker the same way. Seen from a route
 * marker facing {@code R}: its Structure Grower stands on it; each Fracture Jaw stands beside the grower, on its left,
 * its right or behind it, and faces it, with its Work Bed behind it and Collection Cilia facing that bed.
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

    /** One jaw of a gallery face, with the bed it cuts into and the cilia that sweep that bed. */
    record Cutter(FractureJawBlockEntity jaw, WorkBedBlockEntity bed, CollectionCiliaBlockEntity cilia) {}

    /** A gallery face's installed organs and its marker's plan; its cutters in the order left, right, behind. */
    record Face(
            SurveyedTissueBlockEntity marker, Survey survey, StructureGrowerBlockEntity grower, List<Cutter> cutters) {}

    /**
     * How a gallery face stood when the minehead last looked: where its grower stands, what it shows (empty while it
     * works), whether it has a block to cut now, how far its route is built and whether it is wide.
     */
    record FaceLook(BlockPos grower, Optional<Refusal> stop, boolean ready, int built, int steps, boolean wide) {}

    /** A root junction: its root cell and the way its root arrived there. */
    private record Junction(BlockPos root, Direction arrived) {}

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
     * What the Rootstock at {@code head} facing {@code facing} orders now, or why it orders nothing: the plan, a
     * finished section, the parts, loaded chunks and the ground of the next step, in that order; a finished section
     * needs none of its staircase organs. Reads loaded positions only; changes nothing.
     */
    static Outcome<Order> order(ServerLevel level, BlockPos head, Direction facing) {
        if (!Cutting.loaded(level, head, PARTS_REACH)) return Outcome.refused(Rootstocking.UNLOADED);
        if (!TunnelRootBlockEntity.rooted(level, head)) return Outcome.refused(Tunnelling.UNROOTED);
        Outcome<SurveyedTissueBlockEntity> section = section(level, head, facing);
        if (section instanceof Outcome.Refused<SurveyedTissueBlockEntity>(Refusal why)) return Outcome.refused(why);
        SurveyedTissueBlockEntity marker = ((Outcome.Done<SurveyedTissueBlockEntity>) section).value();
        if (marker.survey().orElseThrow().finished()) return Outcome.refused(Rootstocking.FINISHED);
        Outcome<Parts> found = parts(level, head, facing, marker);
        if (found instanceof Outcome.Refused<Parts>(Refusal why)) return Outcome.refused(why);
        Parts parts = ((Outcome.Done<Parts>) found).value();
        Outcome<Optional<BlockPos>> cut =
                cut(level, parts.marker(), parts.survey().next());
        if (cut instanceof Outcome.Refused<Optional<BlockPos>>(Refusal why)) return Outcome.refused(why);
        return Outcome.done(new Order(parts, ((Outcome.Done<Optional<BlockPos>>) cut).value()));
    }

    /**
     * The next block of {@code step} to cut, top down, or none when the step is clear; or why none may be cut: a chunk
     * it reads is not loaded, fluid is in it or touches it, a block that falls stands over it, or there is no ground
     * under a root cell. Reads loaded positions only; changes nothing.
     */
    private static Outcome<Optional<BlockPos>> cut(ServerLevel level, SurveyedTissueBlockEntity marker, PlanStep step) {
        List<BlockPos> cuts =
                Rootstocking.cutOrder(step).stream().map(marker::at).toList();
        for (BlockPos cell : cuts)
            if (!Cutting.loaded(level, cell, STEP_REACH)) return Outcome.refused(Rootstocking.UNLOADED);
        for (BlockPos cell : cuts) if (fluidAt(level, cell)) return Outcome.refused(Rootstocking.FLUID);
        for (Offset over : Rootstocking.overheads(step))
            if (level.getBlockState(marker.at(over)).getBlock() instanceof Fallable)
                return Outcome.refused(Rootstocking.FALLING);
        for (Offset under : Rootstocking.grounds(step)) {
            BlockPos ground = marker.at(under);
            if (!level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP))
                return Outcome.refused(Rootstocking.CAVITY);
        }
        return Outcome.done(
                cuts.stream().filter(cell -> !level.getBlockState(cell).isAir()).findFirst());
    }

    /**
     * The reason the minehead shows on its screen, or empty while its organs work. A finished section still shows
     * the cilia's stop while they cannot clear the last cuts from the bed. Reads; changes nothing.
     */
    static Optional<Refusal> status(ServerLevel level, BlockPos head, Direction facing) {
        Outcome<Order> order = order(level, head, facing);
        if (order instanceof Outcome.Refused<Order>(Refusal why)) {
            if (why.equals(Rootstocking.FINISHED)
                    && section(level, head, facing) instanceof Outcome.Done<SurveyedTissueBlockEntity>(var marker)
                    && parts(level, head, facing, marker) instanceof Outcome.Done<Parts>(Parts parts)
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

    /** The marker in front, with a readable, approved staircase plan; or why there is none. */
    private static Outcome<SurveyedTissueBlockEntity> section(ServerLevel level, BlockPos head, Direction facing) {
        if (!(level.getBlockEntity(marker(head, facing)) instanceof SurveyedTissueBlockEntity marker))
            return Outcome.refused(Rootstocking.NO_PLAN);
        if (marker.facing() != facing) return Outcome.refused(Rootstocking.MARKER_TURNED);
        Optional<Survey> survey = marker.survey();
        if (survey.isEmpty()) return Outcome.refused(Rootstocking.NO_PLAN);
        if (!survey.get().approved()) return Outcome.refused(Rootstocking.NOT_APPROVED);
        if (survey.get().plan().shape() != PlanShape.STAIRCASE) return Outcome.refused(Rootstocking.NOT_STAIRCASE);
        return Outcome.done(marker);
    }

    /** The staircase's installed organs around {@code marker}, or the first that is missing. */
    private static Outcome<Parts> parts(
            ServerLevel level, BlockPos head, Direction facing, SurveyedTissueBlockEntity marker) {
        Survey survey = marker.survey().orElseThrow();
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
        Optional<CollectionCiliaBlockEntity> cilia = cilia(level, bedAt, facing); // facing: the jaw's place
        if (cilia.isEmpty()) return Outcome.refused(Rootstocking.NO_CILIA);
        return Outcome.done(new Parts(marker, survey, grower, jaw, bed, cilia.get()));
    }

    // ---- galleries ----

    /**
     * The markers of the gallery faces of the minehead whose staircase {@code staircase} plans, in the order they take
     * turns; or why it orders none. Only a finished staircase has galleries: its landing is the first root junction,
     * and the last root cell of each finished route another. A route marker counts when it lies right beside a
     * junction's root cell on one of its sides ({@link Galleries#sides}) and faces away from it. The walk refuses as a
     * whole: past {@value Galleries#MAX_ROUTES} routes, past {@value Galleries#MAX_FACES} unfinished ones, or at an
     * unloaded block it must read. Reads; changes nothing.
     */
    static Outcome<List<SurveyedTissueBlockEntity>> galleries(ServerLevel level, SurveyedTissueBlockEntity staircase) {
        Optional<Survey> section = staircase.survey();
        if (section.isEmpty() || !section.get().finished()) return Outcome.done(List.of());
        List<Junction> junctions = new ArrayList<>();
        junctions.add(new Junction(staircase.at(section.get().plan().end()), staircase.facing()));
        Set<BlockPos> walked = new HashSet<>();
        List<SurveyedTissueBlockEntity> faces = new ArrayList<>();
        int routes = 0;
        for (int next = 0; next < junctions.size(); next++) {
            Junction junction = junctions.get(next);
            if (!walked.add(junction.root())) continue;
            for (int quarters : Galleries.sides(0)) {
                Direction side = StructureGrowerBlockEntity.heading(junction.arrived(), quarters);
                BlockPos at = junction.root().relative(side);
                if (!level.hasChunkAt(at)) return Outcome.refused(Rootstocking.UNLOADED);
                if (!(level.getBlockEntity(at) instanceof SurveyedTissueBlockEntity route) || route.facing() != side)
                    continue;
                Optional<Survey> survey = route.survey();
                if (survey.isPresent() && survey.get().plan().shape() != PlanShape.ROUTE) continue;
                if (++routes > Galleries.MAX_ROUTES) return Outcome.refused(Galleries.TOO_MANY_ROUTES);
                if (survey.isPresent() && survey.get().finished()) {
                    List<PlanStep> steps = survey.get().plan().steps();
                    Direction leaving = StructureGrowerBlockEntity.heading(
                            side, steps.get(steps.size() - 1).leaving());
                    junctions.add(new Junction(route.at(survey.get().plan().end()), leaving));
                } else faces.add(route);
            }
        }
        if (faces.size() > Galleries.MAX_FACES) return Outcome.refused(Galleries.TOO_MANY_FACES);
        return Outcome.done(List.copyOf(faces));
    }

    /** The organs of the gallery face {@code marker} plans, or the first that is missing. Reads loaded positions only. */
    static Outcome<Face> face(ServerLevel level, SurveyedTissueBlockEntity marker) {
        BlockPos growerAt = marker.getBlockPos().above();
        if (!Cutting.loaded(level, growerAt, PARTS_REACH)) return Outcome.refused(Rootstocking.UNLOADED);
        Optional<Survey> survey = marker.survey();
        if (survey.isEmpty()) return Outcome.refused(Rootstocking.NO_PLAN);
        if (!survey.get().approved()) return Outcome.refused(Rootstocking.NOT_APPROVED);
        if (!(level.getBlockEntity(growerAt) instanceof StructureGrowerBlockEntity grower) || grower.unreadable())
            return Outcome.refused(Rootstocking.NO_GROWER);
        Direction forward = marker.facing();
        List<Cutter> cutters = new ArrayList<>(Galleries.WIDE_JAWS);
        for (Direction side : List.of(forward.getCounterClockWise(), forward.getClockWise(), forward.getOpposite())) {
            BlockPos jawAt = growerAt.relative(side);
            if (!(level.getBlockEntity(jawAt) instanceof FractureJawBlockEntity jaw)
                    || jaw.unreadable()
                    || jaw.getBlockState().getValue(FacingOrganBlock.FACING) != side.getOpposite()) continue;
            BlockPos bedAt = jawAt.relative(side);
            if (!(level.getBlockEntity(bedAt) instanceof WorkBedBlockEntity bed) || bed.unreadable())
                return Outcome.refused(Rootstocking.NO_BED);
            Optional<CollectionCiliaBlockEntity> cilia = cilia(level, bedAt, side.getOpposite());
            if (cilia.isEmpty()) return Outcome.refused(Rootstocking.NO_CILIA);
            cutters.add(new Cutter(jaw, bed, cilia.get()));
        }
        if (cutters.isEmpty()) return Outcome.refused(Rootstocking.NO_JAW);
        return Outcome.done(new Face(marker, survey.get(), grower, List.copyOf(cutters)));
    }

    /**
     * The next block of {@code face}'s next step to cut, or none while that step is clear for its grower; or why the
     * minehead, whose mutations are {@code mutations}, orders it none. A wide route needs the Wider Section and
     * {@value Galleries#WIDE_JAWS} jaws. Reads loaded positions only; changes nothing.
     */
    static Outcome<Optional<BlockPos>> faceCut(ServerLevel level, Face face, RootMutations mutations) {
        if (face.survey().finished()) return Outcome.refused(Rootstocking.FINISHED);
        if (face.survey().plan().wide()) {
            if (!mutations.has(RootMutation.WIDER_SECTION)) return Outcome.refused(Galleries.NEEDS_WIDER_SECTION);
            if (face.cutters().size() < Galleries.WIDE_JAWS) return Outcome.refused(Galleries.NEEDS_JAWS);
        }
        return cut(level, face.marker(), face.survey().next());
    }

    /**
     * How the gallery face {@code marker} plans stands now: its own stop first, then, while there is a block to cut,
     * the first jaw's that is stopped, with a full bed shown as its cilia's; once the step is clear, the grower's; then
     * the cilia's. Ready when nothing stops it and it has a block to cut. Reads; changes nothing.
     */
    static FaceLook look(ServerLevel level, SurveyedTissueBlockEntity marker, RootMutations mutations) {
        BlockPos growerAt = marker.getBlockPos().above();
        Optional<Survey> survey = marker.survey();
        int built = survey.map(Survey::built).orElse(0);
        int steps = survey.map(s -> s.plan().steps().size()).orElse(0);
        boolean wide = survey.map(s -> s.plan().wide()).orElse(false);
        Outcome<Face> found = face(level, marker);
        if (found instanceof Outcome.Refused<Face>(Refusal why))
            return new FaceLook(growerAt, Optional.of(why), false, built, steps, wide);
        Face face = ((Outcome.Done<Face>) found).value();
        Outcome<Optional<BlockPos>> cut = faceCut(level, face, mutations);
        if (cut instanceof Outcome.Refused<Optional<BlockPos>>(Refusal why))
            return new FaceLook(growerAt, Optional.of(why), false, built, steps, wide);
        boolean cutting = ((Outcome.Done<Optional<BlockPos>>) cut).value().isPresent();
        Optional<Refusal> jaw = Optional.empty();
        Optional<Refusal> cilia = Optional.empty();
        for (Cutter cutter : face.cutters()) {
            Optional<Refusal> stop = cutter.jaw().stop().filter(s -> !s.equals(Fracturing.NO_ORDER));
            if (jaw.isEmpty() && stop.isPresent()) {
                jaw = stop;
                if (stop.get().equals(Fracturing.BED_FULL))
                    cilia = cutter.cilia().stop();
            }
            if (cilia.isEmpty()) cilia = cutter.cilia().stop();
        }
        Optional<Refusal> shown =
                Rootstocking.shown(Optional.empty(), cutting, jaw, face.grower().stop(), cilia);
        return new FaceLook(growerAt, shown, shown.isEmpty() && cutting, built, steps, wide);
    }

    /** Readable cilia beside {@code bed}, not on the side {@code taken}, that face it. */
    private static Optional<CollectionCiliaBlockEntity> cilia(ServerLevel level, BlockPos bed, Direction taken) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (side == taken) continue;
            if (level.getBlockEntity(bed.relative(side)) instanceof CollectionCiliaBlockEntity cilia
                    && !cilia.unreadable()
                    && cilia.getBlockState().getValue(FacingOrganBlock.FACING) == side.getOpposite())
                return Optional.of(cilia);
        }
        return Optional.empty();
    }

    /** True when fluid stands in {@code cell} or next to it, where it would flow into the cut. */
    private static boolean fluidAt(ServerLevel level, BlockPos cell) {
        if (!level.getFluidState(cell).isEmpty()) return true;
        for (Direction side : Direction.values())
            if (!level.getFluidState(cell.relative(side)).isEmpty()) return true;
        return false;
    }
}
