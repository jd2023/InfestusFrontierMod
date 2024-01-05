package org.jd.infestusfrontier.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;

public class Creep extends Block {
    public static final String ID = "creep";
    public Creep() {
        super(Properties.of(Material.MOSS));
    }
}
