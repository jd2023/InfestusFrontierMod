package org.jd.infestusfrontier.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.ZgBlockEntities;
import org.jd.infestusfrontier.ZgBlocks;
import org.jd.infestusfrontier.block.InfestUtils;
import org.jd.infestusfrontier.utils.Infester;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.Queue;


public class CorruptionCoreBlockEntity extends BlockEntity {

    private Infester infester;
    private static final Logger LOGGER = LogUtils.getLogger();

    public CorruptionCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ZgBlockEntities.CORRUPTION_CORE_ENTITY_TYPE.get(), pos, state);
        this.infester = new Infester(4, 4);
        LOGGER.warn("Creating CorruptionCoreBlockEntity at {}", pos);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T be) {
        if (level.isClientSide()) return;
        if (level instanceof ServerLevel serverLevel) {
            if (be instanceof CorruptionCoreBlockEntity corruptionCoreBlockEntity) {
                corruptionCoreBlockEntity.infester.infestNext(serverLevel, pos);
            }
        }
    }
}
