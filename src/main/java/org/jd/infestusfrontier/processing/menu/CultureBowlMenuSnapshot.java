package org.jd.infestusfrontier.processing.menu;

import java.util.List;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Complete immutable Bowl menu view. Its fixed shape encodes below 4 KiB. */
public record CultureBowlMenuSnapshot(
        long revision,
        List<Slot> slots,
        int water,
        int waterCapacity,
        State state,
        int completedWork,
        int requiredWork,
        long completedBatches,
        String selectedRecipe,
        BowlRefusal refusal) {
    public static final int MAX_ENCODED_BYTES = 4096;
    public static final StreamCodec<RegistryFriendlyByteBuf, CultureBowlMenuSnapshot> CODEC = new StreamCodec<>() {
        @Override public CultureBowlMenuSnapshot decode(RegistryFriendlyByteBuf buffer) {
            long revision = buffer.readVarLong();
            var slots = new java.util.ArrayList<Slot>(9);
            for (int i = 0; i < 9; i++) slots.add(new Slot(buffer.readUtf(64), buffer.readVarInt()));
            return new CultureBowlMenuSnapshot(revision, slots, buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readEnum(State.class), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarLong(),
                    buffer.readUtf(16), buffer.readEnum(BowlRefusal.class));
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, CultureBowlMenuSnapshot value) {
            buffer.writeVarLong(value.revision);
            for (var slot : value.slots) {
                buffer.writeUtf(slot.resource, 64);
                buffer.writeVarInt(slot.count);
            }
            buffer.writeVarInt(value.water);
            buffer.writeVarInt(value.waterCapacity);
            buffer.writeEnum(value.state);
            buffer.writeVarInt(value.completedWork);
            buffer.writeVarInt(value.requiredWork);
            buffer.writeVarLong(value.completedBatches);
            buffer.writeUtf(value.selectedRecipe, 16);
            buffer.writeEnum(value.refusal);
        }
    };

    public CultureBowlMenuSnapshot {
        if (revision < 0 || slots == null || slots.size() != 9 || water < 0 || waterCapacity < water
                || waterCapacity > 2000 || completedWork < 0 || requiredWork < completedWork
                || requiredWork > 1200 || completedBatches < 0 || selectedRecipe == null
                || selectedRecipe.length() > 16) throw new IllegalArgumentException("Invalid Bowl menu snapshot");
        slots = List.copyOf(slots);
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(refusal, "refusal");
    }

    public static CultureBowlMenuSnapshot empty() {
        return new CultureBowlMenuSnapshot(0, java.util.Collections.nCopies(9, new Slot("", 0)),
                0, 2000, State.IDLE, 0, 0, 0, "I000", BowlRefusal.NONE);
    }

    public record Slot(String resource, int count) {
        public Slot {
            if (resource == null || resource.length() > 64 || count < 0 || count > 64
                    || (resource.isEmpty() != (count == 0))) throw new IllegalArgumentException("Invalid Bowl menu slot");
        }
    }

    public enum State { IDLE, WORKING, COMPLETION_BLOCKED, INVALID_SAVE }
}
