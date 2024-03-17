package org.jd.infestusfrontier.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import org.jd.infestusfrontier.ZgBlockEntities;
import org.jd.infestusfrontier.ZgBlocks;
import org.jd.infestusfrontier.block.entity.CorruptionCoreBlockEntity;
import org.jd.infestusfrontier.block.entity.InfestusLootLeechBlockEntity;
import org.jetbrains.annotations.Nullable;

public class InfesusLootLeechBlock extends BaseEntityBlock {
    public static final String ID = "infestus_loot_leech";

    public InfesusLootLeechBlock() {
        super(Properties.of(Material.STONE));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ZgBlockEntities.LOOT_LEECH_ENTITY.get().create(pos, state);
    }
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof InfestusLootLeechBlockEntity) {
                ((InfestusLootLeechBlockEntity) blockEntity).drops();
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> be) {
        return level.isClientSide ? null : createTickerHelper(be, ZgBlockEntities.LOOT_LEECH_ENTITY.get(), InfestusLootLeechBlockEntity::pushItemsTick);
    }
}
