package org.jd.infestusfrontier.storage.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bounded item/fluid balances and exact slot reservations for one Bowl core.
 * Reservations hold inputs and destinations without changing balances until commit.
 */
public final class QuantityStore {
    public static final int MAX_ITEM_SLOTS = 9;
    public static final int MAX_TANKS = 2;
    private static final int MAX_RESERVATIONS = 1;

    private long revision;
    private long nextReservationId = 1;
    private final List<ItemSlot> itemSlots;
    private final List<Tank> tanks;
    private final Map<Long, ReservationSnapshot> reservations = new LinkedHashMap<>();

    public QuantityStore(List<ItemSlot> itemSlots, List<Tank> tanks) {
        validateShape(itemSlots, tanks);
        this.itemSlots = new ArrayList<>(itemSlots);
        this.tanks = new ArrayList<>(tanks);
    }

    private QuantityStore(Snapshot snapshot) {
        validateShape(snapshot.itemSlots(), snapshot.tanks());
        if (snapshot.revision() < 0 || snapshot.revision() == Long.MAX_VALUE || snapshot.nextReservationId() < 1) {
            throw new IllegalArgumentException("Invalid store counters");
        }
        if (snapshot.reservations().size() > MAX_RESERVATIONS) {
            throw new IllegalArgumentException("Too many Bowl reservations");
        }
        revision = snapshot.revision();
        nextReservationId = snapshot.nextReservationId();
        itemSlots = new ArrayList<>(snapshot.itemSlots());
        tanks = new ArrayList<>(snapshot.tanks());
        for (var reservation : snapshot.reservations()) {
            validateReservation(reservation);
            if (reservation.reservationId() >= nextReservationId) {
                throw new IllegalArgumentException("Reservation identifier was not advanced");
            }
            if (reservations.putIfAbsent(reservation.reservationId(), reservation) != null) {
                throw new IllegalArgumentException("Duplicate reservation identifier");
            }
        }
        validateReservedState();
    }

    public static QuantityStore restore(Snapshot snapshot) {
        return new QuantityStore(Objects.requireNonNull(snapshot, "snapshot"));
    }

    public long revision() {
        return revision;
    }

    public int itemCount(String item) {
        return snapshot().itemCount(item);
    }

    public int fluidAmount(String fluid) {
        return snapshot().fluidAmount(fluid);
    }

    public int availableItem(String item) {
        int available = itemCount(item);
        for (var reservation : reservations.values()) {
            for (var allocation : reservation.inputItems()) {
                if (allocation.resource().equals(item)) available -= allocation.amount();
            }
        }
        return available;
    }

    public int availableFluid(String fluid) {
        int available = fluidAmount(fluid);
        for (var reservation : reservations.values()) {
            for (var allocation : reservation.inputFluids()) {
                if (allocation.resource().equals(fluid)) available -= allocation.amount();
            }
        }
        return available;
    }

    public boolean hasReservation(long reservationId) {
        return reservations.containsKey(reservationId);
    }

    public PreviewResult preview(ReservationRequest request, long expectedRevision) {
        var before = snapshot();
        var trial = QuantityStore.restore(before);
        var result = trial.reserve(request, expectedRevision);
        if (result instanceof ReservationRefused refused) {
            return new PreviewRefused(refused.reason(), refused.resource(), before);
        }
        return new PreviewAvailable(before);
    }

    public ReserveResult reserve(ReservationRequest request, long expectedRevision) {
        Objects.requireNonNull(request, "request");
        if (expectedRevision != revision) return refused(ReserveRefusal.STALE_REVISION, "");
        if (revision == Long.MAX_VALUE - 1) return refused(ReserveRefusal.REVISION_EXHAUSTED, "");
        if (reservations.size() == MAX_RESERVATIONS) {
            return refused(ReserveRefusal.RESERVATION_LIMIT, "");
        }

        var inputItems = planItemInputs(request.itemInputs());
        if (inputItems == null) return refused(ReserveRefusal.INSUFFICIENT_ITEM, firstMissingItem(request.itemInputs()));
        var inputFluids = planFluidInputs(request.fluidInputs());
        if (inputFluids == null) {
            return refused(ReserveRefusal.INSUFFICIENT_FLUID, firstMissingFluid(request.fluidInputs()));
        }

        var outputResources = reservedOutputResources();
        var outputAmounts = reservedOutputAmounts();
        var returned = planItemOutputs(request.returnedContainers(), outputResources, outputAmounts);
        if (returned == null) {
            return refused(ReserveRefusal.RETURNED_CONTAINER_FULL, firstResource(request.returnedContainers()));
        }
        var outputs = planItemOutputs(request.itemOutputs(), outputResources, outputAmounts);
        if (outputs == null) return refused(ReserveRefusal.OUTPUT_FULL, firstResource(request.itemOutputs()));

        long reservationId = nextReservationId;
        if (nextReservationId == Long.MAX_VALUE) return refused(ReserveRefusal.IDENTIFIER_EXHAUSTED, "");
        nextReservationId++;
        var reservation = new ReservationSnapshot(reservationId, inputItems, inputFluids, outputs, returned);
        reservations.put(reservationId, reservation);
        revision++;
        return new Reserved(reservationId, snapshot());
    }

