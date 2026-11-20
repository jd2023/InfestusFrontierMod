package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.power.PowerStore;
import org.jd.infestusfrontier.power.PowerMenu;
import org.jd.infestusfrontier.power.api.SteamDrive;

record PowerRig(BlockEntity entity) {
    ServerLevel getLevel() {
        return (ServerLevel) entity.getLevel();
    }

    BlockPos getBlockPos() {
        return entity.getBlockPos();
    }

    CompoundTag saved() {
        return entity.saveWithoutMetadata(getLevel().registryAccess());
    }

    CompoundTag saveWithoutMetadata(HolderLookup.Provider registries) {
        return entity.saveWithoutMetadata(registries);
    }

    void loadWithComponents(CompoundTag tag, HolderLookup.Provider registries) {
        entity.loadWithComponents(tag, registries);
    }

    PowerStore store() {
        CompoundTag value = saved().getCompound("power");
        return new PowerStore(
                value.getInt("heat"),
                value.getInt("water"),
                value.getInt("steam"),
                value.getInt("charge"),
                value.getInt("cycles"));
    }

    record Snapshot(int ticks) {}

    Snapshot contents() {
        return new Snapshot(saved().getInt("ticks"));
    }

    IItemHandlerModifiable items() {
        return (IItemHandlerModifiable)
                getLevel().getCapability(Capabilities.ItemHandler.BLOCK, getBlockPos(), Direction.UP);
    }

    IFluidHandler fluids(boolean water) {
        return getLevel()
                .getCapability(
                        Capabilities.FluidHandler.BLOCK, water ? getBlockPos().west() : getBlockPos(), Direction.UP);
    }

    Outcome<Integer> pay(boolean simulate) {
        return getLevel()
                .getCapability(SteamDrive.BLOCK, getBlockPos(), Direction.UP)
                .pay(simulate);
    }

    private PowerMenu menu() {
        var player = FakePlayerFactory.getMinecraft(getLevel());
        player.setPos(Vec3.atCenterOf(getBlockPos()));
        return (PowerMenu) ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
    }

    void toggle() {
        menu().clickMenuButton(FakePlayerFactory.getMinecraft(getLevel()), 0);
    }

    int stop() {
        return menu().stop();
    }
}
