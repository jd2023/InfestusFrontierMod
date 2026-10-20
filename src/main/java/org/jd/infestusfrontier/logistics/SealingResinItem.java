package org.jd.infestusfrontier.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

/** Seals an unsealed Fluid Vein, which carries nothing until then. */
final class SealingResinItem extends Item {
    private static final int SEAL_PARTICLES = 10;

    SealingResinItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos target = context.getClickedPos();
        BlockState state = level.getBlockState(target);
        if (!state.is(LogisticsModule.FLUID_VEIN.get()) || state.getValue(VeinBlock.SEALED))
            return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        level.setBlock(target, state.setValue(VeinBlock.SEALED, true), Block.UPDATE_ALL);
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target, SEAL_PARTICLES);
        Player player = context.getPlayer();
        if (player == null || !player.getAbilities().instabuild)
            context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
