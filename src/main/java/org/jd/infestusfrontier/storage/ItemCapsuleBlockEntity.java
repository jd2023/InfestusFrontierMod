package org.jd.infestusfrontier.storage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.core.storage.Compartments;
import org.jd.infestusfrontier.core.storage.PodKind;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.storage.api.CapsuleStock;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * One pod of a Capsule Stem: the compartments its {@link PodKind} gives it, a bulk pod's one or a Mixed Craw's four,
 * each of one kind of item, its grade and the root of the stem it hangs on, if any. The compartments stay in the pod,
 * so a broken pod carries exactly them; the tree's one store is {@link CapsuleTree}, shared by its pods while it
 * stands. The kind of pod is the block's, never changed in place: a mutation replaces the block, which closes the
 * tree's store. Clients get the compartments and the grade for the front.
 */
class ItemCapsuleBlockEntity extends BlockEntity implements MenuProvider, GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final List<RawAnimation> GRADES = List.of(
            RawAnimation.begin().thenLoop("grade_0"),
            RawAnimation.begin().thenLoop("grade_1"),
            RawAnimation.begin().thenLoop("grade_2"));

    private record Saved(CapsuleStock stock, Optional<BlockPos> root) {}

    private static final MapCodec<Saved> SAVED = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    CapsuleStock.CODEC
                            .optionalFieldOf("stock", CapsuleStock.EMPTY)
                            .forGetter(Saved::stock),
                    BlockPos.CODEC.optionalFieldOf("root").forGetter(Saved::root))
            .apply(instance, Saved::new));

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private final PodKind kind;
    private final ItemStack[] held = new ItemStack[CapsuleStock.COMPARTMENTS];
    private int grade;
    private Optional<BlockPos> root = Optional.empty();
    private Optional<CompoundTag> unreadable = Optional.empty();
    /** The store of the tree while it stands; dropped whenever the tree or the pod's state changes. */
    private @Nullable CapsuleTree store;

    ItemCapsuleBlockEntity(BlockPos pos, BlockState state) {
        this(StorageModule.ITEM_CAPSULE_ENTITY.get(), pos, state, PodKind.BULK);
    }

    ItemCapsuleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, PodKind kind) {
        super(type, pos, state);
        this.kind = kind;
        Arrays.fill(held, ItemStack.EMPTY);
        grade = state.getValue(ItemCapsuleBlock.GRADE);
    }

    PodKind kind() {
        return kind;
    }

    /** How many kinds of item the pod holds at once. */
    int compartments() {
        return kind.compartments();
    }

    /** Stacks of its kind one compartment holds at the pod's grade. */
    int stacks() {
        return kind.stacks(grade);
    }

    /** What a compartment holds, with its whole count; a copy. */
    ItemStack held(int compartment) {
        return held[compartment].copy();
    }

    /** True when compartment {@code compartment} holds {@code like}'s kind. */
    boolean holds(int compartment, ItemStack like) {
        return !held[compartment].isEmpty() && ItemStack.isSameItemSameComponents(held[compartment], like);
    }

    int count(int compartment) {
        return held[compartment].getCount();
    }

    int grade() {
        return grade;
    }

    /** How large the model draws the pod's body at each grade: art/gen_item_capsule.py GRADES. */
    static final float[] SCALES = {0.72F, 0.86F, 1F};

    /** The scale of the pod's body at its grade, for the stock drawn in its socket. */
    double scale() {
        return SCALES[getBlockState().getValue(ItemCapsuleBlock.GRADE)];
    }

    /** Sets a compartment to {@code stack}, its kind and its whole count; an empty stack empties it. */
    void put(int compartment, ItemStack stack) {
        held[compartment] = stack.copy();
        changed();
    }

    /** Adds {@code amount} of {@code kind} to a compartment that holds that kind or is empty. */
    void add(int compartment, ItemStack kind, int amount) {
        if (held[compartment].isEmpty()) held[compartment] = kind.copyWithCount(amount);
        else held[compartment].grow(amount);
        changed();
    }

    /** Takes {@code amount} out of a compartment. */
    void take(int compartment, int amount) {
        held[compartment].shrink(amount);
        if (held[compartment].isEmpty()) held[compartment] = ItemStack.EMPTY;
        changed();
    }

    /**
     * Whether a hand may put {@code stack} into {@code compartment}: a kind a capsule takes, into its own compartment,
     * or into an empty one when every other compartment of this pod holding the kind is full, as the tree's own
     * rules open one.
     */
    boolean accepts(int compartment, ItemStack stack) {
        if (unreadable.isPresent() || !Stems.takes(stack)) return false;
        if (!held[compartment].isEmpty()) return holds(compartment, stack);
        int limit = Compartments.limit(stacks(), stack.getMaxStackSize());
        for (int other = 0; other < compartments(); other++)
            if (holds(other, stack) && held[other].getCount() < limit) return false;
        return true;
    }

    /** The pod grows a grade: more stacks of its kind. False at the last grade. */
    boolean grow() {
        if (grade >= CapsuleStock.LAST_GRADE) return false;
        grade++;
        if (level != null)
            level.setBlock(worldPosition, getBlockState().setValue(ItemCapsuleBlock.GRADE, grade), Block.UPDATE_ALL);
        changed();
        return true;
    }

    /** What the front shows: a copy of each compartment. */
    List<ItemStack> shown() {
        return Arrays.stream(held, 0, compartments()).map(ItemStack::copy).toList();
    }

    CapsuleStock stock() {
        return new CapsuleStock(List.of(Arrays.copyOf(held, compartments())), grade, kind);
    }

    private void changed() {
        setChanged();
        if (store != null) store.touch();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Nullable CapsuleTree store() {
        return store;
    }

    /** Joins this pod to the store {@code next}; the store it had before stops answering. */
    void attach(CapsuleTree next) {
        if (store != null && store != next) store.close();
        store = next;
    }

    private void detach() {
        if (store != null) store.close();
        store = null;
    }

    /** The root of the stem this pod hangs on, as last known; {@link Stems#of} checks it still stands. */
    Optional<BlockPos> root() {
        return root;
    }

    /** The pod now hangs on the stem rooted at {@code pos}, or on none. */
    void hangOn(Optional<BlockPos> pos) {
        if (root.equals(pos)) return;
        root = pos;
        detach();
        setChanged();
    }

    boolean isReadable() {
        return unreadable.isEmpty();
    }

    boolean isEmpty() {
        if (unreadable.isPresent()) return false;
        for (ItemStack stack : held) if (!stack.isEmpty()) return false;
        return true;
    }

    /**
     * Why this pod cannot become a Mixed Craw now, as the end of a language key under
     * {@code gui.infestusfrontier.item_capsule.pod.gel.}, or empty when it can: only a readable, empty bulk pod on a
     * server mutates, so nothing it holds is ever lost or squeezed.
     */
    Optional<String> craw() {
        if (kind != PodKind.BULK) return Optional.of("not_bulk");
        if (unreadable.isPresent()) return Optional.of("unreadable");
        if (!isEmpty()) return Optional.of("not_empty");
        if (level == null || level.isClientSide || isRemoved()) return Optional.of("no_level");
        return Optional.empty();
    }

    /**
     * The pod becomes a Mixed Craw of the same grade, facing and tree. The block is replaced, so this entity is
     * removed and the tree reads its store again with the new one. False, changing nothing, when {@link #craw} refuses.
     */
    boolean mutateToCraw() {
        if (craw().isPresent()) return false;
        BlockState now = getBlockState();
        Optional<BlockPos> hangs = root;
        BlockState craw = StorageModule.MIXED_CRAW
                .get()
                .defaultBlockState()
                .setValue(ItemCapsuleBlock.FACING, now.getValue(ItemCapsuleBlock.FACING))
                .setValue(ItemCapsuleBlock.GRADE, grade);
        Level where = level;
        BlockPos pos = worldPosition;
        where.setBlock(pos, craw, Block.UPDATE_ALL);
        if (where.getBlockEntity(pos) instanceof ItemCapsuleBlockEntity mutated) mutated.hangOn(hangs);
        hangs.flatMap(rootPos -> Stems.rootAt(where, rootPos)).ifPresent(CapsuleStemBlockEntity::touch);
        return true;
    }

    /** A pod back in reach tells its root, so the tree's store is read again with it. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide)
            root.flatMap(pos -> Stems.rootAt(level, pos)).ifPresent(CapsuleStemBlockEntity::touch);
    }

    /** A pod that goes with its chunk takes the tree's store with it; the rest refuse until it is read again. */
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        detach();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        detach();
    }

    private Optional<String> check(Saved saved) {
        if (saved.stock().kind() != kind)
            return Optional.of("a " + saved.stock().kind().id() + " pod's stock in a " + kind.id() + " pod");
        if (saved.stock().held().size() > compartments())
            return Optional.of(saved.stock().held().size() + " compartments in a pod of " + compartments());
        for (ItemStack stack : saved.stock().held()) {
            if (!stack.isEmpty() && !Stems.takes(stack)) return Optional.of("a capsule that holds " + stack);
            int limit = Compartments.limit(kind.stacks(saved.stock().grade()), stack.getMaxStackSize());
            if (stack.getCount() > limit) return Optional.of(stack.getCount() + " " + stack + " is past a compartment");
        }
        return Optional.empty();
    }

    private void restore(CapsuleStock stock) {
        Arrays.fill(held, ItemStack.EMPTY);
        List<ItemStack> stacks = stock.held();
        for (int i = 0; i < Math.min(compartments(), stacks.size()); i++) held[i] = stacks.get(i);
        grade = stock.grade();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        detach();
        Optional<Saved> read =
                Persistence.read(SAVED, tag, registries, StorageModule.LOGGER, "Item Capsule", this::check);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        restore(read.map(Saved::stock).orElse(CapsuleStock.EMPTY));
        root = read.flatMap(Saved::root);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, new Saved(stock(), root), registries, tag);
    }

    /** Clients get the compartment and grade for the front, never the root or unreadable bytes. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        Persistence.write(
                SAVED,
                new Saved(unreadable.isPresent() ? CapsuleStock.EMPTY : stock(), Optional.empty()),
                registries,
                tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent())
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
        else if (!stock().isBare()) {
            components.set(StorageModule.CAPSULE_STOCK.get(), stock());
            components.set(DataComponents.MAX_STACK_SIZE, 1); // each carries its own compartment
        }
    }

    /** Carried stock that fails its check is kept as unreadable bytes, never replaced. */
    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        input.get(DataComponents.MAX_STACK_SIZE); // read, so the pod does not keep it: it is set again while it holds
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            return;
        }
        CapsuleStock carried = input.getOrDefault(StorageModule.CAPSULE_STOCK.get(), CapsuleStock.EMPTY);
        Saved saved = new Saved(carried, Optional.empty());
        Optional<String> problem = check(saved);
        if (problem.isPresent()) {
            StorageModule.LOGGER.warn("Item Capsule keeps unreadable carried state: {}", problem.get());
            unreadable = Optional.of(
                    level == null ? new CompoundTag() : Persistence.encode(SAVED, saved, level.registryAccess()));
            return;
        }
        restore(carried);
        if (level != null && getBlockState().getValue(ItemCapsuleBlock.GRADE) != grade)
            level.setBlock(worldPosition, getBlockState().setValue(ItemCapsuleBlock.GRADE, grade), Block.UPDATE_ALL);
        if (store != null) store.touch();
    }

    /** The stock travels as a component; the root never leaves its place. */
    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("stock");
        tag.remove("root");
    }

    /** The client plays the pod's breath and the look of its grade; nothing else is sent for them. */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "state", 0, state -> state.setAndContinue(IDLE)));
        controllers.add(new AnimationController<>(
                this,
                "grade",
                0,
                state -> state.setAndContinue(
                        GRADES.get(state.getAnimatable().getBlockState().getValue(ItemCapsuleBlock.GRADE)))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    /** A pod opens only itself; the tree's screen is the stem cells'. */
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ItemCapsulePodMenu(id, inventory, this);
    }
}
