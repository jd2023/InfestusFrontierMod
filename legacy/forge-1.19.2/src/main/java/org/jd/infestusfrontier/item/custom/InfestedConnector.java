package org.jd.infestusfrontier.item.custom;

import net.minecraft.world.item.Item;
import org.jd.infestusfrontier.item.ModCreativeModeTab;

public class InfestedConnector extends Item {
    public static final String ID = "infested_connector";

    public InfestedConnector() {
        super(new Properties().tab(ModCreativeModeTab.INFESTUS_TAB));
    }
}
