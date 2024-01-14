package org.jd.infestusfrontier.block;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jd.infestusfrontier.InfestusFrontier;
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

    public static boolean isInfested(BlockPos pos, Level world) {
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
                && !isInfested(pos, world);
    }
}
