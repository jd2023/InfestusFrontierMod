package org.jd.infestusfrontier.mineral;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.core.mineral.Parcel;

/** Mineral Tailings: named for the metal in its {@code parcel}, with the residual units it still holds on its tooltip. */
public final class TailingsItem extends Item {
    TailingsItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Parcel parcel = stack.get(MineralModule.PARCEL.get());
        if (parcel == null) return super.getName(stack);
        return Component.translatable("item." + InfestusFrontier.MOD_ID + ".mineral_tailings."
                + parcel.metal().id());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Parcel parcel = stack.get(MineralModule.PARCEL.get());
        if (parcel == null) {
            tooltip.add(Component.translatable("item.infestusfrontier.mineral_tailings.unparcelled"));
            return;
        }
        tooltip.add(Component.translatable("item.infestusfrontier.mineral_tailings.units", parcel.units()));
        tooltip.add(Component.translatable("item.infestusfrontier.mineral_tailings.inert"));
    }
}
