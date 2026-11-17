package org.jd.infestusfrontier.genetics;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.genetics.Extraction;
import org.jd.infestusfrontier.core.genetics.Genetics;
import org.jd.infestusfrontier.core.genetics.Research;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.genetics.api.DnaSampleRecipe;
import org.jd.infestusfrontier.genetics.api.GenomeCompletedEvent;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.organ.api.Steward;
import org.jd.infestusfrontier.organ.api.Stewarded;
import org.jd.infestusfrontier.storage.api.BiomassHandler;

/**
 * The work of a DNA Bank. It extracts the samples in its slot one at a time: a start takes one sample and its biomass,
 * as its {@link DnaSampleRecipe} says, and fixes the extraction's numbers; after the recipe's loaded ticks the points
 * join that species' genome. A start is judged whole first and refused whole: a sample no recipe reads, a species whose
 * genome is complete, a species past the bank's {@value Genetics#MAX_GENOMES}, too little biomass or no foundation
 * leave the sample and the biomass where they are, name the reason, and look again after
 * {@value Genetics#RETRY_TICKS} ticks or at once when its supplies change. Research only grows and stays with the bank.
 * Samples arrive by hand and through its item handler, which takes only samples the research accepts and gives
 * nothing back; biomass by bucket, pipe or vein. It reads only its own block and the one under it, and loads no chunk.
 */
