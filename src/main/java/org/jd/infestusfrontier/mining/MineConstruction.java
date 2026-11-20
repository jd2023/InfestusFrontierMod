package org.jd.infestusfrontier.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.common.Tags;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.mining.MinePlan;
import org.jd.infestusfrontier.core.mining.PlanStep;
import org.jd.infestusfrontier.core.mining.Surveying;

/**
 * Plans one construction step from loaded terrain and a copy of the grower's stock. It changes neither.
 * The grower applies the placements and pays their cost together, after admission to the shared work budget.
 */
final class MineConstruction {
    /** Neighbours that placements read or update must also be loaded. */
    private static final int REACH = 1;

    /** Placements in build order and the stock they consume; valid for this server tick only. */
    record Build(List<Placement> placements, MinePlan.Needs cost, boolean wide) {
        Build {
            placements = List.copyOf(placements);
        }
    }

    record Placement(BlockPos pos, BlockState state) {}

    private MineConstruction() {}

    /**
     * Whether {@code step} can be built now, and what it places. Reads loaded positions only and changes nothing. Every
     * block of the step must be clear, though fluid may stand in it: the grower builds into it as a player would. A
     * root cell with no solid ground under it is left out, with the tread on it, and not paid: the grower never
     * bridges a gap. Then whether a creature stands in the way, then the stock: segments for the grounded root cells,
     * a tread, a light, and ribs for a framed step. A light, ribs and a scar go only where a wall or a ceiling holds
     * them and the cell is dry; where none can, they are left out and not paid.
     */
    static Outcome<Build> plan(
            ServerLevel level, SurveyedTissueBlockEntity marker, PlanStep step, StructureGrowerContents stock) {
        List<BlockPos> roots = step.roots().stream().map(marker::at).toList();
        List<BlockPos> cleared = step.cleared().stream().map(marker::at).toList();
        for (BlockPos cell : cleared)
            if (!Cutting.loaded(level, cell, REACH)) return Outcome.refused(Surveying.UNLOADED);
        for (BlockPos root : roots)
            if (!Cutting.loaded(level, root.below(), REACH)) return Outcome.refused(Surveying.UNLOADED);
        for (BlockPos cell : cleared)
            if (!Cutting.open(level.getBlockState(cell))) return Outcome.refused(Surveying.NOT_CLEAR);
        List<BlockPos> grounded = roots.stream()
                .filter(root -> level.getBlockState(root.below()).isFaceSturdy(level, root.below(), Direction.UP))
                .toList();
        Optional<BlockPos> treadAt = step.tread().map(marker::at).filter(tread -> grounded.contains(tread.below()));
        List<BlockPos> solids = new ArrayList<>(grounded);
        treadAt.ifPresent(solids::add);
        for (BlockPos solid : solids)
            if (!level.isUnobstructed(null, Shapes.block().move(solid.getX(), solid.getY(), solid.getZ())))
                return Outcome.refused(Surveying.CREATURE);
        if (stock.segments().getCount() < grounded.size()) return Outcome.refused(Surveying.NO_SEGMENT);
        if (treadAt.isPresent() && stock.treads().isEmpty()) return Outcome.refused(Surveying.NO_TREAD);

        Direction forward = marker.facing();
        Direction arrived = MiningDirections.turn(forward, step.arrived());
        Direction leaving = MiningDirections.turn(forward, step.leaving());
        Optional<BlockPos> lightAt = step.light().map(marker::at);
        Optional<Placement> light = lightAt.flatMap(
                at -> light(level, at, leaving, cleared, stock.lights().is(Items.TORCH)));
        if (light.isPresent() && stock.lights().isEmpty()) return Outcome.refused(Surveying.NO_LIGHT);
        List<Placement> ribs = step.framed()
                ? ribs(
                        level,
                        roots,
                        step.passage().stream().map(marker::at).toList(),
                        cleared,
                        lightAt,
                        leaving,
                        !step.flanks().isEmpty())
                : List.of();
        int ribsPaid = (int)
                ribs.stream().map(rib -> columnOf(roots, rib.pos())).distinct().count();
        if (stock.ribs().getCount() < ribsPaid) return Outcome.refused(Surveying.NO_RIB);

        List<Placement> placements = new ArrayList<>();
        for (BlockPos root : grounded) {
            BlockState cell = root.equals(roots.get(0)) && step.bend()
                    ? RootCellBlock.bend(arrived, leaving)
                    : RootCellBlock.straight(leaving);
            placements.add(new Placement(root, cell));
        }
        if (treadAt.isPresent()) {
            BlockState tread = ((BlockItem) stock.treads().getItem())
                    .getBlock()
                    .defaultBlockState()
                    .setValue(StairBlock.FACING, forward.getOpposite())
                    .setValue(StairBlock.HALF, Half.BOTTOM)
                    .setValue(
                            StairBlock.WATERLOGGED,
                            level.getFluidState(treadAt.get()).is(Fluids.WATER));
            placements.add(new Placement(treadAt.get(), Block.updateFromNeighbourShapes(tread, level, treadAt.get())));
        }
        light.ifPresent(placements::add);
        placements.addAll(ribs);
        if (step.scarred())
            scar(level, marker, step, cleared, ribs, lightAt, leaving).ifPresent(placements::add);
        return Outcome.done(new Build(
                placements,
                new MinePlan.Needs(grounded.size(), treadAt.isPresent() ? 1 : 0, light.isPresent() ? 1 : 0, ribsPaid),
                !step.flanks().isEmpty()));
    }

