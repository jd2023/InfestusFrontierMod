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
 * A Fracture Jaw: a stationary organ that cuts the block in front of it into the Work Bed behind it. It never moves and
 * never chooses where to cut. Broken, it comes out whole with its biomass and the cuts it has made.
 */
public final class FractureJawBlock extends FacingOrganBlock {
    private static final MapCodec<FractureJawBlock> CODEC = simpleCodec(FractureJawBlock::new);

    /** The model's boxes facing north, as {x1, y1, z1, x2, y2, z2}: foot, body, and the upper and lower jaw. */
    static final double[][] BOXES = {
        {1, 0, 2, 15, 2, 15}, {2, 2, 4, 14, 12, 15}, {3, 8, 0, 13, 12, 4}, {3, 2, 0, 13, 5, 4}
    };

    FractureJawBlock(Properties properties) {
        super(properties, BOXES);
    }

    @Override
    protected MapCodec<FractureJawBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FractureJawBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, MiningModule.FRACTURE_JAW_ENTITY.get(), FractureJawBlockEntity::serverTick);
    }
}
