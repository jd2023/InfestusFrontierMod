package org.jd.infestusfrontier.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolActions;
import org.jd.infestusfrontier.ZgBlocks;

public class Excavator {
    public static final String TAG = "excavator";
    private boolean done;
    private int radius;
    private int idx;
    private int ticks;
    private int height;
    private final int rate;
    private final int maxRadius;
    private final int maxHeight;
    private final int minHeight;
    private final int pickaxeLevel;

    public Excavator(int rate, int maxRadius, int maxHeight, int minHeight, int pickaxeLevel) {
        this.rate = rate;
        this.maxRadius = maxRadius;
        this.maxHeight = maxHeight;
        this.height = maxHeight;
        this.minHeight = minHeight;
        this.pickaxeLevel = pickaxeLevel;
    }

    public boolean breakNext(ServerLevel serverLevel, BlockPos center) {
        if (!done) {
            ticks++;
            if (ticks % rate == 0) {
                while (radius < maxRadius) {
                    while (idx < Circle.data[radius].length) {
                        while (height > minHeight) {
                            var breakingPos = center.offset(Circle.data[radius][idx][0], height, Circle.data[radius][idx][1]);
                            BlockState blockState = serverLevel.getBlockState(breakingPos);
                            Block block = blockState.getBlock();
                            if (canBreak(breakingPos, blockState, serverLevel, pickaxeLevel)) {
                                if (block.getCloneItemStack(serverLevel, breakingPos, blockState) != ItemStack.EMPTY) {
                                    BlockEntity blockEntity = blockState.hasBlockEntity() ? serverLevel.getBlockEntity(breakingPos) : null;
                                    Block.dropResources(blockState, serverLevel, center.above(), blockEntity);
                                    block.popExperience(serverLevel, center, block.getExpDrop(blockState, serverLevel, serverLevel.random, breakingPos, 0, 0));
                                }
                                InfestUtils.replaceBlockWithParticles(serverLevel, breakingPos, ZgBlocks.EXCAVATED.get().defaultBlockState());
                            }
                            height--;
                            return done;
                        }
                        height = maxHeight;
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

    private boolean canBreak(BlockPos pos, BlockState state, ServerLevel level, int pickaxeLevel) {
        if (!state.requiresCorrectToolForDrops()) {
            return true; // The block does not require a specific tool to be dropped
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness == -1.0F) {
            return false;
        }
        if (hardness < pickaxeLevel ) {
            return true;
        }
        return false;
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
