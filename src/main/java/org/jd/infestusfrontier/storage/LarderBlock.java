package org.jd.infestusfrontier.storage;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.core.storage.PodKind;

/**
 * A Larder (BLOCK_CATALOG T1-09): a Mixed Craw mutated with Sealing Resin while empty, three compartments of the
 * Craw's depth, each with a label and a reserve that no external extraction takes. It keeps its grade, its facing and
 * its tree; no item turns it back.
 */
public final class LarderBlock extends ItemCapsuleBlock {
    private static final MapCodec<LarderBlock> CODEC = simpleCodec(LarderBlock::new);

    LarderBlock(Properties properties) {
        super(properties);
    }

    @Override
    PodKind kind() {
        return PodKind.LARDER;
    }

    @Override
    protected MapCodec<LarderBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LarderBlockEntity(pos, state);
    }
}
