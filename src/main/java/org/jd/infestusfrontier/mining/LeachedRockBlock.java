package org.jd.infestusfrontier.mining;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Ground a Leaching Pod weakened. Glass underneath: translucent, solid, and the faces between two of them are not
 * drawn. It breaks almost at once by hand and leaves nothing. {@link #GLOW} is a few fixed, dim looks; the pod picks
 * one when it converts a block, so nothing rolls again after a reload.
 */
public final class LeachedRockBlock extends TransparentBlock {
    /** Soft: about a fifth of a second by hand. */
    static final float HARDNESS = 0.1F;
    /** The glow looks, from none to the brightest. */
    public static final IntegerProperty GLOW = IntegerProperty.create("glow", 0, 2);
    /** The light each glow look gives: faint at most. */
    private static final int[] LIGHT = {0, 2, 4};

    private static final MapCodec<LeachedRockBlock> CODEC = simpleCodec(LeachedRockBlock::new);

    LeachedRockBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GLOW, 0));
    }

    @Override
    protected MapCodec<LeachedRockBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GLOW);
    }

    static int light(BlockState state) {
        return LIGHT[state.getValue(GLOW)];
    }
}
