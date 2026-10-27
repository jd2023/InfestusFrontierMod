package org.jd.infestusfrontier.storage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.core.storage.StemPlan;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jetbrains.annotations.Nullable;

/**
 * One cell of a Capsule Stem. Every cell knows its root; the root alone keeps the tree's {@link StemPlan} and makes
 * the tree's store from the pods it reaches through standing cells. Growth happens here: a bone meal feeds the root
 * toward its next growth, which first regrows a planned cell or pod that is missing, then grows the plan one step;
 * a growth costs one bone meal at the first level and twice as much each level up ({@link #cost}). A root may also
 * be loaded with growth owed ({@code grow} in its saved data), which it pays free on its first tick.
 */
final class CapsuleStemBlockEntity extends BlockEntity implements MenuProvider {
    private static final int GROWTH_PARTICLES = 10;
    /** Ticks a cell waits to die after its root is gone, per cell up the plan, so a dead tree falls from the bottom. */
    static final int DEATH_TICKS = 3;
    /** The most bone meal a root may hold toward a growth: the dearest growth, at the top of the plan. */
    static final int MOST_FED = cost(StemPlan.MAX_HEIGHT);
    /** Ticks a loaded cell waits before asking again whether its root stands, while the root's chunk is out of reach. */
    static final int RECHECK_TICKS = 100;

    private record Saved(BlockPos root, Optional<StemPlan> plan, int grow, int fed) {}

