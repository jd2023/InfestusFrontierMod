package org.jd.infestusfrontier.equipment;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jd.infestusfrontier.equipment.api.Reserves;

/** An armor piece that lives: it keeps a biomass reserve on the item and shows it in its tooltip. */
final class AwakenedPieceItem extends ArmorItem {
    AwakenedPieceItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Reserves.of(stack)
                .ifPresent(reserve -> tooltip.add(Component.translatable(
                        "item.infestusfrontier.awakened.reserve",
                        reserve.stored().units(),
                        reserve.capacity().units())));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
