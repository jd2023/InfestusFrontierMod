package org.jd.infestusfrontier.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;

public class Creep extends Block {
    public static final String ID = "creep";

    public Creep() {
        super(BlockBehaviour.Properties.of(Material.MOSS));
    }
}
