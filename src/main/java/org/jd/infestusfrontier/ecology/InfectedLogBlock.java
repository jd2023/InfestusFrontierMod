package org.jd.infestusfrontier.ecology;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The trunk of every infected tree, whatever tree it was: a vanilla pillar with only its {@code axis}. Breaking it
 * drops one Infected Log ({@code loot_table/blocks/infected_log.json}), and the item carries nothing, so cutting and
 * replacing a log never multiplies wood. It sits in the {@code logs} block tag, which is how vanilla's
 * {@code LeavesBlock} counts a log as distance 0 and keeps Blighted Leaves alive. It has no block entity, tick, resin or
 * production. A state saved with the {@code source} property of older versions reads as this block with its axis.
 */
public final class InfectedLogBlock extends RotatedPillarBlock {
    /** The one bark colour every Infected Log is drawn in. */
    public static final int TINT = 0x9A7C52;

    private static final MapCodec<InfectedLogBlock> CODEC = simpleCodec(InfectedLogBlock::new);

    InfectedLogBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<InfectedLogBlock> codec() {
        return CODEC;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 5;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 5;
    }
}
