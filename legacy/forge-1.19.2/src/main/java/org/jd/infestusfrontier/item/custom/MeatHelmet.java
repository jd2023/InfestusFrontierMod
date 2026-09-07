package org.jd.infestusfrontier.item.custom;

import net.minecraft.world.item.Item;
import org.jd.infestusfrontier.item.ModCreativeModeTab;

public class MeatHelmet extends Item{
    public static final String ID = "meat_helmet";
    public MeatHelmet(){
        super(new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB));
    }

}
