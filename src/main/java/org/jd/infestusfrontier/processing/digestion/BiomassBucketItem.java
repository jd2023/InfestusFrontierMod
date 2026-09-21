package org.jd.infestusfrontier.processing.digestion;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A sealed transaction item until containment introduces finite world placement. */
final class BiomassBucketItem extends Item {
    BiomassBucketItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.infestusfrontier.biomass_bucket.volume"));
        lines.add(Component.translatable("tooltip.infestusfrontier.biomass_bucket.placement"));
    }
}
