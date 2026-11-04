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
import org.jd.infestusfrontier.processing.api.Batches;
import org.jd.infestusfrontier.processing.api.RunningBatch;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;

/**
 * A running digestion: the batch, the biomass already promised to the tank, who is credited for it, and whether its
 * food came by pipe. The promised yield is saved so a recipe that disappears cannot lose reserved room.
 */
record DigestionBatch(Batch batch, Biomass yield, Optional<UUID> feeder, boolean automatic)
        implements RunningBatch<DigestionBatch> {
    static final Codec<DigestionBatch> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Batches.CODEC.fieldOf("batch").forGetter(DigestionBatch::batch),
                    BiomassCodecs.BIOMASS.fieldOf("yield").forGetter(DigestionBatch::yield),
                    UUIDUtil.CODEC.optionalFieldOf("feeder").forGetter(DigestionBatch::feeder),
                    Codec.BOOL.optionalFieldOf("automatic", false).forGetter(DigestionBatch::automatic))
            .apply(instance, DigestionBatch::new));
    static final StreamCodec<ByteBuf, DigestionBatch> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    @Override
    public DigestionBatch advance() {
        return new DigestionBatch(batch.advance(), yield, feeder, automatic);
    }

    @Override
    public boolean isDone() {
        return batch.isDone();
    }

    /** The player credited: who fed it by hand, or the Sac's steward for a batch taken from the slot. */
    @Override
    public Optional<UUID> starter() {
        return feeder;
    }
}
