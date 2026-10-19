package org.jd.infestusfrontier.storage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;
import org.jd.infestusfrontier.storage.api.BiomassHandler;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** The Bladder's balance. Holds 4000 BU; the block's {@code fill} follows the content. */
final class BiomassBladderBlockEntity extends BlockEntity implements BiomassHandler, MenuProvider, GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final List<RawAnimation> FILL = IntStream.rangeClosed(0, 4)
            .mapToObj(level -> RawAnimation.begin().thenLoop("fill_" + level))
            .toList();
    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    static final Biomass CAPACITY = Biomass.of(4000);
    private static final MapCodec<BiomassTank> SAVED = RecordCodecBuilder.mapCodec(
            instance -> instance.group(BiomassCodecs.TANK.fieldOf("tank").forGetter(tank -> tank))
                    .apply(instance, tank -> tank));

    private BiomassTank tank = BiomassTank.empty(CAPACITY);
    private Optional<CompoundTag> unreadable = Optional.empty();

    BiomassBladderBlockEntity(BlockPos pos, BlockState state) {
        super(StorageModule.BIOMASS_BLADDER_ENTITY.get(), pos, state);
    }

    /**
     * The membrane breathes; the biomass inside follows the block's {@code fill}. Both snap, with no blend: a blend
     * would show the model's rest pose, a full bladder, draining to the real level on every placement and chunk load.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "membrane", 0, state -> state.setAndContinue(IDLE)));
        controllers.add(new AnimationController<>(
                this,
                "fluid",
                0,
                state -> state.setAndContinue(
                        FILL.get(state.getAnimatable().getBlockState().getValue(BiomassBladderBlock.FILL)))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }

    @Override
    public BiomassTank tank() {
        return tank;
    }

    @Override
    public Outcome<BiomassTank> insert(Biomass amount, boolean simulate) {
        return apply(tank.fill(amount), simulate);
    }

    @Override
    public Outcome<BiomassTank> extract(Biomass amount, boolean simulate) {
        return apply(tank.drain(amount), simulate);
    }

    private Outcome<BiomassTank> apply(Outcome<BiomassTank> change, boolean simulate) {
        if (unreadable.isPresent()) return Outcome.refused(Persistence.CORRUPT);
        if (!simulate && change instanceof Outcome.Done<BiomassTank> done) {
            tank = done.value();
            setChanged();
            if (level != null) {
                BlockState shown = getBlockState().setValue(BiomassBladderBlock.FILL, fillLevel(tank));
                if (shown != getBlockState()) level.setBlock(worldPosition, shown, Block.UPDATE_CLIENTS);
            }
        }
        return change;
    }

    /** Why {@code saved} cannot be this Bladder's tank, or empty when it can. */
    private static Optional<String> check(BiomassTank saved) {
        return saved.capacity().equals(CAPACITY)
                ? Optional.empty()
                : Optional.of("tank capacity " + saved.capacity().units());
    }

    /** 0 when empty, 4 when full, otherwise 1 to 3 so any content shows. */
    static int fillLevel(BiomassTank tank) {
        if (tank.stored().isZero()) return 0;
        if (tank.isFull()) return 4;
        return Math.clamp(1 + (long) tank.stored().units() * 3 / tank.capacity().units(), 1, 3);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<BiomassTank> read = Persistence.read(
                SAVED, tag, registries, StorageModule.LOGGER, "Biomass Bladder", BiomassBladderBlockEntity::check);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        read.ifPresent(saved -> tank = saved);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, tank, registries, tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent()) {
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
            return;
        }
        components.set(StorageModule.TANK.get(), tank);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            return;
        }
        BiomassTank carried = input.getOrDefault(StorageModule.TANK.get(), tank);
        Optional<String> problem = check(carried);
        if (problem.isPresent() && level != null) {
            StorageModule.LOGGER.warn("Biomass Bladder keeps unreadable carried state: {}", problem.get());
            unreadable = Optional.of(Persistence.encode(SAVED, carried, level.registryAccess()));
            return;
        }
        tank = carried;
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("tank");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.infestusfrontier.biomass_bladder");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BiomassBladderMenu(id, inventory, this);
    }
}
