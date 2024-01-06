package org.jd.infestusfrontier.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;

public class BioReservoir extends Block {
    public static final String ID = "bio_reservoir";
    public BioReservoir() {
        super(Properties.of(Material.GLASS));
    }
}
