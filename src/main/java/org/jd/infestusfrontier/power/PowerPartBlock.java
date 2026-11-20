package org.jd.infestusfrontier.power;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** The dark connection face points into exactly one adjacent core. Parts never own stores or tick. */
public final class PowerPartBlock extends DirectionalBlock {
    private static final MapCodec<PowerPartBlock> CODEC = simpleCodec(PowerPartBlock::new);

    PowerPartBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<PowerPartBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos,
            Block neighbour,
            net.minecraft.core.BlockPos from,
            boolean moving) {
        super.neighborChanged(state, level, pos, neighbour, from, moving);
        if (!level.isClientSide && pos.relative(state.getValue(FACING)).equals(from)) level.invalidateCapabilities(pos);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(
            net.minecraft.world.item.ItemStack stack,
            BlockState state,
            net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        var fluid = org.jd.infestusfrontier.storage.api.BiomassFluids.useOn(
                stack, level, pos, hit.getDirection(), player, hand);
        if (fluid != net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return fluid;
        return PowerModule.coreAt(level, pos)
                .map(core ->
                        org.jd.infestusfrontier.organ.api.Organs.openScreen(stack, level, core.getBlockPos(), player))
                .orElse(net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(
            BlockState state,
            net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.phys.BlockHitResult hit) {
        return PowerModule.coreAt(level, pos)
                .map(core -> org.jd.infestusfrontier.organ.api.Organs.openScreen(level, core.getBlockPos(), player)
                        .result())
                .orElse(net.minecraft.world.InteractionResult.PASS);
    }
}
