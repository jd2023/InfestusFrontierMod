package org.jd.infestusfrontier.item.custom;

import net.minecraft.world.item.Item;
import org.jd.infestusfrontier.item.ModCreativeModeTab;


public class MeatSword extends Item {
    public static final String ID = "meat_sword";
    public MeatSword(){
        super(new Properties().tab(ModCreativeModeTab.INFESTUS_TAB));
    }

}
