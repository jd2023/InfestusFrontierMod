package org.jd.infestusfrontier.storage;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassBuckets;
import org.jetbrains.annotations.Nullable;

/**
 * A one-block biomass tank. {@code fill} shows the content in four steps. Buckets move biomass; empty hands open the
 * screen; anything else passes through so the player can build against it.
 */
public final class BiomassBladderBlock extends BaseEntityBlock {
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 4);
    private static final MapCodec<BiomassBladderBlock> CODEC = simpleCodec(BiomassBladderBlock::new);

    BiomassBladderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FILL, 0));
    }

    @Override
    protected MapCodec<BiomassBladderBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Organs.hasFoundation(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BiomassBladderBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        return BiomassBuckets.useOn(stack, level, pos, hit.getDirection(), player, hand);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof BiomassBladderBlockEntity be)
            server.openMenu(be, pos);
        return InteractionResult.CONSUME;
    }
}