final class DnaBankBlockEntity extends BlockEntity implements BiomassHandler, Stewarded, MenuProvider {
    private final ItemStackHandler samples = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return unreadable.isEmpty() && admits(stack).isEmpty();
        }

        @Override
        public int getSlotLimit(int slot) {
            return Genetics.MAX_SAMPLES;
        }

        @Override
        protected void onContentsChanged(int slot) {
            supplied();
        }
    };

    private BiomassTank tank = DnaBankContents.EMPTY.tank();
    private Research research = Research.NONE;
    private Optional<Extraction> job = Optional.empty();
    private Optional<CompoundTag> unreadable = Optional.empty();
    private final Steward steward = new Steward();
    /** Why the last try did not start or go on. Not saved: the next try tells again. */
    private Optional<Refusal> stop = Optional.empty();
    /** Loaded ticks before a stopped bank looks again. */
    private int retry;

    DnaBankBlockEntity(BlockPos pos, BlockState state) {
        super(GeneticsModule.DNA_BANK_ENTITY.get(), pos, state);
    }

    @Override
    public Steward steward() {
        return steward;
    }

    ItemStackHandler samples() {
        return samples;
    }

    Research research() {
        return research;
    }

    Optional<Extraction> job() {
        return job;
    }

    Optional<Refusal> stop() {
        return stop;
    }

    /** What automation sees: the sample slot, which takes what {@link #samples} takes and gives nothing back. */
    IItemHandler intake() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return samples.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return samples.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return Genetics.MAX_SAMPLES;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return samples.isItemValid(slot, stack);
            }
        };
    }

    /** Something the bank holds changed: it looks again on its next tick. */
    private void supplied() {
        retry = 0;
        setChanged();
    }

    /**
     * Why {@code stack} cannot go into the sample slot now, or empty when it can: it must be a sample a recipe reads, of
     * a species this bank's research still takes. Samples of a complete genome are never taken in, so they are never
     * used up.
     */
    Optional<Refusal> admits(ItemStack stack) {
        if (level == null) return Optional.of(Genetics.UNKNOWN_SAMPLE);
        Optional<DnaSampleRecipe> recipe = DnaSampleRecipe.forSample(level, stack);
        if (recipe.isEmpty()) return Optional.of(Genetics.UNKNOWN_SAMPLE);
        return research.refusal(recipe.get().species().toString());
    }

    // ---- biomass ----

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
            supplied();
        }
        return change;
    }

    // ---- loading by hand ----

    /** Takes the samples in {@code hand}, whole or as many as fit, or names why it took none. */
    void load(Player player, InteractionHand hand) {
        if (unreadable.isPresent()) {
            Refusals.show(player, Persistence.CORRUPT);
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        Optional<Refusal> refused = admits(held);
        if (refused.isPresent()) {
            Refusals.show(player, refused.get());
            return;
        }
        ItemStack left = samples.insertItem(0, held.copy(), false);
        int taken = held.getCount() - left.getCount();
        if (taken == 0) {
            Refusals.show(player, Genetics.SAMPLES_FULL);
            return;
        }
        if (!player.getAbilities().instabuild) held.shrink(taken);
        steward.assign(player);
        setChanged();
        if (level != null)
            level.playSound(null, worldPosition, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
        player.displayClientMessage(
                Component.translatable(
                        "infestusfrontier.dna_bank.loaded",
                        samples.getStackInSlot(0).getCount(),
                        Genetics.MAX_SAMPLES,
                        tank.stored().units(),
                        tank.capacity().units()),
                true);
    }

    // ---- the work ----

    static void serverTick(Level level, BlockPos pos, BlockState state, DnaBankBlockEntity bank) {
        if (bank.unreadable.isPresent() || !(level instanceof ServerLevel server)) return;
        if (bank.job.isPresent()) {
            bank.extract(server, pos, bank.job.get());
            return;
        }
        if (bank.retry > 0) {
            bank.retry--;
            return;
        }
        bank.tryStart(server, pos);
    }

    /** One loaded tick of the extraction under way; it pauses while the bank has no foundation. */
    private void extract(ServerLevel level, BlockPos pos, Extraction extraction) {
        if (!Organs.hasFoundation(level, pos)) {
            stop = Optional.of(Genetics.UNROOTED);
            return;
        }
        stop = Optional.empty();
        Extraction next = extraction.waited();
        if (next.done()) finish(level, next);
        else job = Optional.of(next);
        setChanged();
    }

    /** The extraction's points join its genome, once; the job ends in the same step, so a reload cannot repeat it. */
    private void finish(ServerLevel level, Extraction done) {
        boolean wasComplete = research.complete(done.species());
        research = research.extracted(done.species(), done.points());
        job = Optional.empty();
        retry = 0;
        steward.worked(level, GeneticsModule.DNA_BANK.getId());
        if (!wasComplete && research.complete(done.species()))
            steward.player()
                    .ifPresent(player -> NeoForge.EVENT_BUS.post(
                            new GenomeCompletedEvent(level, ResourceLocation.parse(done.species()), player)));
        level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6F, 1.4F);
    }

    private void tryStart(ServerLevel level, BlockPos pos) {
        ItemStack waiting = samples.getStackInSlot(0);
        if (waiting.isEmpty()) {
            stop = Optional.empty();
            return;
        }
        Optional<DnaSampleRecipe> recipe = DnaSampleRecipe.forSample(level, waiting);
        Optional<Refusal> refused = blocked(level, pos, recipe);
        if (refused.isPresent()) {
            stop = refused;
            retry = Genetics.RETRY_TICKS;
            return;
        }
        DnaSampleRecipe sample = recipe.get();
        ItemStack left = waiting.copy();
        left.shrink(1);
        tank = ((Outcome.Done<BiomassTank>) tank.drain(sample.biomass())).value();
        job = Optional.of(Extraction.start(sample.species().toString(), sample.points(), sample.ticks()));
        stop = Optional.empty();
        samples.setStackInSlot(0, left);
        setChanged();
    }

    /** Why the sample waiting in the slot cannot start now, or empty when it can. Reads, changes nothing. */
    private Optional<Refusal> blocked(ServerLevel level, BlockPos pos, Optional<DnaSampleRecipe> recipe) {
        if (!Organs.hasFoundation(level, pos)) return Optional.of(Genetics.UNROOTED);
        if (recipe.isEmpty()) return Optional.of(Genetics.UNKNOWN_SAMPLE);
        Optional<Refusal> refused = research.refusal(recipe.get().species().toString());
        if (refused.isPresent()) return refused;
        if (!tank.stored().atLeast(recipe.get().biomass())) return Optional.of(Genetics.NO_BIOMASS);
        return Optional.empty();
    }

    // ---- saved state ----

    DnaBankContents contents() {
        return new DnaBankContents(samples.getStackInSlot(0), tank, research, job);
    }

    private void restore(DnaBankContents contents) {
        samples.setStackInSlot(0, contents.samples().copy());
        tank = contents.tank();
        research = contents.research();
        job = contents.job();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<DnaBankContents> read = Persistence.read(
                DnaBankContents.CODEC, tag, registries, GeneticsModule.LOGGER, "DNA Bank at " + worldPosition);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        restore(read.orElse(DnaBankContents.EMPTY));
        stop = Optional.empty();
        retry = 0;
        steward.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(DnaBankContents.CODEC, contents(), registries, tag);
        steward.save(tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent())
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
        else components.set(GeneticsModule.DNA_BANK_CONTENTS.get(), contents());
    }

    /** A carried bank keeps its samples, biomass, research and extraction; carried bytes that fail their check stay. */
    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            return;
        }
        DnaBankContents carried = input.get(GeneticsModule.DNA_BANK_CONTENTS.get());
        if (carried == null) return;
        Optional<String> problem = carried.problem();
        if (problem.isPresent()) {
            GeneticsModule.LOGGER.warn("DNA Bank keeps unreadable carried state: {}", problem.get());
            unreadable = Optional.of(
                    level == null
                            ? new CompoundTag()
                            : Persistence.encode(DnaBankContents.CODEC, carried, level.registryAccess()));
            return;
        }
        restore(carried);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        for (String key : List.of("samples", "tank", "genomes", "job")) tag.remove(key);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new DnaBankMenu(id, inventory, this);
    }
}
