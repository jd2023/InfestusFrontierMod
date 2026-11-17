package org.jd.infestusfrontier.mining;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jd.infestusfrontier.organ.api.Organs;

/**
 * Surveyed Tissue: a flush marker set into the ground at a section's entrance, a full block the player walks on. It
 * faces the way the section goes. It holds one plan: a draft the player shapes at its screen and sees drawn in the
 * world, then approves; a Structure Grower standing on it builds the approved plan. It is an organ foundation. Broken,
 * it comes out blank: the plan and what was built of it stay with the place, not the item.
 */
public final class SurveyedTissueBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** Packed earth's hardness, as a root cell's. */
    static final float HARDNESS = 1.0F;

    private static final MapCodec<SurveyedTissueBlock> CODEC = simpleCodec(SurveyedTissueBlock::new);

    SurveyedTissueBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<SurveyedTissueBlock> codec() {
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

    /** Faces the way the player looks: the section runs away from them. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SurveyedTissueBlockEntity(pos, state);
    }

    /** A block in hand is placed against the marker, a Structure Grower on top of it; anything else opens it. */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (stack.getItem() instanceof BlockItem) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        return Organs.openScreen(stack, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Organs.openScreen(level, pos, player).result();
    }
}
