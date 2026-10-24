package org.jd.infestusfrontier.ecology;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;

/** A treatment that hardens one living wall of the tier below into the next tier; anything else ignores it. */
final class WallGraftItem extends Item {
    private static final int PARTICLES = 10;
    private final Supplier<? extends Block> from;
    private final Supplier<? extends Block> to;

    WallGraftItem(Properties properties, Supplier<? extends Block> from, Supplier<? extends Block> to) {
        super(properties);
        this.from = from;
        this.to = to;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos target = context.getClickedPos();
        if (!level.getBlockState(target).is(from.get())) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        level.setBlock(target, to.get().defaultBlockState(), Block.UPDATE_ALL);
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target, PARTICLES);
        Player player = context.getPlayer();
        if (player == null || !player.getAbilities().instabuild)
            context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
