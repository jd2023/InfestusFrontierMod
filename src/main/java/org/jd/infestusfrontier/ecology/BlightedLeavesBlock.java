package org.jd.infestusfrontier.ecology;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The crown of every infected tree, whatever leaves it was: vanilla's {@link LeavesBlock} unchanged. Distance,
 * persistence, waterlogging, support by logs and decay are vanilla's. Decay and breaking roll
 * {@code loot_table/blocks/blighted_leaves.json}: nothing, or with shears or Silk Touch one Blighted Leaves, so no
 * sapling, fruit or stick of the tree it was ever comes back. No block entity, no production, no regrowth. A state
 * saved with the {@code source} property of older versions reads as this block with its distance, persistence and
 * water.
 */
public final class BlightedLeavesBlock extends LeavesBlock {
    /** The one colour all Blighted Leaves are drawn in. */
    public static final int TINT = 0x9A9250;

    private static final MapCodec<BlightedLeavesBlock> CODEC = simpleCodec(BlightedLeavesBlock::new);

    BlightedLeavesBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<BlightedLeavesBlock> codec() {
        return CODEC;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 30;
    }
}
