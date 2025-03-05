package org.jd.infestusfrontier.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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

//    @Override
//    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState pNewState, boolean b) {
//        if (state.getBlock() != pNewState.getBlock()) {
//            BlockEntity blockEntity = level.getBlockEntity(pos);
//            if (blockEntity instanceof CollectorBlockEntity) {
//                ((CollectorBlockEntity) blockEntity).drops();
//            }
//        }
//
//        super.onRemove(state, level, pos, pNewState, b);
//    }
}
