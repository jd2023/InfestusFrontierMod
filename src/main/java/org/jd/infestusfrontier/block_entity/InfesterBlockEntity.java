package org.jd.infestusfrontier.block_entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.ZgBlockEntities;
import org.jd.infestusfrontier.ZgBlocks;
import org.jd.infestusfrontier.block.InfestUtils;


import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.Queue;


public class InfesterBlockEntity extends BlockEntity {

    private int ticks = 0;

    private static final Logger LOGGER = LogUtils.getLogger();
    private final Queue<BlockPos> blocksToConvert = new ArrayDeque<>();

    public InfesterBlockEntity(BlockPos pos, BlockState state) {
        super(ZgBlockEntities.INFESTER_BLOCK_ENTITY_TYPE.get(), pos, state);
        LOGGER.warn("Creating InfesterBlockEntity at {}", pos);

    }


    public void enqueueInitialBlocks(BlockPos pos) {
        blocksToConvert.add(pos);
    }


    private static final int MAX_DEV = 2;

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T be) {
        InfesterBlockEntity corruption_core = (InfesterBlockEntity) be;
        if (level.isClientSide() || corruption_core.blocksToConvert.isEmpty()) return;
        corruption_core.ticks++;
        if (corruption_core.ticks % MAX_DEV == 0) {
            if (level instanceof ServerLevel serverLevel) {
                var posToInfest = corruption_core.blocksToConvert.poll();
                for (int i = -4; i < 4; i++) {
                    var mayBeInfestPos = posToInfest.offset(0, i, 0);
                    if (InfestUtils.canBeInfested(mayBeInfestPos, serverLevel) && InfestUtils.isExposed(mayBeInfestPos, serverLevel)) {
                        InfestUtils.replaceBlockWithParticles(serverLevel, mayBeInfestPos, ZgBlocks.INFESTUS_NETWORK.get().defaultBlockState());
                    }
                }
            }
        }
    }
}
