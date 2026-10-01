package org.jd.infestusfrontier.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jd.infestusfrontier.organ.api.OrganHistory;
import org.jd.infestusfrontier.processing.api.BatchWork;
import org.jd.infestusfrontier.storage.api.QuantityStore;

/** Strict schema-1 codec for a single preparation-organ core. */
final class PreparationSave {
    static final int ITEM_SLOTS = 4;
    static final int TANK_CAPACITY = 1000;

    static CompoundTag write(BatchWork.State state) {
        var tag = new CompoundTag();
        tag.putInt("schema", state.schema());
        tag.putLong("revision", state.revision());
        tag.putLong("nextBatch", state.nextBatchId());
        var store = state.quantities();
        tag.putLong("storeRevision", store.revision());
        tag.putLong("nextReservation", store.nextReservationId());
        tag.put("items", list(store.itemSlots(), slot -> balance(slot.resource(), slot.count(), slot.capacity())));
        tag.put("tanks", list(store.tanks(), tank -> balance(tank.resource(), tank.amount(), tank.capacity())));
        tag.put("reservations", list(store.reservations(), reservation -> {
            var out = new CompoundTag();
            out.putLong("id", reservation.reservationId());
            out.put("inputs", list(reservation.inputItems(), a -> allocation(a.index(), a.resource(), a.amount())));
            out.put("fluids", list(reservation.inputFluids(), a -> allocation(a.index(), a.resource(), a.amount())));
            out.put("outputs", list(reservation.itemOutputs(), a -> allocation(a.index(), a.resource(), a.amount())));
            out.put("returns", list(reservation.returnedContainers(), a -> allocation(a.index(), a.resource(), a.amount())));
            return out;
        }));
        var history = state.history();
        tag.putLong("completed", history.completedBatches());
        tag.putLong("lastCompleted", history.lastCompletedBatchId());
        tag.put("choices", list(history.choices(), choice -> {
            var out = new CompoundTag(); out.putString("choice", choice.name()); return out;
        }));
        if (state.activeBatch() != null) {
            var active = state.activeBatch();
            var out = new CompoundTag();
            out.putLong("id", active.batchId()); out.putString("recipe", active.recipeId());
            out.putLong("reservation", active.reservationId()); out.putInt("work", active.completedWorkUnits());
            out.putInt("required", active.requiredWorkUnits()); tag.put("active", out);
        }
        return tag;
    }

    static BatchWork.State read(CompoundTag tag, PreparationOrgan organ) {
        if (integer(tag, "schema") != BatchWork.SNAPSHOT_SCHEMA) throw new IllegalArgumentException("Unsupported preparation schema");
        var items = readList(tag, "items", ITEM_SLOTS, saved -> new QuantityStore.ItemSlot(
                text(saved, "resource"), integer(saved, "count"), integer(saved, "capacity")));
        var tanks = readList(tag, "tanks", 1, saved -> new QuantityStore.Tank(
                text(saved, "resource"), integer(saved, "count"), integer(saved, "capacity")));
        if (items.size() != ITEM_SLOTS || tanks.size() != 1 || tanks.getFirst().capacity() != TANK_CAPACITY
                || (!tanks.getFirst().isEmpty() && !tanks.getFirst().resource().equals(organ.fluid))) {
            throw new IllegalArgumentException("Invalid preparation store shape");
        }
        for (var slot : items) if (!slot.isEmpty()
                && slot.capacity() > PreparationResources.item(slot.resource()).getDefaultMaxStackSize()) {
            throw new IllegalArgumentException("Invalid preparation stack capacity");
        }
        var reservations = readList(tag, "reservations", 1, saved -> new QuantityStore.ReservationSnapshot(
                number(saved, "id"), itemAllocations(saved, "inputs"), fluidAllocations(saved, "fluids"),
                itemAllocations(saved, "outputs"), itemAllocations(saved, "returns")));
        var store = new QuantityStore.Snapshot(number(tag, "storeRevision"), number(tag, "nextReservation"),
                items, tanks, reservations);
        var choices = readList(tag, "choices", 3,
                choice -> OrganHistory.GrowthChoice.valueOf(text(choice, "choice")));
        var history = new OrganHistory.Snapshot(number(tag, "completed"), number(tag, "lastCompleted"), choices);
        BatchWork.ActiveBatch active = null;
        if (tag.contains("active")) {
            require(tag, "active", Tag.TAG_COMPOUND);
            var saved = tag.getCompound("active");
            active = new BatchWork.ActiveBatch(number(saved, "id"), text(saved, "recipe"),
                    number(saved, "reservation"), integer(saved, "work"), integer(saved, "required"));
            if (!organ.supports(active.recipeId())) throw new IllegalArgumentException("Wrong preparation recipe");
        }
        return new BatchWork.State(BatchWork.SNAPSHOT_SCHEMA, number(tag, "revision"), number(tag, "nextBatch"),
                active, store, history);
    }

    private static List<QuantityStore.ItemAllocation> itemAllocations(CompoundTag tag, String key) {
        return readList(tag, key, ITEM_SLOTS, a -> new QuantityStore.ItemAllocation(
                integer(a, "index"), text(a, "resource"), integer(a, "count")));
    }
    private static List<QuantityStore.FluidAllocation> fluidAllocations(CompoundTag tag, String key) {
        return readList(tag, key, 1, a -> new QuantityStore.FluidAllocation(
                integer(a, "index"), text(a, "resource"), integer(a, "count")));
    }
    private static CompoundTag balance(String resource, int count, int capacity) {
        var tag = new CompoundTag(); tag.putString("resource", resource); tag.putInt("count", count); tag.putInt("capacity", capacity); return tag;
    }
    private static CompoundTag allocation(int index, String resource, int count) {
        var tag = new CompoundTag(); tag.putInt("index", index); tag.putString("resource", resource); tag.putInt("count", count); return tag;
    }
    private static <T> ListTag list(List<T> values, Function<T, CompoundTag> encode) {
        var list = new ListTag(); values.forEach(value -> list.add(encode.apply(value))); return list;
    }
    private static <T> List<T> readList(CompoundTag tag, String key, int max, Function<CompoundTag, T> decode) {
        require(tag, key, Tag.TAG_LIST);
        var raw = (ListTag) tag.get(key);
        if (raw.size() > max || (!raw.isEmpty() && raw.getElementType() != Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Invalid preparation list: " + key);
        }
        var values = new ArrayList<T>(raw.size());
        for (int index = 0; index < raw.size(); index++) values.add(decode.apply(raw.getCompound(index)));
        return List.copyOf(values);
    }
    private static String text(CompoundTag tag, String key) {
        require(tag, key, Tag.TAG_STRING); String value = tag.getString(key);
        if (value.length() > 64) throw new IllegalArgumentException("Preparation identifier exceeds 64 characters");
        return value;
    }
    private static int integer(CompoundTag tag, String key) { require(tag, key, Tag.TAG_INT); return tag.getInt(key); }
    private static long number(CompoundTag tag, String key) { require(tag, key, Tag.TAG_LONG); return tag.getLong(key); }
    private static void require(CompoundTag tag, String key, int type) {
        if (!tag.contains(key, type)) throw new IllegalArgumentException("Missing preparation field: " + key);
    }
    private PreparationSave() {}
}
