package org.jd.infestusfrontier.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.GrowerProgress;
import org.jd.infestusfrontier.core.mining.PlanStep;
import org.jd.infestusfrontier.core.mining.Survey;
import org.jd.infestusfrontier.core.mining.Surveying;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Steward;
import org.jd.infestusfrontier.organ.api.Stewarded;

/**
 * The work of a Structure Grower. Every {@value Surveying#GROW_TICKS} loaded ticks it tries to build the next step of
 * the approved plan of the Surveyed Tissue under it: a root cell in the step's floor, a tread on that cell on a
 * staircase, and a wall torch at head height where the plan has a light. It builds only when every block of the step
 * is already clear and loaded, the root cell has solid ground under it, no creature stands in the way, a wall holds
 * the light and its stock covers the whole step; then it builds the step whole in one tick, pays for it and the marker
 * counts it. Otherwise it changes nothing, names the reason and looks again after {@value Surveying#RETRY_TICKS} ticks,
 * or at once when its stock changes. It never cuts, never collects and never touches a built step again, so whatever
 * the player does there stays. The growers of a dimension share {@value Surveying#STEPS_PER_TICK} steps a tick. Stock
 * arrives by hand, at its screen, or through its item handler, which only takes. It loads no chunk. In a Descending
 * Rootstock's minehead it builds just the same, and tells the Rootstock behind it of each step it built.
 */
final class StructureGrowerBlockEntity extends BlockEntity implements Stewarded, MenuProvider {
    /** Blocks around each block a step reads or changes whose chunks must be loaded: its neighbours, which it updates. */
    private static final int REACH = 1;

    private final ItemStackHandler segments = stock(stack -> stack.is(MiningModule.ROOT_SEGMENT.get()));
    private final ItemStackHandler treads = stock(StructureGrowerBlockEntity::tread);
    private final ItemStackHandler lights = stock(StructureGrowerBlockEntity::light);

    private GrowerProgress progress = GrowerProgress.START;
    private Optional<CompoundTag> unreadable = Optional.empty();
    private final Steward steward = new Steward();
    /** Why the last try did not build. Not saved: the next try tells again. */
    private Optional<Refusal> stop = Optional.empty();
    /** Loaded ticks before a stopped grower looks again. */
    private int retry;

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

    /** True for stairs a grower lays as treads: tagged, a plain stair block with no block entity. */
    static boolean tread(ItemStack stack) {
        return stack.is(MiningModule.MINE_TREADS)
                && stack.getItem() instanceof BlockItem item
                && item.getBlock() instanceof StairBlock
                && !item.getBlock().defaultBlockState().hasBlockEntity();
    }

    /** True for what a grower hangs as a light: a torch. */
    static boolean light(ItemStack stack) {
        return stack.is(Items.TORCH);
    }

