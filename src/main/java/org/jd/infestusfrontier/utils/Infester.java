package org.jd.infestusfrontier.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.ZgBlocks;

public class Infester {
    public static final String TAG = "infester";
    private boolean done;
    private int radius;
    private int idx;
    private int ticks;
    private int height;
    private final int rate;
    private final int maxRadius;
    private final int maxHeight;
    private final boolean breakBlocks;
    private final boolean cutTrees;

    public Infester(int rate, int maxRadius) {
        this(rate, maxRadius, 4, false, false);
    }
    public Infester(int rate, int maxRadius, int maxHeight, boolean breakBlocks, boolean cutTrees) {
        this.rate = rate;
        this.maxRadius = maxRadius;
        this.maxHeight = maxHeight;
        this.height = -maxHeight;
        this.breakBlocks = breakBlocks;
        this.cutTrees = cutTrees;
    }

    public boolean infestNext(ServerLevel serverLevel, BlockPos center) {
        if (!done) {
            ticks++;
            if (ticks % rate == 0) {
                while (radius < maxRadius) {
                    while (idx < Circle.data[radius].length) {
                        while (height < maxHeight) {
                            var mayBeInfestPos = center.offset(Circle.data[radius][idx][0], height, Circle.data[radius][idx][1]);
                            if (InfestUtils.isInfestusNetwork(mayBeInfestPos, serverLevel) && !this.breakBlocks) {
                                InfestUtils.replaceBlockWithParticles(serverLevel, mayBeInfestPos, InfestUtils.nextLevelInfesting(mayBeInfestPos, serverLevel));
                            } else if (InfestUtils.canBeInfested(mayBeInfestPos, serverLevel) && (this.breakBlocks || InfestUtils.isExposed(mayBeInfestPos, serverLevel))) {
                                if (this.breakBlocks) {
                                    BlockState blockState = serverLevel.getBlockState(mayBeInfestPos);
                                    Block block = blockState.getBlock();
                                    if (block.getCloneItemStack(serverLevel, mayBeInfestPos, blockState) != ItemStack.EMPTY) {
                                        BlockEntity blockEntity = blockState.hasBlockEntity() ? serverLevel.getBlockEntity(mayBeInfestPos) : null;
                                        Block.dropResources(blockState, serverLevel, center.above(), blockEntity); // Drops items at the block's location
                                        block.popExperience(serverLevel, center, block.getExpDrop(blockState, serverLevel, serverLevel.random, mayBeInfestPos, 0, 0));
                                    }
                                }
                                InfestUtils.replaceBlockWithParticles(serverLevel, mayBeInfestPos, ZgBlocks.INFESTUS_NETWORK.get().defaultBlockState());
                            }
                            height++;
                            return done;
                        }
                        height = -maxHeight;
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

    public CompoundTag serializeNBT() {
        CompoundTag inventory = new CompoundTag();
        inventory.putInt("radius", radius);
        inventory.putInt("idx", idx);
        inventory.putInt("height", height);
        inventory.putInt("ticks", ticks);
        inventory.putBoolean("done", done);
        return inventory;
    }

    public void deserializeNBT(CompoundTag nbt) {
        radius = nbt.getInt("radius");
        idx = nbt.getInt("idx");
        height = nbt.getInt("height");
        ticks = nbt.getInt("ticks");
        done = nbt.getBoolean("done");
    }
}
