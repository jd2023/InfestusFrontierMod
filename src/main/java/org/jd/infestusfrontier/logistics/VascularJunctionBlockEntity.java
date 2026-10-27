package org.jd.infestusfrontier.logistics;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.capability.wrappers.BucketPickupHandlerWrapper;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.logistics.api.PortSwitch;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.jd.infestusfrontier.storage.api.BiomassHandler;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * One 1000 mB tank locked to the fluid it holds, and the pump: every {@value #PULSE_TICKS} ticks it drains up to
 * {@value #TRANSFER_MB} from each intake neighbour and fills each outlet neighbour with as much. A water source
 * block beside an intake counts, by the vanilla bucket pickup. When a pulse moves nothing it waits
 * {@value #RETRY_TICKS} ticks. It works only rooted on a foundation.
 */
final class VascularJunctionBlockEntity extends BlockEntity
        implements BiomassHandler, MenuProvider, GeoBlockEntity, PortSwitch {
    static final int CAPACITY_MB = 1000;
    static final int TRANSFER_MB = 250;
    static final int PULSE_TICKS = 4;
    static final int RETRY_TICKS = 20;
    private static final int HOT_KELVIN = 500;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final MapCodec<SimpleFluidContent> SAVED = SimpleFluidContent.CODEC.fieldOf("fluid");

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private Optional<CompoundTag> unreadable = Optional.empty();
    private final FluidTank tank = new FluidTank(CAPACITY_MB, stack -> unreadable.isEmpty() && isCool(stack)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private int nextTry;

    VascularJunctionBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsModule.VASCULAR_JUNCTION_ENTITY.get(), pos, state);
    }

    /** A fluid cool enough for unlined tissue: water, biomass, not lava. */
    static boolean isCool(FluidStack stack) {
        FluidType type = stack.getFluidType();
        return !type.isLighterThanAir() && type.getTemperature(stack) < HOT_KELVIN;
    }

    Port port(Direction side) {
        return getBlockState().getValue(VascularJunctionBlock.PORT.get(side));
    }

    FluidTank fluid() {
        return tank;
    }

    boolean isEmpty() {
        return unreadable.isEmpty() && tank.isEmpty();
    }

    /** The tank as a neighbour on {@code side} sees it: an intake fills, an outlet drains, a closed face does nothing. */
    IFluidHandler fluidPort(@Nullable Direction side) {
        boolean fills = side == null || port(side) == Port.INTAKE;
        boolean drains = side == null || port(side) == Port.OUTLET;
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return VascularJunctionBlockEntity.this.tank.getFluid();
            }

            @Override
            public int getTankCapacity(int tank) {
                return CAPACITY_MB;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return fills && VascularJunctionBlockEntity.this.tank.isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (!fills) return 0;
                int filled = VascularJunctionBlockEntity.this.tank.fill(resource, action);
                if (filled > 0 && action.execute()) nextTry = 0;
                return filled;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return drains ? VascularJunctionBlockEntity.this.tank.drain(resource, action) : FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return drains ? VascularJunctionBlockEntity.this.tank.drain(maxDrain, action) : FluidStack.EMPTY;
            }
        };
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, VascularJunctionBlockEntity junction) {
        if (junction.unreadable.isPresent() || !Organs.hasFoundation(level, pos)) return;
        if (--junction.nextTry > 0) return;
        if (!NetworkWork.take((net.minecraft.server.level.ServerLevel) level)) {
            junction.nextTry = RETRY_TICKS + 1;
            return;
        }
        junction.nextTry = junction.pump(level, pos) ? PULSE_TICKS : RETRY_TICKS;
    }

    /** Intakes first, then outlets. At most twelve capability reads, all in loaded chunks. */
    private boolean pump(Level level, BlockPos pos) {
        boolean moved = false;
        for (Direction side : Direction.values()) if (port(side) == Port.INTAKE) moved |= pull(level, pos, side);
        for (Direction side : Direction.values()) if (port(side) == Port.OUTLET) moved |= pushOut(level, pos, side);
        return moved;
    }

    private boolean pull(Level level, BlockPos pos, Direction side) {
        BlockPos next = pos.relative(side);
        if (!level.hasChunkAt(next)) return false;
        IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK, next, side.getOpposite());
        int amount = TRANSFER_MB;
        if (source == null) {
            BlockState there = level.getBlockState(next);
            if (!(there.getBlock() instanceof BucketPickup pickup)
                    || !there.getFluidState().isSource()) return false;
            source = new BucketPickupHandlerWrapper(null, pickup, level, next);
            amount = FluidType.BUCKET_VOLUME;
        }
        return !FluidUtil.tryFluidTransfer(tank, source, amount, true).isEmpty();
    }

    private boolean pushOut(Level level, BlockPos pos, Direction side) {
        BlockPos next = pos.relative(side);
        if (!level.hasChunkAt(next)) return false;
        IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, next, side.getOpposite());
        if (target == null) return false;
        return !FluidUtil.tryFluidTransfer(target, tank, TRANSFER_MB, true).isEmpty();
    }

    @Override
    public Component cycle(Direction face) {
        Port next = port(face).next();
        if (level != null) {
            level.setBlock(
                    worldPosition,
                    getBlockState().setValue(VascularJunctionBlock.PORT.get(face), next),
                    Block.UPDATE_ALL);
            nextTry = 0;
        }
        return Component.translatable(
                "infestusfrontier.status.port",
                Component.translatable("infestusfrontier.side." + face.getSerializedName()),
                Component.translatable("infestusfrontier.port." + next.getSerializedName()));
    }

    /** Each mouth plays the animation of its port; the hub breathes. */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "hub", 0, state -> state.setAndContinue(IDLE)));
        for (Direction side : Direction.values()) {
            String name = side.getSerializedName();
            controllers.add(new AnimationController<>(
                    this,
                    name,
                    0,
                    state -> state.setAndContinue(RawAnimation.begin()
                            .thenLoop(name + "_"
                                    + state.getAnimatable().port(side).getSerializedName()))));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }

    /** The biomass view for the probe and the screen: the tank when it holds biomass or nothing, else no room. */
    @Override
    public BiomassTank tank() {
        boolean biomass = tank.isEmpty() || BiomassFluids.isBiomass(tank.getFluid());
        return new BiomassTank(
                Biomass.of(biomass ? tank.getFluidAmount() : 0), Biomass.ZERO, Biomass.of(biomass ? CAPACITY_MB : 0));
    }

    @Override
    public Outcome<BiomassTank> insert(Biomass amount, boolean simulate) {
        if (unreadable.isPresent()) return Outcome.refused(Persistence.CORRUPT);
        if (!tank.isEmpty() && !BiomassFluids.isBiomass(tank.getFluid()))
            return Outcome.refused(BiomassFluids.WRONG_FLUID);
        if (tank.fill(BiomassFluids.of(amount), IFluidHandler.FluidAction.SIMULATE) < amount.units())
            return Outcome.refused(BiomassTank.FULL);
        if (!simulate) tank.fill(BiomassFluids.of(amount), IFluidHandler.FluidAction.EXECUTE);
        return Outcome.done(tank());
    }

    @Override
    public Outcome<BiomassTank> extract(Biomass amount, boolean simulate) {
        if (unreadable.isPresent()) return Outcome.refused(Persistence.CORRUPT);
        if (!BiomassFluids.isBiomass(tank.getFluid()) || tank.getFluidAmount() < amount.units())
            return Outcome.refused(BiomassTank.EMPTY);
        if (!simulate) tank.drain(amount.units(), IFluidHandler.FluidAction.EXECUTE);
        return Outcome.done(tank());
    }

    private static Optional<String> check(SimpleFluidContent saved) {
        if (saved.getAmount() > CAPACITY_MB) return Optional.of(saved.getAmount() + " mB in a 1000 mB tank");
        if (!saved.isEmpty() && !isCool(saved.copy())) return Optional.of("a hot fluid");
        return Optional.empty();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<SimpleFluidContent> read = Persistence.read(
                SAVED,
                tag,
                registries,
                LogisticsModule.LOGGER,
                "Vascular Junction",
                VascularJunctionBlockEntity::check);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        read.ifPresent(saved -> tank.setFluid(saved.copy()));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, SimpleFluidContent.copyOf(tank.getFluid()), registries, tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent()) {
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
            return;
        }
        if (!tank.isEmpty())
            components.set(LogisticsModule.FLUID_CONTENT.get(), SimpleFluidContent.copyOf(tank.getFluid()));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            return;
        }
        SimpleFluidContent carried = input.get(LogisticsModule.FLUID_CONTENT.get());
        if (carried == null) return;
        Optional<String> problem = check(carried);
        if (problem.isPresent()) {
            LogisticsModule.LOGGER.warn("Vascular Junction keeps unreadable carried state: {}", problem.get());
            unreadable = Optional.of(
                    level == null ? new CompoundTag() : Persistence.encode(SAVED, carried, level.registryAccess()));
            return;
        }
        tank.setFluid(carried.copy());
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("fluid");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.infestusfrontier.vascular_junction");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new VascularJunctionMenu(id, inventory, this);
    }
}
