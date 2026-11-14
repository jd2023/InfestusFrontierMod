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
import net.minecraft.server.level.ServerLevel;
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
import org.jd.infestusfrontier.core.storage.Reserves;
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
 * One pod of a Capsule Stem: the compartments its {@link PodKind} gives it, a bulk pod's one, a Mixed Craw's four or a
 * Larder's three, each of one kind of item, its grade and the root of the stem it hangs on, if any. A Larder's
 * compartment also carries a label and a reserve; the pod keeps them, and {@link CapsuleTree} applies the reserve to
 * every external extraction. The compartments stay in the pod,
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
    /** A Larder's label of each compartment: one of its kind, or empty. */
    private final ItemStack[] labels = new ItemStack[CapsuleStock.COMPARTMENTS];
    /** A Larder's reserve of each compartment; always 0 in a pod that keeps none. */
    private final int[] reserves = new int[CapsuleStock.COMPARTMENTS];

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
        Arrays.fill(labels, ItemStack.EMPTY);
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
        label(compartment, stack);
        changed();
    }

    /** Adds {@code amount} of {@code kind} to a compartment that holds that kind or is empty. */
    void add(int compartment, ItemStack kind, int amount) {
        if (held[compartment].isEmpty()) held[compartment] = kind.copyWithCount(amount);
        else held[compartment].grow(amount);
        label(compartment, kind);
        changed();
    }

    /** The first item an unlabelled Larder compartment takes labels it. */
    private void label(int compartment, ItemStack kind) {
        if (this.kind.reserves() && labels[compartment].isEmpty() && !kind.isEmpty()) {
            labels[compartment] = kind.copyWithCount(1);
            reserves[compartment] = Reserves.clamp(reserves[compartment], reserveLimit(compartment));
        }
    }

    /** A Larder compartment's label, or empty; a copy. */
    ItemStack label(int compartment) {
        return labels[compartment].copy();
    }

    /** True when compartment {@code compartment}'s label, if it has one, is {@code like}'s kind. */
    boolean labelAllows(int compartment, ItemStack like) {
        return labels[compartment].isEmpty() || ItemStack.isSameItemSameComponents(labels[compartment], like);
    }

    /** True when the compartment is empty and labelled for {@code like}'s kind. */
    boolean labelledFor(int compartment, ItemStack like) {
        return !labels[compartment].isEmpty()
                && held[compartment].isEmpty()
                && ItemStack.isSameItemSameComponents(labels[compartment], like);
    }

    /** True when an empty compartment is labelled, so it takes only that kind. */
    boolean labelled(int compartment) {
        return !labels[compartment].isEmpty();
    }

    /** The reserve of a compartment: what no external extraction takes. */
    int reserve(int compartment) {
        return reserves[compartment];
    }

    /** What an external extraction may take from a compartment now. */
    int automatable(int compartment) {
        return Reserves.automatable(held[compartment].getCount(), reserves[compartment]);
    }

    /** What the reserve keeps in a compartment now: its reserve, or all it holds when that is less. */
    int reserved(int compartment) {
        return Reserves.reserved(held[compartment].getCount(), reserves[compartment]);
    }

    /** The most a reserve of the compartment may be: its limit of the labelled kind, or of a 64-stack kind. */
    int reserveLimit(int compartment) {
        ItemStack label = labels[compartment];
        return Compartments.limit(stacks(), label.isEmpty() ? 64 : label.getMaxStackSize());
    }

    /**
     * Labels an empty compartment, or one that already holds {@code like}'s kind, with that kind; a label may be set
     * again, never over a compartment that holds another kind. False, changing nothing, for a pod that keeps no
     * labels, a kind a capsule refuses or a compartment that holds another kind.
     */
    boolean setLabel(int compartment, ItemStack like) {
        if (!kind.reserves() || unreadable.isPresent() || like.isEmpty() || !Stems.takes(like)) return false;
        if (!held[compartment].isEmpty() && !holds(compartment, like)) return false;
        labels[compartment] = like.copyWithCount(1);
        reserves[compartment] = Reserves.clamp(reserves[compartment], reserveLimit(compartment));
        changed();
        return true;
    }

    /** Takes the label off a compartment; only while it is empty. False, changing nothing, otherwise. */
    boolean clearLabel(int compartment) {
        if (!kind.reserves() || unreadable.isPresent() || !held[compartment].isEmpty()) return false;
        if (labels[compartment].isEmpty()) return false;
        labels[compartment] = ItemStack.EMPTY;
        reserves[compartment] = Reserves.clamp(reserves[compartment], reserveLimit(compartment));
        changed();
        return true;
    }

    /** The reserve moves to the next setting, or the one before it. False for a pod that keeps no reserve. */
    boolean stepReserve(int compartment, boolean up) {
        if (!kind.reserves() || unreadable.isPresent()) return false;
        reserves[compartment] = Reserves.next(reserves[compartment], reserveLimit(compartment), up);
        changed();
        return true;
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
        if (unreadable.isPresent() || !Stems.takes(stack) || !labelAllows(compartment, stack)) return false;
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
        return new CapsuleStock(List.of(Arrays.copyOf(held, compartments())), grade, kind, marks());
    }

    private List<CapsuleStock.Mark> marks() {
        if (!kind.reserves()) return List.of();
        CapsuleStock.Mark[] marks = new CapsuleStock.Mark[compartments()];
        for (int i = 0; i < marks.length; i++) marks[i] = new CapsuleStock.Mark(labels[i], reserves[i]);
        return List.of(marks);
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
        return mutable(PodKind.BULK, "not_bulk");
    }

    /**
     * The pod becomes a Mixed Craw of the same grade, facing and tree. The block is replaced, so this entity is
     * removed and the tree reads its store again with the new one. False, changing nothing, when {@link #craw} refuses.
     */
    boolean mutateToCraw() {
        return craw().isEmpty() && replaceWith(StorageModule.MIXED_CRAW.get());
    }

    /**
     * Why this Mixed Craw cannot become a Larder now, as the end of a language key under
     * {@code gui.infestusfrontier.item_capsule.pod.resin.}, or empty when it can: only a readable, empty Craw on a
     * server mutates, so nothing it holds is ever lost or squeezed.
     */
    Optional<String> larder() {
        return mutable(PodKind.CRAW, "not_craw");
    }

    /** The Craw becomes a Larder of the same grade, facing and tree. False, changing nothing, when {@link #larder} refuses. */
    boolean mutateToLarder() {
        return larder().isEmpty() && replaceWith(StorageModule.LARDER.get());
    }

    /** The refusal key of a mutation from {@code from}: {@code notFromKey} for another kind, then why this pod is not ready. */
    private Optional<String> mutable(PodKind from, String notFromKey) {
        if (kind != from) return Optional.of(notFromKey);
        if (unreadable.isPresent()) return Optional.of("unreadable");
        if (!isEmpty()) return Optional.of("not_empty");
        if (level == null || level.isClientSide || isRemoved()) return Optional.of("no_level");
        return Optional.empty();
    }

    /** Replaces this pod's block with {@code target}, keeping its facing, grade and root; the root reads its tree again. */
    private boolean replaceWith(Block target) {
        Optional<BlockPos> hangs = root;
        BlockState state = target.defaultBlockState()
                .setValue(ItemCapsuleBlock.FACING, getBlockState().getValue(ItemCapsuleBlock.FACING))
                .setValue(ItemCapsuleBlock.GRADE, grade);
        Level where = level;
        BlockPos pos = worldPosition;
        where.setBlock(pos, state, Block.UPDATE_ALL);
        if (where.getBlockEntity(pos) instanceof ItemCapsuleBlockEntity mutated) mutated.hangOn(hangs);
        hangs.flatMap(rootPos -> Stems.rootAt(where, rootPos)).ifPresent(CapsuleStemBlockEntity::touch);
        return true;
    }

    /**
     * A pod back in reach tells its root, so the tree's store is read again with it, and checks that the cell it hangs
     * on is not gone with its tree ({@link ItemCapsuleBlock#tick}): a pod loaded after its tree died could not be
     * told. The check is asked for once the chunk's ticks are registered, which they may not be while it loads.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (!(level instanceof ServerLevel server) || root.isEmpty()) return;
        Stems.rootAt(level, root.get()).ifPresent(CapsuleStemBlockEntity::touch);
        server.getServer().execute(() -> {
            if (!isRemoved() && server.hasChunkAt(worldPosition))
                server.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        });
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
        List<ItemStack> stacks = saved.stock().held();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && !Stems.takes(stack)) return Optional.of("a capsule that holds " + stack);
            int limit = Compartments.limit(kind.stacks(saved.stock().grade()), stack.getMaxStackSize());
            if (stack.getCount() > limit) return Optional.of(stack.getCount() + " " + stack + " is past a compartment");
        }
        CapsuleStock stock = saved.stock();
        if (stock.marks().size() > compartments()
                || !kind.reserves() && !stock.marks().isEmpty())
            return Optional.of(stock.marks().size() + " labels and reserves in a " + kind.id() + " pod");
        for (int i = 0; kind.reserves() && i < compartments(); i++) {
            ItemStack label = stock.mark(i).label();
            ItemStack stack = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
            if (!label.isEmpty() && !Stems.takes(label)) return Optional.of("a label that is " + label);
            if (!stack.isEmpty() && !ItemStack.isSameItemSameComponents(label, stack))
                return Optional.of(stack + " in a compartment labelled " + label);
            int limit = Compartments.limit(kind.stacks(stock.grade()), label.isEmpty() ? 64 : label.getMaxStackSize());
            if (stock.mark(i).reserve() > limit)
                return Optional.of("a reserve of " + stock.mark(i).reserve() + " past a compartment");
        }
        return Optional.empty();
    }

    private void restore(CapsuleStock stock) {
        Arrays.fill(held, ItemStack.EMPTY);
        Arrays.fill(labels, ItemStack.EMPTY);
        Arrays.fill(reserves, 0);
        List<ItemStack> stacks = stock.held();
        for (int i = 0; i < Math.min(compartments(), stacks.size()); i++) held[i] = stacks.get(i);
        if (kind.reserves())
            for (int i = 0; i < compartments(); i++) {
                labels[i] = stock.mark(i).label();
                reserves[i] = stock.mark(i).reserve();
            }
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
