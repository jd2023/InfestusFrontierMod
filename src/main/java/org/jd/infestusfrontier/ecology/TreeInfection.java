package org.jd.infestusfrontier.ecology;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.Tags;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.core.ecology.TreeDiscovery;

/**
 * How ground conversion reaches a tree, which blocks a tree is made of, and what one block of an admitted tree becomes
 * (VEGETATION_INFECTION.md, "Trees"). A wave cell or a cultured cell touches a tree through {@link #touch}: beside the
 * cell, level with it or one step up or down, stands a root, an upright trunk log on host ground or a cell. Only such
 * a root starts a job; a lying log, a log on sand or planks, a stripped log, wood, planks and an Infected Log never do,
 * and no other covered ground becomes a host. Twelve reads and up to twelve more below them, all in loaded chunks.
 *
 * <p>A trunk log is a block in {@link #TRUNKS} and not in {@link #NOT_TRUNKS} or the common stripped tags, with an
 * {@code axis}, a full collision cube and no block entity. Foliage is a {@link LeavesBlock} in {@link #FOLIAGE} and not
 * in {@link #NOT_FOLIAGE}, with vanilla's distance, persistence and water and no block entity. No species list: a
 * block's kind is its registry id.
 */
final class TreeInfection {
    /**
     * Trunk logs. By default vanilla's {@code overworld_natural_logs}, the natural logs without wood, stripped logs or
     * stems, which mods that add trees fill too. A pack adds a trunk the tag misses here.
     */
    static final TagKey<Block> TRUNKS = tag("infectable_logs");
    /** Logs that are never a tree's, though tagged as trunks: the mangrove's root system, the Infected Log. */
    static final TagKey<Block> NOT_TRUNKS = tag("uninfectable_logs");
    /** Foliage. By default vanilla's {@code leaves}, which mods that add trees fill too. */
    static final TagKey<Block> FOLIAGE = tag("infectable_leaves");
    /** Leaves that are never a tree's, though tagged as foliage: the mangrove's, Blighted Leaves. */
    static final TagKey<Block> NOT_FOLIAGE = tag("uninfectable_leaves");

    /** The steps up and down from the cell at which a trunk base counts as touched. */
    private static final int[] CONTACT_STEPS = {1, 0, -1};

    private TreeInfection() {}

    /** An upright trunk log standing on host ground or a cell: where a tree's infection starts. */
    record Root(BlockPos pos, ResourceLocation log, BlockState state) {}

    /** Asks the dimension's tree work for every root beside {@code cell}. */
    static void touch(ServerLevel level, BlockPos cell) {
        for (Direction side : Direction.Plane.HORIZONTAL)
            for (int dy : CONTACT_STEPS)
                rootAt(level, cell.relative(side).above(dy))
                        .ifPresent(root -> TreeInfections.get(level).request(level, root));
    }

    /** The root at {@code pos}, if a loaded upright trunk log stands there on host ground or a cell. */
    static Optional<Root> rootAt(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return Optional.empty();
        BlockState state = level.getBlockState(pos);
        if (!trunk(level, pos, state) || state.getValue(BlockStateProperties.AXIS) != Direction.Axis.Y)
            return Optional.empty();
        BlockState ground = level.getBlockState(pos.below());
        if (!ground.is(LivingSubstrateBlock.HOSTS) && !ground.is(EcologyModule.LIVING_SUBSTRATE.get()))
            return Optional.empty();
        return Optional.of(new Root(pos.immutable(), kind(state), state));
    }

    /** True when {@code state} at {@code pos} is a log a tree may be made of. */
    static boolean trunk(BlockGetter level, BlockPos pos, BlockState state) {
        return state.is(TRUNKS)
                && !state.is(NOT_TRUNKS)
                && !state.is(Tags.Blocks.STRIPPED_LOGS)
                && !state.is(Tags.Blocks.STRIPPED_WOODS)
                && !state.is(EcologyModule.INFECTED_LOG.get())
                && state.hasProperty(BlockStateProperties.AXIS)
                && !state.hasBlockEntity()
                && state.isCollisionShapeFullBlock(level, pos);
    }

