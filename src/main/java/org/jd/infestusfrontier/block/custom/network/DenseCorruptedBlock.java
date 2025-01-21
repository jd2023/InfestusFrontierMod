package org.jd.infestusfrontier.block.custom.network;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class DenseCorruptedBlock extends Block {
    public static final String ID = "corrupted_block_dense";

    public DenseCorruptedBlock() {
        super(Properties.copy(Blocks.SCULK).strength(1.5F).noLootTable());
    }
}
