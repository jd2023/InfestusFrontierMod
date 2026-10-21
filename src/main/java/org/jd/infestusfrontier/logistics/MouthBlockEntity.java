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
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.logistics.Delivery;
import org.jd.infestusfrontier.core.logistics.Reach;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jetbrains.annotations.Nullable;

/**
 * A Mouth's settings and the packets it has sent. A Mouth whose mode gives offers its partner's items to the Mouths of
 * its network whose mode takes and extracts only what a destination accepted in a simulated insert; the packet stays saved here while
 * it travels, 2 ticks a cell, and is inserted on arrival. A refusal on arrival goes to the next accepting destination
 * or waits here. Item Veins hold nothing.
 */
final class MouthBlockEntity extends BlockEntity implements MenuProvider {
    static final int PULSE_TICKS = 8;
    static final int RETRY_TICKS = 20;
    static final int PACKET = 8;
    /** Packets one output Mouth has on the way at once. */
    static final int IN_FLIGHT = 4;
    /** Slots of the partner an output pulse looks at. */
    static final int SOURCE_SLOTS = 16;
    /** Simulated inserts into destinations one pulse makes. */
    static final int OFFERS = 16;
    /** Slots of a store one insertion or one count touches. */
    static final int STORE_SLOTS = 64;
    /** Cells a network search visits. */
    static final int NETWORK = 256;

    static final int TICKS_PER_CELL = 2;
    static final int FILTER_SLOTS = 9;

