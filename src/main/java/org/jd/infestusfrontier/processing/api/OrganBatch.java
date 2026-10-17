package org.jd.infestusfrontier.processing.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.core.processing.Batch;

/**
 * A running organ batch: the progress, the result promised to the output slot and who started it. The promised
 * result is saved so a recipe that disappears cannot lose a batch whose inputs are already consumed.
 */
public record OrganBatch(Batch batch, ItemStack result, Optional<UUID> starter) {
    private static final Codec<Batch> BATCH_CODEC = RecordCodecBuilder.<RawBatch>create(instance -> instance.group(
                            Codec.STRING.fieldOf("recipe").forGetter(RawBatch::recipe),
                            Codec.INT.fieldOf("ticks_required").forGetter(RawBatch::ticksRequired),
                            Codec.INT.fieldOf("ticks_done").forGetter(RawBatch::ticksDone))
                    .apply(instance, RawBatch::new))
            .flatXmap(RawBatch::toBatch, batch -> DataResult.success(RawBatch.of(batch)));
    public static final Codec<OrganBatch> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    BATCH_CODEC.fieldOf("batch").forGetter(OrganBatch::batch),
                    ItemStack.CODEC.fieldOf("result").forGetter(OrganBatch::result),
                    UUIDUtil.CODEC.optionalFieldOf("starter").forGetter(OrganBatch::starter))
            .apply(instance, OrganBatch::new));

    public OrganBatch advance() {
        return new OrganBatch(batch.advance(), result, starter);
    }

    public boolean isDone() {
        return batch.isDone();
    }

    private record RawBatch(String recipe, int ticksRequired, int ticksDone) {
        static RawBatch of(Batch batch) {
            return new RawBatch(batch.recipe(), batch.ticksRequired(), batch.ticksDone());
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
