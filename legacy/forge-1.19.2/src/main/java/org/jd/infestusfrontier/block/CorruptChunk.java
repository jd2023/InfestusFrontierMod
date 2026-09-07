package org.jd.infestusfrontier.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;

public class CorruptChunk extends Block {
    public static final String ID = "corrupt_chunk";

    public CorruptChunk() {
        super(Properties.of(Material.SCULK));
    }
}
