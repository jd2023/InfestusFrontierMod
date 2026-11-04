package org.jd.infestusfrontier.processing;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.organ.api.Steward;
import org.jd.infestusfrontier.organ.api.Stewarded;
import org.jd.infestusfrontier.processing.api.PalmContents;
import org.jd.infestusfrontier.processing.api.PalmRote;
import org.jd.infestusfrontier.storage.api.BiomassHandler;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A Shaping Palm (BLOCK_CATALOG T1-41): the nine stacks of a crafting grid that stay in the organ when its screen
 * closes, and whether it has been given Reach, the right to use the Mouth under it as its door to the Item Vein
 * network. A craft at the screen is the crafting
 * table's own, taken by the player ({@link ShapingPalmMenu}). A Palm with Rote (stage 3) also keeps one recorded
 * pattern, a biomass tank, a four-stack tray and at most one cycle under way, and repeats the pattern's craft by itself
 * ({@link #serverTick}); only then does it expose an item handler ({@link PalmPorts}) and a biomass handler. The network is reached through
 * the Mouth under the Palm, only when a screen action or a restock asks ({@link #door()}).
 */
final class ShapingPalmBlockEntity extends BlockEntity
        implements MenuProvider, GeoBlockEntity, BiomassHandler, Stewarded {
    /** The organ has no foundation under it, so it keeps its grid and crafts nothing. */
    static final Refusal NO_FOUNDATION = new Refusal("processing.palm_no_foundation");
    /** The organ already has Reach, so a second Contractile Fiber is not spent. */
    static final Refusal ALREADY_REACHES = new Refusal("processing.palm_has_reach");
    /** Rote needs Reach first. */
    static final Refusal ROTE_NEEDS_REACH = new Refusal("processing.palm_rote_needs_reach");
    /** The organ already has Rote, so a second Synaptic Gel is not spent. */
    static final Refusal ALREADY_ROTES = new Refusal("processing.palm_has_rote");
    /** Select and the cycle are Rote's. */
    static final Refusal NO_ROTE = new Refusal("processing.palm_no_rote");
    /** The pattern changes only between cycles. */
    static final Refusal CYCLE_RUNNING = new Refusal("processing.palm_select_running");
    /** Ticks between two attempts to start a cycle that nothing has changed for. */
    static final int RETRY_TICKS = 20;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final int BLEND_TICKS = 10;
    private static final MapCodec<Saved> SAVED = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    PalmContents.CODEC.fieldOf("contents").forGetter(Saved::contents),
                    PalmRote.CODEC.optionalFieldOf("rote").forGetter(Saved::rote),
                    ItemStack.OPTIONAL_CODEC
                            .listOf(0, PalmContents.CELLS)
                            .optionalFieldOf("fetched")
                            .forGetter(Saved::fetched))
            .apply(instance, Saved::new));
    private static final String REACH = "reach";
    /**
     * The direct-link position older saves hold. It is never read: a Palm reaches storage only through its Mouth. It
     * is dropped from every tag the Palm reads, keeps and writes, and the rest of the save loads as it was.
     */
    private static final String LEGACY_LINK = "link";

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private final NonNullList<ItemStack> grid = NonNullList.withSize(PalmContents.CELLS, ItemStack.EMPTY);
    /** What the Mouth fetched into the grid. Saved with the block and dropped when it is broken. */
    private final PalmFetched fetched = new PalmFetched();

    private final Steward steward = new Steward();
    private Optional<CompoundTag> unreadable = Optional.empty();
    /** Counts every change to the grid, so a menu that is open knows when what it shows is out of date. */
    private int version;
    /** A Contractile Fiber was applied: the Palm may use the Mouth under it. Carried on its item. */
    private boolean reach;

    /** A Synaptic Gel was applied: the Palm selects a pattern and repeats it. Everything below is meaningful only then. */
    private boolean rote;

    private Optional<PalmRote.Pattern> pattern = Optional.empty();
    private Optional<PalmRote.Cycle> cycle = Optional.empty();
    /** The result in slot 0, the remainders in the three after it. The cycle's products wait here for a taker. */
    private final NonNullList<ItemStack> tray = NonNullList.withSize(PalmRote.TRAY, ItemStack.EMPTY);

    private BiomassTank biomass = BiomassTank.empty(PalmRote.CAPACITY);
    private int cycles;
    /** A cycle was completed since the grid was last restocked through its Mouth. */
    private boolean restockOwed;
    /** True while something changed since the last attempt, so the next tick looks again at once. */
    private boolean dirty = true;
    /** Ticks until a refused attempt is made again when nothing changes. */
    private int cooldown;

    private PalmStatus judged = PalmStatus.NO_PATTERN;

    ShapingPalmBlockEntity(BlockPos pos, BlockState state) {
        super(ProcessingModule.SHAPING_PALM_ENTITY.get(), pos, state);
    }

    /** The live cells, shared by every menu open on this organ. Only {@link ShapingPalmMenu}'s grid view writes them. */
    NonNullList<ItemStack> grid() {
        return grid;
    }

    int version() {
        return version;
    }

    /** Marks the grid changed: saves it, tells every open menu, and lets the Palm look at once for a cycle to start. */
    void touch() {
        fetched.reconcile(grid);
        version++;
        dirty = true;
        setChanged();
    }

    @Override
    public Steward steward() {
        return steward;
    }

    /** {@code items} of grid cell {@code cell}'s kind just came in from the network. */
    void received(int cell, int items) {
        fetched.received(grid, cell, items);
        setChanged();
    }

    /** What the Mouth's restock put in each cell is network supply. */
    void received(int[] items) {
        for (int cell = 0; cell < items.length; cell++) if (items[cell] > 0) received(cell, items[cell]);
    }

    /** A craft by hand takes one of each filled cell: true when one of them came from the network. */
    boolean spendFetched() {
        boolean supplied = fetched.spendAll(grid);
        if (supplied) setChanged();
        return supplied;
    }

    /** Marks the tray changed: a cycle that waited for room may now deliver. */
    void touchTray() {
        touch();
    }

    /** The live tray, shared by every menu and by the item handler. */
    NonNullList<ItemStack> tray() {
        return tray;
    }

    boolean hasRote() {
        return rote;
    }

    Optional<PalmRote.Pattern> pattern() {
        return pattern;
    }

    /** True when a cycle is under way or finished and waiting for room in the tray. */
    boolean cycling() {
        return cycle.isPresent();
    }

    int cycles() {
        return cycles;
    }

    /** The item handler of a Palm with Rote, and none for a Palm without; hoppers and Lobes use it. */
    @Nullable IItemHandler ports() {
        return rote && unreadable.isEmpty() ? new PalmPorts(this) : null;
    }

    /** True when {@code stack} is the kind the pattern holds in grid cell {@code cell}. */
    boolean takes(int cell, ItemStack stack) {
        return rote
                && unreadable.isEmpty()
                && pattern.map(p -> {
                            ItemStack kind = p.kind(cell);
                            return !kind.isEmpty() && ItemStack.isSameItemSameComponents(kind, stack);
                        })
                        .orElse(false);
    }

    /** False while the saved bytes could not be read; the organ then keeps them and takes nothing in. */
    boolean readable() {
        return unreadable.isEmpty();
    }

    /** Why the organ cannot craft now, or empty when it can. */
    Optional<Refusal> refusal() {
        if (unreadable.isPresent()) return Optional.of(Persistence.CORRUPT);
        if (level == null || !Organs.hasFoundation(level, worldPosition)) return Optional.of(NO_FOUNDATION);
        return Optional.empty();
    }

    boolean hasReach() {
        return reach;
    }

    /** Applies Reach once. Refuses a Palm whose save could not be read, and a Palm that already has it. */
    Optional<Refusal> grantReach() {
        if (unreadable.isPresent()) return Optional.of(Persistence.CORRUPT);
        if (reach) return Optional.of(ALREADY_REACHES);
        reach = true;
        setChanged();
        return Optional.empty();
    }

    /** Applies Rote once. Refuses a Palm whose save could not be read, a Palm without Reach, and a Palm that has it. */
    Optional<Refusal> grantRote() {
        if (unreadable.isPresent()) return Optional.of(Persistence.CORRUPT);
        if (!reach) return Optional.of(ROTE_NEEDS_REACH);
        if (rote) return Optional.of(ALREADY_ROTES);
        rote = true;
        dirty = true;
        setChanged();
        if (level != null) level.invalidateCapabilities(worldPosition);
        show();
        return Optional.empty();
    }

    /**
     * Select: records the grid as the pattern, with the recipe it resolves to and the result it assembles. Refused for
     * a Palm without Rote, while a cycle is under way, and for an empty grid, a grid that matches no recipe, a special
     * recipe, an empty result and more than three kinds of remainder. A refusal changes nothing.
     */
    Optional<Refusal> select() {
        if (unreadable.isPresent()) return Optional.of(Persistence.CORRUPT);
        if (!rote) return Optional.of(NO_ROTE);
        if (cycle.isPresent()) return Optional.of(CYCLE_RUNNING);
        if (level == null) return Optional.of(PalmPlan.NO_RECIPE);
        List<ItemStack> kinds = grid.stream()
                .map(stack -> stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1))
                .toList();
        return switch (PalmPlan.resolve(level, kinds)) {
            case PalmPlan.Plan.Refused refused -> Optional.of(refused.why());
            case PalmPlan.Plan.Found found -> {
                pattern = Optional.of(new PalmRote.Pattern(kinds, found.recipe(), found.result()));
                touch();
                yield Optional.empty();
            }
        };
    }

    /** Biomass the Palm holds, in BU. */
    int biomassUnits() {
        return biomass.stored().units();
    }

    int percent() {
        return cycle.map(PalmRote.Cycle::percent).orElse(0);
    }

    /** Why the Palm is doing what it does: the last judgment of an attempt, or the live fact that overrides it. */
    PalmStatus status() {
        if (!rote) return PalmStatus.NO_ROTE;
        if (unreadable.isPresent()) return PalmStatus.UNREADABLE;
        if (level == null || !Organs.hasFoundation(level, worldPosition)) return PalmStatus.NO_FOUNDATION;
        if (cycle.isPresent() && !cycle.get().isDone()) return PalmStatus.WORKING;
        if (pattern.isEmpty()) return PalmStatus.NO_PATTERN;
        return judged;
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
        if (!rote) return Outcome.refused(NO_ROTE);
        if (!simulate && change instanceof Outcome.Done<BiomassTank> done) {
            biomass = done.value();
            dirty = true;
            setChanged();
        }
        return change;
    }

    /** Mirrors Rote and a running cycle into the block state, so every client can see them. */
    private void show() {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        BlockState shown = state.setValue(ShapingPalmBlock.ROTE, rote)
                .setValue(ShapingPalmBlock.WORKING, rote && cycle.isPresent());
        if (shown != state) level.setBlock(worldPosition, shown, Block.UPDATE_CLIENTS);
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, ShapingPalmBlockEntity palm) {
        if (!palm.rote && !state.getValue(ShapingPalmBlock.ROTE)) return;
        palm.show();
        if (!palm.rote || palm.unreadable.isPresent() || !(level instanceof ServerLevel server)) return;
        if (!Organs.hasFoundation(level, pos)) return;
        if (palm.cycle.isPresent()) palm.work(server);
        else palm.begin(server);
    }

    /** True when an attempt may be made now: something changed, or the retry interval is up. */
    private boolean due() {
        if (dirty) {
            dirty = false;
            return true;
        }
        return cooldown-- <= 0;
    }

    /** One loaded tick of the cycle; a finished cycle delivers its products when the tray has room. */
    private void work(ServerLevel server) {
        PalmRote.Cycle running = cycle.get();
        if (!running.isDone()) {
            running = running.advance();
            cycle = Optional.of(running);
            setChanged();
            if (!running.isDone()) return;
        } else if (!due()) return;
        Optional<NonNullList<ItemStack>> placed = PalmPlan.place(tray, running.result(), running.remainders());
        if (placed.isEmpty()) {
            judged = PalmStatus.TRAY_FULL;
            cooldown = RETRY_TICKS;
            return;
        }
        for (int slot = 0; slot < PalmRote.TRAY; slot++)
            tray.set(slot, placed.get().get(slot));
        cycle = Optional.empty();
        cycles++;
        restockOwed = true;
        judged = PalmStatus.READY;
        touch();
        if (running.supplied()) steward.worked(server, ProcessingModule.SHAPING_PALM.getId());
        supply(server);
    }

    /**
     * Tops the pattern's cells up through the Mouth under the Palm, once for the cycle just completed. Returns true when
     * the network was consulted or there is no Mouth to consult; false leaves the restock owed for a later attempt.
     */
    private boolean supply(ServerLevel server) {
        if (!restockOwed) return true;
        if (pattern.isEmpty() || !(PalmSupply.reach(server, worldPosition) instanceof PalmSupply.Reached.Door door)) {
            restockOwed = false;
            setChanged();
            return true;
        }
        PalmSupply.Moved moved = PalmSupply.restockPattern(grid, pattern.get().kinds(), door.access());
        if (!moved.consulted()) return false;
        received(moved.fetched());
        restockOwed = false;
        if (moved.items() > 0) touch();
        else setChanged();
        return true;
    }

    /**
     * Starts a cycle when every condition holds: the grid is the pattern, 10 BU is in the tank, the recipe and its
     * result are what Select recorded, and the tray has room for the result and every remainder. Each failed
     * attempt names its reason and waits for a change or {@value #RETRY_TICKS} ticks. A start is spent from the
     * shared per-tick quota last, so a refused attempt costs none; a denied start tries again within a few ticks.
     */
    private void begin(ServerLevel server) {
        if (!due()) return;
        cooldown = RETRY_TICKS;
        if (pattern.isEmpty()) return;
        PalmRote.Pattern recorded = pattern.get();
        boolean reachedNetwork = supply(server);
        if (!PalmPlan.matches(recorded.kinds(), grid)) {
            judged = reachedNetwork ? PalmStatus.MISSING : PalmStatus.NO_SUPPLY;
            return;
        }
        if (!biomass.stored().atLeast(PalmRote.COST)) {
            judged = PalmStatus.NO_BIOMASS;
            return;
        }
        if (!(PalmPlan.resolve(server, recorded.kinds()) instanceof PalmPlan.Plan.Found found)
                || !found.recipe().equals(recorded.recipe())
                || !ItemStack.matches(found.result(), recorded.result())) {
            judged = PalmStatus.RECIPE_CHANGED;
            return;
        }
        if (PalmPlan.place(tray, found.result(), found.remainders()).isEmpty()) {
            judged = PalmStatus.TRAY_FULL;
            return;
        }
        if (!server.getServer()
                .overworld()
                .getData(ProcessingModule.PALM_STARTS)
                .take(server.getServer().getTickCount())) {
            judged = PalmStatus.READY;
            cooldown = 5 + Math.floorMod(worldPosition.hashCode(), RETRY_TICKS);
            return;
        }
        boolean supplied = false;
        for (int cell = 0; cell < PalmContents.CELLS; cell++) {
            if (recorded.kind(cell).isEmpty()) continue;
            supplied |= fetched.spend(grid, cell);
            grid.get(cell).shrink(1);
            if (grid.get(cell).isEmpty()) grid.set(cell, ItemStack.EMPTY);
        }
        biomass = ((Outcome.Done<BiomassTank>) biomass.drain(PalmRote.COST)).value();
        cycle = Optional.of(new PalmRote.Cycle(0, found.result(), found.remainders(), supplied));
        judged = PalmStatus.WORKING;
        cooldown = 0;
        touch();
        show();
    }

    /** Why the Palm cannot use its Mouth at all now, or empty. */
    private Optional<Refusal> reachRefusal() {
        if (unreadable.isPresent()) return Optional.of(Persistence.CORRUPT);
        return reach ? Optional.empty() : Optional.of(PalmSupply.NO_REACH);
    }

    /**
     * The door to the network now, or the reason there is none: no Reach, or no Mouth under the Palm. Looks at the one
     * position under the Palm, loads nothing, and keeps nothing, so a Mouth that is placed or broken is seen at once.
     */
    PalmSupply.Reached door() {
        Optional<Refusal> why = reachRefusal();
        if (why.isPresent()) return new PalmSupply.Reached.Refused(why.get());
        if (level == null) return new PalmSupply.Reached.Refused(PalmSupply.NO_MOUTH);
        return PalmSupply.reach(level, worldPosition);
    }

    /** True when the grid is empty and no unreadable bytes are kept. */
    boolean isEmpty() {
        return unreadable.isEmpty() && !rote && grid.stream().allMatch(ItemStack::isEmpty);
    }

    PalmContents contents() {
        return new PalmContents(grid);
    }

    /** The Rote state as one value, or empty for a Palm without Rote. */
    private Optional<PalmRote> roteState() {
        return rote ? Optional.of(new PalmRote(pattern, cycle, tray, biomass, cycles, restockOwed)) : Optional.empty();
    }

    /** The saved form of the whole Palm: its grid and, with Rote, its Rote state. */
    private record Saved(PalmContents contents, Optional<PalmRote> rote, Optional<List<ItemStack>> fetched) {
        Saved(PalmContents contents, Optional<PalmRote> rote) {
            this(contents, rote, Optional.empty());
        }
    }

    /** Why {@code saved} cannot be installed as a whole, or empty when every stack is one the game could hold. */
    private static Optional<String> check(Saved saved) {
        for (ItemStack stack : saved.contents().grid())
            if (stack.getCount() > stack.getMaxStackSize())
                return Optional.of(stack.getCount() + " of " + stack.getItem() + " in one cell");
        if (saved.rote().isEmpty()) return Optional.empty();
        PalmRote state = saved.rote().get();
        if (!state.biomass().capacity().equals(PalmRote.CAPACITY))
            return Optional.of("biomass capacity " + state.biomass().capacity().units());
        for (ItemStack stack : state.tray())
            if (stack.getCount() > stack.getMaxStackSize())
                return Optional.of(stack.getCount() + " of " + stack.getItem() + " in the tray");
        if (state.cycle().isPresent()) {
            PalmRote.Cycle running = state.cycle().get();
            if (running.result().getCount() > running.result().getMaxStackSize())
                return Optional.of("a cycle result over its stack limit");
            for (ItemStack stack : running.remainders())
                if (stack.getCount() > stack.getMaxStackSize())
                    return Optional.of("a cycle remainder over its stack limit");
        }
        return Optional.empty();
    }

    private void restore(Saved saved) {
        for (int cell = 0; cell < PalmContents.CELLS; cell++)
            grid.set(cell, saved.contents().cell(cell));
        fetched.load(saved.fetched(), grid);
        version++;
        rote = saved.rote().isPresent();
        PalmRote state = saved.rote().orElseGet(PalmRote::fresh);
        pattern = state.pattern();
        cycle = state.cycle();
        List<ItemStack> held = state.tray();
        for (int slot = 0; slot < PalmRote.TRAY; slot++) tray.set(slot, held.get(slot));
        biomass = state.biomass();
        cycles = state.cycles();
        restockOwed = state.restockOwed();
        dirty = true;
        cooldown = 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "state", BLEND_TICKS, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<Saved> read = Persistence.read(
                SAVED, tag, registries, ProcessingModule.LOGGER, "shaping_palm", ShapingPalmBlockEntity::check);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        read.ifPresent(this::restore);
        reach = tag.getBoolean(REACH);
        steward.load(tag);
        unreadable.ifPresent(kept -> kept.remove(LEGACY_LINK));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, new Saved(contents(), roteState(), fetched.saved()), registries, tag);
        steward.save(tag);
        tag.remove(REACH);
        tag.remove(LEGACY_LINK);
        if (reach) tag.putBoolean(REACH, true);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent()) {
            CompoundTag kept = unreadable.get().copy();
            kept.remove(LEGACY_LINK);
            kept.remove("fetched");
            kept.remove("steward");
            components.set(OrganModule.UNREADABLE_SAVE.get(), kept);
            return;
        }
        if (!grid.stream().allMatch(ItemStack::isEmpty))
            components.set(ProcessingModule.PALM_CONTENTS.get(), contents());
        if (reach) components.set(ProcessingModule.PALM_REACH.get(), true);
        roteState().ifPresent(state -> components.set(ProcessingModule.PALM_ROTE.get(), state));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            CompoundTag copy = kept.copy();
            copy.remove(LEGACY_LINK);
            unreadable = Optional.of(copy);
            reach = kept.getBoolean(REACH);
            return;
        }
        if (Boolean.TRUE.equals(input.get(ProcessingModule.PALM_REACH.get()))) reach = true;
        PalmContents carried = input.get(ProcessingModule.PALM_CONTENTS.get());
        PalmRote carriedRote = input.get(ProcessingModule.PALM_ROTE.get());
        if (carried == null && carriedRote == null) return;
        Saved saved = new Saved(carried == null ? PalmContents.EMPTY : carried, Optional.ofNullable(carriedRote));
        Optional<String> problem = check(saved);
        if (problem.isPresent()) {
            ProcessingModule.LOGGER.warn("shaping_palm keeps unreadable carried state: {}", problem.get());
            unreadable = Optional.of(
                    level == null ? new CompoundTag() : Persistence.encode(SAVED, saved, level.registryAccess()));
            return;
        }
        restore(saved);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("contents");
        tag.remove("rote");
        tag.remove("fetched");
        tag.remove("steward");
        tag.remove(REACH);
        tag.remove(LEGACY_LINK);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.infestusfrontier.shaping_palm");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ShapingPalmMenu(id, inventory, this);
    }
}