    /** True for what the grower takes from a hand. */
    static boolean loads(ItemStack stack) {
        return stack.is(MiningModule.ROOT_SEGMENT.get()) || tread(stack) || light(stack);
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

    /** The stock changed: a stopped grower looks again on its next tick. */
    private void supplied() {
        retry = 0;
        setChanged();
    }

    /** The grower's stock seen from outside: it takes what each slot takes and gives nothing back. */
    IItemHandler intake() {
        List<ItemStackHandler> slots = List.of(segments, treads, lights);
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
        ItemStackHandler into = held.is(MiningModule.ROOT_SEGMENT.get()) ? segments : tread(held) ? treads : lights;
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
                        lights.getStackInSlot(0).getCount()),
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

    /** A step judged buildable: the marker, and where each of its blocks goes and what goes there, bottom up. */
    private record Build(SurveyedTissueBlockEntity marker, List<Placement> placements) {}

    private record Placement(BlockPos pos, BlockState state) {}

    private void tryGrow(ServerLevel level) {
        Optional<SurveyedTissueBlockEntity> marker = marker();
        Optional<Survey> survey = marker.flatMap(SurveyedTissueBlockEntity::survey);
        Outcome<Build> judged;
        if (survey.isEmpty()) judged = Outcome.refused(Surveying.NO_PLAN);
        else if (!survey.get().approved()) judged = Outcome.refused(Surveying.NOT_APPROVED);
        else if (survey.get().finished()) judged = Outcome.refused(Surveying.FINISHED);
        else judged = judge(level, marker.get(), survey.get().next());
        if (judged instanceof Outcome.Refused<Build>(Refusal why)) {
            stop = Optional.of(why);
            retry = Surveying.RETRY_TICKS;
            return;
        }
        // admitted after judging, so a step over the shared limit is simply tried again next tick
        if (!level.getData(MiningModule.GROWER_STEPS).take(level.getGameTime())) return;
        grow(level, ((Outcome.Done<Build>) judged).value());
    }

    /**
     * Whether {@code step} can be built now, and what it places. Reads loaded positions only and changes nothing. The
     * ground is judged first, then whether a creature stands in the way, then the stock, then the wall for a light.
     */
    private Outcome<Build> judge(ServerLevel level, SurveyedTissueBlockEntity marker, PlanStep step) {
        BlockPos root = marker.at(step.root());
        List<BlockPos> cleared = step.cleared().stream().map(marker::at).toList();
        for (BlockPos cell : cleared)
            if (!Cutting.loaded(level, cell, REACH)) return Outcome.refused(Surveying.UNLOADED);
        if (!Cutting.loaded(level, root.below(), REACH)) return Outcome.refused(Surveying.UNLOADED);
        for (BlockPos cell : cleared) if (!level.getFluidState(cell).isEmpty()) return Outcome.refused(Surveying.FLUID);
        for (BlockPos cell : cleared)
            if (!level.getBlockState(cell).isAir()) return Outcome.refused(Surveying.NOT_CLEAR);
        if (!level.getBlockState(root.below()).isFaceSturdy(level, root.below(), Direction.UP))
            return Outcome.refused(Surveying.NO_GROUND);
        Optional<BlockPos> treadAt = step.tread().map(marker::at);
        for (BlockPos solid : treadAt.isPresent() ? List.of(root, treadAt.get()) : List.of(root))
            if (!level.isUnobstructed(null, Shapes.block().move(solid.getX(), solid.getY(), solid.getZ())))
                return Outcome.refused(Surveying.CREATURE);
        if (segments.getStackInSlot(0).isEmpty()) return Outcome.refused(Surveying.NO_SEGMENT);
        if (treadAt.isPresent() && treads.getStackInSlot(0).isEmpty()) return Outcome.refused(Surveying.NO_TREAD);
        if (step.light().isPresent() && lights.getStackInSlot(0).isEmpty()) return Outcome.refused(Surveying.NO_LIGHT);

        Direction forward = marker.facing();
        Direction arrived = heading(forward, step.arrived());
        Direction leaving = heading(forward, step.leaving());
        List<Placement> placements = new ArrayList<>();
        placements.add(new Placement(
                root, step.bend() ? RootCellBlock.bend(arrived, leaving) : RootCellBlock.straight(leaving)));
        if (treadAt.isPresent()) {
            BlockState tread = ((BlockItem) treads.getStackInSlot(0).getItem())
                    .getBlock()
                    .defaultBlockState()
                    .setValue(StairBlock.FACING, forward.getOpposite())
                    .setValue(StairBlock.HALF, Half.BOTTOM);
            placements.add(new Placement(treadAt.get(), Block.updateFromNeighbourShapes(tread, level, treadAt.get())));
        }
        if (step.light().isPresent()) {
            Optional<Placement> light = light(level, marker.at(step.light().get()), leaving);
            if (light.isEmpty()) return Outcome.refused(Surveying.NO_WALL);
            placements.add(light.get());
        }
        return Outcome.done(new Build(marker, placements));
    }

    /** The way {@code quarters} quarter turns clockwise of {@code forward}. */
    private static Direction heading(Direction forward, int quarters) {
        Direction way = forward;
        for (int n = 0; n < quarters; n++) way = way.getClockWise();
        return way;
    }

    /** A wall torch at {@code at} on the wall left of {@code travel}, else on the right; empty when neither holds it. */
    private static Optional<Placement> light(ServerLevel level, BlockPos at, Direction travel) {
        for (Direction wall : List.of(travel.getCounterClockWise(), travel.getClockWise())) {
            BlockState torch =
                    Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, wall.getOpposite());
            if (torch.canSurvive(level, at)) return Optional.of(new Placement(at, torch));
        }
        return Optional.empty();
    }

    /** Places the judged step from the bottom up, pays for it and has the marker count it. */
    private void grow(ServerLevel level, Build build) {
        for (Placement placement : build.placements()) {
            level.setBlock(placement.pos(), placement.state(), Block.UPDATE_ALL);
            level.gameEvent(GameEvent.BLOCK_PLACE, placement.pos(), GameEvent.Context.of(placement.state()));
        }
        boolean treaded = build.placements().stream().anyMatch(p -> p.state().getBlock() instanceof StairBlock);
        boolean lit = build.placements().stream().anyMatch(p -> p.state().is(Blocks.WALL_TORCH));
        segments.extractItem(0, 1, false);
        if (treaded) treads.extractItem(0, 1, false);
        if (lit) lights.extractItem(0, 1, false);
        build.marker().grown();
        Minehead.rootstockOf(level, build.marker())
                .ifPresent(head -> head.advanced(
                        level, build.marker().survey().map(Survey::finished).orElse(false)));
        progress = progress.grown();
        stop = Optional.empty();
        setChanged();
        level.playSound(
                null, build.placements().get(0).pos(), SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 1F, 1F);
        steward.worked(level, MiningModule.STRUCTURE_GROWER.getId());
    }

    // ---- saved state ----

    StructureGrowerContents contents() {
        return new StructureGrowerContents(
                segments.getStackInSlot(0), treads.getStackInSlot(0), lights.getStackInSlot(0), progress);
    }

    private void restore(StructureGrowerContents contents) {
        segments.setStackInSlot(0, contents.segments().copy());
        treads.setStackInSlot(0, contents.treads().copy());
        lights.setStackInSlot(0, contents.lights().copy());
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
