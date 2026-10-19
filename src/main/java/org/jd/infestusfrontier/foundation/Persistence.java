package org.jd.infestusfrontier.foundation;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.slf4j.Logger;

/**
 * Saved block entity state. Unreadable bytes are never replaced: the owner keeps the original tag, saves it back
 * unchanged and refuses every command with {@link #CORRUPT}.
 */
public final class Persistence {
    public static final Refusal CORRUPT = new Refusal("organ.unreadable_save");

    private Persistence() {}

    /**
     * The decoded state, or empty when {@code tag} cannot be read. The caller then keeps {@code tag}. {@code registries}
     * is the provider the block entity's load gave; item stacks with registry-bound components need it.
     */
    public static <T> Optional<T> read(
            MapCodec<T> codec, CompoundTag tag, HolderLookup.Provider registries, Logger log, String owner) {
        return read(codec, tag, registries, log, owner, value -> Optional.empty());
    }

    /**
     * As {@link #read(MapCodec, CompoundTag, HolderLookup.Provider, Logger, String)}, then {@code check} judges the
     * decoded state as a whole; a reason it returns makes the state unreadable like a decode error. A codec that
     * throws on hostile numbers is a decode error too, never a crash.
     */
    public static <T> Optional<T> read(
            MapCodec<T> codec,
            CompoundTag tag,
            HolderLookup.Provider registries,
            Logger log,
            String owner,
            Function<T, Optional<String>> check) {
        DataResult<T> result;
        try {
            result = codec.codec().parse(ops(registries), tag);
        } catch (RuntimeException e) {
            result = DataResult.error(e::toString);
        }
        Optional<T> value = result.result();
        Optional<String> problem = result.error().map(DataResult.Error::message).or(() -> value.flatMap(check));
        problem.ifPresent(reason -> log.warn("{} keeps unreadable saved data: {}", owner, reason));
        return problem.isPresent() ? Optional.empty() : value;
    }

    /** {@code value} as a tag of its own, for keeping state that failed its check without losing it. */
    public static <T> CompoundTag encode(MapCodec<T> codec, T value, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        write(codec, value, registries, tag);
        return tag;
    }

    public static <T> void write(MapCodec<T> codec, T value, HolderLookup.Provider registries, CompoundTag into) {
        Tag encoded = codec.codec().encodeStart(ops(registries), value).getOrThrow();
        into.merge((CompoundTag) encoded);
    }

    private static DynamicOps<Tag> ops(HolderLookup.Provider registries) {
        return registries.createSerializationContext(NbtOps.INSTANCE);
    }
}
