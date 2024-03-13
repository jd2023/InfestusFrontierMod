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
import net.minecraft.world.level.material.Material;
import org.jd.infestusfrontier.ZgBlockEntities;
import org.jd.infestusfrontier.ZgBlocks;
import org.jd.infestusfrontier.block.entity.InfestedVeinVaporizerEntity;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class InfestedVeinVaporizer extends BaseEntityBlock {
    public static final String ID = "infested_vein_vaporizer";

    private static final Logger LOGGER = LogUtils.getLogger();

    public InfestedVeinVaporizer() {
        super(Properties.of(Material.STONE));
    }


    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ZgBlockEntities.infested_vein_vaporizer_BLOCK_ENTITY_TYPE.get().create(pos, state);
    }
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockPos posBelow = context.getClickedPos().below();
        BlockState stateBelow = context.getLevel().getBlockState(posBelow);

        if (stateBelow.is(ZgBlocks.INFESTUS_NETWORK.get())) {
            return super.getStateForPlacement(context);
        }

        return null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> betype) {
        return createTickerHelper(betype, ZgBlockEntities.infested_vein_vaporizer_BLOCK_ENTITY_TYPE.get(), InfestedVeinVaporizerEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        LOGGER.info("Placed ExcavatorInfestusPod at {}", pos);
    }

    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof InfestedVeinVaporizerEntity blockEntity) {
                blockEntity.remove(state, (ServerLevel)level, pos);
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
}
