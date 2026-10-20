package org.jd.infestusfrontier.processing;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.core.processing.Batch;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.processing.api.OrganBatch;
import org.jd.infestusfrontier.storage.api.BiomassHandler;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * One batch organ: ingredient slots, a water tank, a biomass tank and one running batch, shaped by its
 * {@link BatchOrganKind}. The Bowl, the Rack, the Loom, the Cradle, the Bio-Furnace and the Cyst are this class with
 * different kinds. It runs on its own: whenever its slots, water and biomass can pay for a batch of its {@link BatchBook} and
 * the output has room, the next batch starts, credited to the last player who loaded it by hand. Completed batches
 * carry it through its {@link org.jd.infestusfrontier.core.organ.Growth} stages, shown in the block state.
 */
final class BatchOrganBlockEntity extends BlockEntity implements BiomassHandler, MenuProvider, GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WORKING = RawAnimation.begin().thenLoop("working");
    private static final int BLEND_TICKS = 10;
    private static final List<RawAnimation> GROWN = List.of(
            RawAnimation.begin().thenLoop("grown_0"),
            RawAnimation.begin().thenLoop("grown_1"),
            RawAnimation.begin().thenLoop("grown_2"));

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    static final Refusal NO_SLOT = new Refusal("processing.no_slot");

    private static final MapCodec<BatchOrganContents> SAVED = BatchOrganContents.CODEC.fieldOf("contents");

    private final BatchOrganKind kind;
    private Optional<CompoundTag> unreadable = Optional.empty();
    private final ItemStackHandler ingredients;
    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            changed();
        }
    };
    private final FluidTank water;
    private BiomassTank biomass;
    private OrganHistory history = OrganHistory.NONE;
    private Optional<OrganBatch> batch = Optional.empty();
    private Optional<UUID> feeder = Optional.empty();
    private float experience;
    /** True while something changed since the last look for a recipe, so a tick looks again at most once per change. */
    private boolean retry = true;

    BatchOrganBlockEntity(BatchOrganKind kind, BlockPos pos, BlockState state) {
        super(kind.entity().get(), pos, state);
        this.kind = kind;
        this.biomass = BiomassTank.empty(kind.biomassCapacity());
        this.ingredients = new ItemStackHandler(kind.slots()) {
            /**
             * One slot per kind of item, and only kinds that one batch can use together with what the other slots
             * hold: a hopper that offers one ingredient fills one slot and is then refused, so it offers the next.
             */
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (unreadable.isPresent() || level == null || stack.isEmpty()) return false;
                List<ItemStack> others = new ArrayList<>();
                for (int other = 0; other < getSlots(); other++)
                    if (other != slot && !getStackInSlot(other).isEmpty()) others.add(getStackInSlot(other));
                return kind.book().takes(level, others, stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                changed();
            }
        };
        this.water =
                new FluidTank(
                        kind.waterMb(), stack -> kind.waterMb() > 0 && stack.is(Fluids.WATER) && unreadable.isEmpty()) {
                    @Override
                    protected void onContentsChanged() {
                        changed();
                    }
                };
    }

    private void changed() {
        retry = true;
        setChanged();
    }

    BatchOrganKind kind() {
        return kind;
    }

    FluidTank water() {
        return water;
    }

    ItemStackHandler ingredients() {
        return ingredients;
    }

    ItemStackHandler output() {
        return output;
    }

    /** The organ for hoppers and pipes: ingredients go in, only the output comes out. */
    IItemHandler ports() {
        return new BatchOrganPorts(ingredients, output);
    }

    /** Whether some slot takes {@code stack} now, running batch or not. */
    boolean accepts(ItemStack stack) {
        for (int slot = 0; slot < kind.slots(); slot++) if (ingredients.isItemValid(slot, stack)) return true;
        return false;
    }

    /** The growth stage, from 0. */
    int stage() {
        return kind.growth().stage(history);
    }

    /** Batches to the next stage, 0 at the last. */
    int toNextStage() {
        return kind.growth().toNext(history);
    }

    /** 0 idle, 1 working, 2 paused for want of a foundation. Matches the menu's state int. */
    int state() {
        if (batch.isEmpty()) return 0;
        return level != null && Organs.hasFoundation(level, worldPosition) ? 1 : 2;
    }

    int percent() {
        return batch.map(b -> b.batch().percent()).orElse(0);
    }

    boolean isEmpty() {
        if (unreadable.isPresent()) return false; // the kept bytes are contents too
        if (batch.isPresent()
                || water.getFluidAmount() > 0
                || !biomass.stored().isZero()
                || !output.getStackInSlot(0).isEmpty()) return false;
        for (int slot = 0; slot < kind.slots(); slot++)
            if (!ingredients.getStackInSlot(slot).isEmpty()) return false;
        return true;
    }

    /**
     * The client plays {@code working} while the block state says so, and {@code grown_<stage>} for the stage it
     * says; nothing else is sent for them.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "state",
                BLEND_TICKS,
                state -> state.setAndContinue(
                        state.getAnimatable().getBlockState().getValue(BatchOrganBlock.WORKING) ? WORKING : IDLE)));
        if (kind.grows())
            controllers.add(new AnimationController<>(
                    this,
                    "growth",
                    0,
                    state -> state.setAndContinue(GROWN.get(state.getAnimatable()
                            .getBlockState()
                            .getOptionalValue(BatchOrganBlock.STAGE)
                            .orElse(0)))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }

    /** Mirrors whether a batch exists and the growth stage into the block state, so every client can see them. */
    private void show() {
        if (level == null) return;
        BlockState shown = getBlockState().setValue(BatchOrganBlock.WORKING, batch.isPresent());
        if (shown.hasProperty(BatchOrganBlock.STAGE))
            shown = shown.setValue(
                    BatchOrganBlock.STAGE, Math.min(kind.growth().stage(history), BatchOrganBlock.LAST_STAGE));
        if (shown != getBlockState()) level.setBlock(worldPosition, shown, Block.UPDATE_CLIENTS);
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, BatchOrganBlockEntity organ) {
        organ.show();
        if (organ.unreadable.isPresent() || !Organs.hasFoundation(level, pos)) return;
        if (organ.batch.isEmpty() && organ.retry) organ.start();
        if (organ.batch.isEmpty()) return;
        organ.batch = OrganWork.tick(organ.batch, finished -> {
            if (!organ.output.insertItem(0, finished.result(), true).isEmpty()) return false; // no room: wait
            organ.output.insertItem(0, finished.result(), false);
            organ.history = organ.history.completed();
            organ.experience = Math.min(BatchOrganContents.MAX_EXPERIENCE, organ.experience + finished.experience());
            OrganWork.completed(level, organ.kind.blockId(), finished);
            return true;
        });
        organ.setChanged();
    }

    /**
     * Starts the first batch of the book the slots, the water and the biomass can pay for and whose result fits the
     * output: takes what the plan says, leaves the rest for the next batch, and spends the water and the biomass now.
     * Looks again only after the next change.
     */
    private void start() {
        retry = false;
        if (level == null) return;
        List<ItemStack> held = new ArrayList<>();
        for (int slot = 0; slot < kind.slots(); slot++) held.add(ingredients.getStackInSlot(slot));
        for (BatchBook.Plan plan : kind.book().plans(level, held, history)) {
            if (water.getFluidAmount() < plan.waterMb() || !biomass.stored().atLeast(plan.biomass())) continue;
            if (!output.insertItem(0, plan.result(), true).isEmpty()) continue; // no room for this result
            for (int slot = 0; slot < kind.slots(); slot++) ingredients.extractItem(slot, plan.take()[slot], false);
            water.drain(plan.waterMb(), IFluidHandler.FluidAction.EXECUTE);
            biomass = ((Outcome.Done<BiomassTank>) biomass.drain(plan.biomass())).value();
            batch = Optional.of(
                    new OrganBatch(Batch.start(plan.id(), plan.ticks()), plan.result(), feeder, plan.experience()));
            setChanged();
            return;
        }
    }

    /** Gives {@code player} the smelting experience the organ kept, as orbs where the player stands, like a furnace. */
    void awardExperience(Player player) {
        if (experience <= 0 || !(level instanceof ServerLevel server)) return;
        int whole = Mth.floor(experience);
        if (server.random.nextFloat() < Mth.frac(experience)) whole++;
        ExperienceOrb.award(server, player.position(), whole);
        experience = 0;
        setChanged();
    }

    /** Remembers who loaded the organ by hand; the batches that follow are credited to them. */
    void fedBy(Player player) {
        feeder = Optional.of(player.getUUID());
        setChanged();
    }

    /** Moves one item from {@code stack} into the first slot that holds the same item and has room, else the first empty slot. */
    Optional<Refusal> insert(ItemStack stack) {
        if (unreadable.isPresent()) return Optional.of(Persistence.CORRUPT);
        ItemStack one = stack.copyWithCount(1);
        for (int pass = 0; pass < 2; pass++) {
            for (int slot = 0; slot < kind.slots(); slot++) {
                ItemStack held = ingredients.getStackInSlot(slot);
                if ((pass == 0) == held.isEmpty()) continue;
                if (ingredients.insertItem(slot, one, false).isEmpty()) {
                    stack.shrink(1);
                    return Optional.empty();
                }
            }
        }
        return Optional.of(NO_SLOT);
    }

    /**
     * Gives the player the output stack, with the experience that waits with it, else the first non-empty ingredient
     * stack. Returns whether anything moved.
     */
    boolean collect(Player player) {
        ItemStack taken = output.getStackInSlot(0);
        if (!taken.isEmpty()) {
            output.setStackInSlot(0, ItemStack.EMPTY);
            awardExperience(player);
        } else {
            taken = ItemStack.EMPTY;
            for (int slot = 0; slot < kind.slots() && taken.isEmpty(); slot++) {
                taken = ingredients.getStackInSlot(slot);
                if (!taken.isEmpty()) ingredients.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        if (taken.isEmpty()) return false;
        player.getInventory().placeItemBackInInventory(taken);
        return true;
    }

    @Override
    public BiomassTank tank() {
        return biomass;
    }

    @Override
    public Outcome<BiomassTank> insert(Biomass amount, boolean simulate) {
        return apply(biomass.fill(amount), simulate);
    }

    @Override
    public Outcome<BiomassTank> extract(Biomass amount, boolean simulate) {
        return apply(biomass.drain(amount), simulate);
    }

    private Outcome<BiomassTank> apply(Outcome<BiomassTank> change, boolean simulate) {
        if (unreadable.isPresent()) return Outcome.refused(Persistence.CORRUPT);
        if (!simulate && change instanceof Outcome.Done<BiomassTank> done) {
            biomass = done.value();
            changed();
        }
        return change;
    }

    private BatchOrganContents contents() {
        List<ItemStack> held = new ArrayList<>();
        for (int slot = 0; slot < kind.slots(); slot++)
            held.add(ingredients.getStackInSlot(slot).copy());
        return new BatchOrganContents(
                held,
                output.getStackInSlot(0).copy(),
                water.getFluidAmount(),
                biomass,
                history,
                batch,
                feeder,
                experience);
    }

    /** Why {@code saved} cannot be installed as a whole, or empty when it fits together. */
    private Optional<String> check(BatchOrganContents saved) {
        if (!saved.biomass().capacity().equals(kind.biomassCapacity()))
            return Optional.of("biomass capacity " + saved.biomass().capacity().units());
        if (saved.ingredients().size() > kind.slots())
            return Optional.of(saved.ingredients().size() + " ingredient slots");
        if (saved.waterMb() > kind.waterMb()) return Optional.of(saved.waterMb() + " mB of water");
        if (saved.batch().isPresent() && saved.history().completedBatches() == Integer.MAX_VALUE)
            return Optional.of("a running batch that history cannot count");
        return Optional.empty();
    }

    private void restore(BatchOrganContents saved) {
        List<ItemStack> held = saved.ingredients();
        for (int slot = 0; slot < kind.slots(); slot++)
            ingredients.setStackInSlot(slot, slot < held.size() ? held.get(slot) : ItemStack.EMPTY);
        output.setStackInSlot(0, saved.output().copy());
        water.setFluid(saved.waterMb() > 0 ? new FluidStack(Fluids.WATER, saved.waterMb()) : FluidStack.EMPTY);
        biomass = saved.biomass();
        history = saved.history();
        batch = saved.batch();
        feeder = saved.feeder();
        experience = saved.experience();
        retry = true;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<BatchOrganContents> read =
                Persistence.read(SAVED, tag, registries, ProcessingModule.LOGGER, kind.id(), this::check);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        read.ifPresent(this::restore);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, contents(), registries, tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent()) {
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
            return;
        }
        components.set(ProcessingModule.BATCH_CONTENTS.get(), contents());
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            return;
        }
        BatchOrganContents carried = input.get(ProcessingModule.BATCH_CONTENTS.get());
        if (carried == null) return;
        Optional<String> problem = check(carried);
        if (problem.isPresent()) {
            ProcessingModule.LOGGER.warn("{} keeps unreadable carried state: {}", kind.id(), problem.get());
            unreadable = Optional.of(
                    level == null ? new CompoundTag() : Persistence.encode(SAVED, carried, level.registryAccess()));
            return;
        }
        restore(carried);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("contents");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(kind.nameKey());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BatchOrganMenu(kind.menu().get(), id, inventory, this);
    }
}
