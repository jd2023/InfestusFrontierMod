package org.jd.infestusfrontier.processing.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.core.processing.Batch;

/**
 * A running organ batch: the progress, the result promised to the output slot, who started it and the smelting
 * experience it will earn. The promised result is saved so a recipe that disappears cannot lose a batch whose inputs
 * are already consumed. A value: the result is copied in and out and compared item for item.
 */
public record OrganBatch(Batch batch, ItemStack result, Optional<UUID> starter, float experience)
        implements RunningBatch<OrganBatch> {
    public OrganBatch {
        result = result.copy();
        if (!(experience >= 0 && experience <= BatchOrganContents.MAX_EXPERIENCE))
            throw new IllegalArgumentException("experience " + experience);
    }

    @Override
    public ItemStack result() {
        return result.copy();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OrganBatch that
                && batch.equals(that.batch)
                && ItemStack.matches(result, that.result)
                && starter.equals(that.starter)
                && experience == that.experience;
    }

    @Override
    public int hashCode() {
        return Objects.hash(batch, ItemStack.hashItemAndComponents(result), starter, experience);
    }

    public static final Codec<OrganBatch> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Batches.CODEC.fieldOf("batch").forGetter(OrganBatch::batch),
                    ItemStack.CODEC.fieldOf("result").forGetter(OrganBatch::result),
                    UUIDUtil.CODEC.optionalFieldOf("starter").forGetter(OrganBatch::starter),
                    Codec.floatRange(0, BatchOrganContents.MAX_EXPERIENCE)
                            .optionalFieldOf("experience", 0f)
                            .forGetter(OrganBatch::experience))
            .apply(instance, OrganBatch::new));

    @Override
    public OrganBatch advance() {
        return new OrganBatch(batch.advance(), result, starter, experience);
    }

    @Override
    public boolean isDone() {
        return batch.isDone();
    }
}
