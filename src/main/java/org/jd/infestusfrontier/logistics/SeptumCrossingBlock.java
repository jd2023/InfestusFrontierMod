package org.jd.infestusfrontier.logistics;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jd.infestusfrontier.logistics.api.PortSwitch;

/**
 * A vein cell with two straight passages of one carrier that cross without joining (BLOCK_CATALOG T1-05). It holds
 * nothing: what enters a face of a passage leaves by the opposite face, into the next cell, organ or crossing. No
 * passage runs along {@link #AXIS}; the two faces on that axis show both lumens, and the probe on a face turns the
 * crossing so that face is one of them.
 */
public final class SeptumCrossingBlock extends Block {
    /** The axis no passage runs along. */
    static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public static final MapCodec<SeptumCrossingBlock> CODEC = simpleCodec(SeptumCrossingBlock::new);

    SeptumCrossingBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any().setValue(Routes.CARRIER, Carrier.BIOMASS).setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(Routes.CARRIER, AXIS);
    }

    @Override
    protected MapCodec<SeptumCrossingBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return RotatedPillarBlock.rotatePillar(state, rotation);
    }

    /** Whether a passage of the crossing runs along {@code axis}. */
    static boolean passes(BlockState state, Direction.Axis axis) {
        return state.getValue(AXIS) != axis;
    }

    /** The probe on a face turns the crossing so that face shows both lumens. */
    static PortSwitch ports(Level level, BlockPos pos) {
        return face -> {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof SeptumCrossingBlock)) return Component.empty();
            BlockState turned = state.setValue(AXIS, face.getAxis());
            if (turned != state) level.setBlock(pos, turned, Block.UPDATE_ALL);
            return status(turned);
        };
    }

    static Component status(BlockState state) {
        return Component.translatable("infestusfrontier.status.septum_crossing."
                + state.getValue(AXIS).getSerializedName());
    }

    /** Its capabilities and its item network change with every state; NeoForge's caches and the Mouths must hear. */
    @Override
    protected void neighborChanged(
            BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moved) {
        LogisticsModule.routesChanged(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (level.isClientSide || old == state) return;
        level.invalidateCapabilities(pos);
        LogisticsModule.routesChanged(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!level.isClientSide && !state.is(next.getBlock())) {
            level.invalidateCapabilities(pos);
            LogisticsModule.routesChanged(level, pos);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(LogisticsModule.SEPTUM_CROSSING_ITEM.get());
    }
}
