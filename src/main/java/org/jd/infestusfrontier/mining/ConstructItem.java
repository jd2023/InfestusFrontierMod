package org.jd.infestusfrontier.mining;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import org.jd.infestusfrontier.core.mining.RootMutation;
import org.jd.infestusfrontier.core.mining.Tunnelling;
import org.jd.infestusfrontier.foundation.Refusals;

/**
 * A genetic construct for one Tunnel Root mutation, produced in a DNA Bank. Used on a head it applies the mutation,
 * see {@link TunnelRootBlockEntity#mutate}; the head's block hands it over. Used on any other block it names why
 * nothing happened and is kept.
 */
public final class ConstructItem extends Item {
    private final RootMutation mutation;

    ConstructItem(RootMutation mutation, Properties properties) {
        super(properties);
        this.mutation = mutation;
    }

    public RootMutation mutation() {
        return mutation;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide && context.getPlayer() != null)
            Refusals.show(context.getPlayer(), Tunnelling.NOT_A_HEAD);
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(
                        "item.infestusfrontier.construct." + mutation.id() + ".target", mutation.biomassUnits())
                .withStyle(ChatFormatting.GRAY));
    }
}
