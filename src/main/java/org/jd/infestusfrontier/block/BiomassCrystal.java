package org.jd.infestusfrontier.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;

public class BiomassCrystal extends Block {
    public static final String ID = "biomass_crystal_block";

    public BiomassCrystal() {
        super(Properties.of(Material.AMETHYST));
    }
}
