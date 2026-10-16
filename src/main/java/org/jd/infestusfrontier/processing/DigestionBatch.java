package org.jd.infestusfrontier.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.processing.Batch;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;

/**
 * A running digestion: the batch, the biomass already promised to the tank, and who fed it. The promised yield is
 * saved so a recipe that disappears cannot lose reserved room.
 */
record DigestionBatch(Batch batch, Biomass yield, Optional<UUID> feeder) {
    private static final Codec<Batch> BATCH_CODEC = RecordCodecBuilder.<RawBatch>create(instance -> instance.group(
                            Codec.STRING.fieldOf("recipe").forGetter(RawBatch::recipe),
                            Codec.INT.fieldOf("ticks_required").forGetter(RawBatch::ticksRequired),
                            Codec.INT.fieldOf("ticks_done").forGetter(RawBatch::ticksDone))
                    .apply(instance, RawBatch::new))
            .flatXmap(RawBatch::toBatch, batch -> com.mojang.serialization.DataResult.success(RawBatch.of(batch)));
    static final Codec<DigestionBatch> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    BATCH_CODEC.fieldOf("batch").forGetter(DigestionBatch::batch),
                    BiomassCodecs.BIOMASS.fieldOf("yield").forGetter(DigestionBatch::yield),
                    UUIDUtil.CODEC.optionalFieldOf("feeder").forGetter(DigestionBatch::feeder))
            .apply(instance, DigestionBatch::new));
    static final StreamCodec<ByteBuf, DigestionBatch> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    DigestionBatch advance() {
        return new DigestionBatch(batch.advance(), yield, feeder);
    }

    boolean isDone() {
        return batch.isDone();
    }

    private record RawBatch(String recipe, int ticksRequired, int ticksDone) {
        static RawBatch of(Batch batch) {
            return new RawBatch(batch.recipe(), batch.ticksRequired(), batch.ticksDone());
        }

        com.mojang.serialization.DataResult<Batch> toBatch() {
            try {
                return com.mojang.serialization.DataResult.success(new Batch(recipe, ticksRequired, ticksDone));
            } catch (IllegalArgumentException e) {
                return com.mojang.serialization.DataResult.error(e::getMessage);
            }
        }
    }
}
