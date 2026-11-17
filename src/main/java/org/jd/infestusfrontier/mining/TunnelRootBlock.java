package org.jd.infestusfrontier.mining;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jetbrains.annotations.Nullable;

/**
 * The head of a Tunnel Root, a starter organ grown from an Organ Bud. It faces the way it cuts. The player feeds it
 * Root Segments and biomass by hand, steers its next step at its screen and empties its output; every twenty seconds
 * it cuts the two blocks ahead, lays a root cell under them and moves in. Segments and biomass buckets load it, any
 * other hand opens its screen and sneaking builds against it. Broken, it comes out whole with what it holds and the
 * cells it has laid.
 */
public final class TunnelRootBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final MapCodec<TunnelRootBlock> CODEC = simpleCodec(TunnelRootBlock::new);

    /** The model's boxes facing north, as {x1, y1, z1, x2, y2, z2}: foot, body, crown and the snout it cuts with. */
    private static final double[][] BOXES = {
        {2, 0, 3, 14, 2, 15}, {3, 2, 4, 13, 9, 14}, {4, 9, 6, 12, 12, 13}, {5, 2, 1, 11, 7, 4}, {6, 3, 0, 10, 6, 1}
    };

    private static final Map<Direction, VoxelShape> SHAPES = FacingShapes.of(BOXES);

    TunnelRootBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<TunnelRootBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    /** Placed by hand on a foundation, facing away from the player. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Organs.hasFoundation(context.getLevel(), context.getClickedPos())
                ? defaultBlockState().setValue(FACING, context.getHorizontalDirection())
                : null;
    }

    /** The placer becomes the steward; a head a bud grew turns to face away from the player who grew it. */
    @Override
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        Organs.placedBy(level, pos, placer);
        if (!level.isClientSide && placer != null && state.getValue(FACING) != placer.getDirection())
            level.setBlock(pos, state.setValue(FACING, placer.getDirection()), Block.UPDATE_ALL);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TunnelRootBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, MiningModule.TUNNEL_ROOT_ENTITY.get(), TunnelRootBlockEntity::serverTick);
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
        if (level.getBlockEntity(pos) instanceof TunnelRootBlockEntity head && TunnelRootBlockEntity.loads(stack)) {
            if (!level.isClientSide) head.load(player, hand);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return Organs.openScreen(stack, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Organs.openScreen(level, pos, player).result();
    }
}
