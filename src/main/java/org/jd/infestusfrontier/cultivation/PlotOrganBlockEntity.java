package org.jd.infestusfrontier.cultivation;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;
import org.jd.infestusfrontier.storage.api.BiomassHandler;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * What the two plot organs share: a biomass tank of {@value #CAPACITY_BU} BU that pays {@value #COST_BU} BU per act,
 * item slots that stay with the block and drop when it breaks, and one try every {@value #PULSE_TICKS} ticks while
 * work is found, else every {@value #RETRY_TICKS}. A subclass does one act per try. It works only rooted on a
 * foundation.
 */
abstract class PlotOrganBlockEntity extends BlockEntity implements BiomassHandler, MenuProvider, GeoBlockEntity {
    static final int CAPACITY_BU = 1000;
    static final Biomass COST = Biomass.of(5);
    static final int PULSE_TICKS = 20;
    static final int RETRY_TICKS = 40;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WORKING = RawAnimation.begin().thenLoop("working");
    private static final int BLEND_TICKS = 5;
    private static final String ITEMS_TAG = "items";
    private static final MapCodec<BiomassTank> SAVED = RecordCodecBuilder.mapCodec(
            instance -> instance.group(BiomassCodecs.TANK.fieldOf("tank").forGetter(tank -> tank))
                    .apply(instance, tank -> tank));

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private final String name;
    protected Optional<CompoundTag> unreadable = Optional.empty();
    protected final ItemStackHandler items;
    private BiomassTank tank = BiomassTank.empty(Biomass.of(CAPACITY_BU));
    private int nextTry;

    protected PlotOrganBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, String name, int slots) {
        super(type, pos, state);
        this.name = name;
        this.items = new ItemStackHandler(slots) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return unreadable.isEmpty() && slotTakes(slot, stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    /** Whether {@code slot} may hold {@code stack}. */
    protected abstract boolean slotTakes(int slot, ItemStack stack);

    /** One act on the plot; true when something happened. The cost is paid here when it did. */
    protected abstract boolean act(ServerLevel level);

    /** Whether a held item goes into the organ instead of opening the screen. */
    boolean takesByHand(ItemStack stack) {
        return false;
    }

    /** Puts one item of {@code stack} into the organ; false when it has no place. */
    boolean insertByHand(ItemStack stack) {
        return false;
    }

    ItemStackHandler items() {
        return items;
    }

    /** True when the tank can pay one act. */
    protected boolean canPay() {
        return tank.stored().atLeast(COST);
    }

    protected void pay() {
        tank = ((Outcome.Done<BiomassTank>) tank.drain(COST)).value();
        setChanged();
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, PlotOrganBlockEntity organ) {
        if (--organ.nextTry > 0) return;
        if (organ.unreadable.isPresent() || !Organs.hasFoundation(level, pos) || !(level instanceof ServerLevel server))
            return;
        boolean worked = organ.canPay() && organ.act(server);
        organ.nextTry = worked ? PULSE_TICKS : RETRY_TICKS;
        BlockState shown = state.setValue(PlotOrganBlock.WORKING, worked);
        if (shown != state) level.setBlock(pos, shown, Block.UPDATE_CLIENTS);
    }

    void dropItems(Level level, BlockPos pos) {
        NonNullList<ItemStack> held = NonNullList.withSize(items.getSlots(), ItemStack.EMPTY);
        for (int slot = 0; slot < items.getSlots(); slot++) held.set(slot, items.getStackInSlot(slot));
        Containers.dropContents(level, pos, new SimpleContainer(held.toArray(new ItemStack[0])));
        for (int slot = 0; slot < items.getSlots(); slot++) items.setStackInSlot(slot, ItemStack.EMPTY);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "state",
                BLEND_TICKS,
                state -> state.setAndContinue(
                        state.getAnimatable().getBlockState().getValue(PlotOrganBlock.WORKING) ? WORKING : IDLE)));
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
            nextTry = Math.min(nextTry, PULSE_TICKS); // wakes a resting organ; never shortens a pulse
            setChanged();
        }
        return change;
    }

    private static Optional<String> check(BiomassTank saved) {
        return saved.capacity().units() == CAPACITY_BU
                ? Optional.empty()
                : Optional.of("tank capacity " + saved.capacity().units());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<BiomassTank> read =
                Persistence.read(SAVED, tag, registries, CultivationModule.LOGGER, name, PlotOrganBlockEntity::check);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        read.ifPresent(saved -> tank = saved);
        if (tag.contains(ITEMS_TAG)) items.deserializeNBT(registries, tag.getCompound(ITEMS_TAG));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, tank, registries, tag);
        tag.put(ITEMS_TAG, items.serializeNBT(registries));
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
        if (check(carried).isPresent()) {
            CultivationModule.LOGGER.warn(
                    "{} keeps unreadable carried state: {}",
                    name,
                    check(carried).get());
            unreadable = Optional.of(
                    level == null ? new CompoundTag() : Persistence.encode(SAVED, carried, level.registryAccess()));
            return;
        }
        tank = carried;
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("tank");
    }
}