    /** True when {@code state} is leaves a tree may hold. */
    static boolean foliage(BlockState state) {
        return state.is(FOLIAGE)
                && !state.is(NOT_FOLIAGE)
                && !state.is(EcologyModule.BLIGHTED_LEAVES.get())
                && state.getBlock() instanceof LeavesBlock
                && state.hasProperty(LeavesBlock.DISTANCE)
                && state.hasProperty(LeavesBlock.PERSISTENT)
                && state.hasProperty(LeavesBlock.WATERLOGGED)
                && !state.hasBlockEntity();
    }

    /** The kind of a tree's block: its registry id. */
    static ResourceLocation kind(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock());
    }

    /**
     * The world as one job's discovery reads it: trunk logs and foliage with their kinds, everything else (stripped
     * logs, wood, planks, infected timber, air, ground) as nothing, and the root as nothing once {@code rootStands} no
     * longer holds. Each log and leaf read is handed to {@code seen} with its state. An unloaded chunk is reported,
     * never loaded.
     */
    static TreeDiscovery.Terrain terrain(
            ServerLevel level, BlockPos root, Predicate<BlockState> rootStands, BiConsumer<BlockPos, BlockState> seen) {
        return position -> {
            BlockPos pos = new BlockPos(position.x(), position.y(), position.z());
            if (!level.hasChunkAt(pos)) return new TreeDiscovery.Unloaded();
            BlockState state = level.getBlockState(pos);
            if (trunk(level, pos, state)) {
                if (pos.equals(root) && !rootStands.test(state)) return new TreeDiscovery.Nothing();
                seen.accept(pos, state);
                return new TreeDiscovery.Log(kind(state).toString());
            }
            if (foliage(state)) {
                seen.accept(pos, state);
                return new TreeDiscovery.Leaf(kind(state).toString(), state.getValue(LeavesBlock.DISTANCE));
            }
            return new TreeDiscovery.Nothing();
        };
    }

    /**
     * True when {@code now} is still what a target expected: the same block, and unless {@code identityOnly} the same
     * state but for leaf distance, which vanilla keeps for itself as neighbours change.
     */
    static boolean unchanged(BlockState now, BlockState expected, boolean identityOnly) {
        if (identityOnly) return now.is(expected.getBlock());
        return withoutDistance(now) == withoutDistance(expected);
    }

    private static BlockState withoutDistance(BlockState state) {
        return state.hasProperty(LeavesBlock.DISTANCE) ? state.setValue(LeavesBlock.DISTANCE, 1) : state;
    }

    /**
     * What the log at {@code pos} becomes, judged from the state as it is now: an Infected Log with its axis. Empty
     * when the block is no longer a trunk log or no longer what discovery read there, so a player's edit is left.
     */
    static Optional<BlockState> infectedLog(
            ServerLevel level, BlockPos pos, BlockState now, BlockState expected, boolean identityOnly) {
        if (!trunk(level, pos, now) || !unchanged(now, expected, identityOnly)) return Optional.empty();
        return Optional.of(EcologyModule.INFECTED_LOG
                .get()
                .defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, now.getValue(BlockStateProperties.AXIS)));
    }

    /**
     * What a leaf becomes, judged from the state as it is now: Blighted Leaves with its distance, persistence and
     * water. Empty when the block is no longer foliage or no longer what discovery read there.
     */
    static Optional<BlockState> blightedLeaves(BlockState now, BlockState expected, boolean identityOnly) {
        if (!foliage(now) || !unchanged(now, expected, identityOnly)) return Optional.empty();
        return Optional.of(EcologyModule.BLIGHTED_LEAVES
                .get()
                .defaultBlockState()
                .setValue(LeavesBlock.DISTANCE, now.getValue(LeavesBlock.DISTANCE))
                .setValue(LeavesBlock.PERSISTENT, now.getValue(LeavesBlock.PERSISTENT))
                .setValue(LeavesBlock.WATERLOGGED, now.getValue(LeavesBlock.WATERLOGGED)));
    }

    static TreeDiscovery.Position position(BlockPos pos) {
        return new TreeDiscovery.Position(pos.getX(), pos.getY(), pos.getZ());
    }

    static BlockPos pos(TreeDiscovery.Position position) {
        return new BlockPos(position.x(), position.y(), position.z());
    }

    private static TagKey<Block> tag(String name) {
        return BlockTags.create(ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, name));
    }
}
