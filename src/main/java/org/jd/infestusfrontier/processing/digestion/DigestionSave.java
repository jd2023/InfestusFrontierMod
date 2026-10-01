package org.jd.infestusfrontier.processing.digestion;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jd.infestusfrontier.organ.api.OrganHistory;
import org.jd.infestusfrontier.processing.digestion.api.DigestiveSac;
import org.jd.infestusfrontier.storage.api.QuantityStore;

/** Strict, bounded state codec for the two starter biomass organs. */
final class DigestionSave {
    static CompoundTag writeSac(DigestiveSac.State state) {
        var tag = new CompoundTag();
        tag.putInt("schema", state.schema());
        tag.putLong("revision", state.revision());
        tag.putLong("nextBatch", state.nextBatchId());
        tag.put("store", writeStore(state.quantities()));
        tag.putLong("completed", state.history().completedBatches());
        tag.putLong("lastCompleted", state.history().lastCompletedBatchId());
        tag.put("choices", list(state.history().choices(), choice -> {
            var out = new CompoundTag(); out.putString("choice", choice.name()); return out;
        }));
        if (state.activeBatch() != null) {
            var active = state.activeBatch();
            var out = new CompoundTag();
            out.putLong("id", active.batchId());
            out.putString("feed", active.feed());
            out.putLong("reservation", active.reservationId());
            out.putInt("work", active.completedWorkUnits());
            out.putInt("required", active.requiredWorkUnits());
            tag.put("active", out);
        }
        return tag;
    }

    static DigestiveSac.State readSac(CompoundTag tag) {
        if (integer(tag, "schema") != DigestiveSac.SNAPSHOT_SCHEMA) {
            throw new IllegalArgumentException("Unsupported Digestive Sac schema");
        }
        var choices = readList(tag, "choices", 3,
                choice -> OrganHistory.GrowthChoice.valueOf(text(choice, "choice")));
        var history = new OrganHistory.Snapshot(number(tag, "completed"), number(tag, "lastCompleted"), choices);
        DigestiveSac.ActiveBatch active = null;
        if (tag.contains("active")) {
            require(tag, "active", Tag.TAG_COMPOUND);
            var saved = tag.getCompound("active");
            active = new DigestiveSac.ActiveBatch(number(saved, "id"), text(saved, "feed"),
                    number(saved, "reservation"), integer(saved, "work"), integer(saved, "required"));
        }
        require(tag, "store", Tag.TAG_COMPOUND);
        return new DigestiveSac.State(DigestiveSac.SNAPSHOT_SCHEMA, number(tag, "revision"),
                number(tag, "nextBatch"), active, readStore(tag.getCompound("store"), 1, 1), history);
    }

    static CompoundTag writeStore(QuantityStore.Snapshot state) {
        var tag = new CompoundTag();
        tag.putLong("revision", state.revision());
        tag.putLong("nextReservation", state.nextReservationId());
        tag.put("items", list(state.itemSlots(), slot -> balance(slot.resource(), slot.count(), slot.capacity())));
        tag.put("tanks", list(state.tanks(), tank -> balance(tank.resource(), tank.amount(), tank.capacity())));
        tag.put("reservations", list(state.reservations(), reservation -> {
            var out = new CompoundTag();
            out.putLong("id", reservation.reservationId());
            out.put("itemInputs", list(reservation.inputItems(), DigestionSave::allocation));
            out.put("fluidInputs", list(reservation.inputFluids(), DigestionSave::allocation));
            out.put("itemOutputs", list(reservation.itemOutputs(), DigestionSave::allocation));
            out.put("returns", list(reservation.returnedContainers(), DigestionSave::allocation));
            out.put("fluidOutputs", list(reservation.outputFluids(), DigestionSave::allocation));
            return out;
        }));
        return tag;
    }