    /**
     * The light at {@code at}: from living-light stock a Lumen Node hung from the ceiling, else on the wall left of
     * {@code travel}, else on the right; from torches a wall torch on either wall. Empty when the cell holds fluid or
     * nothing holds the light: it is left out, not bridged or propped.
     */
    private static Optional<Placement> light(
            ServerLevel level, BlockPos at, Direction travel, List<BlockPos> cleared, boolean torchStock) {
        if (!level.getBlockState(at).isAir()) return Optional.empty();
        List<Direction> walls = List.of(travel.getCounterClockWise(), travel.getClockWise());
        if (torchStock) {
            for (Direction wall : walls) {
                BlockState torch =
                        Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, wall.getOpposite());
                if (!cleared.contains(at.relative(wall)) && torch.canSurvive(level, at))
                    return Optional.of(new Placement(at, torch));
            }
            return Optional.empty();
        }
        BlockState node = MiningModule.LUMEN_NODE.get().defaultBlockState();
        List<BlockState> hung = new ArrayList<>();
        hung.add(node.setValue(LumenNodeBlock.FACE, AttachFace.CEILING).setValue(LumenNodeBlock.FACING, travel));
        for (Direction wall : walls)
            hung.add(node.setValue(LumenNodeBlock.FACE, AttachFace.WALL)
                    .setValue(LumenNodeBlock.FACING, wall.getOpposite()));
        for (BlockState state : hung)
            if (!cleared.contains(at.relative(LumenNodeBlock.support(state))) && state.canSurvive(level, at))
                return Optional.of(new Placement(at, state));
        return Optional.empty();
    }

    /**
     * The bone ribs of a framed step: on each dry, empty passage cell but the light's, a rib on each side wall and, on
     * a column's top cell, the ceiling, where that face is outside the step and holds it and is not ore. Thick on a
     * step of the Wider Section. They have no collision, so the passage keeps its headroom and width.
     */
    private static List<Placement> ribs(
            ServerLevel level,
            List<BlockPos> roots,
            List<BlockPos> passage,
            List<BlockPos> cleared,
            Optional<BlockPos> light,
            Direction leaving,
            boolean wide) {
        BlockState rib = MiningModule.BONE_RIB
                .get()
                .defaultBlockState()
                .setValue(BoneRibBlock.AXIS, leaving.getAxis())
                .setValue(BoneRibBlock.THICK, wide);
        List<Placement> ribs = new ArrayList<>();
        for (int column = 0; column < roots.size(); column++) {
            BlockPos top = topOf(passage, roots.get(column));
            for (BlockPos cell : passage) {
                if (columnOf(roots, cell) != column
                        || light.equals(Optional.of(cell))
                        || !level.getBlockState(cell).isAir()) continue;
                List<Direction> sides = new ArrayList<>(List.of(leaving.getCounterClockWise(), leaving.getClockWise()));
                if (cell.equals(top)) sides.add(Direction.UP);
                BlockState faced = rib;
                for (Direction side : sides)
                    if (holds(level, cell, side, cleared))
                        faced = faced.setValue(MultifaceBlock.getFaceProperty(side), true);
                if (faced != rib) ribs.add(new Placement(cell, faced));
            }
        }
        return ribs;
    }

    /**
     * The cut scar of a scarred step: on the lowest passage cell of its outermost column on the scar's side, against that
     * wall, when the cell is dry and empty and holds no rib or light, and the wall holds it and is not ore.
     */
    private static Optional<Placement> scar(
            ServerLevel level,
            SurveyedTissueBlockEntity marker,
            PlanStep step,
            List<BlockPos> cleared,
            List<Placement> ribs,
            Optional<BlockPos> light,
            Direction leaving) {
        Direction side = MiningDirections.turn(leaving, step.scarSide());
        BlockPos root = marker.at(step.root());
        BlockPos outer = root.relative(side);
        BlockPos column = step.roots().stream().map(marker::at).anyMatch(outer::equals) ? outer : root;
        BlockPos cell = column.above(step.tread().isPresent() ? 2 : 1);
        boolean taken = light.equals(Optional.of(cell))
                || ribs.stream().anyMatch(rib -> rib.pos().equals(cell));
        if (taken || !level.getBlockState(cell).isAir() || !holds(level, cell, side, cleared)) return Optional.empty();
        return Optional.of(new Placement(
                cell,
                MiningModule.CUT_SCAR.get().defaultBlockState().setValue(MultifaceBlock.getFaceProperty(side), true)));
    }

    /** True when the block on {@code side} of {@code cell} lies outside the step, holds a growth and is not ore. */
    private static boolean holds(ServerLevel level, BlockPos cell, Direction side, List<BlockPos> cleared) {
        BlockPos wall = cell.relative(side);
        BlockState state = level.getBlockState(wall);
        return !cleared.contains(wall)
                && !state.is(Tags.Blocks.ORES)
                && MultifaceBlock.canAttachTo(level, side, wall, state);
    }

    /** The index among the step's root cells of the column {@code cell} stands over. */
    private static int columnOf(List<BlockPos> roots, BlockPos cell) {
        for (int n = 0; n < roots.size(); n++) {
            BlockPos root = roots.get(n);
            if (root.getX() == cell.getX() && root.getZ() == cell.getZ()) return n;
        }
        return -1;
    }

    /** The highest passage cell over {@code column}. */
    private static BlockPos topOf(List<BlockPos> passage, BlockPos column) {
        BlockPos top = column;
        for (BlockPos cell : passage) {
            if (cell.getX() == top.getX() && cell.getZ() == top.getZ() && cell.getY() > top.getY()) top = cell;
        }
        return top;
    }
}
