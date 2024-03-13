package org.jd.infestusfrontier.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.ZgBlockEntities;
import org.jd.infestusfrontier.utils.Excavator;
import org.jd.infestusfrontier.utils.Infester;
import org.slf4j.Logger;


public class InfestedVeinVaporizerEntity extends BlockEntity {

    private Excavator excavator;
    private static final Logger LOGGER = LogUtils.getLogger();

    public InfestedVeinVaporizerEntity(BlockPos pos, BlockState state) {
        super(ZgBlockEntities.INFESTED_VEIN_VAPORIZER_BLOCK_ENTITY_TYPE.get(), pos, state);
        this.excavator = new Excavator(16, 3, -1, -16, 3);
        LOGGER.warn("Creating InfestedVeinVaporizerEntity at {}", pos);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T be) {
        if (level.isClientSide()) return;
        if (level instanceof ServerLevel serverLevel) {
            if (be instanceof InfestedVeinVaporizerEntity infestusPodBlockEntity) {
                infestusPodBlockEntity.excavator.breakNext(serverLevel, pos);
            }
        }
    }

    protected void saveAdditional(CompoundTag nbt) {
        nbt.put(Infester.TAG, excavator.serializeNBT());
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        excavator.deserializeNBT(nbt.getCompound(Infester.TAG));
    }
}