    private static final MapCodec<Saved> SAVED = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BlockPos.CODEC.fieldOf("root").forGetter(Saved::root),
                    StemCodecs.PLAN.optionalFieldOf("plan").forGetter(Saved::plan),
                    com.mojang.serialization.Codec.intRange(0, 256)
                            .optionalFieldOf("grow", 0)
                            .forGetter(Saved::grow),
                    com.mojang.serialization.Codec.intRange(0, MOST_FED)
                            .optionalFieldOf("fed", 0)
                            .forGetter(Saved::fed))
            .apply(instance, Saved::new));

    /** One growth: a planned cell or pod to regrow, or a new step of the plan. */
    record Growth(Optional<StemPlan.Step> step, int cell, int pod) {
        static Growth cell(int index) {
            return new Growth(Optional.empty(), index, -1);
        }

        static Growth pod(int index) {
            return new Growth(Optional.empty(), -1, index);
        }

        static Growth step(StemPlan.Step step) {
            return new Growth(Optional.of(step), -1, -1);
        }
    }

    private BlockPos root;
    private Optional<StemPlan> plan = Optional.empty();
    private int pending;
    /** Bone meal fed toward the next growth. */
    private int fed;

    private Optional<CompoundTag> unreadable = Optional.empty();
    private @Nullable CapsuleTree tree;

    CapsuleStemBlockEntity(BlockPos pos, BlockState state) {
        super(StorageModule.CAPSULE_STEM_ENTITY.get(), pos, state);
        root = pos;
    }

    /** A root planted by hand starts a plan of its own from {@code seed}. */
    void plant(long seed) {
        root = worldPosition;
        plan = Optional.of(StemPlan.planted(seed));
        setChanged();
    }

    /** A grown cell belongs to the root at {@code rootPos}. */
    void join(BlockPos rootPos) {
        root = rootPos.immutable();
        plan = Optional.empty();
        setChanged();
    }

    boolean isRoot() {
        return plan.isPresent() && root.equals(worldPosition);
    }

    /** False for a cell whose saved data did not read: it keeps its bytes, grows nothing and is never judged dead. */
    boolean isReadable() {
        return unreadable.isEmpty();
    }

    /** The root of this cell's tree, while it stands: this cell, or the one it names. */
    Optional<CapsuleStemBlockEntity> root() {
        if (isRoot()) return Optional.of(this);
        return level == null ? Optional.empty() : Stems.rootAt(level, root);
    }

    BlockPos rootPos() {
        return root;
    }

    /** True when this root's plan has a cell at {@code pos}: a cell of another plan on the same spot is not its own. */
    boolean owns(BlockPos pos) {
        BlockPos offset = pos.subtract(worldPosition);
        return isRoot()
                && plan().cellAt(offset.getX(), offset.getY(), offset.getZ()).isPresent();
    }

    StemPlan plan() {
        return plan.orElseThrow();
    }

    /** The tree changed shape: the store is read again when next asked. */
    void touch() {
        if (tree != null) tree.close();
        tree = null;
        setChanged();
    }

    private BlockPos at(StemPlan.Cell cell) {
        return worldPosition.offset(cell.x(), cell.y(), cell.z());
    }

    private BlockPos at(StemPlan.Pod pod) {
        return worldPosition.offset(pod.x(), pod.y(), pod.z());
    }

    /** True when the cell at index {@code index} stands and belongs to this tree. */
    private boolean standing(LevelReader level, int index) {
        StemPlan.Cell cell = plan().cells().get(index);
        if (index == 0) return true;
        return Stems.cellAt(level, at(cell))
                .filter(standing -> standing.root.equals(worldPosition))
                .isPresent();
    }

    /** The pod planned at index {@code index}, while it stands there facing out and names no other root. */
    private Optional<ItemCapsuleBlockEntity> podAt(LevelReader level, int index) {
        StemPlan.Pod pod = plan().pods().get(index);
        BlockPos pos = at(pod);
        if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof ItemCapsuleBlockEntity standing))
            return Optional.empty();
        boolean mine = standing.root().map(worldPosition::equals).orElse(true);
        boolean facesOut = standing.getBlockState().getValue(ItemCapsuleBlock.FACING) == Stems.direction(pod.side());
        return mine && facesOut && standing.isReadable() ? Optional.of(standing) : Optional.empty();
    }

    /**
     * The store of the tree: the pods the root reaches through standing cells, in growth order. Made when first asked
     * and kept until the tree changes. Only a root answers.
     */
    Optional<CapsuleTree> tree() {
        if (level == null || !isRoot()) return Optional.empty();
        if (tree != null && tree.isUsable() && tree.whole()) return Optional.of(tree);
        StemPlan plan = plan();
        List<BlockPos> span = new ArrayList<>();
        for (StemPlan.Cell cell : plan.cells()) span.add(at(cell));
        for (StemPlan.Pod pod : plan.pods()) span.add(at(pod));
        boolean[] reached = new boolean[plan.cells().size()];
        reached[0] = true;
        for (int i = 1; i < reached.length; i++)
            reached[i] = reached[plan.cells().get(i).parent()] && standing(level, i);
        List<ItemCapsuleBlockEntity> members = new ArrayList<>();
        for (int i = 0; i < plan.pods().size(); i++) {
            if (!reached[plan.pods().get(i).cell()]) continue;
            Optional<ItemCapsuleBlockEntity> pod = podAt(level, i);
            if (pod.isEmpty()) continue;
            pod.get().hangOn(Optional.of(worldPosition));
            members.add(pod.get());
        }
        tree = new CapsuleTree(level, members, span);
        return Optional.of(tree);
    }

    private boolean free(LevelReader level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos)
                && level.hasChunkAt(pos)
                && level.getBlockState(pos).canBeReplaced();
    }

    /** What the next bone meal grows, or empty when nothing is missing and nothing more fits. */
    Optional<Growth> nextGrowth(LevelReader level) {
        if (!isRoot()) return Optional.empty();
        StemPlan plan = plan();
        for (int i = 1; i < plan.cells().size(); i++)
            if (!standing(level, i) && free(level, at(plan.cells().get(i)))) return Optional.of(Growth.cell(i));
        for (int i = 0; i < plan.pods().size(); i++)
            if (podAt(level, i).isEmpty() && free(level, at(plan.pods().get(i)))) return Optional.of(Growth.pod(i));
        return plan.next((x, y, z) -> free(level, worldPosition.offset(x, y, z)))
                .map(Growth::step);
    }

    /** The level of what a growth grows: how far above the root. */
    private int level(Growth growth) {
        if (growth.step().isPresent())
            return switch (growth.step().get()) {
                case StemPlan.Step.Grow grow -> grow.cell().y();
                case StemPlan.Step.Sprout sprout -> sprout.pod().y();
            };
        return growth.cell() >= 0
                ? plan().cells().get(growth.cell()).y()
                : plan().pods().get(growth.pod()).y();
    }

    /** The bone meal a growth at {@code level} costs: one at the first level, twice as much each level up. */
    static int cost(int level) {
        return 1 << Math.max(0, level - 1);
    }

    int cost(Growth growth) {
        return cost(level(growth));
    }

    int fed() {
        return fed;
    }

    /** What the next growth costs, or 0 when nothing is missing and nothing more fits. */
    int nextCost(LevelReader level) {
        return nextGrowth(level).map(this::cost).orElse(0);
    }

    /**
     * One bone meal toward the next growth, which happens when its cost is met. False, and nothing taken, when
     * nothing is missing and nothing more fits.
     */
    boolean feed(ServerLevel level) {
        Optional<Growth> next = nextGrowth(level);
        if (next.isEmpty()) return false;
        fed++;
        int cost = cost(next.get());
        if (fed < cost) {
            setChanged();
            return true;
        }
        fed -= cost;
        grow(level, next.get());
        return true;
    }

    /**
     * Grows the tree by one, free of bone meal: regrows what is missing first, else one step of the plan. False when
     * nothing fits. Growth owed from data is paid this way.
     */
    boolean grow(ServerLevel level) {
        Optional<Growth> next = nextGrowth(level);
        if (next.isEmpty()) return false;
        grow(level, next.get());
        return true;
    }

    private void grow(ServerLevel level, Growth growth) {
        int cell = growth.cell(), pod = growth.pod();
        if (growth.step().isPresent()) {
            plan = Optional.of(plan().with(growth.step().get()));
            switch (growth.step().get()) {
                case StemPlan.Step.Grow grow -> cell = plan().cells().size() - 1;
                case StemPlan.Step.Sprout sprout -> pod = plan().pods().size() - 1;
            }
        }
        BlockPos grown = cell >= 0 ? growCell(level, cell) : growPod(level, pod);
        touch();
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, grown, GROWTH_PARTICLES);
    }

    private BlockPos growCell(ServerLevel level, int index) {
        StemPlan.Cell cell = plan().cells().get(index);
        BlockPos pos = at(cell);
        level.setBlock(pos, look(index), Block.UPDATE_ALL);
        Stems.cellAt(level, pos).ifPresent(grownCell -> grownCell.join(worldPosition));
        refresh(level, cell.parent());
        return pos;
    }

    private BlockPos growPod(ServerLevel level, int index) {
        StemPlan.Pod pod = plan().pods().get(index);
        BlockPos pos = at(pod);
        BlockState state = StorageModule.ITEM_CAPSULE
                .get()
                .defaultBlockState()
                .setValue(ItemCapsuleBlock.FACING, Stems.direction(pod.side()));
        level.setBlock(pos, state, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof ItemCapsuleBlockEntity grownPod)
            grownPod.hangOn(Optional.of(worldPosition));
        refresh(level, pod.cell());
        return pos;
    }

    /** The look of the planned cell at {@code index}: its part, which way a branch grew, and a collar under pods. */
    private BlockState look(int index) {
        StemPlan plan = plan();
        StemPlan.Cell cell = plan.cells().get(index);
        boolean risen =
                plan.cells().stream().anyMatch(other -> other.parent() == index && other.part() == StemPlan.Part.TRUNK);
        StemPart part = switch (cell.part()) {
            case ROOT -> StemPart.ROOT;
            case BRANCH -> StemPart.BRANCH;
            case TRUNK -> risen ? StemPart.TRUNK : StemPart.BUD;
        };
        Direction facing = cell.part() == StemPlan.Part.BRANCH ? Stems.direction(cell.side()) : Direction.NORTH;
        boolean collar = plan.pods().stream().anyMatch(pod -> pod.cell() == index);
        return StorageModule.CAPSULE_STEM
                .get()
                .defaultBlockState()
                .setValue(CapsuleStemBlock.PART, part)
                .setValue(CapsuleStemBlock.FACING, facing)
                .setValue(CapsuleStemBlock.COLLAR, collar);
    }

    /** Shows the planned cell at {@code index} as the plan now has it, if it stands. */
    private void refresh(Level level, int index) {
        if (index < 0 || !standing(level, index)) return;
        BlockPos pos = at(plan().cells().get(index));
        BlockState shown = look(index);
        if (level.getBlockState(pos) != shown) level.setBlock(pos, shown, Block.UPDATE_ALL);
    }

    /**
     * A pod pressed by hand on {@code facing}'s far side of the cell at {@code cellPos} joins the plan, if that cell is
     * a planned trunk cell of this tree. True when it did.
     */
    boolean adopt(Level level, BlockPos cellPos, Direction facing) {
        if (!isRoot()) return false;
        BlockPos offset = cellPos.subtract(worldPosition);
        Optional<StemPlan.Cell> cell = plan().cellAt(offset.getX(), offset.getY(), offset.getZ());
        if (cell.isEmpty() || cell.get().part() != StemPlan.Part.TRUNK) return false;
        BlockPos podOffset = offset.relative(facing);
        if (plan().cellAt(podOffset.getX(), podOffset.getY(), podOffset.getZ()).isPresent()) return false;
        if (plan().pods().size() >= StemCodecs.MOST_PODS) return false;
        int index = plan().indexOf(cell.get());
        plan = Optional.of(plan().adopt(index, Stems.side(facing)));
        touch();
        refresh(level, index);
        return true;
    }

    /**
     * The root is leaving: its cells die from the bottom up, each on its own tick, shedding its pods as items as it
     * goes ({@link CapsuleStemBlock#tick}); a pod whose cell is already gone drops now.
     */
    void dies(ServerLevel level) {
        if (!isRoot()) return;
        StemPlan plan = plan();
        for (int i = 1; i < plan.cells().size(); i++) {
            BlockPos pos = at(plan.cells().get(i));
            if (standing(level, i)) level.scheduleTick(pos, StorageModule.CAPSULE_STEM.get(), DEATH_TICKS * i);
        }
        for (StemPlan.Pod pod : plan.pods()) {
            BlockPos pos = at(pod);
            if (standing(level, pod.cell())
                    || !level.hasChunkAt(pos)
                    || !(level.getBlockEntity(pos) instanceof ItemCapsuleBlockEntity orphan)
                    || orphan.root().filter(worldPosition::equals).isEmpty()) continue;
            level.destroyBlock(pos, true);
        }
        touch();
    }

    /** Growth owed from saved data: as many steps as fit, all at once. */
    void payPending(ServerLevel level) {
        while (pending > 0 && isRoot() && grow(level)) pending--;
        pending = 0;
        setChanged();
    }

    boolean owesGrowth() {
        return pending > 0;
    }

    /**
     * A root back in reach reads its tree again and pays growth it owes. A grown cell back in reach checks its root
     * on the next tick ({@link CapsuleStemBlock#tick}): a root that died while the cell was out of reach could not
     * tell it, so the cell finds out now, or keeps asking while the root's chunk is out of reach.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) return;
        if (isRoot()) {
            touch();
            if (pending > 0) level.scheduleTick(worldPosition, StorageModule.CAPSULE_STEM.get(), 1);
        } else level.scheduleTick(worldPosition, StorageModule.CAPSULE_STEM.get(), 1);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (tree != null) tree.close();
        tree = null;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (tree != null) tree.close();
        tree = null;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tree != null) tree.close();
        tree = null;
        Optional<Saved> read = Persistence.read(SAVED, tag, registries, StorageModule.LOGGER, "Capsule Stem");
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        root = read.map(Saved::root).orElse(worldPosition);
        plan = read.flatMap(Saved::plan);
        pending = read.map(Saved::grow).orElse(0);
        fed = read.map(Saved::fed).orElse(0);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, new Saved(root, plan, pending, fed), registries, tag);
    }

    @Override
    public Component getDisplayName() {
        return StorageModule.CAPSULE_STEM.get().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ItemCapsuleMenu(id, inventory, this, () -> Stems.of(this));
    }
}
