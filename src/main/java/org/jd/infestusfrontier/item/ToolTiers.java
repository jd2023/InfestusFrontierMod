package org.jd.infestusfrontier.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import org.jd.infestusfrontier.ZgItems;

public class ToolTiers {
    public static final ForgeTier TIER1TOOLS = new ForgeTier(2, 100, 2.5f, 1.5f,
            5, BlockTags.NEEDS_IRON_TOOL, () -> Ingredient.of(ZgItems.CORRUPT_CHUNK.get()));
}
