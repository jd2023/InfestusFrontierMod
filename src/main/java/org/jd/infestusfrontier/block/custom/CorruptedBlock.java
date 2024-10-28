package org.jd.infestusfrontier.block.custom;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.block.InfestusBlockEntities;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class CorruptedBlock extends BaseEntityBlock {
    public static final String ID = "corrupted_block";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CorruptedBlock() {
        super(Properties.copy(Blocks.SCULK).strength(1).noLootTable());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return InfestusBlockEntities.CORRUPTION_CORE_ENTITY.get().create(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState b) {
        return RenderShape.MODEL;
    }

}
