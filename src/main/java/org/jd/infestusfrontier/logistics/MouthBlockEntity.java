package org.jd.infestusfrontier.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.logistics.Delivery;
import org.jd.infestusfrontier.core.logistics.NetworkIndex;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.storage.api.ItemStore;
import org.jetbrains.annotations.Nullable;

/** An item endpoint: transfers directly to loaded peers and retains only an undelivered remainder. */
final class MouthBlockEntity extends BlockEntity implements MenuProvider {
    static final int PULSE_TICKS = 8;
    static final int RETRY_TICKS = 20;
    static final int TRANSFER_SIZE = 8;
    /** Slots of the partner an output pulse looks at. */
    static final int SOURCE_SLOTS = 16;
    /** Simulated inserts into destinations one pulse makes. */
    static final int OFFERS = 16;
    /** Slots of a store one insertion or one count touches. */
    static final int STORE_SLOTS = 64;

    static final int FILTER_SLOTS = 9;

    private record Saved(List<ItemStack> filter, boolean deny, int amount, int priority, ItemStack remainder) {}

    private static final MapCodec<Saved> SAVED = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ItemStack.OPTIONAL_CODEC
                            .listOf()
                            .optionalFieldOf("filter", List.of())
                            .forGetter(Saved::filter),
                    Codec.BOOL.optionalFieldOf("deny", false).forGetter(Saved::deny),
                    Codec.INT.optionalFieldOf("amount", 0).forGetter(Saved::amount),
                    Codec.INT.optionalFieldOf("priority", 0).forGetter(Saved::priority),
                    ItemStack.OPTIONAL_CODEC
                            .optionalFieldOf("remainder", ItemStack.EMPTY)
                            .forGetter(Saved::remainder))
            .apply(instance, Saved::new));

    /** A Mouth that takes, found on this Mouth's network. */
    private record Route(BlockPos pos) {}

    private final SimpleContainer filter = new SimpleContainer(FILTER_SLOTS);
    private boolean deny;
    private int amount;
    private int priority;
    private ItemStack remainder = ItemStack.EMPTY;
    private Optional<CompoundTag> unreadable = Optional.empty();
    /** Simulated inserts left in this pulse, for delivery and extraction together. */
    private int offers;

    private int sourceCursor;
    /** Where the next search for a taker starts in the ordered takers; carried from pulse to pulse. */
    private int cursor;

    private int turn;

    MouthBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsModule.MOUTH_ENTITY.get(), pos, state);
        filter.addListener(container -> setChanged());
    }

    MouthMode mode() {
        return getBlockState().getValue(MouthBlock.MODE);
    }

    SimpleContainer filter() {
        return filter;
    }

    boolean deny() {
        return deny;
    }

    int amount() {
        return amount;
    }

    int priority() {
        return priority;
    }

    /** Items retained after a destination partially refused execution. */
    int carried() {
        return remainder.getCount();
    }

    boolean isUnreadable() {
        return unreadable.isPresent();
    }

    // --- the settings the screen changes; every one runs on the server ---

    void cycleMode() {
        if (level == null || unreadable.isPresent()) return;
        level.setBlock(worldPosition, getBlockState().setValue(MouthBlock.MODE, mode().next()), 3);
    }

    void toggleList() {
        if (unreadable.isPresent()) return;
        deny = !deny;
        setChanged();
    }

    void stepAmount(boolean up) {
        if (unreadable.isPresent()) return;
        amount = Delivery.nextAmount(amount, up);
        setChanged();
    }

    void stepPriority(boolean up) {
        if (level == null || unreadable.isPresent()) return;
        priority = Delivery.nextPriority(priority, up);
        setChanged();
    }

    // --- the pulse ---

    /** Schedules the next pulse when there is work and none is scheduled. */
    void wake() {
        if (level == null || level.isClientSide || unreadable.isPresent()) return;
        if (!mode().gives() && remainder.isEmpty()) return;
        if (!level.getBlockTicks()
                .hasScheduledTick(worldPosition, getBlockState().getBlock()))
            level.scheduleTick(worldPosition, getBlockState().getBlock(), PULSE_TICKS);
    }

    /** One pulse recovers a remainder or performs one new transfer. */
    void pulse() {
        if (level == null || level.isClientSide || unreadable.isPresent()) return;
        boolean output = mode().gives();
        if (!output && remainder.isEmpty()) return;
        if (!NetworkWork.take((ServerLevel) level)) {
            schedule(RETRY_TICKS + 1);
            return;
        }
        offers = OFFERS;
        boolean worked = remainder.isEmpty() ? output && extract() : deliverRemainder();
        if (output || !remainder.isEmpty()) schedule(worked ? PULSE_TICKS : RETRY_TICKS);
    }

    private void schedule(int ticks) {
        level.scheduleTick(worldPosition, getBlockState().getBlock(), ticks);
    }

    private boolean deliverRemainder() {
        if (remainder.isEmpty()) return false;
        Optional<Taker> target = taker(ordered(), remainder);
        if (target.isEmpty()) return false;
        int before = remainder.getCount();
        remainder = destination(target.get().route().pos())
                .map(m -> m.accept(remainder))
                .orElse(remainder);
        if (remainder.getCount() != before) setChanged();
        return remainder.getCount() < before;
    }

    private boolean extract() {
        IItemHandler source = partner();
        if (source == null || source.getSlots() == 0) return false;
        List<Route> order = ordered();
        if (order.isEmpty()) return false;
        int slots = source.getSlots();
        sourceCursor = Math.floorMod(sourceCursor, slots);
        int look = Math.min(SOURCE_SLOTS, slots);
        for (int n = 0; n < look && offers > 0; n++) {
            int slot = (sourceCursor + n) % slots;
            ItemStack offered = source.extractItem(slot, TRANSFER_SIZE, true);
            if (offered.isEmpty() || !permits(offered)) continue;
            int allowed = amount == 0
                    ? offered.getCount()
                    : Delivery.outputAllowance(amount, count(source, offered), offered.getCount());
            if (allowed == 0) continue;
            ItemStack wanted = offered.copyWithCount(allowed);
            Optional<Taker> taker = taker(order, wanted);
            if (taker.isEmpty()) continue;
            ItemStack taken = source.extractItem(slot, taker.get().room(), false);
            if (taken.isEmpty()) continue;
            remainder = destination(taker.get().route().pos())
                    .map(m -> m.accept(taken))
                    .orElse(taken);
            sourceCursor = slot;
            turn++;
            setChanged();
            return true;
        }
        sourceCursor = (sourceCursor + look) % slots;
        return false;
    }

    /** A taker that accepted {@code room} items in a simulated insert. */
    private record Taker(Route route, int room) {}

    /**
     * The first taker in {@code order}, from {@link #cursor} on and wrapping, whose store takes some of {@code stack}
     * now. Every candidate costs one offer, including a filtered or unavailable endpoint. When the
     * budget runs out the cursor keeps its place, so the next pulse asks the takers after it: every taker is asked
     * within a few pulses however many refuse ahead of it. An acceptance sends the cursor back to the top.
     */
    private Optional<Taker> taker(List<Route> order, ItemStack stack) {
        int size = order.size();
        if (size == 0) return Optional.empty();
        int start = Math.floorMod(cursor, size);
        for (int step = 0; step < size && offers > 0; step++) {
            int index = (start + step) % size;
            Route route = order.get(index);
            offers--;
            cursor = (index + 1) % size;
            Optional<MouthBlockEntity> mouth = destination(route.pos());
            if (mouth.isEmpty() || !mouth.get().wants(stack)) continue;
            int room = mouth.get().room(stack);
            if (room > 0) {
                cursor = 0;
                return Optional.of(new Taker(route, room));
            }
        }
        return Optional.empty();
    }

    private List<Route> ordered() {
        List<Delivery.Destination<Route>> candidates = new ArrayList<>();
        for (Route route : routes()) {
            Optional<MouthBlockEntity> mouth = destination(route.pos());
            if (mouth.isPresent())
                candidates.add(new Delivery.Destination<>(route, mouth.get().priority()));
        }
        return Delivery.order(candidates, turn);
    }

    /** Shared component membership; endpoint mode, filters and inventories are always read live. */
    private List<Route> routes() {
        Optional<NetworkIndex.Component<NetworkTopology.Node>> component =
                NetworkTopology.get((ServerLevel) level).network((ServerLevel) level, worldPosition, null);
        if (component.isEmpty() || component.get().status() != NetworkIndex.Status.READY) return List.of();
        List<Route> found = new ArrayList<>();
        for (NetworkTopology.Node node : component.get().nodes()) {
            if (node.pos().equals(worldPosition) || node.faces() != (63 ^ NetworkTopology.bit(Direction.UP))) continue;
            if (destination(node.pos()).filter(mouth -> mouth.mode().takes()).isPresent())
                found.add(new Route(node.pos()));
        }
        return found;
    }

    private Optional<MouthBlockEntity> destination(BlockPos pos) {
        if (!level.hasChunkAt(pos)) return Optional.empty();
        return level.getBlockEntity(pos) instanceof MouthBlockEntity mouth ? Optional.of(mouth) : Optional.empty();
    }

    // --- this Mouth as a destination ---

    /** Whether this Mouth's mode and filter let {@code stack} in; its store is not asked. */
    boolean wants(ItemStack stack) {
        return unreadable.isEmpty() && mode().takes() && permits(stack);
    }

    /** How many of {@code stack} this Mouth's store takes now, when its mode takes; nothing changes. */
    int room(ItemStack stack) {
        if (unreadable.isPresent() || !mode().takes() || !permits(stack)) return 0;
        IItemHandler store = partner();
        if (store == null) return 0;
        int allowed = Delivery.intakeRoom(amount, amount == 0 ? 0 : count(store, stack), stack.getCount());
        if (allowed == 0) return 0;
        return allowed - insert(store, stack.copyWithCount(allowed), true).getCount();
    }

    /** Inserts what {@link #room} allows and returns the rest. */
    ItemStack accept(ItemStack stack) {
        int room = room(stack);
        if (room == 0) return stack;
        IItemHandler store = partner();
        int inserted = room - insert(store, stack.copyWithCount(room), false).getCount();
        return stack.copyWithCount(stack.getCount() - inserted);
    }

    private boolean permits(ItemStack stack) {
        boolean listed = false;
        boolean match = false;
        for (ItemStack entry : filter.getItems()) {
            if (entry.isEmpty()) continue;
            listed = true;
            if (stack.is(entry.getItem())) match = true;
        }
        if (!listed) return true;
        return deny != match;
    }

    /**
     * The store standing on this Mouth, or null. A double chest whose other half is in an unloaded chunk counts as
     * absent: vanilla's chest capability reads that half and would load its chunk.
     */
    private @Nullable IItemHandler partner() {
        BlockPos above = worldPosition.above();
        if (!level.hasChunkAt(above)) return null;
        BlockState state = level.getBlockState(above);
        if (state.getBlock() instanceof ChestBlock
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                && !level.hasChunkAt(above.relative(ChestBlock.getConnectedDirection(state)))) return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, above, Direction.DOWN);
    }

    /** A store the colony knows takes the stack by its own rules; any other store slot by slot. */
    private static ItemStack insert(IItemHandler store, ItemStack stack, boolean simulate) {
        if (store instanceof ItemStore known) return known.insert(stack, simulate);
        int slots = Math.min(STORE_SLOTS, store.getSlots());
        for (int slot = 0; slot < slots && !stack.isEmpty(); slot++) stack = store.insertItem(slot, stack, simulate);
        return stack;
    }

    /** A store the colony knows gives its whole count; any other store is counted over its first slots. */
    private static int count(IItemHandler store, ItemStack like) {
        if (store instanceof ItemStore known) return known.count(like);
        int total = 0;
        int slots = Math.min(STORE_SLOTS, store.getSlots());
        for (int slot = 0; slot < slots; slot++) {
            ItemStack held = store.getStackInSlot(slot);
            if (ItemStack.isSameItemSameComponents(held, like)) total += held.getCount();
        }
        return total;
    }

    /** Breaking an endpoint recovers its undelivered remainder exactly once. */
    void dropRemainder() {
        if (level == null || level.isClientSide || unreadable.isPresent() || remainder.isEmpty()) return;
        Containers.dropItemStack(
                level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), remainder);
        remainder = ItemStack.EMPTY;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        wake();
    }

    // --- saved state ---

    private Optional<String> check(Saved saved) {
        if (saved.filter().size() > FILTER_SLOTS)
            return Optional.of(saved.filter().size() + " filter entries");
        if (saved.filter().stream().anyMatch(s -> s.getCount() > 1)) return Optional.of("a filter entry of many");
        if (!Delivery.AMOUNTS.contains(saved.amount())) return Optional.of("amount " + saved.amount());
        if (saved.priority() < Delivery.MIN_PRIORITY || saved.priority() > Delivery.MAX_PRIORITY)
            return Optional.of("priority " + saved.priority());
        if (saved.remainder().getCount() > TRANSFER_SIZE) return Optional.of("remainder exceeds transfer size");
        return Optional.empty();
    }

    private Saved saved() {
        List<ItemStack> entries = new ArrayList<>();
        for (ItemStack entry : filter.getItems()) entries.add(entry.copy());
        return new Saved(entries, deny, amount, priority, remainder.copy());
    }

    private void restore(Saved saved) {
        for (int slot = 0; slot < FILTER_SLOTS; slot++)
            filter.getItems()
                    .set(
                            slot,
                            slot < saved.filter().size()
                                    ? saved.filter().get(slot).copy()
                                    : ItemStack.EMPTY);
        deny = saved.deny();
        amount = saved.amount();
        priority = saved.priority();
        remainder = saved.remainder().copy();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<Saved> read = Persistence.read(SAVED, tag, registries, LogisticsModule.LOGGER, "Mouth", this::check);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        read.ifPresent(this::restore);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, saved(), registries, tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        unreadable.ifPresent(tag -> components.set(OrganModule.UNREADABLE_SAVE.get(), tag.copy()));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) unreadable = Optional.of(kept.copy());
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        for (String key : List.of("filter", "deny", "amount", "priority", "remainder")) tag.remove(key);
    }

    // --- the screen ---

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MouthMenu(id, inventory, this);
    }
}
