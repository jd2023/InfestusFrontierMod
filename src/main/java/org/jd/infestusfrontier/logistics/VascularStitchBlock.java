package org.jd.infestusfrontier.logistics;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A tendon in the corner of a one-block step that joins the vein cell below it (or above it, on a ceiling) to the
 * vein cell beside it (BLOCK_CATALOG T1-06). Both ends are vein cells of its carrier; it holds nothing and passes
 * what enters one end on to the other. It drops off, like a torch, when either end stops being such a cell.
 */
public final class VascularStitchBlock extends Block {
    /** Toward the vein cell beside the stitch. */
    static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Floor: the other end is below; ceiling: above. */
    static final EnumProperty<AttachFace> FACE =
            EnumProperty.create("face", AttachFace.class, AttachFace.FLOOR, AttachFace.CEILING);

    public static final MapCodec<VascularStitchBlock> CODEC = simpleCodec(VascularStitchBlock::new);
    private static final Map<Direction, VoxelShape> FLOOR_SHAPES = shapes(false);
    private static final Map<Direction, VoxelShape> CEILING_SHAPES = shapes(true);

    VascularStitchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition
                .any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.FLOOR)
                .setValue(Routes.CARRIER, Carrier.BIOMASS));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE, Routes.CARRIER);
    }

    @Override
    protected MapCodec<VascularStitchBlock> codec() {
        return CODEC;
    }

    /** The two faces the stitch joins through: toward the cell below or above, and toward the cell beside. */
    static List<Direction> ends(BlockState state) {
        return List.of(vertical(state), state.getValue(FACING));
    }

    private static Direction vertical(BlockState state) {
        return state.getValue(FACE) == AttachFace.FLOOR ? Direction.DOWN : Direction.UP;
    }

    /** Whether {@code other} can hold one end of a stitch of {@code carrier}: a vein cell of that carrier. */
    private static boolean holds(BlockState other, Carrier carrier) {
        return other.getBlock() instanceof VeinBlock vein && vein.carrier() == carrier;
    }

    /**
     * Placed against the top or bottom face of a vein cell, the stitch reaches for the vein beside the corner the
     * player faces first; placed against the side of a vein cell, for the vein below, then above.
     */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction clicked = context.getClickedFace();
        List<BlockState> options = new ArrayList<>();
        if (clicked.getAxis().isHorizontal()) {
            for (AttachFace face : List.of(AttachFace.FLOOR, AttachFace.CEILING))
                options.add(defaultBlockState()
                        .setValue(FACING, clicked.getOpposite())
                        .setValue(FACE, face));
        } else {
            AttachFace face = clicked == Direction.UP ? AttachFace.FLOOR : AttachFace.CEILING;
            Direction side = context.getHorizontalDirection();
            for (int turn = 0; turn < 4; turn++, side = side.getClockWise())
                options.add(defaultBlockState().setValue(FACING, side).setValue(FACE, face));
        }
        for (BlockState option : options) {
            BlockPos end = pos.relative(vertical(option));
            if (!level.hasChunkAt(end) || !(level.getBlockState(end).getBlock() instanceof VeinBlock vein)) continue;
            BlockState state = option.setValue(Routes.CARRIER, vein.carrier());
            if (canSurvive(state, level, pos)) return state;
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Carrier carrier = state.getValue(Routes.CARRIER);
        for (Direction end : ends(state)) {
            BlockPos at = pos.relative(end);
            if (!level.hasChunkAt(at) || !holds(level.getBlockState(at), carrier)) return false;
        }
        return true;
    }

    /** An end that stops being a vein cell of the carrier lets go. */
    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        if (ends(state).contains(direction) && !holds(neighborState, state.getValue(Routes.CARRIER)))
            return Blocks.AIR.defaultBlockState();
        return state;
    }

    /** The fluid side of an end: a passage to the vein cell at the other end. Null on every other face. */
    static @Nullable IFluidHandler fluid(Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
        Carrier carrier = state.getValue(Routes.CARRIER);
        if (side == null || !carrier.isFluid() || !ends(state).contains(side)) return null;
        return new Passage(carrier, () -> beyond(level, pos, side));
    }

    /** The handler a fill entering the stitch at {@code pos} through the end {@code entry} reaches at the other end. */
    private static @Nullable IFluidHandler beyond(Level level, BlockPos pos, Direction entry) {
        if (!level.hasChunkAt(pos)) return null;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof VascularStitchBlock) || !ends(state).contains(entry)) return null;
        Direction out = entry == vertical(state) ? state.getValue(FACING) : vertical(state);
        BlockPos far = pos.relative(out);
        if (!level.hasChunkAt(far)) return null;
        return level.getCapability(Capabilities.FluidHandler.BLOCK, far, out.getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(FACE) == AttachFace.FLOOR ? FLOOR_SHAPES : CEILING_SHAPES).get(state.getValue(FACING));
    }

    /** The tendon's outline: up the side cell's face and along the floor to the middle, turned to each facing. */
    private static Map<Direction, VoxelShape> shapes(boolean ceiling) {
        double[][] boxes = {{4, 0, 0, 12, 16, 5}, {4, 0, 0, 12, 4, 12}};
        Map<Direction, VoxelShape> out = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (double[] box : boxes) {
                double[] a = turn(box[0], box[2], facing);
                double[] b = turn(box[3], box[5], facing);
                double y0 = ceiling ? 16 - box[4] : box[1];
                double y1 = ceiling ? 16 - box[1] : box[4];
                shape = Shapes.or(
                        shape,
                        Block.box(
                                Math.min(a[0], b[0]),
                                y0,
                                Math.min(a[1], b[1]),
                                Math.max(a[0], b[0]),
                                y1,
                                Math.max(a[1], b[1])));
            }
            out.put(facing, shape);
        }
        return out;
    }

    /** A point of the north-facing outline turned about the block's vertical centre line to face {@code facing}. */
    private static double[] turn(double x, double z, Direction facing) {
        return switch (facing) {
            case EAST -> new double[] {16 - z, x};
            case SOUTH -> new double[] {16 - x, 16 - z};
            case WEST -> new double[] {z, 16 - x};
            default -> new double[] {x, z};
        };
    }

    /** Its capabilities and its item network come and go with it; NeoForge's caches and the Mouths must hear. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (level.isClientSide || old == state) return;
        level.invalidateCapabilities(pos);
        if (state.getValue(Routes.CARRIER) == Carrier.ITEM) LogisticsModule.routesChanged(level);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!level.isClientSide && !state.is(next.getBlock())) {
            level.invalidateCapabilities(pos);
            if (state.getValue(Routes.CARRIER) == Carrier.ITEM) LogisticsModule.routesChanged(level);
        }
        super.onRemove(state, level, pos, next, moved);
    }
}
