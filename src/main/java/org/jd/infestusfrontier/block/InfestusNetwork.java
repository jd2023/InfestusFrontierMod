package org.jd.infestusfrontier.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;

public class InfestusNetwork extends Block {
    public static final String ID = "infestus_network";

    public InfestusNetwork() {
        super(Properties.of(Material.MOSS));
    }
}
