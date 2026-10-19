package org.jd.infestusfrontier.processing.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jd.infestusfrontier.core.processing.Batch;

/** Serialization of batch progress, shared by every organ. Progress out of range is a decode error. */
public final class Batches {
    public static final Codec<Batch> CODEC = RecordCodecBuilder.<Raw>create(instance -> instance.group(
                            Codec.STRING.fieldOf("recipe").forGetter(Raw::recipe),
                            Codec.INT.fieldOf("ticks_required").forGetter(Raw::ticksRequired),
                            Codec.INT.fieldOf("ticks_done").forGetter(Raw::ticksDone))
                    .apply(instance, Raw::new))
            .flatXmap(Raw::toBatch, batch -> DataResult.success(Raw.of(batch)));

    private Batches() {}

    private record Raw(String recipe, int ticksRequired, int ticksDone) {
        static Raw of(Batch batch) {
            return new Raw(batch.recipe(), batch.ticksRequired(), batch.ticksDone());
        }

        DataResult<Batch> toBatch() {
            try {
                return DataResult.success(new Batch(recipe, ticksRequired, ticksDone));
            } catch (IllegalArgumentException e) {
                return DataResult.error(e::getMessage);
            }
        }
    }
}
