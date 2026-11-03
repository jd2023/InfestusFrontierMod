package org.jd.infestusfrontier.ecology;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A sunflower, lilac, rose bush or peony killed by substrate growth under it: one dry stalk across two blocks with
 * spent seed heads, the same model for all four. Inert (see {@link DeadTallPlantBlock}); breaking either half gives
 * the flower it was once ({@code loot_table/blocks/tall_withered_flower.json}).
 */
public final class TallWitheredFlowerBlock extends DeadTallPlantBlock {
    public static final EnumProperty<TallFlowerSource> SOURCE = EnumProperty.create("source", TallFlowerSource.class);
    /** Muted ash, bone, brown and plum, as for the small Withered Flower. */
    static final int[] TINTS = WitheredFlowerBlock.TINTS;

    private static final MapCodec<TallWitheredFlowerBlock> CODEC = simpleCodec(TallWitheredFlowerBlock::new);

    TallWitheredFlowerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SOURCE, TallFlowerSource.SUNFLOWER));
    }

    /** The lower half of the dead form of {@code plant}; empty for every block that is not a supported tall flower. */
    static Optional<BlockState> wither(BlockState plant) {
        return TallFlowerSource.of(plant.getBlock())
                .map(source -> EcologyModule.TALL_WITHERED_FLOWER
                        .get()
                        .defaultBlockState()
                        .setValue(SOURCE, source));
    }

    /** The tint a plant standing at {@code pos} is drawn in; both halves answer alike. */
    public static int tintAt(BlockState state, BlockPos pos) {
        return TINTS[tintIndexAt(state, pos, TINTS.length)];
    }

    @Override
    public MapCodec<TallWitheredFlowerBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SOURCE);
    }

    @Override
    Block plant(BlockState state) {
        return state.getValue(SOURCE).plant();
    }
}
