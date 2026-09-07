package org.jd.infestusfrontier.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;

public class ExcavatedBlock extends Block {
    public static final String ID = "excavated";

    public ExcavatedBlock() {
        super(Properties.of(Material.CLAY)
                .instabreak()
                .noOcclusion()  // Make sure the block doesn't block rendering on adjacent blocks
                .sound(SoundType.SLIME_BLOCK));
    }
}
