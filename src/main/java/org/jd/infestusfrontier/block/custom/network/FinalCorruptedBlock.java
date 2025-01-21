package org.jd.infestusfrontier.block.custom.network;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class FinalCorruptedBlock extends Block {
    public static final String ID = "corrupted_block_final";

    public FinalCorruptedBlock() {
        super(Properties.copy(Blocks.SCULK).strength(4).noLootTable());
    }
}
