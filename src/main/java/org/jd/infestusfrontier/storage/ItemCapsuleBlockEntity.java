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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.core.storage.Box;
import org.jd.infestusfrontier.core.storage.Compartments;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.storage.api.CapsuleStock;
import org.jetbrains.annotations.Nullable;

/**
 * One Item Capsule cell: its own {@value CapsuleStock#COMPARTMENTS} compartments and the box of the body it belongs
 * to. The compartments stay in the cell, so a broken cell carries exactly them; the body's one store is
 * {@link CapsuleBody}, shared by its cells while it stands. Clients get the compartments for the front.
 */
final class ItemCapsuleBlockEntity extends BlockEntity implements MenuProvider, Bodies.Cell {
    static final int COMPARTMENTS = CapsuleStock.COMPARTMENTS;

    private record Saved(CapsuleStock stock, Optional<Box> body) {}

    private static final MapCodec<Saved> SAVED = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    CapsuleStock.CODEC
                            .optionalFieldOf("stock", CapsuleStock.EMPTY)
                            .forGetter(Saved::stock),
                    Bodies.BOX.optionalFieldOf("body").forGetter(Saved::body))
            .apply(instance, Saved::new));

    private final ItemStack[] held = new ItemStack[COMPARTMENTS];
    private Box body;
    private Optional<CompoundTag> unreadable = Optional.empty();
    /** The store of the body while it stands; dropped whenever the body or the cell's state changes. */
    private @Nullable CapsuleBody store;

    ItemCapsuleBlockEntity(BlockPos pos, BlockState state) {
        super(StorageModule.ITEM_CAPSULE_ENTITY.get(), pos, state);
        Arrays.fill(held, ItemStack.EMPTY);
        body = Bodies.single(pos);
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
     * Whether a hand may put {@code stack} into {@code compartment}: a kind the capsule takes, into its own
     * compartment, or into an empty one when no other compartment of this cell holds it.
     */
    boolean accepts(int compartment, ItemStack stack) {
        if (unreadable.isPresent() || !Capsules.takes(stack)) return false;
        if (!held[compartment].isEmpty()) return holds(compartment, stack);
        for (int other = 0; other < COMPARTMENTS; other++) if (holds(other, stack)) return false;
        return true;
    }

    /** What the front shows: a copy of each compartment. */
    List<ItemStack> shown() {
        return Arrays.stream(held).map(ItemStack::copy).toList();
    }

    CapsuleStock stock() {
        return new CapsuleStock(List.of(held));
    }

    private void changed() {
        setChanged();
        if (store != null) store.touch();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Nullable CapsuleBody store() {
        return store;
    }

    /** Joins this cell to the body's store {@code next}; the store it had before stops answering. */
    void attach(CapsuleBody next) {
        if (store != null && store != next) store.close();
        store = next;
    }

    private void detach() {
        if (store != null) store.close();
        store = null;
    }

    @Override
    public Box body() {
        return body;
    }

    @Override
    public void setBody(Box next) {
        body = next;
        detach();
        setChanged();
    }

    @Override
    public boolean isReadable() {
        return unreadable.isEmpty();
    }

    boolean isEmpty() {
        if (unreadable.isPresent()) return false;
        for (ItemStack stack : held) if (!stack.isEmpty()) return false;
        return true;
    }

    /** Every cell looks at its body once it is back, as a Reservoir cell does. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide)
            level.scheduleTick(worldPosition, getBlockState().getBlock(), Bodies.FORM_TICKS);
    }

    /** A cell that goes with its chunk takes the body's store with it; the rest refuse until it forms again. */
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

    private static Optional<String> check(Saved saved) {
        for (ItemStack stack : saved.stock().held())
            if (!stack.isEmpty() && !Capsules.takes(stack)) return Optional.of("a capsule that holds " + stack);
        return Optional.empty();
    }

    private void restore(CapsuleStock stock) {
        List<ItemStack> stacks = stock.held();
        for (int i = 0; i < COMPARTMENTS; i++) held[i] = stacks.get(i);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        detach();
        Optional<Saved> read = Persistence.read(
                SAVED, tag, registries, StorageModule.LOGGER, "Item Capsule", ItemCapsuleBlockEntity::check);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        Arrays.fill(held, ItemStack.EMPTY);
        read.ifPresent(saved -> restore(saved.stock()));
        body = read.flatMap(Saved::body)
                .filter(box -> Bodies.holds(box, worldPosition))
                .orElse(Bodies.single(worldPosition));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else
            Persistence.write(
                    SAVED, new Saved(stock(), body.single() ? Optional.empty() : Optional.of(body)), registries, tag);
    }

    /** Clients get the compartments for the front, never the body or unreadable bytes. */
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
        else if (!isEmpty()) {
            components.set(StorageModule.CAPSULE_STOCK.get(), stock());
            components.set(DataComponents.MAX_STACK_SIZE, 1); // each carries its own compartments
        }
    }

    /** Carried compartments that fail their check are kept as unreadable bytes, never replaced. */
    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        input.get(DataComponents.MAX_STACK_SIZE); // read, so the cell does not keep it: it is set again while it holds
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            return;
        }
        CapsuleStock carried = input.getOrDefault(StorageModule.CAPSULE_STOCK.get(), CapsuleStock.EMPTY);
        Saved saved = new Saved(carried, Optional.empty());
        Optional<String> problem = check(saved).or(() -> overfull(carried));
        if (problem.isPresent()) {
            StorageModule.LOGGER.warn("Item Capsule keeps unreadable carried state: {}", problem.get());
            unreadable = Optional.of(
                    level == null ? new CompoundTag() : Persistence.encode(SAVED, saved, level.registryAccess()));
            return;
        }
        restore(carried);
    }

    /** A carried value is checked as the saved one is, since it never passed the codec's limits. */
    private static Optional<String> overfull(CapsuleStock stock) {
        for (ItemStack stack : stock.held())
            if (stack.getCount() > Compartments.limit(stack.getMaxStackSize()))
                return Optional.of(stack.getCount() + " " + stack + " is past a compartment");
        return Optional.empty();
    }

    /** The compartments travel as a component; the body never leaves its place. */
    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("stock");
        tag.remove("body");
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ItemCapsuleMenu(id, inventory, this);
    }
}
