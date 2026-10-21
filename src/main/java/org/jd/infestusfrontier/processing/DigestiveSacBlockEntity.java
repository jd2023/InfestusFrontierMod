package org.jd.infestusfrontier.processing;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.foundation.RetryGate;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.core.processing.Batch;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.OrganCodecs;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.processing.api.DigestionRecipe;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;
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
 * One batch at a time. Feeding reserves the whole yield in the 1000 BU tank before the item is taken, so completion
 * can never be refused. Work advances only while loaded and rooted on a foundation. A finished batch flows into any
 * fluid tank touching the Sac, such as a Biomass Bladder; the rest leaves by bucket.
 *
 * <p>The feed slot holds one stack of digestible items, filled by hand through the screen or by any item pipe or
 * hopper through the item capability. Whenever the Sac is idle it takes one item from it. The slot's content stays
 * with the block and drops when the Sac is broken; it is not carried on the item.
 */
final class DigestiveSacBlockEntity extends BlockEntity implements BiomassHandler, GeoBlockEntity, MenuProvider {

    private static final String FEED_TAG = "feed";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WORKING = RawAnimation.begin().thenLoop("working");
    private static final int BLEND_TICKS = 5;
    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    static final Biomass CAPACITY = Biomass.of(1000);
    static final Refusal BUSY = new Refusal("processing.busy");
    static final Refusal NOT_ACCEPTED = new Refusal("processing.not_accepted");
    private static final MapCodec<Saved> SAVED = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BiomassCodecs.TANK.fieldOf("tank").forGetter(Saved::tank),
                    OrganCodecs.HISTORY.fieldOf("history").forGetter(Saved::history),
                    DigestionBatch.CODEC.optionalFieldOf("batch").forGetter(Saved::batch))
            .apply(instance, Saved::new));

    private BiomassTank tank = BiomassTank.empty(CAPACITY);
    private OrganHistory history = OrganHistory.NONE;
    private Optional<DigestionBatch> batch = Optional.empty();
    private Optional<CompoundTag> unreadable = Optional.empty();
    /** A refused feed slot is retried when the tank or the slot changed, else once every {@value #RETRY_TICKS} ticks. */
    private static final int RETRY_TICKS = 20;

    private final RetryGate<FeedState> feedGate = new RetryGate<>();

    private final ItemStackHandler feed = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return unreadable.isEmpty() && recipeFor(stack).isPresent();
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    DigestiveSacBlockEntity(BlockPos pos, BlockState state) {
        super(ProcessingModule.DIGESTIVE_SAC_ENTITY.get(), pos, state);
    }

    /** The feed slot, for the screen. */
    ItemStackHandler feed() {
        return feed;
    }

    /** The feed slot as hoppers, pipes and Mouths see it: they put feed in and never take it out. */
    IItemHandler port() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return feed.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return feed.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return feed.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return feed.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return feed.isItemValid(slot, stack);
            }
        };
    }

    int percent() {
        return batch.map(b -> b.batch().percent()).orElse(0);
    }

    private Optional<RecipeHolder<DigestionRecipe>> recipeFor(ItemStack stack) {
        if (level == null || stack.isEmpty()) return Optional.empty();
        return level.getRecipeManager()
                .getRecipeFor(ProcessingModule.DIGESTION.get(), new SingleRecipeInput(stack), level);
    }

    private record Saved(BiomassTank tank, OrganHistory history, Optional<DigestionBatch> batch) {
        /** Why this state cannot be installed as a whole, or empty when it fits together. */
        Optional<String> check() {
            if (!tank.capacity().equals(CAPACITY))
                return Optional.of("tank capacity " + tank.capacity().units());
            Biomass promised = batch.map(DigestionBatch::yield).orElse(Biomass.ZERO);
            if (!tank.reserved().equals(promised))
                return Optional.of("reserved " + tank.reserved().units() + " but batch promises " + promised.units());
            if (batch.isPresent() && history.completedBatches() == Integer.MAX_VALUE)
                return Optional.of("a running batch that history cannot count");
            return Optional.empty();
        }
    }

    /** The client plays {@code working} while the block state says so; nothing else is sent for it. */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "state",
                BLEND_TICKS,
                state -> state.setAndContinue(
                        state.getAnimatable().getBlockState().getValue(DigestiveSacBlock.WORKING) ? WORKING : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }

    /** Mirrors whether a batch exists into the block state, so every client can see the Sac work. */
    private void showWorking(boolean working) {
        if (level == null) return;
        BlockState shown = getBlockState().setValue(DigestiveSacBlock.WORKING, working);
        if (shown != getBlockState()) level.setBlock(worldPosition, shown, Block.UPDATE_CLIENTS);
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, DigestiveSacBlockEntity sac) {
        if (sac.unreadable.isPresent() || !Organs.hasFoundation(level, pos)) return;
        if (sac.batch.isEmpty()) {
            sac.takeFromFeedSlot();
            return;
        }
        sac.batch = OrganWork.tick(sac.batch, finished -> {
            sac.tank = sac.tank.commit(finished.yield());
            sac.history = sac.history.completed();
            sac.showWorking(false);
            OrganWork.completed(level, ProcessingModule.DIGESTIVE_SAC.getId(), finished);
            sac.pushToNeighbors(level, pos);
            return true;
        });
        sac.setChanged();
    }

    /** Starts a batch from the held item when the Sac is idle and the whole yield fits. Refusals change nothing. */
    ItemInteractionResult feed(Player player, InteractionHand hand, ItemStack stack) {
        Optional<RecipeHolder<DigestionRecipe>> recipe = recipeFor(stack);
        if (recipe.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        Optional<Refusal> refusal = refusalFor(recipe.get().value().biomass());
        if (refusal.isPresent()) {
            Refusals.show(player, refusal.get());
            return ItemInteractionResult.FAIL;
        }
        start(recipe.get(), Optional.of(player.getUUID()));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return ItemInteractionResult.CONSUME;
    }

    /** One item from the feed slot starts a batch when the Sac is idle and the yield fits; otherwise it waits. */
    private void takeFromFeedSlot() {
        ItemStack stack = feed.getStackInSlot(0);
        if (stack.isEmpty()) return;
        FeedState now = new FeedState(tank, stack.getItem(), stack.getCount());
        if (!feedGate.due(now) && level.getGameTime() % RETRY_TICKS != 0) return;
        Optional<RecipeHolder<DigestionRecipe>> recipe = recipeFor(stack);
        if (recipe.isEmpty() || refusalFor(recipe.get().value().biomass()).isPresent()) {
            feedGate.refused(now);
            return;
        }
        feedGate.clear();
        start(recipe.get(), Optional.empty());
        feed.extractItem(0, 1, false);
    }

    /** What an admission attempt from the feed slot depends on; the gate retries only when it changed. */
    private record FeedState(BiomassTank tank, Item item, int count) {}

    private void start(RecipeHolder<DigestionRecipe> recipe, Optional<UUID> feeder) {
        DigestionRecipe value = recipe.value();
        tank = ((Outcome.Done<BiomassTank>) tank.reserve(value.biomass())).value();
        batch = Optional.of(
                new DigestionBatch(Batch.start(recipe.id().toString(), value.ticks()), value.biomass(), feeder));
        showWorking(true);
        setChanged();
    }

    /** Spills the feed slot when the Sac is broken. */
    void dropFeed(Level level, BlockPos pos) {
        Containers.dropContents(level, pos, new SimpleContainer(feed.getStackInSlot(0)));
        feed.setStackInSlot(0, ItemStack.EMPTY);
    }

    /** Offers the stored biomass to the six touching blocks, once, in a fixed order. Six capability reads at most. */
    private void pushToNeighbors(Level level, BlockPos pos) {
        IFluidHandler own = BiomassFluids.handler(this);
        for (Direction side : Direction.values()) {
            if (tank.stored().isZero()) return;
            BlockPos next = pos.relative(side);
            if (!level.hasChunkAt(next)) continue;
            IFluidHandler neighbor = level.getCapability(Capabilities.FluidHandler.BLOCK, next, side.getOpposite());
            if (neighbor != null)
                FluidUtil.tryFluidTransfer(neighbor, own, tank.stored().units(), true);
        }
    }

    private Optional<Refusal> refusalFor(Biomass yield) {
        if (unreadable.isPresent()) return Optional.of(Persistence.CORRUPT);
        if (batch.isPresent()) return Optional.of(BUSY);
        if (!tank.room().atLeast(yield)) return Optional.of(BiomassTank.FULL);
        return Optional.empty();
    }

    Optional<UUID> feeder() {
        return batch.flatMap(DigestionBatch::feeder);
    }

    @Override
    public BiomassTank tank() {
        return tank;
    }

    @Override
    public Outcome<BiomassTank> insert(Biomass amount, boolean simulate) {
        return Outcome.refused(NOT_ACCEPTED);
    }

    @Override
    public Outcome<BiomassTank> extract(Biomass amount, boolean simulate) {
        if (unreadable.isPresent()) return Outcome.refused(Persistence.CORRUPT);
        Outcome<BiomassTank> drained = tank.drain(amount);
        if (!simulate && drained instanceof Outcome.Done<BiomassTank> done) {
            tank = done.value();
            setChanged();
        }
        return drained;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<Saved> read =
                Persistence.read(SAVED, tag, registries, ProcessingModule.LOGGER, "Digestive Sac", Saved::check);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        read.ifPresent(saved -> {
            tank = saved.tank();
            history = saved.history();
            batch = saved.batch();
        });
        if (tag.contains(FEED_TAG)) feed.deserializeNBT(registries, tag.getCompound(FEED_TAG));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, new Saved(tank, history, batch), registries, tag);
        tag.put(FEED_TAG, feed.serializeNBT(registries));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent()) {
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
            return;
        }
        components.set(StorageModule.TANK.get(), tank);
        components.set(OrganModule.HISTORY.get(), history);
        batch.ifPresent(running -> components.set(ProcessingModule.DIGESTION_BATCH.get(), running));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            return;
        }
        Saved carried = new Saved(
                input.getOrDefault(StorageModule.TANK.get(), tank),
                input.getOrDefault(OrganModule.HISTORY.get(), history),
                Optional.ofNullable(input.get(ProcessingModule.DIGESTION_BATCH.get())));
        Optional<String> problem = carried.check();
        if (problem.isPresent()) {
            ProcessingModule.LOGGER.warn("Digestive Sac keeps unreadable carried state: {}", problem.get());
            unreadable = Optional.of(
                    level == null ? new CompoundTag() : Persistence.encode(SAVED, carried, level.registryAccess()));
            return;
        }
        tank = carried.tank();
        history = carried.history();
        batch = carried.batch();
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("tank");
        tag.remove("history");
        tag.remove("batch");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.infestusfrontier.digestive_sac");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new DigestiveSacMenu(id, inventory, this);
    }
}
