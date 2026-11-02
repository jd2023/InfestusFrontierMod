package org.jd.infestusfrontier.storage;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.core.storage.PodKind;

/**
 * A Mixed Craw (BLOCK_CATALOG T1-09): a pod mutated with Elastic Gel while empty, four compartments of one kind each at
 * a quarter of a bulk pod's depth. It keeps its grade, its facing and its tree; no item turns it back.
 */
public final class MixedCrawBlock extends ItemCapsuleBlock {
    private static final MapCodec<MixedCrawBlock> CODEC = simpleCodec(MixedCrawBlock::new);

    MixedCrawBlock(Properties properties) {
        super(properties);
    }

    @Override
    PodKind kind() {
        return PodKind.CRAW;
    }

    @Override
    protected MapCodec<MixedCrawBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MixedCrawBlockEntity(pos, state);
    }
}
