package org.jd.infestusfrontier.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BigDripleafBlock;
import net.minecraft.world.level.block.BigDripleafStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What stands on a host decides whether the host can become a cell, and what becomes of it when it does
 * (VEGETATION_INFECTION.md). Spore Culture and the bone-meal wave both convert ground through {@link #convert}, so
 * a flower is accounted for before the ground under it changes and vanilla's support update finds a dead flower that
 * stands on substrate, never a live one to uproot and drop. Limits: ENGINEERING.md, "Vegetation infection".
 */
final class Vegetation {
    private Vegetation() {}

    /** What the block over a host means for converting the host. */
    sealed interface Cover permits Clear, Withers, Rooted {}

    /** Nothing that depends on the ground: air, a withered flower, a carpet, a torch. The host may convert. */
    record Clear() implements Cover {}

    /** A supported flower. The host may convert once the flower is replaced by {@code dead}. */
    record Withers(BlockState dead) implements Cover {}

    /**
     * A plant rooted in the ground with no mapping: a sapling, grass, a crop, a bush, a mushroom, sugar cane, bamboo,
     * a big dripleaf. Converting the host would uproot it, so the host stays as it is.
     */
    record Rooted() implements Cover {}

    /** Judges the block at {@code above}, the position over a host. One block read; the chunk must be loaded. */
    static Cover cover(LevelReader level, BlockPos above) {
        BlockState state = level.getBlockState(above);
        if (state.is(EcologyModule.WITHERED_FLOWER.get())) return new Clear();
        return WitheredFlowerBlock.wither(state)
                .<Cover>map(Withers::new)
                .orElseGet(() -> isRooted(state) ? new Rooted() : new Clear());
    }

    /** A host block, nothing solid on top of it, and nothing rooted in it that infection cannot account for. */
    static boolean isExposedHost(LevelReader level, BlockPos host) {
        BlockPos above = host.above();
        return level.getBlockState(host).is(LivingSubstrateBlock.HOSTS)
                && level.getBlockState(above).getCollisionShape(level, above).isEmpty()
                && !(cover(level, above) instanceof Rooted);
    }

    /**
     * Makes {@code host} the cell {@code cell}, withering a supported flower on it first. The cover is judged again
     * here, so a rooted plant without a mapping refuses the conversion and nothing changes: callers that judged the
     * cover earlier in the same tick may ignore the result.
     */
    static boolean convert(Level level, BlockPos host, BlockState cell) {
        BlockPos above = host.above();
        switch (cover(level, above)) {
            case Rooted r -> {
                return false;
            }
            case Withers w -> level.setBlock(above, w.dead(), Block.UPDATE_ALL);
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
