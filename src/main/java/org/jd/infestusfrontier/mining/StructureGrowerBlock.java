package org.jd.infestusfrontier.mining;

import com.mojang.serialization.MapCodec;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jetbrains.annotations.Nullable;

/**
 * The Structure Grower: an organ that stands on a Surveyed Tissue marker and builds the marker's approved plan, a
 * step at a time, from the stock it holds: a root cell in each step's floor, a tread on it on a staircase, and a
 * light where the plan has one. It builds only into blocks that are already clear; it cuts and collects nothing.
 * Root Segments, stairs and torches load it by hand, any other hand opens its screen and sneaking builds against it.
 * Broken, it comes out whole with its stock and the steps it has built.
 */
public final class StructureGrowerBlock extends BaseEntityBlock {
    private static final MapCodec<StructureGrowerBlock> CODEC = simpleCodec(StructureGrowerBlock::new);

    /** The model's boxes, as {x1, y1, z1, x2, y2, z2}: foot, body and three spinnerets. It has no facing. */
    static final double[][] BOXES = {
        {1, 0, 1, 15, 2, 15}, {3, 2, 3, 13, 10, 13}, {4, 10, 4, 7, 14, 7}, {9, 10, 5, 12, 13, 8}, {6, 10, 9, 9, 15, 12}
    };

    private static final VoxelShape SHAPE = FacingShapes.of(BOXES).get(Direction.NORTH);

    StructureGrowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<StructureGrowerBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Placed by hand on a foundation; it works only on Surveyed Tissue. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Organs.hasFoundation(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    /** The placer becomes the steward. */
    @Override
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        Organs.placedBy(level, pos, placer);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StructureGrowerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(
                type, MiningModule.STRUCTURE_GROWER_ENTITY.get(), StructureGrowerBlockEntity::serverTick);
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
        if (level.getBlockEntity(pos) instanceof StructureGrowerBlockEntity grower
                && StructureGrowerBlockEntity.loads(stack)) {
            if (!level.isClientSide) grower.load(player, hand);
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
