package org.jd.infestusfrontier.block;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import org.jd.infestusfrontier.ZgBlockEntities;
import org.jd.infestusfrontier.ZgBlocks;
import org.jd.infestusfrontier.block.entity.CorruptionCoreBlockEntity;
import org.jd.infestusfrontier.block.entity.InfestusPodBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class CorruptionCore extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final String ID = "corruption_core";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CorruptionCore() {
        super(Properties.of(Material.STONE));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ZgBlockEntities.CORRUPTION_CORE_ENTITY_TYPE.get().create(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> betype) {
        return createTickerHelper(betype, ZgBlockEntities.CORRUPTION_CORE_ENTITY_TYPE.get(), CorruptionCoreBlockEntity::tick);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockPos posBelow = context.getClickedPos().below();
        BlockState stateBelow = context.getLevel().getBlockState(posBelow);

        if (stateBelow.is(ZgBlocks.INFESTUS_NETWORK.get())) {
            return null;
        }

        return super.getStateForPlacement(context);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        LOGGER.info("Placed corruption_core at {}", pos);
    }

    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof CorruptionCoreBlockEntity blockEntity) {
                blockEntity.remove(state, (ServerLevel)level, pos);
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
}
