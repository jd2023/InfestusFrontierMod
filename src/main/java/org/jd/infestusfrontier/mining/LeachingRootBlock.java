package org.jd.infestusfrontier.mining;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The root a Leaching Pod grows down its own column as it works: a bark pillar with glowing veins through the weakened
 * ground, and the pit's only light, by ordinary block light. It grows only into ground the pod converts and only below
 * itself, so an ore, a fluid, a cavity or anything else the pod leaves alone ends it for good: it never replaces ore and
 * never bridges a gap. Grown tissue: no block entity, no ticks, and breaking it gives nothing back. Each part stands
 * by itself, so breaking one leaves the rest; breaking the part under the pod kills the pod.
 */
public final class LeachingRootBlock extends Block {
    /** Packed earth's hardness, as a root cell. */
    static final float HARDNESS = 1.0F;
    /** The light it gives: enough to see the whole 7-wide pit around it. */
    static final int LIGHT = 12;

    private static final MapCodec<LeachingRootBlock> CODEC = simpleCodec(LeachingRootBlock::new);
    /** The pillar: the model's one element. */
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    LeachingRootBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<LeachingRootBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
