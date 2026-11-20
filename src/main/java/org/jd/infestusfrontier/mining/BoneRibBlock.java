package org.jd.infestusfrontier.mining;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A bone rib a Structure Grower grows from supplied Bone Plates to frame a mine's entrance, its junctions and, on the
 * Wider Section, its larger passage: a flat ivory band across each wall and the ceiling of a passage block, at right
 * angles to the way the passage runs ({@link #AXIS}), {@link #THICK} on the Wider Section. It has no collision, so the
 * passage keeps its headroom and width; it holds nothing up and keeps out no fluid. When its wall goes it goes too. Grown
 * tissue: it gives nothing back.
 */
public final class BoneRibBlock extends MultifaceBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty THICK = BooleanProperty.create("thick");

    private static final MapCodec<BoneRibBlock> CODEC = simpleCodec(BoneRibBlock::new);
    /** Required by the multiface block; a rib never spreads, so it is never asked to. */
    private final MultifaceSpreader spreader = new MultifaceSpreader(this);

    BoneRibBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                defaultBlockState().setValue(AXIS, Direction.Axis.Z).setValue(THICK, false));
    }

    @Override
    protected MapCodec<BoneRibBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS, THICK);
    }

    /** Walls and the ceiling; never the floor a player walks on. */
    @Override
    protected boolean isFaceSupported(Direction face) {
        return face != Direction.DOWN;
    }

    /** A quarter turn turns the way the passage runs with its faces. */
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState turned = super.rotate(state, rotation);
        boolean quarter = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
        if (!quarter) return turned;
        return turned.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
    }

    @Override
    public MultifaceSpreader getSpreader() {
        return spreader;
    }
}
