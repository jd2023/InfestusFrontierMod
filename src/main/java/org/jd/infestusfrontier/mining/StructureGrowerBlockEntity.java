package org.jd.infestusfrontier.mining;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.Fracturing;
import org.jd.infestusfrontier.core.mining.GrowerProgress;
import org.jd.infestusfrontier.core.mining.Rootstocking;
import org.jd.infestusfrontier.core.mining.Survey;
import org.jd.infestusfrontier.core.mining.Surveying;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Steward;
import org.jd.infestusfrontier.organ.api.Stewarded;

/**
 * Owns a grower's stock, progress and steward. At each due step, {@link MineConstruction} plans the placements
 * without changing the world; this entity applies them and pays only after admission to the shared work budget.
 * Refusals retry on changed stock or after {@value Surveying#RETRY_TICKS} loaded ticks.
 * The marker owns the approved route and counts built steps. A Rootstock receives credit for its staircase or
 * gallery, and a commissioned gallery grower forwards its jaws' cut requests to that minehead.
 */
final class StructureGrowerBlockEntity extends BlockEntity implements Stewarded, MenuProvider {
    private final ItemStackHandler segments = stock(stack -> stack.is(MiningModule.ROOT_SEGMENT.get()));
    private final ItemStackHandler treads = stock(StructureGrowerContents::tread);
    private final ItemStackHandler lights = stock(StructureGrowerContents::light);
    private final ItemStackHandler ribStock = stock(StructureGrowerContents::rib);

    private GrowerProgress progress = GrowerProgress.START;
    private Optional<CompoundTag> unreadable = Optional.empty();
    private final Steward steward = new Steward();
    /** Why the last try did not build. Not saved: the next try tells again. */
    private Optional<Refusal> stop = Optional.empty();
    /** Loaded ticks before a stopped grower looks again. */
    private int retry;
    /**
     * The Rootstock whose gallery face this grower is, as that minehead last said; not saved, it says so again within
     * {@value Rootstocking#CHECK_TICKS} loaded ticks. Older than {@link #COMMISSION_TICKS} it no longer counts.
     */
    private Optional<BlockPos> minehead = Optional.empty();
    /** The game time the minehead last said so. */
    private long commissioned;

    /** Game ticks a minehead's word that this grower is one of its faces holds. */
    private static final int COMMISSION_TICKS = 2 * Rootstocking.CHECK_TICKS;

    StructureGrowerBlockEntity(BlockPos pos, BlockState state) {
        super(MiningModule.STRUCTURE_GROWER_ENTITY.get(), pos, state);
    }