    static QuantityStore.Snapshot readStore(CompoundTag tag, int itemSlots, int tanks) {
        var items = readList(tag, "items", itemSlots, saved -> new QuantityStore.ItemSlot(
                text(saved, "resource"), integer(saved, "count"), integer(saved, "capacity")));
        var fluids = readList(tag, "tanks", tanks, saved -> new QuantityStore.Tank(
                text(saved, "resource"), integer(saved, "count"), integer(saved, "capacity")));
        if (items.size() != itemSlots || fluids.size() != tanks) {
            throw new IllegalArgumentException("Invalid biomass store shape");
        }
        var reservations = readList(tag, "reservations", 1, saved -> new QuantityStore.ReservationSnapshot(
                number(saved, "id"), itemAllocations(saved, "itemInputs"),
                fluidAllocations(saved, "fluidInputs"), itemAllocations(saved, "itemOutputs"),
                itemAllocations(saved, "returns"), fluidAllocations(saved, "fluidOutputs")));
        return new QuantityStore.Snapshot(number(tag, "revision"), number(tag, "nextReservation"),
                items, fluids, reservations);
    }

    private static List<QuantityStore.ItemAllocation> itemAllocations(CompoundTag tag, String key) {
        return readList(tag, key, QuantityStore.MAX_ITEM_SLOTS, saved -> new QuantityStore.ItemAllocation(
                integer(saved, "index"), text(saved, "resource"), integer(saved, "count")));
    }
    private static List<QuantityStore.FluidAllocation> fluidAllocations(CompoundTag tag, String key) {
        return readList(tag, key, QuantityStore.MAX_TANKS, saved -> new QuantityStore.FluidAllocation(
                integer(saved, "index"), text(saved, "resource"), integer(saved, "count")));
    }
    private static CompoundTag balance(String resource, int count, int capacity) {
        var tag = new CompoundTag();
        tag.putString("resource", resource); tag.putInt("count", count); tag.putInt("capacity", capacity);
        return tag;
    }
    private static CompoundTag allocation(QuantityStore.ItemAllocation allocation) {
        return allocation(allocation.index(), allocation.resource(), allocation.amount());
    }
    private static CompoundTag allocation(QuantityStore.FluidAllocation allocation) {
        return allocation(allocation.index(), allocation.resource(), allocation.amount());
    }
    private static CompoundTag allocation(int index, String resource, int amount) {
        var tag = new CompoundTag();
        tag.putInt("index", index); tag.putString("resource", resource); tag.putInt("count", amount);
        return tag;
    }
    private static <T> ListTag list(List<T> values, Function<T, CompoundTag> encode) {
        var list = new ListTag(); values.forEach(value -> list.add(encode.apply(value))); return list;
    }
    private static <T> List<T> readList(CompoundTag tag, String key, int max, Function<CompoundTag, T> decode) {
        require(tag, key, Tag.TAG_LIST);
        var raw = (ListTag) tag.get(key);
        if (raw.size() > max || (!raw.isEmpty() && raw.getElementType() != Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Invalid biomass list: " + key);
        }
        var values = new ArrayList<T>(raw.size());
        for (int index = 0; index < raw.size(); index++) values.add(decode.apply(raw.getCompound(index)));
        return List.copyOf(values);
    }
    private static String text(CompoundTag tag, String key) {
        require(tag, key, Tag.TAG_STRING);
        String value = tag.getString(key);
        if (value.length() > 64) throw new IllegalArgumentException("Biomass identifier exceeds 64 characters");
        return value;
    }
    private static int integer(CompoundTag tag, String key) { require(tag, key, Tag.TAG_INT); return tag.getInt(key); }
    private static long number(CompoundTag tag, String key) { require(tag, key, Tag.TAG_LONG); return tag.getLong(key); }
    private static void require(CompoundTag tag, String key, int type) {
        if (!tag.contains(key, type)) throw new IllegalArgumentException("Missing biomass field: " + key);
    }
    private DigestionSave() {}
}