    private record Packet(ItemStack items, BlockPos to, long arrival) {
        static final Codec<Packet> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        ItemStack.CODEC.fieldOf("items").forGetter(Packet::items),
                        BlockPos.CODEC.fieldOf("to").forGetter(Packet::to),
                        Codec.LONG.fieldOf("arrival").forGetter(Packet::arrival))
                .apply(instance, Packet::new));
    }

    private record Saved(List<ItemStack> filter, boolean deny, int amount, int priority, List<Packet> packets) {}

    private static final MapCodec<Saved> SAVED = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ItemStack.OPTIONAL_CODEC
                            .listOf()
                            .optionalFieldOf("filter", List.of())
                            .forGetter(Saved::filter),
                    Codec.BOOL.optionalFieldOf("deny", false).forGetter(Saved::deny),
                    Codec.INT.optionalFieldOf("amount", 0).forGetter(Saved::amount),
                    Codec.INT.optionalFieldOf("priority", 0).forGetter(Saved::priority),
                    Packet.CODEC.listOf().optionalFieldOf("packets", List.of()).forGetter(Saved::packets))
            .apply(instance, Saved::new));

    /** A Mouth that takes, found on this Mouth's network. */
    private record Route(BlockPos pos, int distance) {}

    private final SimpleContainer filter = new SimpleContainer(FILTER_SLOTS);
    private boolean deny;
    private int amount;
    private int priority;
    private final List<Packet> packets = new ArrayList<>();
    private Optional<CompoundTag> unreadable = Optional.empty();
    private List<Route> routes = List.of();
    private long routesSeen = -1;
    private int sourceCursor;
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

    /** Items on the way from this Mouth. */
    int carried() {
        return packets.stream().mapToInt(p -> p.items().getCount()).sum();
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
        LogisticsModule.routesChanged(level);
    }

    // --- the pulse ---

    /** Schedules the next pulse when there is work and none is scheduled. */
    void wake() {
        if (level == null || level.isClientSide || unreadable.isPresent()) return;
        if (!mode().gives() && packets.isEmpty()) return;
        if (!level.getBlockTicks()
                .hasScheduledTick(worldPosition, getBlockState().getBlock()))
            level.scheduleTick(worldPosition, getBlockState().getBlock(), PULSE_TICKS);
    }

    /** One pulse: deliver what arrived, then send one new packet. Called from the block's scheduled tick. */
    void pulse() {
        if (level == null || level.isClientSide || unreadable.isPresent()) return;
        boolean output = mode().gives();
        if (!output && packets.isEmpty()) return;
        long now = level.getGameTime();
        if (!level.getData(LogisticsModule.MOUTH_QUOTA).take(now)) {
            schedule(RETRY_TICKS + 1);
            return;
        }
        boolean worked = deliverArrived(now);
        if (output && packets.size() < IN_FLIGHT) worked |= extract(now);
        long next = worked ? PULSE_TICKS : RETRY_TICKS;
        for (Packet packet : packets) if (packet.arrival() > now) next = Math.min(next, packet.arrival() - now);
        if (output || !packets.isEmpty()) schedule((int) Math.max(1, next));
    }

    private void schedule(int ticks) {
        level.scheduleTick(worldPosition, getBlockState().getBlock(), ticks);
    }

    private boolean deliverArrived(long now) {
        boolean moved = false;
        for (int i = 0; i < packets.size(); i++) {
            Packet packet = packets.get(i);
            if (packet.arrival() > now) continue;
            ItemStack rest = isRoute(packet.to())
                    ? destination(packet.to())
                            .map(d -> d.accept(packet.items()))
                            .orElse(packet.items())
                    : packet.items();
            if (rest.getCount() < packet.items().getCount()) moved = true;
            if (rest.isEmpty()) {
                packets.remove(i--);
            } else {
                Optional<Route> next = firstAccepting(rest);
                packets.set(
                        i,
                        next.map(r -> new Packet(rest, r.pos(), now + travel(r)))
                                .orElseGet(() -> new Packet(rest, packet.to(), now + RETRY_TICKS)));
            }
            setChanged();
        }
        return moved;
    }

    private boolean extract(long now) {
        IItemHandler source = partner();
        if (source == null || source.getSlots() == 0) return false;
        List<Route> order = ordered();
        if (order.isEmpty()) return false;
        int slots = source.getSlots();
        sourceCursor = Math.floorMod(sourceCursor, slots);
        int look = Math.min(SOURCE_SLOTS, slots);
        int offers = OFFERS;
        for (int n = 0; n < look && offers > 0; n++) {
            int slot = (sourceCursor + n) % slots;
            ItemStack offered = source.extractItem(slot, PACKET, true);
            if (offered.isEmpty() || !permits(offered)) continue;
            int allowed = amount == 0
                    ? offered.getCount()
                    : Delivery.outputAllowance(amount, count(source, offered), offered.getCount());
            if (allowed == 0) continue;
            ItemStack wanted = offered.copyWithCount(allowed);
            for (Route route : order) {
                if (offers-- == 0) break;
                int room = destination(route.pos()).map(d -> d.room(wanted)).orElse(0);
                if (room == 0) continue;
                ItemStack taken = source.extractItem(slot, room, false);
                if (taken.isEmpty()) break;
                packets.add(new Packet(taken, route.pos(), now + travel(route)));
                sourceCursor = slot;
                turn++;
                setChanged();
                return true;
            }
        }
        sourceCursor = (sourceCursor + look) % slots;
        return false;
    }

    private static long travel(Route route) {
        return (long) Math.max(1, route.distance()) * TICKS_PER_CELL;
    }

    private Optional<Route> firstAccepting(ItemStack stack) {
        for (Route route : ordered()) {
            if (destination(route.pos()).map(d -> d.room(stack)).orElse(0) > 0) return Optional.of(route);
        }
        return Optional.empty();
    }

    private boolean isRoute(BlockPos pos) {
        return routes().stream().anyMatch(r -> r.pos().equals(pos));
    }

    private List<Route> ordered() {
        List<Delivery.Destination<Route>> candidates = new ArrayList<>();
        for (Route route : routes()) {
            Optional<MouthBlockEntity> mouth = destination(route.pos());
            if (mouth.isPresent())
                candidates.add(new Delivery.Destination<>(route, mouth.get().priority(), route.distance()));
        }
        return Delivery.order(candidates, turn);
    }

    /** The Mouths of this network that take, found again only after a network in the dimension changed. */
    private List<Route> routes() {
        long epoch = level.getData(LogisticsModule.ROUTE_EPOCH).current();
        if (epoch == routesSeen) return routes;
        List<Route> found = new ArrayList<>();
        for (Reach.Reached<BlockPos> cell :
                Reach.walk(worldPosition, this::links, NETWORK).reached()) {
            if (cell.distance() == 0) continue;
            BlockState state = level.getBlockState(cell.node());
            if (state.getBlock() instanceof MouthBlock
                    && state.getValue(MouthBlock.MODE).takes()) found.add(new Route(cell.node(), cell.distance()));
        }
        routes = List.copyOf(found);
        routesSeen = epoch;
        return routes;
    }

    /** The loaded network cells joined to {@code pos}; a Mouth joins nothing through its top face. */
    private List<BlockPos> links(BlockPos pos) {
        List<BlockPos> out = new ArrayList<>(6);
        boolean mouth = level.getBlockState(pos).getBlock() instanceof MouthBlock;
        for (Direction side : Direction.values()) {
            if (mouth && side == Direction.UP) continue;
            BlockPos next = pos.relative(side);
            if (!level.hasChunkAt(next)) continue;
            BlockState state = level.getBlockState(next);
            if (!MouthBlock.isNetwork(state)) continue;
            if (state.getBlock() instanceof MouthBlock && side == Direction.DOWN) continue;
            out.add(next);
        }
        return out;
    }

    private Optional<MouthBlockEntity> destination(BlockPos pos) {
        if (!level.hasChunkAt(pos)) return Optional.empty();
        return level.getBlockEntity(pos) instanceof MouthBlockEntity mouth ? Optional.of(mouth) : Optional.empty();
    }

    // --- this Mouth as a destination ---

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

    private @Nullable IItemHandler partner() {
        BlockPos above = worldPosition.above();
        if (!level.hasChunkAt(above)) return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, above, Direction.DOWN);
    }

    private static ItemStack insert(IItemHandler store, ItemStack stack, boolean simulate) {
        int slots = Math.min(STORE_SLOTS, store.getSlots());
        for (int slot = 0; slot < slots && !stack.isEmpty(); slot++) stack = store.insertItem(slot, stack, simulate);
        return stack;
    }

    private static int count(IItemHandler store, ItemStack like) {
        int total = 0;
        int slots = Math.min(STORE_SLOTS, store.getSlots());
        for (int slot = 0; slot < slots; slot++) {
            ItemStack held = store.getStackInSlot(slot);
            if (ItemStack.isSameItemSameComponents(held, like)) total += held.getCount();
        }
        return total;
    }

    /** Drops every packet on the way once; a Mouth whose save could not be read keeps its bytes instead. */
    void dropPackets() {
        if (level == null || level.isClientSide || unreadable.isPresent()) return;
        for (Packet packet : packets)
            Containers.dropItemStack(
                    level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), packet.items());
        packets.clear();
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
        if (saved.packets().size() > IN_FLIGHT)
            return Optional.of(saved.packets().size() + " packets on the way");
        if (saved.packets().stream().anyMatch(p -> p.items().getCount() > PACKET))
            return Optional.of("a packet of more than " + PACKET);
        return Optional.empty();
    }

    private Saved saved() {
        List<ItemStack> entries = new ArrayList<>();
        for (ItemStack entry : filter.getItems()) entries.add(entry.copy());
        List<Packet> copies = new ArrayList<>();
        for (Packet packet : packets) copies.add(new Packet(packet.items().copy(), packet.to(), packet.arrival()));
        return new Saved(entries, deny, amount, priority, copies);
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
        packets.clear();
        for (Packet packet : saved.packets())
            packets.add(new Packet(packet.items().copy(), packet.to(), packet.arrival()));
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
        for (String key : List.of("filter", "deny", "amount", "priority", "packets")) tag.remove(key);
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
