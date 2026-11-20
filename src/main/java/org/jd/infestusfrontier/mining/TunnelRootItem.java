package org.jd.infestusfrontier.mining;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.jd.infestusfrontier.core.mining.RootMutation;
import org.jd.infestusfrontier.integration.geckolib.OrganItem;

/**
 * A Tunnel Root head as an item, drawn with the head's own model at rest. A head broken with mutations carries them,
 * and the tooltip names them.
 */
public final class TunnelRootItem extends OrganItem {
    TunnelRootItem(Block block, Properties properties) {
        super(block, properties, "idle");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        TunnelRootContents carried = stack.get(MiningModule.TUNNEL_ROOT_CONTENTS.get());
        if (carried == null) return;
        for (RootMutation mutation : carried.mutations().installed())
            tooltip.add(Component.translatable("gui.infestusfrontier.tunnel_root.mutation." + mutation.id())
                    .withStyle(ChatFormatting.GRAY));
    }
}
