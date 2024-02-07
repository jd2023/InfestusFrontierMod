package org.jd.infestusfrontier.block;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
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

import org.jd.infestusfrontier.block.entity.InfesterBlockEntity;
import org.jd.infestusfrontier.utils.Circle;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;

public class CorruptionCore extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final String ID = "corruption_core";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CorruptionCore() {
        super(Properties.of(Material.STONE));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ZgBlockEntities.INFESTER_BLOCK_ENTITY_TYPE.get().create(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> betype) {
        return createTickerHelper(betype, ZgBlockEntities.INFESTER_BLOCK_ENTITY_TYPE.get(), InfesterBlockEntity::tick);
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

        if (!world.isClientSide) {
            BlockPos corruption_corePos = pos.below();
            if (!InfestUtils.canBeInfested(corruption_corePos, world)) {
                return;
            }
            var someEntity = world.getBlockEntity(pos);
            if (someEntity instanceof InfesterBlockEntity entity) {
                for (int i = 0; i < 4; i++) {
                    var level = Circle.data[i];
                    var randomLevel = Arrays.asList(level);
                    Collections.shuffle(randomLevel);
                    for (int[] offset : randomLevel) {
                        var posToInfest = corruption_corePos.offset(offset[0], 0, offset[1]);
                        entity.enqueueInitialBlocks(posToInfest);
                    }
                }
            } else {
                LOGGER.error("Unexpected corruption_coreBlockEntity type: {}", someEntity);
            }
        }
    }
}
