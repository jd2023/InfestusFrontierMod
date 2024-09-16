package org.jd.infestusfrontier.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BiomassReservoirBlockEntity  extends BlockEntity {
    public BiomassReservoirBlockEntity(BlockEntityType<?> bt, BlockPos pos, BlockState state) {
        super(bt, pos, state);
    }
}
