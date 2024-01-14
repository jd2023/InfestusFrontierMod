package org.jd.infestusfrontier.block;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.ZgBlockEntities;
import org.jd.infestusfrontier.ZgBlocks;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.Queue;


public class InfesterBlockEntity extends BlockEntity {
    private int ticks = 0;

    private static final Logger LOGGER = LogUtils.getLogger();
    private final Queue<BlockPos> blocksToConvert = new ArrayDeque<>();

    public InfesterBlockEntity(BlockPos pos, BlockState state) {
        super(ZgBlockEntities.NEST_BLOCK_ENTITY_TYPE.get(), pos, state);
        LOGGER.warn("Creating NestBlockEntity at {}", pos);
    }

    public void enqueueInitialBlocks(BlockPos pos) {
        blocksToConvert.add(pos);
    }

    private static final int MAX_DEV = 2;

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T be) {
        InfesterBlockEntity nest = (InfesterBlockEntity) be;
        if (level.isClientSide() || nest.blocksToConvert.isEmpty()) return;
        nest.ticks++;
        if (nest.ticks % MAX_DEV == 0) {
            if (level instanceof ServerLevel serverLevel) {
                var posToInfest = nest.blocksToConvert.poll();
                if (InfestUtils.canBeInfested(posToInfest, serverLevel)) {
                    InfestUtils.replaceBlockWithParticles(serverLevel, posToInfest, ZgBlocks.CREEP.get().defaultBlockState());
                }
                for (int i = -4; i < 4; i++) {
                    var mayBeInfestPos = posToInfest.offset(0, i, 0);
                    if (InfestUtils.canBeInfested(mayBeInfestPos, serverLevel) && !(
                            InfestUtils.canBeInfested(mayBeInfestPos.east(), serverLevel) &&
                                    InfestUtils.canBeInfested(mayBeInfestPos.north(), serverLevel) &&
                                    InfestUtils.canBeInfested(mayBeInfestPos.west(), serverLevel) &&
                                    InfestUtils.canBeInfested(mayBeInfestPos.south(), serverLevel)
                    )) {
                        nest.enqueueInitialBlocks(mayBeInfestPos);
                        break;
                    }
                }
            }
        }
    }
}
