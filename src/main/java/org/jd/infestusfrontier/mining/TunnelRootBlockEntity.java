package org.jd.infestusfrontier.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.Counts;
import org.jd.infestusfrontier.core.mining.Fracturing;
import org.jd.infestusfrontier.core.mining.Galleries;
import org.jd.infestusfrontier.core.mining.RootMutation;
import org.jd.infestusfrontier.core.mining.RootMutations;
import org.jd.infestusfrontier.core.mining.Rootstocking;
import org.jd.infestusfrontier.core.mining.Steer;
import org.jd.infestusfrontier.core.mining.Survey;
import org.jd.infestusfrontier.core.mining.TunnelProgress;
import org.jd.infestusfrontier.core.mining.Tunnelling;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.organ.api.Steward;
import org.jd.infestusfrontier.organ.api.Stewarded;
import org.jd.infestusfrontier.organ.api.WorkshopActionEvent;
import org.jd.infestusfrontier.storage.StorageModule;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The work of a Tunnel Root head. Every {@value Tunnelling#STEP_TICKS} loaded ticks it tries one step: it turns as
 * steered, cuts the two blocks ahead at a stone pickaxe's grade into its output, lays a root cell in the floor under
 * them, pays one segment and {@value Tunnelling#STEP_BIOMASS_BU} BU, and moves into the cut. Everything a step reads
 * and changes is judged first, so a step is done whole in one tick or not at all; a stopped head keeps its step due,
 * names the reason and looks again after {@value Tunnelling#RETRY_TICKS} ticks, or at once when its supplies change.
 * Only its supplies, its grade, its length and unloaded chunks stop it. It cuts into water and lava, under gravel and
 * over holes as the player steered it: fluid in the cut is simply left to flow, and over a missing floor it lays no
 * cell, steps onto nothing and dies. Its steering is its block state, which the client shows.
 * Nothing is fed to it by veins, hoppers or pipes: it has no capability. It loads no chunk. A genetic construct
 * applied by hand gives it a permanent mutation, see {@link #mutate}; a head with Stairs is a Descending Rootstock.
 * Mutations change none of the steps it takes by hand.
 *
 * <p>A Rootstock that faces a Surveyed Tissue marker is a minehead ({@link Minehead}) and takes no steps: it stays
 * where it is, orders its Fracture Jaw's cuts through {@link #order}, counts each staircase step its Structure Grower
 * builds as its own advance, and every {@value Rootstocking#CHECK_TICKS} loaded ticks looks at its parts and its next
 * step to name what holds the minehead up. Its segments, output, cells, biomass, mutations and steward stay as they
 * were; broken and placed again it comes back with them and with its advance.
 *
 * <p>Once its staircase's landing is built the minehead also looks, on the same beat, for its gallery faces
 * ({@link Galleries}), names what holds each one up and tells each face's grower it is one of its faces. A jaw of a
 * face asks through that grower, see {@link #orderFace}; the minehead gives its faces cuts in turn. What it knows of its
 * faces is not saved: the next look tells again.
 *
 * <p>Moving is a new block entity at the cut: this one hands it its whole saved state, steward included, and leaves.
 */
final class TunnelRootBlockEntity extends BlockEntity implements Stewarded, MenuProvider, GeoBlockEntity {
    /** At rest the head leans towards its next cut: straight, left or right. */
    private static final RawAnimation REST = RawAnimation.begin().thenLoop("idle");

    private static final RawAnimation AIM_LEFT = RawAnimation.begin().thenLoop("aim_left");
    private static final RawAnimation AIM_RIGHT = RawAnimation.begin().thenLoop("aim_right");
    /** Just moved: it reaches into the new cell, grips and pulls its body in, turning on a turn, then holds still. */
    private static final RawAnimation CRAWL = RawAnimation.begin().thenPlayAndHold("crawl");

    private static final RawAnimation CRAWL_LEFT = RawAnimation.begin().thenPlayAndHold("crawl_left");
    private static final RawAnimation CRAWL_RIGHT = RawAnimation.begin().thenPlayAndHold("crawl_right");
    /** Ticks the model blends from one clip into the next. */
    private static final int BLEND_TICKS = 4;

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    /** Blocks from the head whose chunks a step needs: the cut, its neighbours, and the blocks their updates reach. */
    private static final int REACH = 3;

    private final ItemStackHandler segments = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return unreadable.isEmpty() && stack.is(MiningModule.ROOT_SEGMENT.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return Tunnelling.MAX_SEGMENTS;
        }

        @Override
        protected void onContentsChanged(int slot) {
            supplied();
        }
    };

    /** Only steps put items here; the screen's slots take them out and never put any in. */
    private final ItemStackHandler output = new ItemStackHandler(Tunnelling.OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            supplied();
        }
    };

    private BiomassTank tank = TunnelRootContents.EMPTY.tank();
    private TunnelProgress progress = TunnelProgress.START;
    private RootMutations mutations = RootMutations.NONE;
    /** Staircase steps built under this Rootstock's order: its minehead history. */
    private int descended;

    private Optional<CompoundTag> unreadable = Optional.empty();
    private final Steward steward = new Steward();
    /** Why the last try did not step. Not saved: the next try tells again. */
    private Optional<Refusal> stop = Optional.empty();
    /** Loaded ticks before a stopped head looks again. */
    private int retry;
    /** True while the head works as a minehead. Not saved: the next tick tells again. */
    private boolean minehead;
    /** Steps of the minehead's section built, as last looked at. Not saved: the marker owns them. */
    private int built;
    /** Steps the minehead's section has in all, as last looked at. Not saved. */
    private int steps;
    /** The minehead's gallery faces as last looked at, in the order they take turns. Not saved. */
    private List<Minehead.FaceLook> faces = List.of();
    /** The face whose turn it is to be given a cut. Not saved: after a reload the first face goes first. */
    private int turn;

    TunnelRootBlockEntity(BlockPos pos, BlockState state) {
        super(MiningModule.TUNNEL_ROOT_ENTITY.get(), pos, state);
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

    ItemStackHandler output() {
        return output;
    }

    BiomassTank tank() {
        return tank;
    }

    TunnelProgress progress() {
        return progress;
    }

    /** The way the next step goes, as its block state says. */
    Steer steer() {
        return restoredSteer.orElseGet(
                () -> getBlockState().getValue(TunnelRootBlock.STEER).steer());
    }

    RootMutations mutations() {
        return mutations;
    }

    /** True for a readable head with Stairs: a Descending Rootstock. */
    boolean rootstock() {
        return unreadable.isEmpty() && mutations.rootstock();
    }

    int descended() {
        return descended;
    }

    boolean minehead() {
        return minehead;
    }

    int built() {
        return built;
    }

    int steps() {
        return steps;
    }

    List<Minehead.FaceLook> faces() {
        return faces;
    }

    Optional<Refusal> stop() {
        return stop;
    }

    /** The player changed what the head holds: it looks again on its next tick. */
    private void supplied() {
        retry = 0;
        setChanged();
    }

    /** Sets the way of the next step. Refused, changing nothing, while the saved state is unreadable. */
    void steer(Steer way) {
        if (unreadable.isPresent() || level == null || steer() == way) return;
        level.setBlock(
                worldPosition, getBlockState().setValue(TunnelRootBlock.STEER, Stride.of(way)), Block.UPDATE_CLIENTS);
        restoredSteer = Optional.empty();
        supplied();
    }

    // ---- loading by hand ----

    /** True for what the head takes from a hand: Root Segments and a bucket of biomass. */
    static boolean loads(ItemStack stack) {
        return stack.is(MiningModule.ROOT_SEGMENT.get()) || stack.is(StorageModule.BIOMASS_BUCKET.get());
    }

    /** Takes the segments or the biomass in {@code hand}, whole or as much as fits, or names why it took nothing. */
    void load(Player player, InteractionHand hand) {
        if (unreadable.isPresent()) {
            Refusals.show(player, Persistence.CORRUPT);
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        Optional<Refusal> refused =
                held.is(MiningModule.ROOT_SEGMENT.get()) ? loadSegments(player, held) : loadBiomass(player, hand);
        if (refused.isPresent()) {
            Refusals.show(player, refused.get());
            return;
        }
        steward.assign(player);
        setChanged();
        player.displayClientMessage(
                Component.translatable(
                        "infestusfrontier.tunnel_root.loaded",
                        segments.getStackInSlot(0).getCount(),
                        Tunnelling.MAX_SEGMENTS,
                        tank.stored().units(),
                        tank.capacity().units()),
                true);
    }

    private Optional<Refusal> loadSegments(Player player, ItemStack held) {
        ItemStack left = segments.insertItem(0, held.copy(), false);
        int taken = held.getCount() - left.getCount();
        if (taken == 0) return Optional.of(Tunnelling.SEGMENTS_FULL);
        if (!player.getAbilities().instabuild) held.shrink(taken);
        return Optional.empty();
    }

    private Optional<Refusal> loadBiomass(Player player, InteractionHand hand) {
        Outcome<BiomassTank> filled = tank.fill(Biomass.BUCKET);
        if (!(filled instanceof Outcome.Done<BiomassTank> done))
            return Optional.of(((Outcome.Refused<BiomassTank>) filled).refusal());
        tank = done.value();
        player.setItemInHand(
                hand, ItemUtils.createFilledResult(player.getItemInHand(hand), player, new ItemStack(Items.BUCKET)));
        if (level != null) level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1F, 1F);
        supplied();
        return Optional.empty();
    }

    // ---- mutation ----

    /**
     * Applies the construct in {@code hand} for its mutation, or names why not and takes nothing. The head must stand
     * on its root, must not have the mutation yet, must already be a Rootstock for a mutation that needs one, and its
     * tank must hold the mutation's biomass. Then it takes one construct and that biomass once and keeps the mutation
     * for good. Its segments, output, cells and steward stay as they were: a mutation permits an operation
     * and supplies nothing that operation uses.
     */
    void mutate(Player player, InteractionHand hand, RootMutation mutation) {
        Optional<Refusal> refused = mutationRefusal(mutation);
        if (refused.isPresent()) {
            Refusals.show(player, refused.get());
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild) held.shrink(1);
        tank = ((Outcome.Done<BiomassTank>) tank.drain(Biomass.of(mutation.biomassUnits()))).value();
        mutations = mutations.with(mutation);
        steward.assign(player);
        supplied();
        if (level != null)
            level.playSound(null, worldPosition, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 1F, 0.6F);
        player.displayClientMessage(
                Component.translatable(
                        "infestusfrontier.tunnel_root.mutated",
                        Component.translatable("gui.infestusfrontier.tunnel_root.mutation." + mutation.id())),
                true);
        if (player instanceof ServerPlayer server)
            NeoForge.EVENT_BUS.post(new WorkshopActionEvent(
                    server,
                    mutation == RootMutation.STAIRS
                            ? WorkshopActionEvent.Action.STAIRS_INSTALLED
                            : WorkshopActionEvent.Action.WIDER_SECTION_INSTALLED));
    }

    /** Why {@code mutation} cannot be applied now, or empty when it can. Reads, changes nothing. */
    private Optional<Refusal> mutationRefusal(RootMutation mutation) {
        if (unreadable.isPresent()) return Optional.of(Persistence.CORRUPT);
        if (!(level instanceof ServerLevel server) || !rooted(server, worldPosition))
            return Optional.of(Tunnelling.UNROOTED);
        Optional<Refusal> refused = mutations.refusal(mutation);
        if (refused.isPresent()) return refused;
        if (!tank.stored().atLeast(Biomass.of(mutation.biomassUnits())))
            return Optional.of(Tunnelling.MUTATION_BIOMASS);
        return Optional.empty();
    }

    // ---- the work ----

    static void serverTick(Level level, BlockPos pos, BlockState state, TunnelRootBlockEntity head) {
        if (head.unreadable.isPresent() || !(level instanceof ServerLevel server)) return;
        if (head.restoredSteer.isPresent()) {
            state = state.setValue(TunnelRootBlock.STEER, Stride.of(head.restoredSteer.get()));
            head.restoredSteer = Optional.empty();
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
        if (state.getValue(TunnelRootBlock.STRIDE) != Stride.REST && head.progress.ticks() >= Tunnelling.CRAWL_TICKS)
            level.setBlock(pos, state.setValue(TunnelRootBlock.STRIDE, Stride.REST), Block.UPDATE_CLIENTS);
        Direction facing = state.getValue(TunnelRootBlock.FACING);
        head.minehead = head.mutations.rootstock() && Minehead.faces(level, pos, facing);
        if (head.minehead) {
            head.oversee(server, pos, facing);
            return;
        }
        if (!head.progress.due()) {
            head.progress = head.progress.waited();
            head.setChanged();
            return;
        }
        if (head.retry > 0) {
            head.retry--;
            return;
        }
        head.tryStep(server, pos, state);
    }

    // ---- the minehead ----

    /**
     * Every {@value Rootstocking#CHECK_TICKS} loaded ticks, or at once after a change, names what holds it up; once its
     * landing is built, also its gallery faces: it looks at each, names what holds it up and tells its grower it is one
     * of this minehead's faces.
     */
    private void oversee(ServerLevel level, BlockPos pos, Direction facing) {
        if (retry > 0) {
            retry--;
            return;
        }
        retry = Rootstocking.CHECK_TICKS;
        stop = Minehead.status(level, pos, facing);
        Optional<SurveyedTissueBlockEntity> marker =
                level.getBlockEntity(Minehead.marker(pos, facing)) instanceof SurveyedTissueBlockEntity found
                        ? Optional.of(found)
                        : Optional.empty();
        Optional<Survey> survey = marker.flatMap(SurveyedTissueBlockEntity::survey);
        built = survey.map(Survey::built).orElse(0);
        steps = survey.map(s -> s.plan().steps().size()).orElse(0);
        faces = List.of();
        if (marker.isEmpty() || survey.isEmpty() || !survey.get().finished()) return;
        Outcome<List<SurveyedTissueBlockEntity>> walked = Minehead.galleries(level, marker.get());
        if (walked instanceof Outcome.Refused<List<SurveyedTissueBlockEntity>>(Refusal why)) {
            stop = Optional.of(why);
            return;
        }
        List<Minehead.FaceLook> looked = new ArrayList<>();
        for (SurveyedTissueBlockEntity face : ((Outcome.Done<List<SurveyedTissueBlockEntity>>) walked).value()) {
            Minehead.FaceLook look = Minehead.look(level, face, mutations);
            looked.add(look);
            if (level.getBlockEntity(look.grower()) instanceof StructureGrowerBlockEntity grower)
                grower.commission(pos, level.getGameTime());
        }
        faces = List.copyOf(looked);
        if (turn >= faces.size()) turn = 0;
        if (!faces.isEmpty() && stop.equals(Optional.of(Rootstocking.FINISHED))) stop = Optional.empty();
        if (faces.stream().filter(Minehead.FaceLook::ready).count() >= 2)
            steward.worked(level, MiningModule.SHARED_FACES);
    }

    /**
     * The block a jaw of the gallery face whose grower stands at {@code grower} is to cut now, or
     * {@link Fracturing#NO_ORDER}: the face is not one of this minehead's as last looked at, something holds it up, its
     * next step is clear and waits for the grower, or another face that can cut has its turn first. A face given a cut
     * passes the turn to the next face.
     */
    Outcome<BlockPos> orderFace(ServerLevel level, BlockPos grower) {
        int index = -1;
        for (int face = 0; face < faces.size(); face++)
            if (faces.get(face).grower().equals(grower)) index = face;
        if (!rootstock() || index < 0 || !level.hasChunkAt(grower.below())) return Outcome.refused(Fracturing.NO_ORDER);
        if (!(level.getBlockEntity(grower.below()) instanceof SurveyedTissueBlockEntity marker)
                || !(Minehead.face(level, marker) instanceof Outcome.Done<Minehead.Face>(Minehead.Face face))
                || !(Minehead.faceCut(level, face, mutations)
                        instanceof Outcome.Done<Optional<BlockPos>>(Optional<BlockPos> cut))
                || cut.isEmpty()) return Outcome.refused(Fracturing.NO_ORDER);
        boolean[] ready = new boolean[faces.size()];
        for (int other = 0; other < ready.length; other++)
            ready[other] = faces.get(other).ready();
        if (!Galleries.admitted(turn, index, ready)) return Outcome.refused(Fracturing.NO_ORDER);
        turn = Galleries.after(index, faces.size());
        return Outcome.done(cut.get());
    }

    /** A gallery face of this minehead built its next step: credit for the gallery, and for a wide step its width. */
    void galleryAdvanced(ServerLevel level, boolean wide) {
        if (unreadable.isPresent()) return;
        retry = 0;
        steward.worked(level, MiningModule.GALLERY_STEP);
        if (wide) steward.worked(level, MiningModule.WIDE_GALLERY_STEP);
    }

    /**
     * The block the jaw behind this Rootstock is to cut now, or {@link Fracturing#NO_ORDER} when it orders none: it is
     * no minehead, something holds the minehead up, or the next step is clear and waits for the grower.
     */
    Outcome<BlockPos> order(ServerLevel level) {
        Direction facing = getBlockState().getValue(TunnelRootBlock.FACING);
        if (!rootstock() || !Minehead.faces(level, worldPosition, facing)) return Outcome.refused(Fracturing.NO_ORDER);
        if (Minehead.order(level, worldPosition, facing) instanceof Outcome.Done<Minehead.Order>(Minehead.Order now)
                && now.cut().isPresent()) return Outcome.done(now.cut().get());
        return Outcome.refused(Fracturing.NO_ORDER);
    }

    /**
     * The grower built the next step of this minehead's section: the root advanced one step, once. {@code landed}
     * when that step was the landing.
     */
    void advanced(ServerLevel level, boolean landed) {
        if (unreadable.isPresent()) return;
        descended = Counts.more(descended, 1);
        retry = 0;
        setChanged();
        steward.worked(level, MiningModule.ROOTSTOCK_STEP);
        if (landed) steward.worked(level, MiningModule.ROOTSTOCK_LANDING);
    }

    /** The way the next step goes: as steered from where the head faces. */
    private Direction way(Direction facing) {
        return MiningDirections.turn(facing, steer().quarters());
    }

    private void tryStep(ServerLevel level, BlockPos pos, BlockState state) {
        Direction way = way(state.getValue(TunnelRootBlock.FACING));
        BlockPos ahead = pos.relative(way);
        Optional<Refusal> blocked = blocked(level, pos, ahead);
        Optional<List<ItemStack>> after = Optional.empty();
        if (blocked.isEmpty()) {
            List<ItemStack> drops = new ArrayList<>(Cutting.loot(level, ahead.above()));
            drops.addAll(Cutting.loot(level, ahead));
            after = contents().outputWith(drops);
            if (after.isEmpty()) blocked = Optional.of(Tunnelling.OUTPUT_FULL);
        }
        if (blocked.isPresent()) {
            stop = blocked;
            retry = Tunnelling.RETRY_TICKS;
            return;
        }
        step(level, pos, state, way, after.get());
    }

    /** Why a step from {@code pos} into {@code ahead} cannot be taken now, or empty when it can. Reads, changes nothing. */
    private Optional<Refusal> blocked(ServerLevel level, BlockPos pos, BlockPos ahead) {
        if (!rooted(level, pos)) return Optional.of(Tunnelling.UNROOTED);
        if (progress.finished()) return Optional.of(Tunnelling.FULL_LENGTH);
        if (segments.getStackInSlot(0).isEmpty()) return Optional.of(Tunnelling.NO_SEGMENT);
        if (!tank.stored().atLeast(Biomass.of(Tunnelling.STEP_BIOMASS_BU))) return Optional.of(Tunnelling.NO_BIOMASS);
        if (!Cutting.loaded(level, pos, REACH)) return Optional.of(Tunnelling.UNLOADED);
        BlockPos top = ahead.above();
        BlockPos floor = ahead.below();
        if (level.isOutsideBuildHeight(top.above()) || level.isOutsideBuildHeight(floor))
            return Optional.of(Tunnelling.UNSUPPORTED);
        for (BlockPos cut : List.of(top, ahead)) {
            Optional<Refusal> refused = cuttable(level, cut);
            if (refused.isPresent()) return refused;
        }
        if (!missing(level, floor) && !floor(level, floor)) return Optional.of(Tunnelling.NO_FLOOR);
        return Optional.empty();
    }

    /** True while the head stands on its root: a root cell, or the foundation it was grown or placed on. */
    static boolean rooted(ServerLevel level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(MiningModule.ROOT_CELL.get()) || below.is(Organs.FOUNDATIONS);
    }

    /** Why the block at {@code cut} cannot be cut, or empty when it can (air included). */
    private static Optional<Refusal> cuttable(ServerLevel level, BlockPos cut) {
        return switch (Cutting.judge(level, cut)) {
            case CLEAR, CUTTABLE -> Optional.empty();
            case UNSUPPORTED -> Optional.of(Tunnelling.UNSUPPORTED);
            case TOO_HARD -> Optional.of(Tunnelling.TOO_HARD);
        };
    }

    /** True for a floor that is not there: air, fluid, or anything open a root cell would float over. */
    private static boolean missing(ServerLevel level, BlockPos floor) {
        return level.getBlockState(floor).canBeReplaced();
    }

    /** True for plain ground a root cell may replace: tagged, dry, no block entity. */
    private static boolean floor(ServerLevel level, BlockPos floor) {
        BlockState state = level.getBlockState(floor);
        return state.is(MiningModule.TUNNEL_FLOOR) && state.getFluidState().isEmpty() && !state.hasBlockEntity();
    }

    /**
     * Takes the judged step: clears the cut, lays the cell, pays, and moves the head with everything it owns into the
     * cut, where it shows the stride it took while it crawls in. The lower cut block goes first and quietly, so neither
     * cut block's removal can update the other into a second, loose drop (a door's halves, a lantern hanging from the
     * upper block, a torch standing on the lower one); a waterlogged block leaves its fluid, as breaking it would. The
     * lower block's outside neighbours get their updates when the head moves in. Over a missing floor no cell is laid
     * and no segment paid, and the head, standing on nothing, dies.
     */
    private void step(ServerLevel level, BlockPos pos, BlockState state, Direction way, List<ItemStack> after) {
        Direction facing = state.getValue(TunnelRootBlock.FACING);
        BlockPos ahead = pos.relative(way);
        BlockState lower = level.getBlockState(ahead);
        if (!Cutting.open(lower)) {
            level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, ahead, Block.getId(lower));
            level.setBlock(
                    ahead, lower.getFluidState().createLegacyBlock(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            level.gameEvent(GameEvent.BLOCK_DESTROY, ahead, GameEvent.Context.of(lower));
        }
        if (!Cutting.open(level.getBlockState(ahead.above()))) level.destroyBlock(ahead.above(), false);
        boolean laid = !missing(level, ahead.below());
        if (laid) level.setBlock(ahead.below(), RootCellBlock.straight(way), Block.UPDATE_ALL);
        if (way != facing && level.getBlockState(pos.below()).is(MiningModule.ROOT_CELL.get()))
            level.setBlock(pos.below(), RootCellBlock.bend(facing, way), Block.UPDATE_ALL);

        ItemStack paid = segments.getStackInSlot(0).copy();
        if (laid) paid.shrink(1);
        restore(new TunnelRootContents(
                paid,
                after,
                ((Outcome.Done<BiomassTank>) tank.drain(Biomass.of(Tunnelling.STEP_BIOMASS_BU))).value(),
                progress.stepped(),
                Optional.of(Steer.STRAIGHT),
                mutations,
                descended));
        stop = Optional.empty();

        CompoundTag carried = saveWithoutMetadata(level.registryAccess());
        level.setBlock(
                ahead,
                state.setValue(TunnelRootBlock.FACING, way)
                        .setValue(TunnelRootBlock.STEER, Stride.STRAIGHT)
                        .setValue(TunnelRootBlock.STRIDE, state.getValue(TunnelRootBlock.STEER)),
                Block.UPDATE_ALL);
        if (!(level.getBlockEntity(ahead) instanceof TunnelRootBlockEntity moved)) {
            MiningModule.LOGGER.error("Tunnel Root at {} could not move into {}; it stays", pos, ahead);
            return;
        }
        carried.remove("steer");
        moved.loadWithComponents(carried, level.registryAccess());
        moved.setChanged();
        restore(TunnelRootContents.EMPTY); // this block entity leaves: nothing stays behind to drop or duplicate
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.playSound(null, ahead, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 0.8F, 0.8F);
        moved.steward.worked(level, MiningModule.TUNNEL_ROOT.getId());
        if (!MiningOrganBlock.supported(level, ahead)) MiningOrganBlock.die(level, ahead);
    }

    // ---- appearance ----

    /** The client shows the crawl while the block state names a stride, else the lean towards the next cut. */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "crawl", BLEND_TICKS, state -> state.setAndContinue(shown())));
    }

    private RawAnimation shown() {
        BlockState state = getBlockState();
        return switch (state.getValue(TunnelRootBlock.STRIDE)) {
            case STRAIGHT -> CRAWL;
            case LEFT -> CRAWL_LEFT;
            case RIGHT -> CRAWL_RIGHT;
            case REST ->
                switch (state.getValue(TunnelRootBlock.STEER)) {
                    case LEFT -> AIM_LEFT;
                    case RIGHT -> AIM_RIGHT;
                    case REST, STRAIGHT -> REST;
                };
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }

    // ---- saved state ----

    private Optional<Steer> restoredSteer = Optional.empty();

    TunnelRootContents contents() {
        List<ItemStack> held = new ArrayList<>(Tunnelling.OUTPUT_SLOTS);
        for (int slot = 0; slot < Tunnelling.OUTPUT_SLOTS; slot++) held.add(output.getStackInSlot(slot));
        return new TunnelRootContents(
                segments.getStackInSlot(0), held, tank, progress, Optional.of(steer()), mutations, descended);
    }

    private void restore(TunnelRootContents contents) {
        restoredSteer = contents.steer();
        segments.setStackInSlot(0, contents.segments().copy());
        for (int slot = 0; slot < Tunnelling.OUTPUT_SLOTS; slot++)
            output.setStackInSlot(slot, contents.output().get(slot).copy());
        tank = contents.tank();
        progress = contents.progress();
        mutations = contents.mutations();
        descended = contents.descended();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<TunnelRootContents> read = Persistence.read(
                TunnelRootContents.CODEC, tag, registries, MiningModule.LOGGER, "Tunnel Root at " + worldPosition);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        restore(read.orElse(TunnelRootContents.EMPTY));
        stop = Optional.empty();
        retry = 0;
        steward.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(TunnelRootContents.CODEC, contents(), registries, tag);
        steward.save(tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent())
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
        else components.set(MiningModule.TUNNEL_ROOT_CONTENTS.get(), contents());
    }

    /** A carried head keeps what it held and the cells it laid; carried bytes that fail their check stay unreadable. */
    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            return;
        }
        TunnelRootContents carried = input.get(MiningModule.TUNNEL_ROOT_CONTENTS.get());
        if (carried == null) return;
        Optional<String> problem = carried.problem();
        if (problem.isPresent()) {
            MiningModule.LOGGER.warn("Tunnel Root keeps unreadable carried state: {}", problem.get());
            unreadable = Optional.of(
                    level == null
                            ? new CompoundTag()
                            : Persistence.encode(TunnelRootContents.CODEC, carried, level.registryAccess()));
            return;
        }
        restore(carried);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        for (String key : List.of("segments", "output", "tank", "cells", "ticks", "steer", "mutations", "descended"))
            tag.remove(key);
    }

    /** A head with Stairs is titled a Descending Rootstock. */
    @Override
    public Component getDisplayName() {
        return mutations.rootstock()
                ? Component.translatable("container.infestusfrontier.descending_rootstock")
                : getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TunnelRootMenu(id, inventory, this);
    }
}
