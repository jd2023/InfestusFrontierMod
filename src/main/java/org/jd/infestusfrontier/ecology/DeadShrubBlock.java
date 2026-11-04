package org.jd.infestusfrontier.ecology;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a shrub, short grass, fern or sapling leaves when substrate growth kills it: a plain {@link BushBlock}, so it
 * stands where its plant stood and breaks through the same player and neighbour paths. Inert: no block entity, no
 * ticks, no growth, no berry picking, no contact damage and no bone meal response. Each family keeps only what its
 * harvest needs and the loot table answers it with the plant's own drop rules (VEGETATION_INFECTION.md, "Shipped:
 * shrubs, grass and saplings").
 *
 * <p>Appearance is never saved: the blockstate lists the family's looks and vanilla picks one by the position's seed.
 */
public abstract sealed class DeadShrubBlock extends BushBlock
        permits DeadBerryBushBlock, BlightedDeadBushBlock, BlightedUndergrowthBlock, WitheredSaplingBlock {
    private static final int FLAMMABILITY = 100;
    private static final int FIRE_SPREAD_SPEED = 60;

    DeadShrubBlock(Properties properties) {
        super(properties);
    }

    /** The dead form of {@code plant}, keeping what its harvest needs; empty for every unsupported block. */
    static Optional<BlockState> wither(BlockState plant) {
        return DeadBerryBushBlock.wither(plant)
                .or(() -> BlightedDeadBushBlock.wither(plant))
                .or(() -> BlightedUndergrowthBlock.wither(plant))
                .or(() -> WitheredSaplingBlock.wither(plant));
    }

    /** True for any of the four dead families. */
    static boolean isDead(BlockState state) {
        return state.getBlock() instanceof DeadShrubBlock;
    }

    /** Which of {@code count} tints a plant at {@code pos} wears: never saved. */
    static int tintIndexAt(BlockPos pos, int count) {
        return (int) ((Mth.getSeed(pos) >>> 23) % count);
    }

    /** The item pick block gives: the plant it was. */
    abstract ItemStack plantStack(BlockState state);

    /** Stands where its plant stood and on the cell that replaced that ground. */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return super.mayPlaceOn(state, level, pos) || state.is(EcologyModule.LIVING_SUBSTRATE.get());
    }

    /** Pick block gives the plant it was; there is no dead-plant item. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return plantStack(state);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return FLAMMABILITY;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return FIRE_SPREAD_SPEED;
    }
}
