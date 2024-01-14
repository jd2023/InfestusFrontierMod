package org.jd.infestusfrontier.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;

public class FleshBlock extends Block {
    public static final String ID = "flesh_block";

    public FleshBlock() {
        super(Properties.of(Material.FROGLIGHT));
    }
}
