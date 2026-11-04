package org.jd.infestusfrontier.ecology;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A sapling killed by substrate growth under it. Its one property is {@link #SOURCE}, the species it was, and breaking
 * it returns that sapling through the sapling's own loot table ({@code loot_table/blocks/withered_sapling.json}). It
 * is not a {@code SaplingBlock}, so it takes no random tick, has no growth stage, never grows a tree and answers no
 * bone meal.
 */
public final class WitheredSaplingBlock extends DeadShrubBlock {
    public static final EnumProperty<SaplingSource> SOURCE = EnumProperty.create("source", SaplingSource.class);

    private static final MapCodec<WitheredSaplingBlock> CODEC = simpleCodec(WitheredSaplingBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

    WitheredSaplingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SOURCE, SaplingSource.OAK));
    }

    /** The dead form of {@code plant}; empty for every block that is not a supported sapling. */
    static Optional<BlockState> wither(BlockState plant) {
        return SaplingSource.of(plant.getBlock())
                .map(source ->
                        EcologyModule.WITHERED_SAPLING.get().defaultBlockState().setValue(SOURCE, source));
    }

    /** The tint a sapling is drawn in: its species' tone. */
    public static int tintOf(BlockState state) {
        return state.getValue(SOURCE).tint();
    }

    @Override
    protected MapCodec<WitheredSaplingBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SOURCE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    ItemStack plantStack(BlockState state) {
        return new ItemStack(state.getValue(SOURCE).sapling());
    }
}
