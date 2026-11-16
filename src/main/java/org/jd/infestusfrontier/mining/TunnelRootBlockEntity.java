package org.jd.infestusfrontier.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
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
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.Steer;
import org.jd.infestusfrontier.core.mining.TunnelProgress;
import org.jd.infestusfrontier.core.mining.Tunnelling;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.organ.api.Steward;
import org.jd.infestusfrontier.organ.api.Stewarded;
import org.jd.infestusfrontier.storage.StorageModule;

/**
 * The work of a Tunnel Root head. Every {@value Tunnelling#STEP_TICKS} loaded ticks it tries one step: it turns as
 * steered, cuts the two blocks ahead at a stone pickaxe's grade into its output, lays a root cell in the floor under
 * them, pays one segment and {@value Tunnelling#STEP_BIOMASS_BU} BU, and moves into the cut. Everything a step reads
 * and changes is judged first, so a step is done whole in one tick or not at all; a stopped head keeps its step due,
 * names the reason and looks again after {@value Tunnelling#RETRY_TICKS} ticks, or at once when its supplies change.
 * Nothing is fed to it by veins, hoppers or pipes: it has no capability. It loads no chunk.
 *
 * <p>Moving is a new block entity at the cut: this one hands it its whole saved state, steward included, and leaves.
 */
final class TunnelRootBlockEntity extends BlockEntity implements Stewarded, MenuProvider {
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
    private Steer steer = Steer.STRAIGHT;
    private Optional<CompoundTag> unreadable = Optional.empty();
    private final Steward steward = new Steward();
    /** Why the last try did not step. Not saved: the next try tells again. */
    private Optional<Refusal> stop = Optional.empty();
    /** Loaded ticks before a stopped head looks again. */
    private int retry;

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