    public CommitResult commit(long reservationId) {
        var reservation = reservations.get(reservationId);
        if (reservation == null) return CommitResult.UNKNOWN_RESERVATION;
        if (revision == Long.MAX_VALUE - 1) throw new IllegalStateException("Store revision is exhausted");
        validateCommit(reservation);
        applyItemInputs(reservation.inputItems());
        applyFluidInputs(reservation.inputFluids());
        applyItemOutputs(reservation.returnedContainers());
        applyItemOutputs(reservation.itemOutputs());
        reservations.remove(reservationId);
        revision++;
        return CommitResult.COMMITTED;
    }

    public ReleaseResult release(long reservationId) {
        if (revision == Long.MAX_VALUE - 1 && reservations.containsKey(reservationId)) {
            throw new IllegalStateException("Store revision is exhausted");
        }
        if (reservations.remove(reservationId) == null) return ReleaseResult.UNKNOWN_RESERVATION;
        revision++;
        return ReleaseResult.RELEASED;
    }

    public Snapshot snapshot() {
        return new Snapshot(revision, nextReservationId, itemSlots, tanks, new ArrayList<>(reservations.values()));
    }

    private List<ItemAllocation> planItemInputs(Map<String, Integer> requested) {
        var reserved = reservedInputAmounts();
        var planned = new ArrayList<ItemAllocation>();
        for (var request : requested.entrySet()) {
            int remaining = request.getValue();
            for (int slot = 0; slot < itemSlots.size() && remaining > 0; slot++) {
                var held = itemSlots.get(slot);
                if (!held.resource().equals(request.getKey())) continue;
                int available = held.count() - reserved[slot];
                int take = Math.min(available, remaining);
                if (take > 0) {
                    planned.add(new ItemAllocation(slot, request.getKey(), take));
                    reserved[slot] += take;
                    remaining -= take;
                }
            }
            if (remaining != 0) return null;
        }
        return planned;
    }

    private List<FluidAllocation> planFluidInputs(Map<String, Integer> requested) {
        var reserved = reservedFluidInputAmounts();
        var planned = new ArrayList<FluidAllocation>();
        for (var request : requested.entrySet()) {
            int remaining = request.getValue();
            for (int tank = 0; tank < tanks.size() && remaining > 0; tank++) {
                var held = tanks.get(tank);
                if (!held.resource().equals(request.getKey())) continue;
                int available = held.amount() - reserved[tank];
                int take = Math.min(available, remaining);
                if (take > 0) {
                    planned.add(new FluidAllocation(tank, request.getKey(), take));
                    reserved[tank] += take;
                    remaining -= take;
                }
            }
            if (remaining != 0) return null;
        }
        return planned;
    }

    private List<ItemAllocation> planItemOutputs(
            Map<String, Integer> requested, String[] reservedResources, int[] reservedAmounts) {
        var planned = new ArrayList<ItemAllocation>();
        for (var request : requested.entrySet()) {
            int remaining = request.getValue();
            remaining = allocateItemOutput(
                    request.getKey(), remaining, true, reservedResources, reservedAmounts, planned);
            remaining = allocateItemOutput(
                    request.getKey(), remaining, false, reservedResources, reservedAmounts, planned);
            if (remaining != 0) return null;
        }
        return planned;
    }

    private int allocateItemOutput(
            String resource,
            int remaining,
            boolean matchingDestinations,
            String[] reservedResources,
            int[] reservedAmounts,
            List<ItemAllocation> planned) {
        for (int slot = 0; slot < itemSlots.size() && remaining > 0; slot++) {
            var held = itemSlots.get(slot);
            String destination = held.isEmpty() ? reservedResources[slot] : held.resource();
            boolean matches = destination.equals(resource);
            if (matchingDestinations != matches || (!matchingDestinations && !destination.isEmpty())) continue;
            int room = held.capacity() - held.count() - reservedAmounts[slot];
            int put = Math.min(room, remaining);
            if (put > 0) {
                planned.add(new ItemAllocation(slot, resource, put));
                reservedResources[slot] = resource;
                reservedAmounts[slot] += put;
                remaining -= put;
            }
        }
        return remaining;
    }

