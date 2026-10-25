package org.jd.infestusfrontier.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How a batch organ shows its work beyond its model, the vanilla way: the light its state gives
 * ({@code Properties.lightLevel}) and the particles and sounds of its client block ticks ({@code animateTick}).
 * Places are in the organ's model, which faces north.
 */
interface OrganEffects {
    /** An organ that shows nothing more than its model. */
    OrganEffects NONE = new OrganEffects() {};

    default int light(BlockState state) {
        return 0;
    }

    default void animate(BlockState state, Level level, BlockPos pos, RandomSource random) {}
}
