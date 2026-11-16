package org.jd.infestusfrontier.mining;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * One cell of a Tunnel Root: grown tissue a head lays flush in the tunnel floor, a full block the player walks on. It
 * never moves and gives nothing back when broken. Its look follows the root: a straight cell runs along
 * {@link #FACING}; a {@link #BEND} cell is where the root turned and opens towards {@link #FACING} and the side
 * clockwise of it. No block entity, no ticks.
 */
public final class RootCellBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty BEND = BooleanProperty.create("bend");

    /** Packed earth's hardness: quick with a shovel or a pick, slow by hand. */
    static final float HARDNESS = 1.0F;

    private static final MapCodec<RootCellBlock> CODEC = simpleCodec(RootCellBlock::new);

    RootCellBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BEND, false));
    }

    @Override
    protected MapCodec<RootCellBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BEND);
    }

    /** A straight cell along {@code travel}. */
    static BlockState straight(Direction travel) {
        return MiningModule.ROOT_CELL.get().defaultBlockState().setValue(FACING, travel);
    }

    /** The cell where a root that arrived facing {@code arrived} leaves towards {@code leaving}, a quarter turn away. */
    static BlockState bend(Direction arrived, Direction leaving) {
        Direction back = arrived.getOpposite();
        Direction first = leaving == back.getClockWise() ? back : leaving;
        return MiningModule.ROOT_CELL
                .get()
                .defaultBlockState()
                .setValue(FACING, first)
                .setValue(BEND, true);
    }
}
