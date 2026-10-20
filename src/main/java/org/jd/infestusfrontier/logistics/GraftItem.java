package org.jd.infestusfrontier.logistics;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.ecology.LivingSubstrateBlock;
import org.jd.infestusfrontier.ecology.SubstrateStage;

/** A treatment that turns the one exposed mature Living Substrate cell it is used on into the block it grows. */
public final class GraftItem extends Item {
    private static final int GROWTH_PARTICLES = 15;
    private final Supplier<Block> grows;

    public GraftItem(Properties properties, Supplier<Block> grows) {
        super(properties);
        this.grows = grows;
    }

    /** True for a mature cell with nothing solid on top of it. */
    public static boolean isExposedMature(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockPos above = pos.above();
        return state.is(EcologyModule.LIVING_SUBSTRATE.get())
                && state.getValue(LivingSubstrateBlock.STAGE) == SubstrateStage.MATURE
                && level.hasChunkAt(above)
                && level.getBlockState(above).getCollisionShape(level, above).isEmpty();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos target = context.getClickedPos();
        if (!isExposedMature(level, target)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockState grown = Block.updateFromNeighbourShapes(grows.get().defaultBlockState(), level, target);
        level.setBlock(target, grown, Block.UPDATE_ALL);
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target, GROWTH_PARTICLES);
        Player player = context.getPlayer();
        if (player == null || !player.getAbilities().instabuild)
            context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
