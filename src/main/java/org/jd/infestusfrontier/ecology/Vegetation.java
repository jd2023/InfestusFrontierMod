package org.jd.infestusfrontier.ecology;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BigDripleafBlock;
import net.minecraft.world.level.block.BigDripleafStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * What stands on a host decides whether the host can become a cell, and what becomes of it when it does
 * (VEGETATION_INFECTION.md). Spore Culture and the bone-meal wave both convert ground through {@link #convert}, so
 * a plant is accounted for before the ground under it changes and vanilla's support update finds a dead plant that
 * stands on substrate, never a live one to uproot and drop. Limits: ENGINEERING.md, "Vegetation infection".
 */
final class Vegetation {
    private Vegetation() {}

    /** What the block over a host means for converting the host. */
    sealed interface Cover permits Clear, Withers, WithersTall, Rooted {}

    /**
     * Nothing that depends on the ground: air, a withered flower or tall plant, a carpet, a torch. The host may
     * convert.
     */
    record Clear() implements Cover {}

    /**
     * A supported flower, shrub, undergrowth or sapling. The host may convert once the plant is replaced by
     * {@code dead}.
     */
    record Withers(BlockState dead) implements Cover {}

    /**
     * A complete two-block plant: both halves are the same supported plant. The host may convert once the halves are
     * replaced by {@code lower} and {@code upper}.
     */
    record WithersTall(BlockState lower, BlockState upper) implements Cover {}

    /**
     * A plant rooted in the ground with no mapping, or a tall plant that is not whole: a crop, a torchflower, an
     * azalea, a mushroom, sugar cane, bamboo, a big dripleaf, a tall plant whose upper half is missing, another plant
     * or out of reach. Converting the host would uproot it, so the host stays as it is.
     */
    record Rooted() implements Cover {}

    /**
     * Judges the block at {@code above}, the position over a host. One block read, two for a supported tall plant;
     * the chunk must be loaded, and the one over {@code above} is checked here.
     */
    static Cover cover(LevelReader level, BlockPos above) {
        BlockState state = level.getBlockState(above);
        if (state.is(EcologyModule.WITHERED_FLOWER.get())
                || DeadTallPlantBlock.isDead(state)
                || DeadShrubBlock.isDead(state)) return new Clear();
        Optional<BlockState> small = WitheredFlowerBlock.wither(state).or(() -> DeadShrubBlock.wither(state));
        if (small.isPresent()) return new Withers(small.get());
        Optional<BlockState> tall = DeadTallPlantBlock.wither(state);
        if (tall.isPresent()) return wholeTall(level, above, state, tall.get());
        return isRooted(state) ? new Rooted() : new Clear();
    }

    /** A tall plant is whole when this is its lower half and the loaded block over it is its own upper half. */
    private static Cover wholeTall(LevelReader level, BlockPos lowerPos, BlockState plant, BlockState dead) {
        BlockPos upperPos = lowerPos.above();
        if (plant.getValue(DoublePlantBlock.HALF) != DoubleBlockHalf.LOWER || !level.hasChunkAt(upperPos))
            return new Rooted();
        BlockState upper = level.getBlockState(upperPos);
        if (!upper.is(plant.getBlock()) || upper.getValue(DoublePlantBlock.HALF) != DoubleBlockHalf.UPPER)
            return new Rooted();
        return new WithersTall(dead, dead.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** A host block, nothing solid on top of it, and nothing rooted in it that infection cannot account for. */
    static boolean isExposedHost(LevelReader level, BlockPos host) {
        BlockPos above = host.above();
        return level.getBlockState(host).is(LivingSubstrateBlock.HOSTS)
                && level.getBlockState(above).getCollisionShape(level, above).isEmpty()
                && !(cover(level, above) instanceof Rooted);
    }

    /**
     * Makes {@code host} the cell {@code cell}, withering a supported flower or tall plant on it first. The cover is
     * judged again here, so a rooted plant without a mapping, or a tall plant that is no longer whole, refuses the
     * conversion and nothing changes: callers that judged the cover earlier may ignore the result.
     *
     * <p>A tall plant's halves are replaced together without shape updates: either half alone would tell the other
     * that its partner is gone and drop it. Once both stand, the ground change updates the lower half normally and
     * finds it supported.
     */
    static boolean convert(Level level, BlockPos host, BlockState cell) {
        BlockPos above = host.above();
        switch (cover(level, above)) {
            case Rooted r -> {
                return false;
            }
            case Withers w -> level.setBlock(above, w.dead(), Block.UPDATE_ALL);
            case WithersTall w -> {
                level.setBlock(above, w.lower(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                level.setBlock(above.above(), w.upper(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
            case Clear c -> {}
        }
        level.setBlock(host, cell, Block.UPDATE_ALL);
        return true;
    }

    /**
     * Vanilla's rooted plants: every bush-family block, sugar cane, bamboo (shoot and stalk) and the big dripleaf
     * (leaf and stem). A plain list of classes, so a plant's own survival rule is never simulated.
     */
    private static boolean isRooted(BlockState state) {
        Block block = state.getBlock();
        return block instanceof BushBlock
                || block instanceof SugarCaneBlock
                || block instanceof BambooSaplingBlock
                || block instanceof BambooStalkBlock
                || block instanceof BigDripleafBlock
                || block instanceof BigDripleafStemBlock;
    }
}
