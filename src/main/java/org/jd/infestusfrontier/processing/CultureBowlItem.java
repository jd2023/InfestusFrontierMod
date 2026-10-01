package org.jd.infestusfrontier.processing;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

final class CultureBowlItem extends BlockItem {
    CultureBowlItem(Block block, Properties properties) { super(block, properties); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.infestusfrontier.bowl.load"));
        lines.add(Component.translatable("tooltip.infestusfrontier.bowl.start"));
        lines.add(Component.translatable("tooltip.infestusfrontier.bowl.collect"));
        lines.add(Component.translatable("tooltip.infestusfrontier.bowl.growth"));
    }
}