    private int[] reservedInputAmounts() {
        var amounts = new int[itemSlots.size()];
        for (var reservation : reservations.values()) {
            for (var allocation : reservation.inputItems()) amounts[allocation.index()] += allocation.amount();
        }
        return amounts;
    }

    private int[] reservedFluidInputAmounts() {
        var amounts = new int[tanks.size()];
        for (var reservation : reservations.values()) {
            for (var allocation : reservation.inputFluids()) amounts[allocation.index()] += allocation.amount();
        }
        return amounts;
    }

    private String[] reservedOutputResources() {
        var resources = new String[itemSlots.size()];
        for (int slot = 0; slot < resources.length; slot++) resources[slot] = "";
        for (var reservation : reservations.values()) {
            markOutputResources(resources, reservation.returnedContainers());
            markOutputResources(resources, reservation.itemOutputs());
        }
        return resources;
    }

    private int[] reservedOutputAmounts() {
        var amounts = new int[itemSlots.size()];
        for (var reservation : reservations.values()) {
            for (var allocation : reservation.returnedContainers()) amounts[allocation.index()] += allocation.amount();
            for (var allocation : reservation.itemOutputs()) amounts[allocation.index()] += allocation.amount();
        }
        return amounts;
    }

    private static void markOutputResources(String[] resources, List<ItemAllocation> allocations) {
        for (var allocation : allocations) resources[allocation.index()] = allocation.resource();
    }

    private String firstMissingItem(Map<String, Integer> requested) {
        for (var request : requested.entrySet()) {
            if (availableItem(request.getKey()) < request.getValue()) return request.getKey();
        }
        return firstResource(requested);
    }

    private String firstMissingFluid(Map<String, Integer> requested) {
        for (var request : requested.entrySet()) {
            if (availableFluid(request.getKey()) < request.getValue()) return request.getKey();
        }
        return firstResource(requested);
    }

    private static String firstResource(Map<String, Integer> requested) {
        return requested.isEmpty() ? "" : requested.keySet().iterator().next();
    }

    private ReservationRefused refused(ReserveRefusal reason, String resource) {
        return new ReservationRefused(reason, resource, snapshot());
    }

    private void validateCommit(ReservationSnapshot reservation) {
        for (var allocation : reservation.inputItems()) {
            var slot = itemSlots.get(allocation.index());
            if (!slot.resource().equals(allocation.resource()) || slot.count() < allocation.amount()) {
                throw new IllegalStateException("Reserved item input changed");
            }
        }
        for (var allocation : reservation.inputFluids()) {
            var tank = tanks.get(allocation.index());
            if (!tank.resource().equals(allocation.resource()) || tank.amount() < allocation.amount()) {
                throw new IllegalStateException("Reserved fluid input changed");
            }
        }
    }

    private void applyItemInputs(List<ItemAllocation> allocations) {
        for (var allocation : allocations) {
            var slot = itemSlots.get(allocation.index());
            int remaining = slot.count() - allocation.amount();
            itemSlots.set(
                    allocation.index(),
                    remaining == 0 ? ItemSlot.empty(slot.capacity()) : new ItemSlot(slot.resource(), remaining, slot.capacity()));
        }
    }

    private void applyFluidInputs(List<FluidAllocation> allocations) {
        for (var allocation : allocations) {
            var tank = tanks.get(allocation.index());
            int remaining = tank.amount() - allocation.amount();
            tanks.set(
                    allocation.index(),
                    remaining == 0 ? Tank.empty(tank.capacity()) : new Tank(tank.resource(), remaining, tank.capacity()));
        }
    }

    private void applyItemOutputs(List<ItemAllocation> allocations) {
        for (var allocation : allocations) {
            var slot = itemSlots.get(allocation.index());
            if (!slot.isEmpty() && !slot.resource().equals(allocation.resource())) {
                throw new IllegalStateException("Reserved item output changed");
            }
            int count = Math.addExact(slot.count(), allocation.amount());
            if (count > slot.capacity()) throw new IllegalStateException("Reserved item output no longer fits");
            itemSlots.set(allocation.index(), new ItemSlot(allocation.resource(), count, slot.capacity()));
        }
    }

