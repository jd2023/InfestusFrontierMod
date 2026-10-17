package org.jd.infestusfrontier.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;

/** Converts the one eligible, exposed ground block the player points at into basic Living Substrate. */
final class SporeCultureItem extends Item {
    private static final int GROWTH_PARTICLES = 15;

    SporeCultureItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos target = context.getClickedPos();
        if (!level.getBlockState(target).is(LivingSubstrateBlock.HOSTS)) return InteractionResult.PASS;
        BlockPos facing = target.relative(context.getClickedFace());
        if (!level.getBlockState(facing).getCollisionShape(level, facing).isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        level.setBlock(target, EcologyModule.LIVING_SUBSTRATE.get().cellAt(target), Block.UPDATE_ALL);
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target, GROWTH_PARTICLES);
        Player player = context.getPlayer();
        if (player == null || !player.getAbilities().instabuild)
            context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
