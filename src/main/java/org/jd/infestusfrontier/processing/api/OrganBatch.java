package org.jd.infestusfrontier.processing.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.core.processing.Batch;

/**
 * A running organ batch: the progress, the results promised to the output slots, who started it and the smelting
 * experience it will earn. The promised results are saved so a recipe that disappears cannot lose a batch whose inputs
 * are already consumed. A value: the results are copied in and out and compared item for item. Result {@code n} goes
 * to output slot {@code slot + n}: the {@code result} first, then the {@code byproducts}.
 */
public record OrganBatch(
        Batch batch, ItemStack result, List<ItemStack> byproducts, int slot, Optional<UUID> starter, float experience)
        implements RunningBatch<OrganBatch> {
    public OrganBatch {
        result = result.copy();
        byproducts = byproducts.stream().map(ItemStack::copy).toList();
        if (slot < 0 || slot + 1 + byproducts.size() > BatchOrganContents.MAX_OUTPUTS)
            throw new IllegalArgumentException("results from slot " + slot);
        if (!(experience >= 0 && experience <= BatchOrganContents.MAX_EXPERIENCE))
            throw new IllegalArgumentException("experience " + experience);
    }

    /** A batch whose results start at the first output slot. */
    public OrganBatch(
            Batch batch, ItemStack result, List<ItemStack> byproducts, Optional<UUID> starter, float experience) {
        this(batch, result, byproducts, 0, starter, experience);
    }

    /** A batch with one result. */
    public OrganBatch(Batch batch, ItemStack result, Optional<UUID> starter, float experience) {
        this(batch, result, List.of(), starter, experience);
    }

    @Override
    public ItemStack result() {
        return result.copy();
    }

    @Override
    public List<ItemStack> byproducts() {
        return byproducts.stream().map(ItemStack::copy).toList();
    }

    /** Every result, in output slot order, the first for output slot {@link #slot()}. */
    public List<ItemStack> results() {
        List<ItemStack> all = new ArrayList<>();
        all.add(result());
        all.addAll(byproducts());
        return all;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OrganBatch that
                && batch.equals(that.batch)
                && ItemStack.matches(result, that.result)
                && ItemStack.listMatches(byproducts, that.byproducts)
                && slot == that.slot
                && starter.equals(that.starter)
                && experience == that.experience;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                batch,
                ItemStack.hashItemAndComponents(result),
                ItemStack.hashStackList(byproducts),
                slot,
                starter,
                experience);
    }

    public static final Codec<OrganBatch> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Batches.CODEC.fieldOf("batch").forGetter(OrganBatch::batch),
                    ItemStack.CODEC.fieldOf("result").forGetter(OrganBatch::result),
                    ItemStack.CODEC
                            .listOf(0, OrganBatchRecipe.MAX_BYPRODUCTS)
                            .optionalFieldOf("byproducts", List.of())
                            .forGetter(OrganBatch::byproducts),
                    Codec.intRange(0, BatchOrganContents.MAX_OUTPUTS - 1)
                            .optionalFieldOf("slot", 0)
                            .forGetter(OrganBatch::slot),
                    UUIDUtil.CODEC.optionalFieldOf("starter").forGetter(OrganBatch::starter),
                    Codec.floatRange(0, BatchOrganContents.MAX_EXPERIENCE)
                            .optionalFieldOf("experience", 0f)
                            .forGetter(OrganBatch::experience))
            .apply(instance, OrganBatch::new));

    @Override
    public Optional<ResourceLocation> recipe() {
        return Optional.ofNullable(ResourceLocation.tryParse(batch.recipe()));
    }

    @Override
    public OrganBatch advance() {
        return new OrganBatch(batch.advance(), result, byproducts, slot, starter, experience);
    }

    @Override
    public boolean isDone() {
        return batch.isDone();
    }
}
