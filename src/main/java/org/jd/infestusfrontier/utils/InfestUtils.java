package org.jd.infestusfrontier.utils;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.ZgBlocks;
import org.jd.infestusfrontier.block.InfestusNetwork;
import org.jd.infestusfrontier.block.InfestusNetworkAdvanced;
import org.jd.infestusfrontier.block.InfestusNetworkDense;
import org.jd.infestusfrontier.block.InfestusNetworkFinal;
import org.slf4j.Logger;

public class InfestUtils {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void replaceBlockWithParticles(ServerLevel serverLevel, BlockPos pos, BlockState newBlockState) {
        serverLevel.setBlockAndUpdate(pos, newBlockState);
        pos = pos.above();
        ParticleOptions particleType = ParticleTypes.SPORE_BLOSSOM_AIR;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        int count = 10;
        double xOffset = 0.5;
        double yOffset = 0.5;
        double zOffset = 0.5;
        double speed = 0.0;
        serverLevel.sendParticles(particleType, x, y, z, count, xOffset, yOffset, zOffset, speed);
    }

    public static boolean isInfestusNetwork(BlockPos pos, Level world) {
        Block block = world.getBlockState(pos).getBlock();
        var key = ForgeRegistries.BLOCKS.getKey(block);
        return key.toString().startsWith(InfestusFrontier.MODID + ":" + InfestusNetwork.ID);
    }

    public static boolean isInfestusBlock(BlockPos pos, Level world) {
        Block block = world.getBlockState(pos).getBlock();
        var key = ForgeRegistries.BLOCKS.getKey(block);
        return key.toString().startsWith(InfestusFrontier.MODID);
    }

    public static boolean canBeInfested(BlockPos pos, Level world) {
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();

        return blockState.getMaterial().isSolidBlocking()
                && !blockState.getMaterial().isLiquid()
                && !(block instanceof BushBlock)
                && block != Blocks.AIR
                && !isInfestusBlock(pos, world)
                && !isBlockFeaturesCannotReplace(blockState)
                && !(block instanceof EntityBlock);
    }

    public static boolean isExposed(BlockPos pos, Level world) {
        return isOpen(pos.above(), world)
                || isOpen(pos.below(), world)
                || isOpen(pos.north(), world)
                || isOpen(pos.south(), world)
                || isOpen(pos.east(), world)
                || isOpen(pos.west(), world);
    }

    public static BlockState nextLevelInfesting(BlockPos pos, Level world) {
        Block block = world.getBlockState(pos).getBlock();
        var key = ForgeRegistries.BLOCKS.getKey(block);
        return switch (key.toString().split(":")[1]) {
            case InfestusNetwork.ID -> ZgBlocks.INFESTUS_NETWORK_DENSE.get().defaultBlockState();
            case InfestusNetworkDense.ID -> ZgBlocks.INFESTUS_NETWORK_ADVANCED.get().defaultBlockState();
            case InfestusNetworkAdvanced.ID -> ZgBlocks.INFESTUS_NETWORK_FINAL.get().defaultBlockState();
            case InfestusNetworkFinal.ID -> ZgBlocks.INFESTUS_NETWORK_FINAL.get().defaultBlockState();
            default -> throw new IllegalStateException("Unexpected value: " + key);
        };
    }

    private static boolean isOpen(BlockPos pos, Level world) {
        BlockState blockState = world.getBlockState(pos);
        var result = !blockState.getMaterial().isSolidBlocking();
        return result;
    }

    public static boolean isBlockFeaturesCannotReplace(BlockState state) {
        TagKey<Block> tagKey = BlockTags.create(new ResourceLocation("minecraft", "features_cannot_replace"));
        return state.is(tagKey);
    }
}
