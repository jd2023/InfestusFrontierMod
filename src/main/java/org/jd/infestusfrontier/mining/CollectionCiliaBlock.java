package org.jd.infestusfrontier.mining;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Collection Cilia: an organ that stands on a Mouth facing a Work Bed and sweeps what the bed holds into the Mouth's
 * Item Vein network. Broken, it comes out whole with its biomass and the items it has sent. {@link #WORKING} is true
 * while its last sweep moved something, for its model to show.
 */
public final class CollectionCiliaBlock extends FacingOrganBlock {
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    private static final MapCodec<CollectionCiliaBlock> CODEC = simpleCodec(CollectionCiliaBlock::new);

    /**
     * The model's rest boxes facing north, as {x1, y1, z1, x2, y2, z2}: base, body, the intake and three fronds reaching
     * forward.
     */
    static final double[][] BOXES = {
        {2, 0, 2, 14, 3, 14},
        {4, 3, 5, 12, 10, 13},
        {5.5, 4.5, 4.5, 10.5, 8.5, 5.5},
        {3, 5, 1, 5, 13, 7},
        {7, 5, 1, 9, 14, 7},
        {11, 5, 1, 13, 13, 7}
    };

    CollectionCiliaBlock(Properties properties) {
        super(properties, BOXES);
        registerDefaultState(defaultBlockState().setValue(WORKING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WORKING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
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
