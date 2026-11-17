package org.jd.infestusfrontier.mining;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Collection Cilia: an organ that stands on a Mouth facing a Work Bed and sweeps what the bed holds into the Mouth's
 * Item Vein network. Broken, it comes out whole with its biomass and the items it has sent.
 */
public final class CollectionCiliaBlock extends FacingOrganBlock {
    private static final MapCodec<CollectionCiliaBlock> CODEC = simpleCodec(CollectionCiliaBlock::new);

    /** The model's boxes facing north, as {x1, y1, z1, x2, y2, z2}: base, body and three fronds reaching forward. */
    static final double[][] BOXES = {
        {2, 0, 2, 14, 3, 14}, {4, 3, 5, 12, 10, 13}, {3, 5, 0, 5, 13, 6}, {7, 5, 0, 9, 14, 6}, {11, 5, 0, 13, 13, 6}
    };

    CollectionCiliaBlock(Properties properties) {
        super(properties, BOXES);
    }

    @Override
    protected MapCodec<CollectionCiliaBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CollectionCiliaBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(
                type, MiningModule.COLLECTION_CILIA_ENTITY.get(), CollectionCiliaBlockEntity::serverTick);
    }
}
