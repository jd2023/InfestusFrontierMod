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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jd.infestusfrontier.logistics.api.PortSwitch;
import org.jetbrains.annotations.Nullable;

/**
 * A vein cell with two straight passages of one carrier that cross without joining (BLOCK_CATALOG T1-05). It holds
 * nothing: what enters a face of a passage leaves by the opposite face, into the next cell, organ or crossing. No
 * passage runs along {@link #AXIS}; the two faces on that axis show both lumens, and the probe on a face turns the
 * crossing so that face is one of them.
 */
public final class SeptumCrossingBlock extends Block {
    /** The axis no passage runs along. */
    static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    /** Further crossings in a row a fill passes through after the first before it gives up. */
    static final int CHAIN = 8;

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

    /**
     * The fluid side of the face {@code side}: a passage on to the far end, or null on the two faces without one. A
     * fill passes through at most {@value #CHAIN} further crossings in a row and stops at an unloaded chunk.
     */
    static @Nullable IFluidHandler fluid(Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
        Carrier carrier = state.getValue(Routes.CARRIER);
        if (side == null || !carrier.isFluid() || !passes(state, side.getAxis())) return null;
        return new Passage(carrier, () -> beyond(level, pos, side));
    }

    /** The handler a fill entering the crossing at {@code pos} through {@code entry} reaches on the far side. */
    private static @Nullable IFluidHandler beyond(Level level, BlockPos pos, Direction entry) {
        if (!level.hasChunkAt(pos)) return null;
        BlockState here = level.getBlockState(pos);
        if (!(here.getBlock() instanceof SeptumCrossingBlock) || !passes(here, entry.getAxis())) return null;
        Carrier carrier = here.getValue(Routes.CARRIER);
        Direction travel = entry.getOpposite();
        BlockPos next = pos.relative(travel);
        for (int step = 0; step <= CHAIN; step++) {
            if (!level.hasChunkAt(next)) return null;
            BlockState state = level.getBlockState(next);
            boolean onward = state.getBlock() instanceof SeptumCrossingBlock
                    && state.getValue(Routes.CARRIER) == carrier
                    && passes(state, travel.getAxis());
            if (!onward) return level.getCapability(Capabilities.FluidHandler.BLOCK, next, entry);
            next = next.relative(travel);
        }
        return null;
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
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (level.isClientSide || old == state) return;
        level.invalidateCapabilities(pos);
        if (carries(state, Carrier.ITEM) || carries(old, Carrier.ITEM)) LogisticsModule.routesChanged(level);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!level.isClientSide && !state.is(next.getBlock())) {
            level.invalidateCapabilities(pos);
            if (carries(state, Carrier.ITEM)) LogisticsModule.routesChanged(level);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    private static boolean carries(BlockState state, Carrier carrier) {
        return state.getBlock() instanceof SeptumCrossingBlock && state.getValue(Routes.CARRIER) == carrier;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(LogisticsModule.SEPTUM_CROSSING_ITEM.get());
    }
}
