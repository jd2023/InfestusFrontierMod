package org.jd.infestusfrontier.blocks;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.ForgeRegistries;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.ZgBlocks;
import org.jd.infestusfrontier.precomp.Circle;
import org.slf4j.Logger;

public class Nest extends Block{

    public static final String ID = "nest";
    private static final Logger LOGGER = LogUtils.getLogger();
    public Nest() {
        super(Properties.of(Material.STONE));
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);

        LOGGER.info("Placed Nest at {}", pos);

        if (!world.isClientSide) {
            BlockPos nestPos = pos.below();
            if (!canBeInfested(nestPos, world)) {
                return;
            }
            for (int[][] level : Circle.data) {
                for (int[] offset : level) {
                    var posToInfest = nestPos.offset(offset[0], 0, offset[1]);
                    if (canBeInfested(posToInfest, world)) {
                        world.setBlockAndUpdate(posToInfest, ZgBlocks.CREEP.get().defaultBlockState());
                        for (int i = 1; i < 4; i++) {
                            if (!canBeInfested(posToInfest.above(i), world)) {
                                break;
                            }
                            world.setBlockAndUpdate(posToInfest.above(i), ZgBlocks.CREEP.get().defaultBlockState());
                        }
                    } else {
                        for (int i = 1; i < 4; i++) {
                            if (canBeInfested(posToInfest.below(i), world)) {
                                world.setBlockAndUpdate(posToInfest.below(i), ZgBlocks.CREEP.get().defaultBlockState());
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isInfested(BlockPos pos, Level world) {
        Block block = world.getBlockState(pos).getBlock();
        var key = ForgeRegistries.BLOCKS.getKey(block);
        LOGGER.info("Checking if {} is infested at {}. key: {}, namespace: {}", block, pos, key, key.getNamespace());
        return key.toString().startsWith(InfestusFrontier.MODID);
    }

    private static boolean canBeInfested(BlockPos pos, Level world) {
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();

        return blockState.getMaterial().isSolidBlocking()
                && !blockState.getMaterial().isLiquid()
                && !(block instanceof BushBlock)
                && block != Blocks.AIR
                && !isInfested(pos, world);
    }
}
