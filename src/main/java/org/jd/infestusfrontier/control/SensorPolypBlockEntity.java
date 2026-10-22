package org.jd.infestusfrontier.control;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassHandler;
import org.jd.infestusfrontier.storage.api.ItemStore;

/**
 * Reads the faced block every {@value #READ_TICKS} ticks: biomass first, then any fluid, then items, as a fullness of
 * 0 to 15, 1 at least for anything at all. Three capability reads at most, in a loaded chunk. The block state carries
 * the result; a change notifies the neighbours.
 */
final class SensorPolypBlockEntity extends BlockEntity {
    static final int READ_TICKS = 10;
    private int nextRead;

    SensorPolypBlockEntity(BlockPos pos, BlockState state) {
        super(ControlModule.SENSOR_POLYP_ENTITY.get(), pos, state);
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, SensorPolypBlockEntity polyp) {
        if (--polyp.nextRead > 0) return;
        polyp.nextRead = READ_TICKS;
        int power = measure(level, pos, state.getValue(SensorPolypBlock.FACING));
        if (power == state.getValue(SensorPolypBlock.POWER)) return;
        level.setBlock(pos, state.setValue(SensorPolypBlock.POWER, power), Block.UPDATE_ALL);
    }

    /** The fullness of the block on {@code facing}, 0 when there is nothing measurable. */
    static int measure(Level level, BlockPos pos, Direction facing) {
        BlockPos target = pos.relative(facing);
        if (!level.hasChunkAt(target)) return 0;
        Direction side = facing.getOpposite();
        BiomassHandler biomass = level.getCapability(BiomassCapabilities.BLOCK, target, side);
        if (biomass != null && !biomass.tank().capacity().isZero())
            return fullness(
                    biomass.tank().stored().units(), biomass.tank().capacity().units());
        IFluidHandler fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, target, side);
        if (fluids != null && fluids.getTanks() > 0) {
            long held = 0, room = 0;
            for (int tank = 0; tank < fluids.getTanks(); tank++) {
                held += fluids.getFluidInTank(tank).getAmount();
                room += fluids.getTankCapacity(tank);
            }
            if (room > 0) return fullness(held, room);
        }
        IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, target, side);
        if (items instanceof ItemStore known)
            return known.capacity() > 0 ? fullness(known.stored(), known.capacity()) : 0;
        return items == null ? 0 : Math.clamp(ItemHandlerHelper.calcRedstoneFromInventory(items), 0, 15);
    }

    /** 0 for nothing, 1 to 15 otherwise; a store that reports more than its capacity still reads 15. */
    private static int fullness(long held, long capacity) {
        if (held <= 0) return 0;
        return Math.clamp(Mth.floor(held * 15.0 / capacity), 1, 15);
    }
}
