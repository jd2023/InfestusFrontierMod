package org.jd.infestusfrontier.equipment;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A Living Hatchet (ITEM_CATALOG I016): a vanilla axe of the living tier that carries a biomass reserve, mends from it while carried, and breaks the blocks round the one it mines once grafted Contractile. */
final class LivingHatchetItem extends AxeItem {
    LivingHatchetItem(Properties properties) {
        super(LivingTiers.LIVING, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        LivingTools.describe(stack, tooltip);
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        LivingTools.mend(stack, level, entity);
    }

    /** The vanilla wear, then the Contractile spread. */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean mined = super.mineBlock(stack, level, state, pos, miner);
        LivingTools.spread(stack, level, pos, state, miner);
        return mined;
    }
}