    private ItemStackHandler stock(Predicate<ItemStack> valid) {
        return new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return unreadable.isEmpty() && valid.test(stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return Surveying.MAX_STOCK;
            }

            @Override
            protected void onContentsChanged(int slot) {
                supplied();
            }
        };
    }

    @Override
    public Steward steward() {
        return steward;
    }

    boolean unreadable() {
        return unreadable.isPresent();
    }

    ItemStackHandler segments() {
        return segments;
    }

    ItemStackHandler treads() {
        return treads;
    }

    ItemStackHandler lights() {
        return lights;
    }

    ItemStackHandler ribStock() {
        return ribStock;
    }

    GrowerProgress progress() {
        return progress;
    }

    Optional<Refusal> stop() {
        return stop;
    }

    /** The plan of the marker under the grower, while there is a readable one. */
    Optional<Survey> survey() {
        return marker().flatMap(SurveyedTissueBlockEntity::survey);
    }

    private Optional<SurveyedTissueBlockEntity> marker() {
        return level != null && level.getBlockEntity(worldPosition.below()) instanceof SurveyedTissueBlockEntity marker
                ? Optional.of(marker)
                : Optional.empty();
    }

    // ---- a gallery face ----

    /** The minehead at {@code head} found this grower on one of its gallery faces at game time {@code now}. */
    void commission(BlockPos head, long now) {
        minehead = Optional.of(head);
        commissioned = now;
    }

    /** The Rootstock whose gallery face this grower is, while its word is fresh and it stands loaded and readable. */
    private Optional<TunnelRootBlockEntity> minehead(ServerLevel level) {
        if (minehead.isEmpty() || level.getGameTime() - commissioned > COMMISSION_TICKS) return Optional.empty();
        BlockPos head = minehead.get();
        return level.hasChunkAt(head)
                        && level.getBlockEntity(head) instanceof TunnelRootBlockEntity rootstock
                        && rootstock.rootstock()
                ? Optional.of(rootstock)
                : Optional.empty();
    }

    /**
     * The block a jaw facing this grower is to cut now: the one its minehead orders for this face, or
     * {@link Fracturing#NO_ORDER} when no minehead orders one.
     */
    Outcome<BlockPos> order(ServerLevel level) {
        Optional<TunnelRootBlockEntity> head = minehead(level);
        return head.isPresent() ? head.get().orderFace(level, worldPosition) : Outcome.refused(Fracturing.NO_ORDER);
    }

    /** The stock changed: a stopped grower looks again on its next tick. */
    private void supplied() {
        retry = 0;
        setChanged();
    }

    /** The grower's stock seen from outside: it takes what each slot takes and gives nothing back. */
    IItemHandler intake() {
        List<ItemStackHandler> slots = List.of(segments, treads, lights, ribStock);
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return slots.size();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return slots.get(slot).getStackInSlot(0);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return slots.get(slot).insertItem(0, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return Surveying.MAX_STOCK;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slots.get(slot).isItemValid(0, stack);
            }
        };
    }

    // ---- loading by hand ----

    /** Takes what is in {@code hand} into the stock it belongs to, as much as fits, or names why it took nothing. */
    void load(Player player, InteractionHand hand) {
        if (unreadable.isPresent()) {
            Refusals.show(player, Persistence.CORRUPT);
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        ItemStackHandler into = held.is(MiningModule.ROOT_SEGMENT.get())
                ? segments
                : StructureGrowerContents.tread(held)
                        ? treads
                        : StructureGrowerContents.light(held) ? lights : ribStock;
        ItemStack left = into.insertItem(0, held.copy(), false);
        int taken = held.getCount() - left.getCount();
        if (taken == 0) {
            Refusals.show(player, Surveying.STOCK_FULL);
            return;
        }
        if (!player.getAbilities().instabuild) held.shrink(taken);
        steward.assign(player);
        player.displayClientMessage(
                Component.translatable(
                        "infestusfrontier.structure_grower.loaded",
                        segments.getStackInSlot(0).getCount(),
                        treads.getStackInSlot(0).getCount(),
                        lights.getStackInSlot(0).getCount(),
                        ribStock.getStackInSlot(0).getCount()),
                true);
    }

    // ---- the work ----

    static void serverTick(Level level, BlockPos pos, BlockState state, StructureGrowerBlockEntity grower) {
        if (grower.unreadable.isPresent() || !(level instanceof ServerLevel server)) return;
        if (!grower.progress.due()) {
            grower.progress = grower.progress.waited();
            grower.setChanged();
            return;
        }
        if (grower.retry > 0) {
            grower.retry--;
            return;
        }
        grower.tryGrow(server);
    }

    private void tryGrow(ServerLevel level) {
        Optional<SurveyedTissueBlockEntity> marker = marker();
        Optional<Survey> survey = marker.flatMap(SurveyedTissueBlockEntity::survey);
        Outcome<MineConstruction.Build> judged;
        if (survey.isEmpty()) judged = Outcome.refused(Surveying.NO_PLAN);
        else if (!survey.get().approved()) judged = Outcome.refused(Surveying.NOT_APPROVED);
        else if (survey.get().finished()) judged = Outcome.refused(Surveying.FINISHED);
        else
            judged = MineConstruction.plan(
                    level, marker.orElseThrow(), survey.get().next(), contents());
        if (judged instanceof Outcome.Refused<MineConstruction.Build>(Refusal why)) {
            stop = Optional.of(why);
            retry = Surveying.RETRY_TICKS;
            return;
        }
        // admitted after judging, so a step over the shared limit is simply tried again next tick
        if (!level.getData(MiningModule.GROWER_STEPS).take(level.getGameTime())) return;
        grow(level, marker.orElseThrow(), ((Outcome.Done<MineConstruction.Build>) judged).value());
    }

    /** Places the judged step from the bottom up, pays for it and has the marker count it. */
    private void grow(ServerLevel level, SurveyedTissueBlockEntity marker, MineConstruction.Build build) {
        for (MineConstruction.Placement placement : build.placements()) {
            level.setBlock(placement.pos(), placement.state(), Block.UPDATE_ALL);
            level.gameEvent(GameEvent.BLOCK_PLACE, placement.pos(), GameEvent.Context.of(placement.state()));
        }
        segments.extractItem(0, build.cost().segments(), false);
        treads.extractItem(0, build.cost().treads(), false);
        lights.extractItem(0, build.cost().lights(), false);
        ribStock.extractItem(0, build.cost().ribs(), false);
        marker.grown();
        Optional<TunnelRootBlockEntity> staircase = Minehead.rootstockOf(level, marker);
        if (staircase.isPresent())
            staircase
                    .get()
                    .advanced(level, marker.survey().map(Survey::finished).orElse(false));
        else minehead(level).ifPresent(head -> head.galleryAdvanced(level, build.wide()));
        progress = progress.grown();
        stop = Optional.empty();
        setChanged();
        BlockPos sound = build.placements().isEmpty()
                ? marker.getBlockPos()
                : build.placements().get(0).pos();
        level.playSound(null, sound, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 1F, 1F);
        steward.worked(level, MiningModule.STRUCTURE_GROWER.getId());
    }

    // ---- saved state ----

    StructureGrowerContents contents() {
        return new StructureGrowerContents(
                segments.getStackInSlot(0),
                treads.getStackInSlot(0),
                lights.getStackInSlot(0),
                ribStock.getStackInSlot(0),
                progress);
    }

    private void restore(StructureGrowerContents contents) {
        segments.setStackInSlot(0, contents.segments().copy());
        treads.setStackInSlot(0, contents.treads().copy());
        lights.setStackInSlot(0, contents.lights().copy());
        ribStock.setStackInSlot(0, contents.ribs().copy());
        progress = contents.progress();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<StructureGrowerContents> read = Persistence.read(
                StructureGrowerContents.CODEC,
                tag,
                registries,
                MiningModule.LOGGER,
                "Structure Grower at " + worldPosition,
                StructureGrowerContents::problem);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        restore(read.orElse(StructureGrowerContents.EMPTY));
        stop = Optional.empty();
        retry = 0;
        steward.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(StructureGrowerContents.CODEC, contents(), registries, tag);
        steward.save(tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent())
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
        else components.set(MiningModule.STRUCTURE_GROWER_CONTENTS.get(), contents());
    }

    /** A carried grower keeps its stock and its steps; carried bytes that fail their check stay unreadable. */
    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            return;
        }
        StructureGrowerContents carried = input.get(MiningModule.STRUCTURE_GROWER_CONTENTS.get());
        if (carried == null) return;
        Optional<String> problem = carried.problem();
        if (problem.isPresent()) {
            MiningModule.LOGGER.warn("Structure Grower keeps unreadable carried state: {}", problem.get());
            unreadable = Optional.of(
                    level == null
                            ? new CompoundTag()
                            : Persistence.encode(StructureGrowerContents.CODEC, carried, level.registryAccess()));
            return;
        }
        restore(carried);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        for (String key : StructureGrowerContents.KEYS) tag.remove(key);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new StructureGrowerMenu(id, inventory, this);
    }
}
