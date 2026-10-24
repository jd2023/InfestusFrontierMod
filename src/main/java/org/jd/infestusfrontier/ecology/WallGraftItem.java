package org.jd.infestusfrontier.ecology;

import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * A treatment that hardens one living wall, or one doorway, of the tier below into the next tier; anything else
 * ignores it. A doorway hardens whole: both halves change together and keep their facing, hinge and power.
 */
final class WallGraftItem extends Item {
    private static final int PARTICLES = 10;
    /** Both halves are set before either sees the other, so neither breaks itself off as a lone half. */
    private static final int DOOR_FLAGS = Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE;

    private final Map<? extends Supplier<? extends Block>, ? extends Supplier<? extends Block>> steps;

    WallGraftItem(
            Properties properties,
            Map<? extends Supplier<? extends Block>, ? extends Supplier<? extends Block>> steps) {
        super(properties);
        this.steps = steps;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos target = context.getClickedPos();
        BlockState state = level.getBlockState(target);
        Block next = next(state);
        if (next == null) return InteractionResult.PASS;
        if (state.getBlock() instanceof DoorBlock) {
            BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? target : target.below();
            BlockState bottom = level.getBlockState(lower);
            BlockState top = level.getBlockState(lower.above());
            if (!bottom.is(state.getBlock()) || !top.is(state.getBlock())) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            level.setBlock(lower, harden(bottom, next), DOOR_FLAGS);
            level.setBlock(lower.above(), harden(top, next), DOOR_FLAGS);
            level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, lower.above(), PARTICLES);
        } else {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            level.setBlock(target, next.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target, PARTICLES);
        Player player = context.getPlayer();
        if (player == null || !player.getAbilities().instabuild)
            context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    private Block next(BlockState state) {
        for (var step : steps.entrySet())
            if (state.is(step.getKey().get())) return step.getValue().get();
        return null;
    }

    /**
     * The same half of the harder door. A door opened by hand closes as it hardens unless a signal holds it open, since
     * only redstone moves the harder one.
     */
    private static BlockState harden(BlockState half, Block next) {
        return next.defaultBlockState()
                .setValue(DoorBlock.FACING, half.getValue(DoorBlock.FACING))
                .setValue(DoorBlock.HALF, half.getValue(DoorBlock.HALF))
                .setValue(DoorBlock.HINGE, half.getValue(DoorBlock.HINGE))
                .setValue(DoorBlock.POWERED, half.getValue(DoorBlock.POWERED))
                .setValue(DoorBlock.OPEN, half.getValue(DoorBlock.POWERED));
    }
}
