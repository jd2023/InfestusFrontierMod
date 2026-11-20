package org.jd.infestusfrontier.organ.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Shared vanilla support lifecycle for mining bodies and their physical steam driver. */
public abstract class SupportedOrganBlock extends BaseEntityBlock {
    protected SupportedOrganBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction side,
            BlockState neighbour,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighbourPos) {
        if (side == Direction.DOWN && level.hasChunkAt(pos.below()) && !state.canSurvive(level, pos))
            level.scheduleTick(pos, this, 1);
        return super.updateShape(state, side, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.hasChunkAt(pos.below()) && !state.canSurvive(level, pos)) level.destroyBlock(pos, false);
    }
}
