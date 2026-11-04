package org.jd.infestusfrontier.ecology;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.core.ecology.TreeDiscovery;

/**
 * How ground conversion reaches a tree, and what one block of an admitted tree becomes (VEGETATION_INFECTION.md,
 * "Trees"). A wave cell or a cultured cell touches a tree through {@link #touch}: beside the cell, level with it or
 * one step up or down, stands a root, an upright log of a supported species on host ground or a cell. Only such a
 * root starts a job; a lying log, a log on sand or planks, a stripped log, wood, planks and an Infected Log never do,
 * and no other covered ground becomes a host. Twelve reads and up to twelve more below them, all in loaded chunks.
 */
final class TreeInfection {
    /** The steps up and down from the cell at which a trunk base counts as touched. */
    private static final int[] CONTACT_STEPS = {1, 0, -1};

    private TreeInfection() {}

    /** An upright supported log standing on host ground or a cell: where a tree's infection starts. */
    record Root(BlockPos pos, TreeSource species) {}

    /** Asks the dimension's tree work for every root beside {@code cell}. */
    static void touch(ServerLevel level, BlockPos cell) {
        for (Direction side : Direction.Plane.HORIZONTAL)
            for (int dy : CONTACT_STEPS)
                rootAt(level, cell.relative(side).above(dy))
                        .ifPresent(root -> TreeInfections.get(level).request(level, root));
    }

    /** The root at {@code pos}, if a loaded upright supported log stands there on host ground or a cell. */
    static Optional<Root> rootAt(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return Optional.empty();
        BlockState state = level.getBlockState(pos);
        Optional<TreeSource> species = TreeSource.ofLog(state.getBlock());
        if (species.isEmpty() || state.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Y) return Optional.empty();
        BlockState ground = level.getBlockState(pos.below());
        if (!ground.is(LivingSubstrateBlock.HOSTS) && !ground.is(EcologyModule.LIVING_SUBSTRATE.get()))
            return Optional.empty();
        return Optional.of(new Root(pos.immutable(), species.get()));
    }

    /**
     * The world as a discovery of {@code species} reads it: the species' own log and leaves, any other supported log as
     * a foreign one, everything else (stripped logs, wood, planks, other leaves, infected timber, air, ground) as
     * nothing. An unloaded chunk is reported, never loaded.
     */
    static TreeDiscovery.Terrain terrain(ServerLevel level, TreeSource species) {
        return position -> {
            BlockPos pos = new BlockPos(position.x(), position.y(), position.z());
            if (!level.hasChunkAt(pos)) return new TreeDiscovery.Unloaded();
            BlockState state = level.getBlockState(pos);
            if (state.is(species.log())) return new TreeDiscovery.Log();
            if (TreeSource.ofLog(state.getBlock()).isPresent()) return new TreeDiscovery.ForeignLog();
            if (state.is(species.leaves())) return new TreeDiscovery.Leaf(state.getValue(LeavesBlock.DISTANCE));
            return new TreeDiscovery.Nothing();
        };
    }

    /**
     * What a block of an admitted tree of {@code species} becomes, judged from the state as it is now: a log keeps its
     * axis, leaves keep distance, persistence and water. Empty when the block is no longer the living log or leaf
     * discovery saw, so a player's edit is left as it is.
     */
    static Optional<BlockState> infected(BlockState state, TreeSource species) {
        if (state.is(species.log()))
            return Optional.of(EcologyModule.INFECTED_LOG
                    .get()
                    .defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS))
                    .setValue(InfectedLogBlock.SOURCE, species));
        if (state.is(species.leaves()))
            return Optional.of(EcologyModule.BLIGHTED_LEAVES
                    .get()
                    .defaultBlockState()
                    .setValue(LeavesBlock.DISTANCE, state.getValue(LeavesBlock.DISTANCE))
                    .setValue(LeavesBlock.PERSISTENT, state.getValue(LeavesBlock.PERSISTENT))
                    .setValue(LeavesBlock.WATERLOGGED, state.getValue(LeavesBlock.WATERLOGGED))
                    .setValue(BlightedLeavesBlock.SOURCE, species));
        return Optional.empty();
    }

    static TreeDiscovery.Position position(BlockPos pos) {
        return new TreeDiscovery.Position(pos.getX(), pos.getY(), pos.getZ());
    }

    static BlockPos pos(TreeDiscovery.Position position) {
        return new BlockPos(position.x(), position.y(), position.z());
    }
}
