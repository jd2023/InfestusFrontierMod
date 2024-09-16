package org.jd.infestusfrontier.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BiomassReservoirBlock extends BaseEntityBlock {
    public BiomassReservoirBlock(BlockBehaviour.Properties p) {
        super(p);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos p_153215_, BlockState p_153216_) {
        return  null;
    }

    @Override
    public RenderShape getRenderShape(BlockState b) {
        return RenderShape.MODEL;
    }
}
