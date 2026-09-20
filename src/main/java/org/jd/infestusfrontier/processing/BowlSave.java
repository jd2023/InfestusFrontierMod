package org.jd.infestusfrontier.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jd.infestusfrontier.organ.api.OrganHistory;
import org.jd.infestusfrontier.processing.api.BatchWork;
import org.jd.infestusfrontier.processing.api.CultureBowlRecipes;
import org.jd.infestusfrontier.storage.api.QuantityStore;

/** Strict bounded platform codec. Domain restore validates conservation and recipe identity. */
final class BowlSave {
    static final int WATER_CAPACITY = 2000;
    record Loaded(BatchWork.State state, String selected) {}
    static Loaded read(CompoundTag tag) {
        int schema = integer(tag, "schema");
        if (schema != 1) throw new IllegalArgumentException("Unsupported Bowl schema");
        String selected = text(tag, "selected");
        CultureBowlRecipes.recipe(selected);
        var slots = readList(tag, "items", 9, slot -> new QuantityStore.ItemSlot(
                text(slot, "resource"), integer(slot, "count"), integer(slot, "capacity")));
        var tanks = readList(tag, "tanks", 1, tank -> new QuantityStore.Tank(
                text(tank, "resource"), integer(tank, "count"), integer(tank, "capacity")));
        if (slots.size() != 9 || tanks.size() != 1 || tanks.getFirst().capacity() != WATER_CAPACITY
                || (!tanks.getFirst().isEmpty() && !tanks.getFirst().resource().equals("water"))) {
            throw new IllegalArgumentException("Invalid Bowl store shape");
        }
        for (var slot : slots) {
            if (!slot.isEmpty() && slot.capacity() > BowlResources.item(slot.resource()).getDefaultMaxStackSize()) {
                throw new IllegalArgumentException("Invalid Bowl stack capacity");
            }
        }
        var reservations = readList(tag, "reservations", 1, reservation -> new QuantityStore.ReservationSnapshot(
                number(reservation, "id"), itemAllocations(reservation, "inputs"),
                readList(reservation, "fluids", 1, a -> new QuantityStore.FluidAllocation(integer(a, "index"), text(a, "resource"), integer(a, "count"))),
                itemAllocations(reservation, "outputs"), itemAllocations(reservation, "returns")));
        var quantities = new QuantityStore.Snapshot(number(tag, "storeRevision"), number(tag, "nextReservation"), slots, tanks, reservations);
        var choices = readList(tag, "choices", 3, choice -> OrganHistory.GrowthChoice.valueOf(text(choice, "choice")));
        var history = new OrganHistory.Snapshot(number(tag, "completed"), number(tag, "lastCompleted"), choices);
        BatchWork.ActiveBatch active = null;
        if (tag.contains("active")) {
            require(tag, "active", Tag.TAG_COMPOUND);
            var a = tag.getCompound("active");
            active = new BatchWork.ActiveBatch(number(a, "id"), text(a, "recipe"), number(a, "reservation"), integer(a, "work"), integer(a, "required"));
        }
        return new Loaded(new BatchWork.State(schema, number(tag, "revision"), number(tag, "nextBatch"), active, quantities, history), selected);
    }
    static CompoundTag write(BatchWork.State state, String selected) {
        var tag = new CompoundTag();
        tag.putInt("schema", state.schema()); tag.putString("selected", selected);
        tag.putLong("revision", state.revision()); tag.putLong("nextBatch", state.nextBatchId());
        var q = state.quantities();
        tag.putLong("storeRevision", q.revision()); tag.putLong("nextReservation", q.nextReservationId());
        tag.put("items", list(q.itemSlots(), s -> balance(s.resource(), s.count(), s.capacity())));
        tag.put("tanks", list(q.tanks(), t -> balance(t.resource(), t.amount(), t.capacity())));
        tag.put("reservations", list(q.reservations(), r -> {
            var out = new CompoundTag(); out.putLong("id", r.reservationId());
            out.put("inputs", list(r.inputItems(), a -> allocation(a.index(), a.resource(), a.amount())));
            out.put("fluids", list(r.inputFluids(), a -> allocation(a.index(), a.resource(), a.amount())));
            out.put("outputs", list(r.itemOutputs(), a -> allocation(a.index(), a.resource(), a.amount())));
            out.put("returns", list(r.returnedContainers(), a -> allocation(a.index(), a.resource(), a.amount())));
            return out;
        }));
        var h = state.history(); tag.putLong("completed", h.completedBatches()); tag.putLong("lastCompleted", h.lastCompletedBatchId());
        tag.put("choices", list(h.choices(), choice -> { var out = new CompoundTag(); out.putString("choice", choice.name()); return out; }));
        if (state.activeBatch() != null) {
            var a = state.activeBatch(); var out = new CompoundTag();
            out.putLong("id", a.batchId()); out.putString("recipe", a.recipeId()); out.putLong("reservation", a.reservationId());
            out.putInt("work", a.completedWorkUnits()); out.putInt("required", a.requiredWorkUnits()); tag.put("active", out);
        }
        return tag;
    }
    private static List<QuantityStore.ItemAllocation> itemAllocations(CompoundTag tag, String key) {
        return readList(tag, key, 9, a -> new QuantityStore.ItemAllocation(integer(a, "index"), text(a, "resource"), integer(a, "count")));
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
            throw new IllegalArgumentException("Invalid Bowl list: " + key);
        }
        var values = new ArrayList<T>(raw.size());
        for (int i = 0; i < raw.size(); i++) values.add(decode.apply(raw.getCompound(i)));
        return List.copyOf(values);
    }
    private static String text(CompoundTag tag, String key) {
        require(tag, key, Tag.TAG_STRING); String value = tag.getString(key);
        if (value.length() > 64) throw new IllegalArgumentException("Bowl identifier exceeds 64 characters");
        return value;
    }
    private static int integer(CompoundTag tag, String key) { require(tag, key, Tag.TAG_INT); return tag.getInt(key); }
    private static long number(CompoundTag tag, String key) { require(tag, key, Tag.TAG_LONG); return tag.getLong(key); }
    private static void require(CompoundTag tag, String key, int type) {
        if (!tag.contains(key, type)) throw new IllegalArgumentException("Missing or invalid Bowl field: " + key);
    }
    private BowlSave() {}
}