    private void validateReservation(ReservationSnapshot reservation) {
        if (reservation.reservationId() < 1) throw new IllegalArgumentException("Invalid reservation identifier");
        for (var allocation : reservation.inputItems()) validateItemAllocation(allocation);
        for (var allocation : reservation.itemOutputs()) validateItemAllocation(allocation);
        for (var allocation : reservation.returnedContainers()) validateItemAllocation(allocation);
        for (var allocation : reservation.inputFluids()) {
            if (allocation.index() < 0 || allocation.index() >= tanks.size()) {
                throw new IllegalArgumentException("Fluid reservation is outside the store");
            }
        }
    }

    private void validateItemAllocation(ItemAllocation allocation) {
        if (allocation.index() < 0 || allocation.index() >= itemSlots.size()) {
            throw new IllegalArgumentException("Item reservation is outside the store");
        }
    }

    private void validateReservedState() {
        var inputItems = reservedInputAmounts();
        var inputFluids = reservedFluidInputAmounts();
        var outputResources = reservedOutputResources();
        var outputAmounts = reservedOutputAmounts();
        for (int slot = 0; slot < itemSlots.size(); slot++) {
            var held = itemSlots.get(slot);
            if (inputItems[slot] > held.count()) throw new IllegalArgumentException("Reserved item input is absent");
            if (!held.isEmpty() && !outputResources[slot].isEmpty() && !held.resource().equals(outputResources[slot])) {
                throw new IllegalArgumentException("Reserved item output has the wrong destination");
            }
            if (outputAmounts[slot] > held.capacity() - held.count()) {
                throw new IllegalArgumentException("Reserved item output exceeds capacity");
            }
        }
        for (int tank = 0; tank < tanks.size(); tank++) {
            if (inputFluids[tank] > tanks.get(tank).amount()) {
                throw new IllegalArgumentException("Reserved fluid input is absent");
            }
        }
        for (var reservation : reservations.values()) validateReservationResources(reservation);
    }

    private void validateReservationResources(ReservationSnapshot reservation) {
        for (var allocation : reservation.inputItems()) {
            if (!itemSlots.get(allocation.index()).resource().equals(allocation.resource())) {
                throw new IllegalArgumentException("Reserved item input names the wrong resource");
            }
        }
        for (var allocation : reservation.inputFluids()) {
            if (!tanks.get(allocation.index()).resource().equals(allocation.resource())) {
                throw new IllegalArgumentException("Reserved fluid input names the wrong resource");
            }
        }
        var resources = new String[itemSlots.size()];
        for (int slot = 0; slot < resources.length; slot++) resources[slot] = itemSlots.get(slot).resource();
        validateOutputResources(resources, reservation.returnedContainers());
        validateOutputResources(resources, reservation.itemOutputs());
    }

    private static void validateOutputResources(String[] resources, List<ItemAllocation> allocations) {
        for (var allocation : allocations) {
            String selected = resources[allocation.index()];
            if (!selected.isEmpty() && !selected.equals(allocation.resource())) {
                throw new IllegalArgumentException("Reserved outputs conflict in one item slot");
            }
            resources[allocation.index()] = allocation.resource();
        }
    }

    private static void validateShape(List<ItemSlot> itemSlots, List<Tank> tanks) {
        Objects.requireNonNull(itemSlots, "itemSlots");
        Objects.requireNonNull(tanks, "tanks");
        if (itemSlots.size() > MAX_ITEM_SLOTS) throw new IllegalArgumentException("At most nine item slots are allowed");
        if (tanks.size() > MAX_TANKS) throw new IllegalArgumentException("At most two tanks are allowed");
        for (var slot : itemSlots) Objects.requireNonNull(slot, "item slot");
        for (var tank : tanks) Objects.requireNonNull(tank, "tank");
    }

    private static Map<String, Integer> quantities(Map<String, Integer> source, int maximumEntries) {
        Objects.requireNonNull(source, "quantities");
        if (source.size() > maximumEntries) throw new IllegalArgumentException("Too many resource entries");
        var copy = new LinkedHashMap<String, Integer>();
        for (var entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue() < 1) {
                throw new IllegalArgumentException("Quantities require named resources and positive amounts");
            }
            copy.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(copy);
    }

    public sealed interface ReserveResult permits Reserved, ReservationRefused {
        Snapshot state();
    }

    public sealed interface PreviewResult permits PreviewAvailable, PreviewRefused {
        Snapshot state();
    }

    public record PreviewAvailable(Snapshot state) implements PreviewResult {}

    public record PreviewRefused(ReserveRefusal reason, String resource, Snapshot state) implements PreviewResult {}

    public record Reserved(long reservationId, Snapshot state) implements ReserveResult {}

    public record ReservationRefused(ReserveRefusal reason, String resource, Snapshot state) implements ReserveResult {}

