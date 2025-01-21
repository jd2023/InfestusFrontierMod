package org.jd.infestusfrontier.block.custom.network;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class AdvanceCorruptedBlock extends Block {
    public static final String ID = "corrupted_block_advance";

    public AdvanceCorruptedBlock() {
        super(Properties.copy(Blocks.SCULK).strength(2).noLootTable());
    }
}
