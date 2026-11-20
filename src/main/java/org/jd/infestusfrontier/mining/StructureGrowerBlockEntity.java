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
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.Fracturing;
import org.jd.infestusfrontier.core.mining.GrowerProgress;
import org.jd.infestusfrontier.core.mining.Offset;
import org.jd.infestusfrontier.core.mining.PlanStep;
import org.jd.infestusfrontier.core.mining.Rootstocking;
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
 * staircase, a light where the plan has one, bone ribs where the plan frames the step and a cut scar where it marks
 * one. It builds only when every block of the step is already clear, of anything but fluid, and loaded, no creature
 * stands in the way and its stock covers the whole step; then it builds the step whole in one tick, pays for what it
 * placed and the marker counts it. Over missing ground it leaves the root cell and its tread out: an accident is built
 * through, never bridged. Otherwise it changes nothing, names the reason and looks again after
 * {@value Surveying#RETRY_TICKS} ticks, or at once when its stock changes. It never cuts, never collects and never
 * touches a built step again, so whatever the player does there stays. The growers of a dimension share {@value Surveying#STEPS_PER_TICK} steps a tick. Stock
 * arrives by hand, at its screen, or through its item handler, which only takes. It loads no chunk. In a Descending
 * Rootstock's minehead it builds just the same, and tells the Rootstock behind it of each step it built. On a gallery
 * face it builds a wide step's three root cells together, for a segment each, and tells the minehead that last named
 * it one of its faces; jaws facing it ask that minehead, through {@link #order}, which block to cut.
 */
final class StructureGrowerBlockEntity extends BlockEntity implements Stewarded, MenuProvider {
    /** Blocks around each block a step reads or changes whose chunks must be loaded: its neighbours, which it updates. */
    private static final int REACH = 1;

    private final ItemStackHandler segments = stock(stack -> stack.is(MiningModule.ROOT_SEGMENT.get()));
    private final ItemStackHandler treads = stock(StructureGrowerBlockEntity::tread);
    private final ItemStackHandler lights = stock(StructureGrowerBlockEntity::light);
    private final ItemStackHandler ribStock = stock(StructureGrowerBlockEntity::rib);

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

    /** True for stairs a grower lays as treads: tagged, a plain stair block with no block entity. */
    static boolean tread(ItemStack stack) {
        return stack.is(MiningModule.MINE_TREADS)
                && stack.getItem() instanceof BlockItem item
                && item.getBlock() instanceof StairBlock
                && !item.getBlock().defaultBlockState().hasBlockEntity();
    }

    /** True for what a grower lights a step with: a torch, or what it grows a Lumen Node from. */
    static boolean light(ItemStack stack) {
        return stack.is(Items.TORCH) || stack.is(MiningModule.LIVING_LIGHTS);
    }

    /** True for what a grower frames a step with, one a column: Bone Plates. */
    static boolean rib(ItemStack stack) {
        return stack.is(MiningModule.MINE_RIBS);
    }

    /** True for what the grower takes from a hand. */
    static boolean loads(ItemStack stack) {
        return stack.is(MiningModule.ROOT_SEGMENT.get()) || tread(stack) || light(stack) || rib(stack);
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
                : tread(held) ? treads : light(held) ? lights : ribStock;
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

    /**
     * A step judged buildable: the marker, where each of its blocks goes and what goes there, bottom up, what it pays
     * from each stock, and whether it is a step of the Wider Section.
     */
    private record Build(
            SurveyedTissueBlockEntity marker,
            List<Placement> placements,
            int segments,
            int treads,
            int lights,
            int ribs,
            boolean wide) {}

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
     * Whether {@code step} can be built now, and what it places. Reads loaded positions only and changes nothing. Every
     * block of the step must be clear, though fluid may stand in it: the grower builds into it as a player would. A
     * root cell with no solid ground under it is left out, with the tread on it, and not paid: the grower never
     * bridges a gap. Then whether a creature stands in the way, then the stock: segments for the grounded root cells,
     * a tread, a light, and ribs for a framed step. A light, ribs and a scar go only where a wall or a ceiling holds
     * them and the cell is dry; where none can, they are left out and not paid.
     */
    private Outcome<Build> judge(ServerLevel level, SurveyedTissueBlockEntity marker, PlanStep step) {
        List<BlockPos> roots = step.roots().stream().map(marker::at).toList();
        List<BlockPos> cleared = step.cleared().stream().map(marker::at).toList();
        for (BlockPos cell : cleared)
            if (!Cutting.loaded(level, cell, REACH)) return Outcome.refused(Surveying.UNLOADED);
        for (BlockPos root : roots)
            if (!Cutting.loaded(level, root.below(), REACH)) return Outcome.refused(Surveying.UNLOADED);
        for (BlockPos cell : cleared)
            if (!Cutting.open(level.getBlockState(cell))) return Outcome.refused(Surveying.NOT_CLEAR);
        List<BlockPos> grounded = roots.stream()
                .filter(root -> level.getBlockState(root.below()).isFaceSturdy(level, root.below(), Direction.UP))
                .toList();
        Optional<BlockPos> treadAt = step.tread().map(marker::at).filter(tread -> grounded.contains(tread.below()));
        List<BlockPos> solids = new ArrayList<>(grounded);
        treadAt.ifPresent(solids::add);
        for (BlockPos solid : solids)
            if (!level.isUnobstructed(null, Shapes.block().move(solid.getX(), solid.getY(), solid.getZ())))
                return Outcome.refused(Surveying.CREATURE);
        if (segments.getStackInSlot(0).getCount() < grounded.size()) return Outcome.refused(Surveying.NO_SEGMENT);
        if (treadAt.isPresent() && treads.getStackInSlot(0).isEmpty()) return Outcome.refused(Surveying.NO_TREAD);

        Direction forward = marker.facing();
        Direction arrived = MiningDirections.turn(forward, step.arrived());
        Direction leaving = MiningDirections.turn(forward, step.leaving());
        Optional<BlockPos> lightAt = step.light().map(marker::at);
        Optional<Placement> light = lightAt.flatMap(at -> light(level, at, leaving, cleared));
        if (light.isPresent() && lights.getStackInSlot(0).isEmpty()) return Outcome.refused(Surveying.NO_LIGHT);
        List<Placement> ribs = step.framed() ? ribs(level, marker, step, cleared, lightAt, leaving) : List.of();
        int ribsPaid = (int) ribs.stream()
                .map(rib -> columnOf(marker, step, rib.pos()))
                .distinct()
                .count();
        if (ribStock.getStackInSlot(0).getCount() < ribsPaid) return Outcome.refused(Surveying.NO_RIB);

        List<Placement> placements = new ArrayList<>();
        for (BlockPos root : grounded) {
            BlockState cell = root.equals(roots.get(0)) && step.bend()
                    ? RootCellBlock.bend(arrived, leaving)
                    : RootCellBlock.straight(leaving);
            placements.add(new Placement(root, cell));
        }
        if (treadAt.isPresent()) {
            BlockState tread = ((BlockItem) treads.getStackInSlot(0).getItem())
                    .getBlock()
                    .defaultBlockState()
                    .setValue(StairBlock.FACING, forward.getOpposite())
                    .setValue(StairBlock.HALF, Half.BOTTOM)
                    .setValue(
                            StairBlock.WATERLOGGED,
                            level.getFluidState(treadAt.get()).is(Fluids.WATER));
            placements.add(new Placement(treadAt.get(), Block.updateFromNeighbourShapes(tread, level, treadAt.get())));
        }
        light.ifPresent(placements::add);
        placements.addAll(ribs);
        if (step.scarred())
            scar(level, marker, step, cleared, ribs, lightAt, leaving).ifPresent(placements::add);
        return Outcome.done(new Build(
                marker,
                placements,
                grounded.size(),
                treadAt.isPresent() ? 1 : 0,
                light.isPresent() ? 1 : 0,
                ribsPaid,
                !step.flanks().isEmpty()));
    }

    /**
     * The light at {@code at}: from living-light stock a Lumen Node hung from the ceiling, else on the wall left of
     * {@code travel}, else on the right; from torches a wall torch on either wall. Empty when the cell holds fluid or
     * nothing holds the light: it is left out, not bridged or propped.
     */
    private Optional<Placement> light(ServerLevel level, BlockPos at, Direction travel, List<BlockPos> cleared) {
        if (!level.getBlockState(at).isAir()) return Optional.empty();
        List<Direction> walls = List.of(travel.getCounterClockWise(), travel.getClockWise());
        if (lights.getStackInSlot(0).is(Items.TORCH)) {
            for (Direction wall : walls) {
                BlockState torch =
                        Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, wall.getOpposite());
                if (!cleared.contains(at.relative(wall)) && torch.canSurvive(level, at))
                    return Optional.of(new Placement(at, torch));
            }
            return Optional.empty();
        }
        BlockState node = MiningModule.LUMEN_NODE.get().defaultBlockState();
        List<BlockState> hung = new ArrayList<>();
        hung.add(node.setValue(LumenNodeBlock.FACE, AttachFace.CEILING).setValue(LumenNodeBlock.FACING, travel));
        for (Direction wall : walls)
            hung.add(node.setValue(LumenNodeBlock.FACE, AttachFace.WALL)
                    .setValue(LumenNodeBlock.FACING, wall.getOpposite()));
        for (BlockState state : hung)
            if (!cleared.contains(at.relative(LumenNodeBlock.support(state))) && state.canSurvive(level, at))
                return Optional.of(new Placement(at, state));
        return Optional.empty();
    }

    /**
     * The bone ribs of a framed step: on each dry, empty passage cell but the light's, a rib on each side wall and, on
     * a column's top cell, the ceiling, where that face is outside the step and holds it and is not ore. Thick on a
     * step of the Wider Section. They have no collision, so the passage keeps its headroom and width.
     */
    private static List<Placement> ribs(
            ServerLevel level,
            SurveyedTissueBlockEntity marker,
            PlanStep step,
            List<BlockPos> cleared,
            Optional<BlockPos> light,
            Direction leaving) {
        BlockState rib = MiningModule.BONE_RIB
                .get()
                .defaultBlockState()
                .setValue(BoneRibBlock.AXIS, leaving.getAxis())
                .setValue(BoneRibBlock.THICK, !step.flanks().isEmpty());
        List<Placement> ribs = new ArrayList<>();
        for (Offset column : step.roots()) {
            BlockPos top = topOf(marker, step, column);
            for (Offset offset : step.passage()) {
                BlockPos cell = marker.at(offset);
                if (columnOf(marker, step, cell) != step.roots().indexOf(column)
                        || light.equals(Optional.of(cell))
                        || !level.getBlockState(cell).isAir()) continue;
                List<Direction> sides = new ArrayList<>(List.of(leaving.getCounterClockWise(), leaving.getClockWise()));
                if (cell.equals(top)) sides.add(Direction.UP);
                BlockState faced = rib;
                for (Direction side : sides)
                    if (holds(level, cell, side, cleared))
                        faced = faced.setValue(MultifaceBlock.getFaceProperty(side), true);
                if (faced != rib) ribs.add(new Placement(cell, faced));
            }
        }
        return ribs;
    }

    /**
     * The cut scar of a scarred step: on the lowest passage cell of its outermost column on the scar's side, against that
     * wall, when the cell is dry and empty and holds no rib or light, and the wall holds it and is not ore.
     */
    private static Optional<Placement> scar(
            ServerLevel level,
            SurveyedTissueBlockEntity marker,
            PlanStep step,
            List<BlockPos> cleared,
            List<Placement> ribs,
            Optional<BlockPos> light,
            Direction leaving) {
        Direction side = MiningDirections.turn(leaving, step.scarSide());
        BlockPos root = marker.at(step.root());
        BlockPos outer = root.relative(side);
        BlockPos column = step.roots().stream().map(marker::at).anyMatch(outer::equals) ? outer : root;
        BlockPos cell = column.above(step.tread().isPresent() ? 2 : 1);
        boolean taken = light.equals(Optional.of(cell))
                || ribs.stream().anyMatch(rib -> rib.pos().equals(cell));
        if (taken || !level.getBlockState(cell).isAir() || !holds(level, cell, side, cleared)) return Optional.empty();
        return Optional.of(new Placement(
                cell,
                MiningModule.CUT_SCAR.get().defaultBlockState().setValue(MultifaceBlock.getFaceProperty(side), true)));
    }

    /** True when the block on {@code side} of {@code cell} lies outside the step, holds a growth and is not ore. */
    private static boolean holds(ServerLevel level, BlockPos cell, Direction side, List<BlockPos> cleared) {
        BlockPos wall = cell.relative(side);
        BlockState state = level.getBlockState(wall);
        return !cleared.contains(wall)
                && !state.is(Tags.Blocks.ORES)
                && MultifaceBlock.canAttachTo(level, side, wall, state);
    }

    /** The index among the step's root cells of the column {@code cell} stands over. */
    private static int columnOf(SurveyedTissueBlockEntity marker, PlanStep step, BlockPos cell) {
        List<Offset> roots = step.roots();
        for (int n = 0; n < roots.size(); n++) {
            BlockPos root = marker.at(roots.get(n));
            if (root.getX() == cell.getX() && root.getZ() == cell.getZ()) return n;
        }
        return -1;
    }

    /** The highest passage cell over {@code column}. */
    private static BlockPos topOf(SurveyedTissueBlockEntity marker, PlanStep step, Offset column) {
        BlockPos top = marker.at(column);
        for (Offset offset : step.passage()) {
            BlockPos cell = marker.at(offset);
            if (cell.getX() == top.getX() && cell.getZ() == top.getZ() && cell.getY() > top.getY()) top = cell;
        }
        return top;
    }

    /** Places the judged step from the bottom up, pays for it and has the marker count it. */
    private void grow(ServerLevel level, Build build) {
        for (Placement placement : build.placements()) {
            level.setBlock(placement.pos(), placement.state(), Block.UPDATE_ALL);
            level.gameEvent(GameEvent.BLOCK_PLACE, placement.pos(), GameEvent.Context.of(placement.state()));
        }
        segments.extractItem(0, build.segments(), false);
        treads.extractItem(0, build.treads(), false);
        lights.extractItem(0, build.lights(), false);
        ribStock.extractItem(0, build.ribs(), false);
        build.marker().grown();
        Optional<TunnelRootBlockEntity> staircase = Minehead.rootstockOf(level, build.marker());
        if (staircase.isPresent())
            staircase
                    .get()
                    .advanced(
                            level, build.marker().survey().map(Survey::finished).orElse(false));
        else minehead(level).ifPresent(head -> head.galleryAdvanced(level, build.wide()));
        progress = progress.grown();
        stop = Optional.empty();
        setChanged();
        BlockPos sound = build.placements().isEmpty()
                ? build.marker().getBlockPos()
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
