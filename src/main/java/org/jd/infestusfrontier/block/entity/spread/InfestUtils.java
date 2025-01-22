package org.jd.infestusfrontier.block.entity.spread;

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
import org.jd.infestusfrontier.block.InfestusBlocks;
import org.jd.infestusfrontier.block.custom.network.*;
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
        var state = world.getBlockState(pos);

        Block block = world.getBlockState(pos).getBlock();

        var key = ForgeRegistries.BLOCKS.getKey(block);
        return key.toString().startsWith(InfestusFrontier.MODID + ":corrupted_block_");
    }

    public static boolean isInfestusBlock(BlockPos pos, Level world) {
        Block block = world.getBlockState(pos).getBlock();
        var key = ForgeRegistries.BLOCKS.getKey(block);
        return key.toString().startsWith(InfestusFrontier.MODID);
    }

    public static boolean canBeInfested(BlockPos pos, Level world) {
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();

        // Early exits for common disqualifiers
        if (!blockState.isSolidRender(world, pos)) return false; // Not a solid block
        if (!blockState.getFluidState().isEmpty()) return false; // Is a liquid
        if (block instanceof BushBlock || block instanceof EntityBlock) return false; // Excludes bushes and entity blocks
        if (block == Blocks.AIR) return false; // Excludes air
        if (isInfestusBlock(pos, world)) return false; // Custom check for infestus blocks
        if (isBlockFeaturesCannotReplace(blockState)) return false; // Custom check for replaceable blocks

        // If none of the conditions above were met, the block can be infested
        return true;
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
            case DeadCorruptedBlock.ID -> InfestusBlocks.BASIC_CORRUPTED_BLOCK.get().defaultBlockState();
            case BasicCorruptedBlock.ID -> InfestusBlocks.DENSE_CORRUPTED_BLOCK.get().defaultBlockState();
            case DenseCorruptedBlock.ID -> InfestusBlocks.ADVANCE_CORRUPTED_BLOCK.get().defaultBlockState();
            case AdvanceCorruptedBlock.ID, FinalCorruptedBlock.ID -> InfestusBlocks.FINAL_CORRUPTED_BLOCK.get().defaultBlockState();
            default -> throw new IllegalStateException("Unexpected value: " + key);
        };
    }

    private static boolean isOpen(BlockPos pos, Level world) {
        BlockState blockState = world.getBlockState(pos);
        return !blockState.isSolidRender(world, pos);
    }

    public static boolean isBlockFeaturesCannotReplace(BlockState state) {
        TagKey<Block> tagKey = BlockTags.create(new ResourceLocation("minecraft", "features_cannot_replace"));
        return state.is(tagKey);
    }

    public static BlockState previousLevelInfesting(BlockPos pos, ServerLevel serverLevel) {
        Block block = serverLevel.getBlockState(pos).getBlock();
        var key = ForgeRegistries.BLOCKS.getKey(block);
        return switch (key.toString().split(":")[1]) {
            case FinalCorruptedBlock.ID -> InfestusBlocks.FINAL_CORRUPTED_BLOCK.get().defaultBlockState();
            case AdvanceCorruptedBlock.ID -> InfestusBlocks.DENSE_CORRUPTED_BLOCK.get().defaultBlockState();
            case DenseCorruptedBlock.ID -> InfestusBlocks.BASIC_CORRUPTED_BLOCK.get().defaultBlockState();
            case BasicCorruptedBlock.ID, DeadCorruptedBlock.ID -> InfestusBlocks.DEAD_CORRUPTED_BLOCK.get().defaultBlockState();
            default -> throw new IllegalStateException("Unexpected value: " + key);
        };
    }
}
