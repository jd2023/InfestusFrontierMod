package org.jd.infestusfrontier.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.organ.api.Organs;

/**
 * A mining organ: it stands on the block below it and dies with it. When that block is gone, broken, burnt, washed or
 * fallen away, the organ breaks on its next scheduled tick and drops nothing, neither itself nor what it held: losing
 * support is destruction, not a pause. Only a change below it is looked at, through vanilla's shape update, and the
 * block below an organ is always in the organ's own chunk, so an organ is never judged by ground that is not loaded.
 * Whether it may work, for one standing on something other than a foundation, is each organ's own rule.
 */
abstract class MiningOrganBlock extends BaseEntityBlock {
    MiningOrganBlock(Properties properties) {
        super(properties);
    }

    /**
     * True while the block below {@code pos} holds an organ up: a foundation, a root cell or a Leaching Pod's root, or any
     * block whose top is solid. Air, fluid, plants and other open blocks hold nothing.
     */
    static boolean supported(LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        return ground.is(Organs.FOUNDATIONS)
                || ground.is(MiningModule.ROOT_CELL.get())
                || ground.is(MiningModule.LEACHING_ROOT.get())
                || ground.isFaceSturdy(level, below, Direction.UP);
    }

    /** The organ at {@code pos} dies: it breaks with its break effect and drops nothing. */
    static void die(Level level, BlockPos pos) {
        level.destroyBlock(pos, false);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return supported(level, pos);
    }

    /** The block below changed: the organ looks at its support on its next tick. */
    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighbour,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighbourPos) {
        if (direction == Direction.DOWN && !supported(level, pos)) level.scheduleTick(pos, this, 1);
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!supported(level, pos)) die(level, pos);
    }
}
