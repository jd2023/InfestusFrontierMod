package org.jd.infestusfrontier.ecology;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A dead bush darkened by substrate growth under it, with root threads at its foot. It keeps no property: breaking it
 * rolls the dead bush's own loot once ({@code loot_table/blocks/blighted_dead_bush.json}).
 */
public final class BlightedDeadBushBlock extends DeadShrubBlock {
    /** Soot, umber, ash and bark: the tints a Blighted Dead Bush is drawn in. */
    static final int[] TINTS = {0x4F4339, 0x6B5A48, 0x9C9890, 0x7A6A55};

    private static final MapCodec<BlightedDeadBushBlock> CODEC = simpleCodec(BlightedDeadBushBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    BlightedDeadBushBlock(Properties properties) {
        super(properties);
    }

    /** The blighted form of a dead bush; empty for every other block. */
    static Optional<BlockState> wither(BlockState plant) {
        return plant.is(Blocks.DEAD_BUSH)
                ? Optional.of(EcologyModule.BLIGHTED_DEAD_BUSH.get().defaultBlockState())
                : Optional.empty();
    }

    /** The tint a bush at {@code pos} is drawn in: one of {@link #TINTS}. */
    public static int tintAt(BlockPos pos) {
        return TINTS[tintIndexAt(pos, TINTS.length)];
    }

    @Override
    protected MapCodec<BlightedDeadBushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    ItemStack plantStack(BlockState state) {
        return new ItemStack(Blocks.DEAD_BUSH);
    }
}
