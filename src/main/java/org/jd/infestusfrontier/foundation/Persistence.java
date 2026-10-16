package org.jd.infestusfrontier.foundation;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
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

    /** The decoded state, or empty when {@code tag} cannot be read. The caller then keeps {@code tag}. */
    public static <T> Optional<T> read(MapCodec<T> codec, CompoundTag tag, Logger log, String owner) {
        DataResult<T> result = codec.codec().parse(NbtOps.INSTANCE, tag);
        result.error().ifPresent(error -> log.warn("{} keeps unreadable saved data: {}", owner, error.message()));
        return result.result();
    }

    public static <T> void write(MapCodec<T> codec, T value, CompoundTag into) {
        Tag encoded = codec.codec().encodeStart(NbtOps.INSTANCE, value).getOrThrow();
        into.merge((CompoundTag) encoded);
    }
}
