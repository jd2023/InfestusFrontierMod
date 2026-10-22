package org.jd.infestusfrontier.storage;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jd.infestusfrontier.core.storage.Box;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.jetbrains.annotations.Nullable;

/**
 * One cell of a Biomass Reservoir (BLOCK_CATALOG T1-07). Touching cells form one body only as a full rectangular box
 * ({@link org.jd.infestusfrontier.core.storage.Body}); the six side properties say which faces join a cell of the
 * same body, and {@link #FILL} is the level of the cell's layer in sixteenths. A cell stands on a foundation, on
 * another cell or on a block that pumps into it from below. Buckets move biomass, anything else opens the screen,
 * sneaking builds against it.
 */
public final class ReservoirCellBlock extends BaseEntityBlock {
    public static final int FILL_STEPS = 16;
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, FILL_STEPS);
    public static final Map<Direction, BooleanProperty> JOINED = PipeBlock.PROPERTY_BY_DIRECTION;
    private static final MapCodec<ReservoirCellBlock> CODEC = simpleCodec(ReservoirCellBlock::new);

    ReservoirCellBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(FILL, 0);
        for (BooleanProperty side : JOINED.values()) state = state.setValue(side, false);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL);
        JOINED.values().forEach(builder::add);
    }

    @Override
    protected MapCodec<ReservoirCellBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** The look of the cell at {@code pos} in body {@code box}, showing {@code fill}. */
    static BlockState shaped(BlockState state, BlockPos pos, Box box, int fill) {
        for (Map.Entry<Direction, BooleanProperty> side : JOINED.entrySet()) {
            BlockPos next = pos.relative(side.getKey());
            state = state.setValue(side.getValue(), box.contains(next.getX(), next.getY(), next.getZ()));
        }
        return state.setValue(FILL, fill);
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /** A cell stands on a foundation, on another cell, or on a block that pumps into it from below. */
    static boolean canStand(LevelReader level, BlockPos pos) {
        if (Organs.hasFoundation(level, pos)) return true;
        BlockPos below = pos.below();
        if (!level.hasChunkAt(below)) return false;
        BlockState under = level.getBlockState(below);
        return under.is(StorageModule.RESERVOIR_CELL.get()) || under.is(StorageModule.RESERVOIR_SUPPORTS);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return canStand(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReservoirCellBlockEntity(pos, state);
    }

    /** A new cell looks for a body on the next tick, once its carried biomass is in. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(this)) level.scheduleTick(pos, this, Bodies.FORM_TICKS);
    }

    /** A cell that leaves takes its own share; the rest of its body falls apart into cells that form again. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ReservoirCellBlockEntity cell)
            Bodies.leave(cell, Reservoir.KIND);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof ReservoirCellBlockEntity cell) Bodies.form(cell, Reservoir.KIND);
    }

    /** A Creative player who breaks a cell holding biomass still gets it back, as with a shulker box. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide
                && player.isCreative()
                && level.getBlockEntity(pos) instanceof ReservoirCellBlockEntity cell
                && !cell.isEmpty()) {
            ItemStack stack = new ItemStack(this);
            stack.applyComponents(cell.collectComponents());
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        ItemInteractionResult fluid = BiomassFluids.useOn(stack, level, pos, hit.getDirection(), player, hand);
        if (fluid != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return fluid;
        return Organs.openScreen(stack, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Organs.openScreen(level, pos, player).result();
    }
}
