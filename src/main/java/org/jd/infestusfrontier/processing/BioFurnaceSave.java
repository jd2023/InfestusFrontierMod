package org.jd.infestusfrontier.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jd.infestusfrontier.organ.api.OrganHistory;
import org.jd.infestusfrontier.processing.api.BatchWork;
import org.jd.infestusfrontier.processing.api.BioFurnaceRecipes;
import org.jd.infestusfrontier.storage.api.QuantityStore;

/** Strict schema-1 codec for the one recoverable Bio-Furnace core. */
final class BioFurnaceSave {
    private static final int ITEM_SLOTS = 2;
    private static final int BIOMASS_CAPACITY = 2_000;
    private static final int EXPERIENCE_CAPACITY = 100_000;

    static CompoundTag write(BatchWork.State state, BioFurnaceEntity furnace) {
        var tag = new CompoundTag();
        tag.putInt("schema", state.schema());
        tag.putInt("experience", furnace.storedExperience());
        tag.putLong("revision", state.revision());
        tag.putLong("nextBatch", state.nextBatchId());
        var store = state.quantities();
        tag.putLong("storeRevision", store.revision());
        tag.putLong("nextReservation", store.nextReservationId());
        tag.put("items", list(store.itemSlots(), slot -> stack(slot.resource(), slot.count(), slot.capacity())));
        tag.put("fluids", list(store.tanks(), tank -> stack(tank.resource(), tank.amount(), tank.capacity())));
        tag.put("reservations", list(store.reservations(), BioFurnaceSave::reservation));
        var history = state.history();
        tag.putLong("completed", history.completedBatches());
        tag.putLong("lastCompleted", history.lastCompletedBatchId());
        tag.put("choices", list(history.choices(), choice -> {
            var out = new CompoundTag();
            out.putString("choice", choice.name());
            return out;
        }));
        if (state.activeBatch() != null) {
            var active = state.activeBatch();
            var out = new CompoundTag();
            out.putLong("id", active.batchId());
            out.putString("recipe", active.recipeId());
            out.putString("output", furnace.resultOf(active.recipeId()).orElseThrow());
            out.putLong("reservation", active.reservationId());
            out.putInt("work", active.completedWorkUnits());
            out.putInt("required", active.requiredWorkUnits());
            tag.put("active", out);
        }
        return tag;
    }

    static BatchWork.State read(CompoundTag tag, BioFurnaceEntity furnace) {
        if (integer(tag, "schema") != BatchWork.SNAPSHOT_SCHEMA) throw new IllegalArgumentException("Unsupported Bio-Furnace schema");
        experience(tag);
        var items = readList(tag, "items", ITEM_SLOTS, saved -> itemStack(saved));
        var tanks = readList(tag, "fluids", 1, BioFurnaceSave::tank);
        if (items.size() != ITEM_SLOTS || tanks.size() != 1 || tanks.getFirst().capacity() != BIOMASS_CAPACITY
                || (!tanks.getFirst().isEmpty() && !tanks.getFirst().resource().equals(BioFurnaceRecipes.BIOMASS))) {
            throw new IllegalArgumentException("Invalid Bio-Furnace store shape");
        }
        var reservations = readList(tag, "reservations", 1, BioFurnaceSave::reservation);
        var history = new OrganHistory.Snapshot(number(tag, "completed"), number(tag, "lastCompleted"),
                readList(tag, "choices", OrganHistory.MAX_CHOICES,
                        choice -> OrganHistory.GrowthChoice.valueOf(text(choice, "choice"))));
        BatchWork.ActiveBatch active = null;
        if (tag.contains("active")) {
            require(tag, "active", Tag.TAG_COMPOUND);
            var saved = tag.getCompound("active");
            String recipe = itemId(text(saved, "recipe"));
            String output = itemId(text(saved, "output"));
            if (!furnace.resultOf(recipe).filter(output::equals).isPresent()) {
                throw new IllegalArgumentException("Bio-Furnace active output is not the smelting result");
            }
            active = new BatchWork.ActiveBatch(number(saved, "id"), recipe, number(saved, "reservation"),
                    integer(saved, "work"), integer(saved, "required"));
        }
        var store = new QuantityStore.Snapshot(number(tag, "storeRevision"), number(tag, "nextReservation"),
                items, tanks, reservations);
        var state = new BatchWork.State(BatchWork.SNAPSHOT_SCHEMA, number(tag, "revision"), number(tag, "nextBatch"),
                active, store, history);
        BatchWork.restore(state, BioFurnaceRecipes.catalog(furnace::resultOf), furnace::admit);
        return state;
    }

    static int experience(CompoundTag tag) {
        // Earlier schema-1 saves predate stored experience.
        if (!tag.contains("experience")) return 0;
        int experience = integer(tag, "experience");
        if (experience < 0 || experience > EXPERIENCE_CAPACITY) {
            throw new IllegalArgumentException("Invalid Bio-Furnace experience");
        }
        return experience;
    }

