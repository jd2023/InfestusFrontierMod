package org.jd.infestusfrontier.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;

public class CreepTwo extends Block {
    public static final String ID = "creep_two";

    public CreepTwo() {
        super(Properties.of(Material.MOSS).lightLevel(state -> 5));
    }
}
