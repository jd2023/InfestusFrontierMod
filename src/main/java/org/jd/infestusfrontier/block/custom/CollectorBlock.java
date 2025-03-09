package org.jd.infestusfrontier.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jd.infestusfrontier.block.entity.CollectorBlockEntity;
import org.jd.infestusfrontier.block.entity.MutationPoolBlockEntity;
import org.jetbrains.annotations.Nullable;

public class CollectorBlock extends BaseEntityBlock {
    public CollectorBlock(Properties p) {
        super(p);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos p_153215_, BlockState p_153216_) {
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState b) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState pNewState, boolean b) {
        if (state.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CollectorBlockEntity) {
                ((CollectorBlockEntity) blockEntity).drops();
            }
        }

        super.onRemove(state, level, pos, pNewState, b);
    }
@Override
public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
    if (!pLevel.isClientSide()) {
        BlockEntity entity = pLevel.getBlockEntity(pPos);
        if(entity instanceof MutationPoolBlockEntity) {
            NetworkHooks.openScreen(((ServerPlayer)pPlayer), (MutationPoolBlockEntity)entity, pPos);
        } else {
            throw new IllegalStateException("Our Container provider is missing!");
        }
    }

    return InteractionResult.sidedSuccess(pLevel.isClientSide());
}
}
