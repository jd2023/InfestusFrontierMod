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
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.foundation.Refusals;

/**
 * The Septum Crossing as a graft: used on a vein cell, it turns the cell into a crossing of the vein's carrier whose
 * clicked face shows both lumens. A Fluid Vein must be sealed first.
 */
final class SeptumCrossingItem extends Item {
    static final Refusal UNSEALED = new Refusal("logistics.unsealed");
    private static final int GROWTH_PARTICLES = 10;

    SeptumCrossingItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos target = context.getClickedPos();
        BlockState state = level.getBlockState(target);
        if (!(state.getBlock() instanceof VeinBlock vein)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        if (!state.getValue(VeinBlock.SEALED)) return refuse(player, UNSEALED);
        BlockState crossing = LogisticsModule.SEPTUM_CROSSING
                .get()
                .defaultBlockState()
                .setValue(Routes.CARRIER, vein.carrier())
                .setValue(SeptumCrossingBlock.AXIS, context.getClickedFace().getAxis());
        level.setBlock(target, crossing, Block.UPDATE_ALL);
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target, GROWTH_PARTICLES);
        if (player == null || !player.getAbilities().instabuild)
            context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    private static InteractionResult refuse(Player player, Refusal refusal) {
        if (player != null) Refusals.show(player, refusal);
        return InteractionResult.FAIL;
    }
}
