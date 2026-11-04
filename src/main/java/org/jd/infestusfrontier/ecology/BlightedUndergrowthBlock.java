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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Short grass or a fern killed by substrate growth under it: dry blades or a curled frond. Inert (see
 * {@link DeadShrubBlock}); breaking it rolls the plant's own drops once ({@code loot_table/blocks/blighted_undergrowth.json}).
 * Replaceable, as the living plants are.
 */
public final class BlightedUndergrowthBlock extends DeadShrubBlock {
    public static final EnumProperty<UndergrowthSource> SOURCE = EnumProperty.create("source", UndergrowthSource.class);
    /** Straw, bone, ash and dry brown: the tints a dead undergrowth is drawn in. */
    static final int[] TINTS = TallBlightedUndergrowthBlock.TINTS;

    private static final MapCodec<BlightedUndergrowthBlock> CODEC = simpleCodec(BlightedUndergrowthBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    BlightedUndergrowthBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SOURCE, UndergrowthSource.SHORT_GRASS));
    }

    /** The dead form of {@code plant}; empty for every block that is not a supported undergrowth. */
    static Optional<BlockState> wither(BlockState plant) {
        return UndergrowthSource.of(plant.getBlock())
                .map(source -> EcologyModule.BLIGHTED_UNDERGROWTH
                        .get()
                        .defaultBlockState()
                        .setValue(SOURCE, source));
    }

    /** The tint a plant at {@code pos} is drawn in: one of {@link #TINTS}. */
    public static int tintAt(BlockPos pos) {
        return TINTS[tintIndexAt(pos, TINTS.length)];
    }

    @Override
    protected MapCodec<BlightedUndergrowthBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SOURCE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(level, pos);
        return SHAPE.move(offset.x, offset.y, offset.z);
    }

    @Override
    ItemStack plantStack(BlockState state) {
        return new ItemStack(state.getValue(SOURCE).plant());
    }
}
