package org.jd.infestusfrontier.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;

public class BioReservoir extends Block {
    public static final String ID = "bio_reservoir";

    public BioReservoir() {
        super(Properties.of(Material.GLASS)
                .strength(1.5F, 6.0F) // Adjust hardness and resistance as needed
                .noOcclusion());  // Prevent visual occlusion for transparent blocks


    }


}
