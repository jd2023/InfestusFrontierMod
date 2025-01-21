package org.jd.infestusfrontier.block.custom.network;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class DeadCorruptedBlock extends Block {
    public static final String ID = "corrupted_block_dead";

    public DeadCorruptedBlock() {
        super(Properties.copy(Blocks.SCULK).strength(0.5F).noLootTable());
    }
}
