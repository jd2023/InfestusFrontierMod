package org.jd.infestusfrontier.item.custom;

import net.minecraft.world.item.Item;
import org.jd.infestusfrontier.item.ModCreativeModeTab;

public class MeatChestplate extends Item{
    public static final String ID = "meat_chestplate";
    public MeatChestplate(){
        super(new Properties().tab(ModCreativeModeTab.INFESTUS_TAB));
    }

}
