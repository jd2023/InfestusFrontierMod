package org.jd.infestusfrontier.construction;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Thin skin which can occupy any supported face without a block entity. */
final class LivingSkinCoveringBlock extends MultifaceBlock {
    private final net.minecraft.world.level.block.MultifaceSpreader spreader;
    LivingSkinCoveringBlock(BlockBehaviour.Properties properties) {
        super(properties);
        spreader = new net.minecraft.world.level.block.MultifaceSpreader(this);
    }
    @Override protected MapCodec<? extends MultifaceBlock> codec() {
        return simpleCodec(LivingSkinCoveringBlock::new);
    }
    @Override public net.minecraft.world.level.block.MultifaceSpreader getSpreader() { return spreader; }
}
