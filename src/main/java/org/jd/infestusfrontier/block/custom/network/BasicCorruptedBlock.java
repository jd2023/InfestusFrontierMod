package org.jd.infestusfrontier.block.custom.network;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class BasicCorruptedBlock extends Block {
    public static final String ID = "corrupted_block_basic";

    public BasicCorruptedBlock() {
        super(Properties.copy(Blocks.SCULK).strength(1).noLootTable());
    }
}
