package org.jd.infestusfrontier.ecology;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Tall grass or a large fern killed by substrate growth under it: dry blades or curled fronds across two blocks, one
 * shared shape for each. Inert (see {@link DeadTallPlantBlock}); breaking either half rolls the plant's own drops once
 * ({@code loot_table/blocks/tall_blighted_undergrowth.json}). Replaceable, as the living plants are.
 */
public final class TallBlightedUndergrowthBlock extends DeadTallPlantBlock {
    public static final EnumProperty<TallUndergrowthSource> SOURCE =
            EnumProperty.create("source", TallUndergrowthSource.class);
    /** Straw, bone, ash and dry brown: the tints a dead undergrowth is drawn in. */
    static final int[] TINTS = {0xB3A369, 0xCFC6A8, 0x9C9890, 0x8A6F48};

    private static final MapCodec<TallBlightedUndergrowthBlock> CODEC = simpleCodec(TallBlightedUndergrowthBlock::new);

    TallBlightedUndergrowthBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SOURCE, TallUndergrowthSource.TALL_GRASS));
    }

    /** The lower half of the dead form of {@code plant}; empty for every block that is not a supported undergrowth. */
    static Optional<BlockState> wither(BlockState plant) {
        return TallUndergrowthSource.of(plant.getBlock())
                .map(source -> EcologyModule.TALL_BLIGHTED_UNDERGROWTH
                        .get()
                        .defaultBlockState()
                        .setValue(SOURCE, source));
    }

    /** The tint a plant standing at {@code pos} is drawn in; both halves answer alike. */
    public static int tintAt(BlockState state, BlockPos pos) {
        return TINTS[tintIndexAt(state, pos, TINTS.length)];
    }

    @Override
    public MapCodec<TallBlightedUndergrowthBlock> codec() {
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