    private static QuantityStore.ItemSlot itemStack(CompoundTag tag) {
        String resource = text(tag, "resource");
        int count = integer(tag, "count");
        int capacity = integer(tag, "capacity");
        if (count > 0) {
            var item = BioFurnaceResources.item(itemId(resource));
            if (capacity > item.getDefaultMaxStackSize()) throw new IllegalArgumentException("Bio-Furnace stack exceeds item limit");
        }
        return new QuantityStore.ItemSlot(resource, count, capacity);
    }

    private static QuantityStore.Tank tank(CompoundTag tag) {
        String resource = text(tag, "resource");
        int amount = integer(tag, "count");
        return new QuantityStore.Tank(resource, amount, integer(tag, "capacity"));
    }

    private static QuantityStore.ReservationSnapshot reservation(CompoundTag tag) {
        // Smelting consumes biomass and never reserves fluid output.
        require(tag, "fluidOutputs", Tag.TAG_LIST);
        if (!((ListTag) tag.get("fluidOutputs")).isEmpty()) {
            throw new IllegalArgumentException("Bio-Furnace cannot produce fluid");
        }
        return new QuantityStore.ReservationSnapshot(number(tag, "id"),
                itemAllocations(tag, "inputs"), fluidAllocations(tag, "fluids"), itemAllocations(tag, "outputs"),
                itemAllocations(tag, "returns"), fluidAllocations(tag, "fluidOutputs"));
    }

    private static List<QuantityStore.ItemAllocation> itemAllocations(CompoundTag tag, String key) {
        return readList(tag, key, ITEM_SLOTS, allocation -> new QuantityStore.ItemAllocation(integer(allocation, "index"),
                itemId(text(allocation, "resource")), integer(allocation, "count")));
    }

    private static List<QuantityStore.FluidAllocation> fluidAllocations(CompoundTag tag, String key) {
        return readList(tag, key, 1, allocation -> new QuantityStore.FluidAllocation(integer(allocation, "index"),
                biomass(text(allocation, "resource")), integer(allocation, "count")));
    }

    private static CompoundTag reservation(QuantityStore.ReservationSnapshot reservation) {
        var tag = new CompoundTag();
        tag.putLong("id", reservation.reservationId());
        tag.put("inputs", list(reservation.inputItems(), BioFurnaceSave::allocation));
        tag.put("fluids", list(reservation.inputFluids(), BioFurnaceSave::allocation));
        tag.put("outputs", list(reservation.itemOutputs(), BioFurnaceSave::allocation));
        tag.put("returns", list(reservation.returnedContainers(), BioFurnaceSave::allocation));
        tag.put("fluidOutputs", list(reservation.outputFluids(), BioFurnaceSave::allocation));
        return tag;
    }

    private static CompoundTag stack(String resource, int count, int capacity) {
        var tag = new CompoundTag();
        tag.putString("resource", resource);
        tag.putInt("count", count);
        tag.putInt("capacity", capacity);
        return tag;
    }

    private static CompoundTag allocation(QuantityStore.ItemAllocation allocation) {
        return allocation(allocation.index(), allocation.resource(), allocation.amount());
    }

    private static CompoundTag allocation(QuantityStore.FluidAllocation allocation) {
        return allocation(allocation.index(), allocation.resource(), allocation.amount());
    }

    private static CompoundTag allocation(int index, String resource, int count) {
        var tag = new CompoundTag();
        tag.putInt("index", index);
        tag.putString("resource", resource);
        tag.putInt("count", count);
        return tag;
    }

    private static <T> ListTag list(List<T> values, Function<T, CompoundTag> encode) {
        var list = new ListTag();
        values.forEach(value -> list.add(encode.apply(value)));
        return list;
    }

    private static <T> List<T> readList(CompoundTag tag, String key, int max, Function<CompoundTag, T> decode) {
        require(tag, key, Tag.TAG_LIST);
        var raw = (ListTag) tag.get(key);
        if (raw.size() > max || (!raw.isEmpty() && raw.getElementType() != Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Invalid Bio-Furnace list: " + key);
        }
        var values = new ArrayList<T>(raw.size());
        for (int index = 0; index < raw.size(); index++) values.add(decode.apply(raw.getCompound(index)));
        return List.copyOf(values);
    }

    private static String itemId(String value) {
        BioFurnaceResources.item(value);
        return value;
    }

    private static String biomass(String value) {
        if (!BioFurnaceRecipes.BIOMASS.equals(value)) throw new IllegalArgumentException("Unknown Bio-Furnace fluid");
        return value;
    }

    private static String text(CompoundTag tag, String key) {
        require(tag, key, Tag.TAG_STRING);
        String value = tag.getString(key);
        if (value.length() > 64) throw new IllegalArgumentException("Bio-Furnace identifier exceeds 64 characters");
        return value;
    }

    private static int integer(CompoundTag tag, String key) { require(tag, key, Tag.TAG_INT); return tag.getInt(key); }
    private static long number(CompoundTag tag, String key) { require(tag, key, Tag.TAG_LONG); return tag.getLong(key); }
    private static void require(CompoundTag tag, String key, int type) {
        if (!tag.contains(key, type)) throw new IllegalArgumentException("Missing Bio-Furnace field: " + key);
    }

    private BioFurnaceSave() {}
}
