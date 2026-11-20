package org.jd.infestusfrontier.mining;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jetbrains.annotations.Nullable;

/**
 * A Work Bed: a living trough that holds what the Fracture Jaw in front of it cuts, until Collection Cilia send it on
 * or the player takes it at its screen. Any hand opens the screen and sneaking builds against it. Broken, it comes out
 * whole with what it holds.
 */
public final class WorkBedBlock extends MiningOrganBlock {
    private static final MapCodec<WorkBedBlock> CODEC = simpleCodec(WorkBedBlock::new);

    /** The model's boxes, as {x1, y1, z1, x2, y2, z2}: the floor and its four rims. The bed has no facing. */
    private static final double[][] BOXES = {
        {0, 0, 0, 16, 3, 16}, {0, 3, 0, 16, 8, 2}, {0, 3, 14, 16, 8, 16}, {0, 3, 2, 2, 8, 14}, {14, 3, 2, 16, 8, 14}
    };

    private static final VoxelShape SHAPE = FacingShapes.of(BOXES).get(Direction.NORTH);

    WorkBedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<WorkBedBlock> codec() {
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

    /** Placed by hand on a foundation. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Organs.hasFoundation(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WorkBedBlockEntity(pos, state);
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
        return Organs.openScreen(stack, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Organs.openScreen(level, pos, player).result();
    }
}
