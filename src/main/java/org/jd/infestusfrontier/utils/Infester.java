package org.jd.infestusfrontier.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jd.infestusfrontier.ZgBlocks;
import org.jd.infestusfrontier.block.InfestUtils;

public class Infester {
    private boolean done;
    private int height = -4;
    private int radius;
    private int idx;
    private int ticks;
    private final int rate;
    private final int maxRadius;

    public Infester(int rate, int maxRadius) {
        this.rate = rate;
        this.maxRadius = maxRadius;
    }

    public boolean infestNext(ServerLevel serverLevel, BlockPos center) {
        if (!done) {
            ticks++;
            if (ticks % rate == 0) {
                while (radius < 4) {
                    while (idx < Circle.data[radius].length) {
                        while (height < 4) {
                            var mayBeInfestPos = center.offset(Circle.data[radius][idx][0], height, Circle.data[radius][idx][1]);
                            if (InfestUtils.canBeInfested(mayBeInfestPos, serverLevel) && InfestUtils.isExposed(mayBeInfestPos, serverLevel)) {
                                InfestUtils.replaceBlockWithParticles(serverLevel, mayBeInfestPos, ZgBlocks.INFESTUS_NETWORK.get().defaultBlockState());
                            }
                            height++;
                            return done;
                        }
                        height = -4;
                        idx++;
                    }
                    idx = 0;
                    radius++;
                }
                done = true;
            }
        }
        return done;
    }
}