    public enum ReserveRefusal {
        STALE_REVISION,
        RESERVATION_LIMIT,
        INSUFFICIENT_ITEM,
        INSUFFICIENT_FLUID,
        OUTPUT_FULL,
        RETURNED_CONTAINER_FULL,
        IDENTIFIER_EXHAUSTED,
        REVISION_EXHAUSTED
    }

    public enum CommitResult {
        COMMITTED,
        UNKNOWN_RESERVATION
    }

    public enum ReleaseResult {
        RELEASED,
        UNKNOWN_RESERVATION
    }

    public record ReservationRequest(
            Map<String, Integer> itemInputs,
            Map<String, Integer> fluidInputs,
            Map<String, Integer> itemOutputs,
            Map<String, Integer> returnedContainers) {
        public ReservationRequest {
            itemInputs = quantities(itemInputs, MAX_ITEM_SLOTS);
            fluidInputs = quantities(fluidInputs, MAX_TANKS);
            itemOutputs = quantities(itemOutputs, MAX_ITEM_SLOTS);
            returnedContainers = quantities(returnedContainers, MAX_ITEM_SLOTS);
        }
    }

    public record ItemSlot(String resource, int count, int capacity) {
        public ItemSlot {
            if (capacity < 1 || capacity > 64) throw new IllegalArgumentException("Item slot capacity must be 1..64");
            if (count < 0 || count > capacity) throw new IllegalArgumentException("Item count is outside slot capacity");
            if (count == 0) resource = "";
            if (resource == null || (count > 0 && resource.isBlank())) {
                throw new IllegalArgumentException("A non-empty slot requires an item");
            }
        }

        public static ItemSlot empty(int capacity) {
            return new ItemSlot("", 0, capacity);
        }

        public boolean isEmpty() {
            return count == 0;
        }
    }

    public record Tank(String resource, int amount, int capacity) {
        public Tank {
            if (capacity < 1) throw new IllegalArgumentException("Tank capacity must be positive");
            if (amount < 0 || amount > capacity) throw new IllegalArgumentException("Fluid amount is outside tank capacity");
            if (amount == 0) resource = "";
            if (resource == null || (amount > 0 && resource.isBlank())) {
                throw new IllegalArgumentException("A non-empty tank requires a fluid");
            }
        }

        public static Tank empty(int capacity) {
            return new Tank("", 0, capacity);
        }

        public boolean isEmpty() {
            return amount == 0;
        }
    }

    public record ItemAllocation(int index, String resource, int amount) {
        public ItemAllocation {
            if (index < 0 || resource == null || resource.isBlank() || amount < 1) {
                throw new IllegalArgumentException("Invalid item allocation");
            }
        }
    }

    public record FluidAllocation(int index, String resource, int amount) {
        public FluidAllocation {
            if (index < 0 || resource == null || resource.isBlank() || amount < 1) {
                throw new IllegalArgumentException("Invalid fluid allocation");
            }
        }
    }

    public record ReservationSnapshot(
            long reservationId,
            List<ItemAllocation> inputItems,
            List<FluidAllocation> inputFluids,
            List<ItemAllocation> itemOutputs,
            List<ItemAllocation> returnedContainers) {
        public ReservationSnapshot {
            inputItems = List.copyOf(Objects.requireNonNull(inputItems, "inputItems"));
            inputFluids = List.copyOf(Objects.requireNonNull(inputFluids, "inputFluids"));
            itemOutputs = List.copyOf(Objects.requireNonNull(itemOutputs, "itemOutputs"));
            returnedContainers = List.copyOf(Objects.requireNonNull(returnedContainers, "returnedContainers"));
        }
    }

    public record Snapshot(
            long revision,
            long nextReservationId,
            List<ItemSlot> itemSlots,
            List<Tank> tanks,
            List<ReservationSnapshot> reservations) {
        public Snapshot {
            itemSlots = List.copyOf(Objects.requireNonNull(itemSlots, "itemSlots"));
            tanks = List.copyOf(Objects.requireNonNull(tanks, "tanks"));
            reservations = List.copyOf(Objects.requireNonNull(reservations, "reservations"));
        }

        public int itemCount(String item) {
            int count = 0;
            for (var slot : itemSlots) if (slot.resource().equals(item)) count = Math.addExact(count, slot.count());
            return count;
        }

        public int fluidAmount(String fluid) {
            int amount = 0;
            for (var tank : tanks) if (tank.resource().equals(fluid)) amount = Math.addExact(amount, tank.amount());
            return amount;
        }
    }
}
