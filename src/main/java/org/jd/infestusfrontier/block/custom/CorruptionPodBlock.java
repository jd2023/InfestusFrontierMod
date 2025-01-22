package org.jd.infestusfrontier.block.custom;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.block.InfestusBlockEntities;
import org.jd.infestusfrontier.block.entity.CorruptionCoreBlockEntity;
import org.jd.infestusfrontier.block.entity.CorruptionPodBlockEntity;
import org.jd.infestusfrontier.block.entity.spread.InfestUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class CorruptionPodBlock extends BaseEntityBlock {
    public static final String ID = "corruption_pod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CorruptionPodBlock() {
        super(Properties.copy(Blocks.STONE).strength(1).noOcclusion());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return InfestusBlockEntities.CORRUPTION_POD_ENTITY.get().create(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState b) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> betype) {
        return createTickerHelper(betype, InfestusBlockEntities.CORRUPTION_POD_ENTITY.get(), CorruptionPodBlockEntity::tick);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        LOGGER.info("[CorruptionPodBlock] Getting state for placement {} by {}", context.getClickedPos(), context.getPlayer());
        if (!InfestUtils.isInfestusNetwork(context.getClickedPos().below(), context.getLevel())) {
            return null;
        }

        return super.getStateForPlacement(context);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {

        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof CorruptionCoreBlockEntity blockEntity) {
                blockEntity.remove(state, (ServerLevel) level, pos);
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
}
