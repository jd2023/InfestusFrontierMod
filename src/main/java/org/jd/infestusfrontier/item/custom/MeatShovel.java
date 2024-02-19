package org.jd.infestusfrontier.item.custom;

import net.minecraft.world.item.Item;
import org.jd.infestusfrontier.item.ModCreativeModeTab;


public class MeatShovel extends Item {
    public static final String ID = "meat_shovel";
    public MeatShovel(){
        super(new Properties().tab(ModCreativeModeTab.INFESTUS_TAB));
    }

}
