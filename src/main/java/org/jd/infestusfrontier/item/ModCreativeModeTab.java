package org.jd.infestusfrontier.item;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.ZgBlocks;
import org.jd.infestusfrontier.ZgItems;

public class ModCreativeModeTab {
    public static final CreativeModeTab INFESTUS_TAB = new CreativeModeTab("infestus_frontier_tab") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ZgBlocks.CORRUPTION_CORE.get());
        }
    };
}
