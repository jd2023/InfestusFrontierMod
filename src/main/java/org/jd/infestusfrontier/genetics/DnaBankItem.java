package org.jd.infestusfrontier.genetics;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.jd.infestusfrontier.core.genetics.Genome;

/** A DNA Bank as an item. A bank broken with its research carries it, and the tooltip lists its genomes. */
public final class DnaBankItem extends BlockItem {
    DnaBankItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        DnaBankContents carried = stack.get(GeneticsModule.DNA_BANK_CONTENTS.get());
        if (carried == null) return;
        for (Genome genome : carried.research().genomes())
            tooltip.add(Species.progress(Species.name(genome.species()), genome.points())
                    .withStyle(ChatFormatting.GRAY));
    }
}
