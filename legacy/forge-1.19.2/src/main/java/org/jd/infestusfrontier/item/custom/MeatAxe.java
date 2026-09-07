package org.jd.infestusfrontier.item.custom;

import net.minecraft.world.item.Item;
import org.jd.infestusfrontier.item.ModCreativeModeTab;


public class MeatAxe extends Item {
    public static final String ID = "meat_axe";
    public MeatAxe(){
        super(new Properties().tab(ModCreativeModeTab.INFESTUS_TAB));
    }

}
