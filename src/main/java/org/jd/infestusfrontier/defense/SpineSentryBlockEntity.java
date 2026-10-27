package org.jd.infestusfrontier.defense;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jd.infestusfrontier.core.defense.Sentry;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;
import org.jd.infestusfrontier.storage.api.BiomassHandler;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The Spine Sentry (BLOCK_CATALOG T2-30): rooted on a foundation, every {@link Sentry#PULSE_TICKS} it strikes the
 * nearest hostile it can see within {@link Sentry#REACH}, for {@link Sentry#DAMAGE}, paying {@link Sentry#COST} BU
 * from a tank of {@link Sentry#CAPACITY}; what its kills drop goes into its store of {@link Sentry#STORE_SLOTS}
 * slots, which hoppers, Mouths and Yield Lobes take from and nothing fills. It rests while the tank cannot pay or
 * the store has no empty slot, looking again every {@link Sentry#RETRY_TICKS}. Players and anything that is not a
 * hostile are never struck.
 */
final class SpineSentryBlockEntity extends BlockEntity implements BiomassHandler, MenuProvider, GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WORKING = RawAnimation.begin().thenLoop("working");
    private static final int BLEND_TICKS = 3;
    private static final String ITEMS_TAG = "items";
    private static final String NAME = "Spine Sentry";
    private static final MapCodec<BiomassTank> SAVED = RecordCodecBuilder.mapCodec(
            instance -> instance.group(BiomassCodecs.TANK.fieldOf("tank").forGetter(tank -> tank))
                    .apply(instance, tank -> tank));

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private Optional<CompoundTag> unreadable = Optional.empty();
    private final ItemStackHandler store = new ItemStackHandler(Sentry.STORE_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private BiomassTank tank = BiomassTank.empty(Biomass.of(Sentry.CAPACITY));
    private int nextTry;

    SpineSentryBlockEntity(BlockPos pos, BlockState state) {
        super(DefenseModule.SPINE_SENTRY_ENTITY.get(), pos, state);
    }

    /** The store as the world sees it: taken from, never filled. */
    IItemHandler output() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return store.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return store.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return unreadable.isPresent() ? ItemStack.EMPTY : store.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return store.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return false;
            }
        };
    }

    ItemStackHandler store() {
        return store;
    }

    int emptySlots() {
        int empty = 0;
        for (int slot = 0; slot < store.getSlots(); slot++)
            if (store.getStackInSlot(slot).isEmpty()) empty++;
        return empty;
    }

    /**
     * Keeps what a kill of this sentry dropped: each stack goes into the store as far as it fits; what does not fit
     * is handed back to fall as it would have.
     */
    ItemStack keep(ItemStack dropped) {
        if (unreadable.isPresent()) return dropped;
        ItemStack left = ItemHandlerHelper.insertItemStacked(store, dropped, false);
        if (left.getCount() != dropped.getCount()) setChanged();
        return left;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, SpineSentryBlockEntity sentry) {
        if (sentry.nextTry > 0 && --sentry.nextTry > 0) return;
        boolean struck = false;
        if (sentry.unreadable.isEmpty()
                && Organs.hasFoundation(level, pos)
                && level instanceof ServerLevel server
                && Sentry.works(sentry.tank.stored().units(), sentry.emptySlots())) struck = sentry.strike(server);
        sentry.nextTry = struck ? Sentry.PULSE_TICKS : Sentry.RETRY_TICKS;
        BlockState shown = state.setValue(SpineSentryBlock.WORKING, struck);
        if (shown != state) level.setBlock(pos, shown, Block.UPDATE_CLIENTS);
    }

    /** One strike at the nearest hostile in sight; true when one was struck and paid for. */
    private boolean strike(ServerLevel level) {
        Vec3 eye = eye();
        Optional<LivingEntity> prey =
                level.getEntitiesOfClass(LivingEntity.class, reach(), entity -> targets(level, entity, eye)).stream()
                        .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(eye)));
        if (prey.isEmpty()) return false;
        LivingEntity target = prey.get();
        DamageSource spine = new DamageSource(
                level.damageSources().source(DefenseModule.SPINE).typeHolder(), Vec3.atCenterOf(worldPosition));
        if (!target.hurt(spine, Sentry.DAMAGE)) return false;
        tank = ((Outcome.Done<BiomassTank>) tank.drain(Biomass.of(Sentry.COST))).value();
        setChanged();
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.5), target.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, worldPosition, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.BLOCKS, 1.0F, 0.7F);
        return true;
    }

    /** Where the lash looks from: just over the block, above its own shape, where the coiled spine stands. */
    private Vec3 eye() {
        return Vec3.atBottomCenterOf(worldPosition).add(0, 15.5 / 16.0, 0);
    }

    private AABB reach() {
        return new AABB(worldPosition).inflate(Sentry.REACH, Sentry.RISE, Sentry.REACH);
    }

    /** A hostile, alive and not invulnerable, in reach, whose eyes the socket can see through loaded blocks. */
    private boolean targets(ServerLevel level, LivingEntity entity, Vec3 eye) {
        if (!(entity instanceof Enemy)
                || entity instanceof Player
                || !entity.isAlive()
                || entity.isInvulnerable()
                || !EntitySelector.NO_SPECTATORS.test(entity)) return false;
        Vec3 at = entity.position();
        Vec3 centre = Vec3.atCenterOf(worldPosition);
        if (!Sentry.inReach(at.x - centre.x, at.y - centre.y, at.z - centre.z)) return false;
        if (!level.hasChunkAt(entity.blockPosition())) return false;
        HitResult sight = level.clip(new ClipContext(
                eye,
                entity.getEyePosition(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()));
        return sight.getType() == HitResult.Type.MISS;
    }

    void dropItems(Level level, BlockPos pos) {
        NonNullList<ItemStack> held = NonNullList.withSize(store.getSlots(), ItemStack.EMPTY);
        for (int slot = 0; slot < store.getSlots(); slot++) held.set(slot, store.getStackInSlot(slot));
        Containers.dropContents(level, pos, new SimpleContainer(held.toArray(new ItemStack[0])));
        for (int slot = 0; slot < store.getSlots(); slot++) store.setStackInSlot(slot, ItemStack.EMPTY);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "state",
                BLEND_TICKS,
                state -> state.setAndContinue(
                        state.getAnimatable().getBlockState().getValue(SpineSentryBlock.WORKING) ? WORKING : IDLE)));
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

    /** Biomass arriving wakes a resting sentry within one pulse and never shortens a pulse counting down. */
    private Outcome<BiomassTank> apply(Outcome<BiomassTank> change, boolean simulate) {
        if (unreadable.isPresent()) return Outcome.refused(Persistence.CORRUPT);
        if (!simulate && change instanceof Outcome.Done<BiomassTank> done) {
            tank = done.value();
            nextTry = Math.min(nextTry, Sentry.PULSE_TICKS);
            setChanged();
        }
        return change;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.infestusfrontier.spine_sentry");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SpineSentryMenu(id, inventory, this);
    }

    private static Optional<String> check(BiomassTank saved) {
        return saved.capacity().units() == Sentry.CAPACITY
                ? Optional.empty()
                : Optional.of("tank capacity " + saved.capacity().units());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<BiomassTank> read =
                Persistence.read(SAVED, tag, registries, DefenseModule.LOGGER, NAME, SpineSentryBlockEntity::check);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        read.ifPresent(saved -> tank = saved);
        if (tag.contains(ITEMS_TAG)) store.deserializeNBT(registries, tag.getCompound(ITEMS_TAG));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, tank, registries, tag);
        tag.put(ITEMS_TAG, store.serializeNBT(registries));
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
            DefenseModule.LOGGER.warn(
                    "{} keeps unreadable carried state: {}",
                    NAME,
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
