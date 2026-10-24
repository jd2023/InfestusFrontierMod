package org.jd.infestusfrontier.equipment;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import org.jd.infestusfrontier.logistics.GraftItem;

/**
 * The Fuel Papilla as a graft (BLOCK_CATALOG T1-36): used on an exposed cell of mature Living Substrate, it turns
 * the cell into a papilla, flush with the ground. A broken papilla dies into Dead Tissue with its buffer.
 */
final class FuelPapillaItem extends Item {
    private static final int GROWTH_PARTICLES = 15;

    FuelPapillaItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos target = context.getClickedPos();
        if (!GraftItem.isExposedMature(level, target)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        level.setBlock(target, EquipmentModule.FUEL_PAPILLA.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(target) instanceof FuelPapillaBlockEntity papilla)
            papilla.applyComponentsFromItemStack(context.getItemInHand());
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target, GROWTH_PARTICLES);
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild)
            context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
