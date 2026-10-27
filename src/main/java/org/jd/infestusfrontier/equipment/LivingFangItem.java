package org.jd.infestusfrontier.equipment;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A Living Fang (ITEM_CATALOG I018): a vanilla sword of the living tier that carries a biomass reserve, mends from it
 * while carried, and grips what it hits once grafted Grasping.
 */
final class LivingFangItem extends SwordItem {
    LivingFangItem(Properties properties) {
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

    /** The vanilla wear, then the grip. */
    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        LivingTools.grip(stack, target);
    }
}