    Steer steer() {
        return steer;
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
        if (unreadable.isPresent() || steer == way) return;
        steer = way;
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

    // ---- the work ----

    static void serverTick(Level level, BlockPos pos, BlockState state, TunnelRootBlockEntity head) {
        if (head.unreadable.isPresent() || !(level instanceof ServerLevel server)) return;
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

    /** The way the next step goes: as steered from where the head faces. */
    private Direction way(Direction facing) {
        return switch (steer) {
            case STRAIGHT -> facing;
            case LEFT -> facing.getCounterClockWise();
            case RIGHT -> facing.getClockWise();
        };
    }

    private void tryStep(ServerLevel level, BlockPos pos, BlockState state) {
        Direction way = way(state.getValue(TunnelRootBlock.FACING));
        BlockPos ahead = pos.relative(way);
        Optional<Refusal> blocked = blocked(level, pos, ahead);
        Optional<List<ItemStack>> after = Optional.empty();
        if (blocked.isEmpty()) {
            List<ItemStack> drops = new ArrayList<>(loot(level, ahead.above()));
            drops.addAll(loot(level, ahead));
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
        if (!loaded(level, pos)) return Optional.of(Tunnelling.UNLOADED);
        BlockPos top = ahead.above();
        BlockPos floor = ahead.below();
        if (level.isOutsideBuildHeight(top.above()) || level.isOutsideBuildHeight(floor))
            return Optional.of(Tunnelling.UNSUPPORTED);
        for (BlockPos cut : List.of(top, ahead)) {
            Optional<Refusal> refused = cuttable(level, cut);
            if (refused.isPresent()) return refused;
        }
        if (fluidTouches(level, ahead)) return Optional.of(Tunnelling.FLUID);
        if (level.getBlockState(top.above()).getBlock() instanceof Fallable) return Optional.of(Tunnelling.FALLING);
        if (!floor(level, floor)) return Optional.of(Tunnelling.NO_FLOOR);
        return Optional.empty();
    }

    /** True while the head stands on its root: a root cell, or the foundation it was grown or placed on. */
    private static boolean rooted(ServerLevel level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(MiningModule.ROOT_CELL.get()) || below.is(Organs.FOUNDATIONS);
    }

    /** True when every chunk within {@link #REACH} of the head is loaded. The head forces none. */
    private static boolean loaded(ServerLevel level, BlockPos pos) {
        int lowX = SectionPos.blockToSectionCoord(pos.getX() - REACH);
        int highX = SectionPos.blockToSectionCoord(pos.getX() + REACH);
        int lowZ = SectionPos.blockToSectionCoord(pos.getZ() - REACH);
        int highZ = SectionPos.blockToSectionCoord(pos.getZ() + REACH);
        for (int x = lowX; x <= highX; x++) for (int z = lowZ; z <= highZ; z++) if (!level.hasChunk(x, z)) return false;
        return true;
    }

    /**
     * Why the block at {@code cut} cannot be cut, or empty when it can (air included). A piston arm is refused: removing
     * it breaks its piston with drops whatever the update flags, so the piston would drop loose or twice.
     */
    private static Optional<Refusal> cuttable(ServerLevel level, BlockPos cut) {
        BlockState state = level.getBlockState(cut);
        if (!state.getFluidState().isEmpty()) return Optional.of(Tunnelling.FLUID);
        if (state.isAir()) return Optional.empty();
        if (state.hasBlockEntity() || state.getBlock() instanceof PistonHeadBlock)
            return Optional.of(Tunnelling.UNSUPPORTED);
        if (!withinGrade(state, level, cut)) return Optional.of(Tunnelling.TOO_HARD);
        return Optional.empty();
    }

    /** True for a breakable block a stone pickaxe harvests: one that needs no tool, or one stone is enough for. */
    static boolean withinGrade(BlockState state, Level level, BlockPos pos) {
        return state.getDestroySpeed(level, pos) >= 0
                && (!state.requiresCorrectToolForDrops()
                        || new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(state));
    }

    /** True when fluid stands next to either cut block, which would flow into the tunnel. */
    private static boolean fluidTouches(ServerLevel level, BlockPos ahead) {
        BlockPos top = ahead.above();
        for (BlockPos cut : List.of(ahead, top))
            for (Direction side : Direction.values()) {
                BlockPos next = cut.relative(side);
                if (next.equals(ahead) || next.equals(top)) continue;
                if (!level.getFluidState(next).isEmpty()) return true;
            }
        return false;
    }

    /** True for plain ground a root cell may replace: tagged, dry, no block entity. */
    private static boolean floor(ServerLevel level, BlockPos floor) {
        BlockState state = level.getBlockState(floor);
        return state.is(MiningModule.TUNNEL_FLOOR) && state.getFluidState().isEmpty() && !state.hasBlockEntity();
    }

    /** The ordinary drops of the block at {@code cut}, rolled once in its real loot context with a stone pickaxe. */
    private static List<ItemStack> loot(ServerLevel level, BlockPos cut) {
        BlockState state = level.getBlockState(cut);
        if (state.isAir()) return List.of();
        return Block.getDrops(state, level, cut, null, null, new ItemStack(Items.STONE_PICKAXE));
    }

    /**
     * Takes the judged step: clears the cut, lays the cell, pays, and moves the head with everything it owns into the
     * cut. The lower cut block goes first and quietly, so neither cut block's removal can update the other into a
     * second, loose drop (a door's halves, a lantern hanging from the upper block, a torch standing on the lower one).
     * The lower block's outside neighbours get their updates when the head moves in.
     */
    private void step(ServerLevel level, BlockPos pos, BlockState state, Direction way, List<ItemStack> after) {
        Direction facing = state.getValue(TunnelRootBlock.FACING);
        BlockPos ahead = pos.relative(way);
        BlockState lower = level.getBlockState(ahead);
        if (!lower.isAir()) {
            level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, ahead, Block.getId(lower));
            level.setBlock(ahead, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            level.gameEvent(GameEvent.BLOCK_DESTROY, ahead, GameEvent.Context.of(lower));
        }
        level.destroyBlock(ahead.above(), false);
        level.setBlock(ahead.below(), RootCellBlock.straight(way), Block.UPDATE_ALL);
        if (way != facing && level.getBlockState(pos.below()).is(MiningModule.ROOT_CELL.get()))
            level.setBlock(pos.below(), RootCellBlock.bend(facing, way), Block.UPDATE_ALL);

        ItemStack paid = segments.getStackInSlot(0).copy();
        paid.shrink(1);
        restore(new TunnelRootContents(
                paid,
                after,
                ((Outcome.Done<BiomassTank>) tank.drain(Biomass.of(Tunnelling.STEP_BIOMASS_BU))).value(),
                progress.stepped(),
                Steer.STRAIGHT));
        stop = Optional.empty();

        CompoundTag carried = saveWithoutMetadata(level.registryAccess());
        level.setBlock(ahead, state.setValue(TunnelRootBlock.FACING, way), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(ahead) instanceof TunnelRootBlockEntity moved)) {
            MiningModule.LOGGER.error("Tunnel Root at {} could not move into {}; it stays", pos, ahead);
            return;
        }
        moved.loadWithComponents(carried, level.registryAccess());
        moved.setChanged();
        restore(TunnelRootContents.EMPTY); // this block entity leaves: nothing stays behind to drop or duplicate
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.playSound(null, ahead, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 0.8F, 0.8F);
        moved.steward.worked(level, MiningModule.TUNNEL_ROOT.getId());
    }

    // ---- saved state ----

    TunnelRootContents contents() {
        List<ItemStack> held = new ArrayList<>(Tunnelling.OUTPUT_SLOTS);
        for (int slot = 0; slot < Tunnelling.OUTPUT_SLOTS; slot++) held.add(output.getStackInSlot(slot));
        return new TunnelRootContents(segments.getStackInSlot(0), held, tank, progress, steer);
    }

    private void restore(TunnelRootContents contents) {
        segments.setStackInSlot(0, contents.segments().copy());
        for (int slot = 0; slot < Tunnelling.OUTPUT_SLOTS; slot++)
            output.setStackInSlot(slot, contents.output().get(slot).copy());
        tank = contents.tank();
        progress = contents.progress();
        steer = contents.steer();
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
        for (String key : List.of("segments", "output", "tank", "cells", "ticks", "steer")) tag.remove(key);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TunnelRootMenu(id, inventory, this);
    }
}
