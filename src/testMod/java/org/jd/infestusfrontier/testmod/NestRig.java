package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.storage.api.BiomassHandler;
import org.jd.infestusfrontier.vigil.VigilModule;
import org.jd.infestusfrontier.vigil.VigilNestMenu;

/** Tests use the same menu, item interaction, capability and saved state as a player or pipe. */
record NestRig(BlockEntity entity) {
    ServerLevel level() {
        return (ServerLevel) entity.getLevel();
    }

    BlockPos getBlockPos() {
        return entity.getBlockPos();
    }

    CompoundTag saved() {
        return entity.saveWithoutMetadata(level().registryAccess());
    }

    CompoundTag saveWithoutMetadata(HolderLookup.Provider registries) {
        return entity.saveWithoutMetadata(registries);
    }

    void loadWithComponents(CompoundTag tag, HolderLookup.Provider registries) {
        entity.loadWithComponents(tag, registries);
    }

    int mask() {
        return saved().getInt("mask");
    }

    int paid() {
        return saved().getInt("paid");
    }

    boolean enabled() {
        return saved().getBoolean("enabled");
    }

    boolean egg() {
        return saved().getBoolean("egg");
    }

    BiomassTank tank() {
        return ((BiomassHandler) entity).tank();
    }

    Outcome<BiomassTank> insert(Biomass amount, boolean simulate) {
        return ((BiomassHandler) entity).insert(amount, simulate);
    }

    Outcome<BiomassTank> extract(Biomass amount, boolean simulate) {
        return ((BiomassHandler) entity).extract(amount, simulate);
    }

    private VigilNestMenu menu() {
        var player = FakePlayerFactory.getMinecraft(level());
        player.setPos(Vec3.atCenterOf(getBlockPos()));
        return (VigilNestMenu) ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
    }

    void action(int button) {
        menu().clickMenuButton(FakePlayerFactory.getMinecraft(level()), button);
    }

    int stop() {
        return menu().stop();
    }

    boolean seed() {
        var player = FakePlayerFactory.getMinecraft(level());
        player.setPos(Vec3.atCenterOf(getBlockPos()));
        ItemStack egg = new ItemStack(VigilModule.EGG.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, egg);
        level().getBlockState(getBlockPos())
                .useItemOn(
                        egg,
                        level(),
                        player,
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(getBlockPos()), Direction.UP, getBlockPos(), false));
        return egg.isEmpty();
    }
}
