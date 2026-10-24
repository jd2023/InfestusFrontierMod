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
 * ignores it. On a doorway it hardens the clicked half, as an axe scrapes a copper door, and the vanilla door carries
 * the change to its other half.
 */
final class WallGraftItem extends Item {
    private static final int PARTICLES = 10;

    private final Map<? extends Supplier<? extends Block>, ? extends Supplier<? extends Block>> steps;

    WallGraftItem(
            Properties properties,
            Map<? extends Supplier<? extends Block>, ? extends Supplier<? extends Block>> steps) {
        super(properties);
        this.steps = steps;
    }

    /** True when this graft has a step for {@code state}'s block. */
    boolean hardens(BlockState state) {
        return next(state) != null;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos target = context.getClickedPos();
        BlockState state = level.getBlockState(target);
        Block next = next(state);
        if (next == null) return InteractionResult.PASS;
        boolean door = state.getBlock() instanceof DoorBlock;
        if (door && !level.getBlockState(other(target, state)).is(state.getBlock())) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockState hardened = door ? harden(state, next) : next.defaultBlockState();
        level.setBlock(target, hardened, door ? Block.UPDATE_ALL_IMMEDIATE : Block.UPDATE_ALL);
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

    private static BlockPos other(BlockPos half, BlockState state) {
        return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? half.above() : half.below();
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
