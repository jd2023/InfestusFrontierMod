package org.jd.infestusfrontier.ecology;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * What a two-block plant leaves when substrate growth kills it: vanilla's own {@link DoublePlantBlock}, so the halves
 * support each other, vanish together and break through the same player and neighbour paths as the living plant, with
 * no bone meal response. The family's one property besides the half is {@code source}, the plant it was, and the loot
 * table answers it with that plant's own drop rules (VEGETATION_INFECTION.md, "Shipped: tall plants").
 *
 * <p>Appearance is never saved. Vanilla seeds a double plant's look from its lower half's position, so both halves
 * pick the same turn of the blockstate's list; {@link #tintIndexAt} does the same for the block colour.
 */
public abstract sealed class DeadTallPlantBlock extends DoublePlantBlock
        permits TallWitheredFlowerBlock, TallBlightedUndergrowthBlock {
    private static final int FLAMMABILITY = 100;
    private static final int FIRE_SPREAD_SPEED = 60;

    DeadTallPlantBlock(Properties properties) {
        super(properties);
    }

    /** The dead form of the same half of {@code plant}; empty for every block that is not a supported tall plant. */
    static Optional<BlockState> wither(BlockState plant) {
        if (!plant.hasProperty(HALF)) return Optional.empty();
        DoubleBlockHalf half = plant.getValue(HALF);
        return TallWitheredFlowerBlock.wither(plant)
                .or(() -> TallBlightedUndergrowthBlock.wither(plant))
                .map(dead -> dead.setValue(HALF, half));
    }

    /** True for either dead family. */
    static boolean isDead(BlockState state) {
        return state.getBlock() instanceof DeadTallPlantBlock;
    }

    /** Which of {@code count} tints a plant at {@code pos} wears: the same for both halves, never saved. */
    static int tintIndexAt(BlockState state, BlockPos pos, int count) {
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        return (int) ((Mth.getSeed(lower) >>> 23) % count);
    }

    /** The living plant this block was. */
    abstract Block plant(BlockState state);

    /** Stands where its plant stood and on the cell that replaced that ground. */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return super.mayPlaceOn(state, level, pos) || state.is(EcologyModule.LIVING_SUBSTRATE.get());
    }

    /** Pick block gives the plant it was; there is no dead-plant item. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(plant(state));
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
