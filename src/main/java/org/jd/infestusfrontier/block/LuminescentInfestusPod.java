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
import net.minecraft.world.level.material.Material;
import org.jd.infestusfrontier.ZgBlockEntities;
import org.jd.infestusfrontier.ZgBlocks;
import org.jd.infestusfrontier.block.entity.InfestusPodBlockEntity;
import org.jd.infestusfrontier.block.entity.LuminescentInfestusPodBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class LuminescentInfestusPod extends BaseEntityBlock {
    public static final String ID = "luminescent_infestus_pod";

    private static final Logger LOGGER = LogUtils.getLogger();

    public LuminescentInfestusPod() {
        super(Properties.of(Material.STONE)
                .strength(1.5f, 6.0f)
                .lightLevel(state -> 15));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ZgBlockEntities.LUMINESCENT_INFESTUS_POD_BLOCK_ENTITY_TYPE.get().create(pos, state);
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
        return createTickerHelper(betype, ZgBlockEntities.LUMINESCENT_INFESTUS_POD_BLOCK_ENTITY_TYPE.get(), LuminescentInfestusPodBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        LOGGER.info("Placed LuminescentInfestusPod at {}", pos);
    }
}
