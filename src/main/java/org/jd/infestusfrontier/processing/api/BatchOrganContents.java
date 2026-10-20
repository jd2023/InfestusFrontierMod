package org.jd.infestusfrontier.processing.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.organ.api.OrganCodecs;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;

/**
 * The saved and carried form of a batch organ (Bowl, Rack, Loom, Cradle, Bio-Furnace): what it holds, the batch it is
 * running, the last player who loaded it by hand, whom its batches are credited to, and the smelting experience it
 * keeps for whoever takes its output. A value: the stacks are copied in and copied out, and two contents are equal
 * when every stack matches item for item.
 */
public record BatchOrganContents(
        List<ItemStack> ingredients,
        ItemStack output,
        int waterMb,
        BiomassTank biomass,
        OrganHistory history,
        Optional<OrganBatch> batch,
        Optional<UUID> feeder,
        float experience) {
    public BatchOrganContents {
        ingredients = ingredients.stream().map(ItemStack::copy).toList();
        output = output.copy();
        if (!(experience >= 0 && experience <= MAX_EXPERIENCE))
            throw new IllegalArgumentException("experience " + experience);
    }

    /** Contents nobody has fed by hand and that keep no experience. */
    public BatchOrganContents(
            List<ItemStack> ingredients,
            ItemStack output,
            int waterMb,
            BiomassTank biomass,
            OrganHistory history,
            Optional<OrganBatch> batch) {
        this(ingredients, output, waterMb, biomass, history, batch, Optional.empty(), 0);
    }

    @Override
    public List<ItemStack> ingredients() {
        return ingredients.stream().map(ItemStack::copy).toList();
    }

    @Override
    public ItemStack output() {
        return output.copy();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BatchOrganContents that
                && ItemStack.listMatches(ingredients, that.ingredients)
                && ItemStack.matches(output, that.output)
                && waterMb == that.waterMb
                && biomass.equals(that.biomass)
                && history.equals(that.history)
                && batch.equals(that.batch)
                && feeder.equals(that.feeder)
                && experience == that.experience;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                ItemStack.hashStackList(ingredients),
                ItemStack.hashItemAndComponents(output),
                waterMb,
                biomass,
                history,
                batch,
                feeder,
                experience);
    }

    /** The most smelting experience an organ keeps for whoever takes its output; what a batch earns beyond it is lost. */
    public static final float MAX_EXPERIENCE = 1000;
    /** The most water any batch organ holds; a kind may hold less. */
    public static final int MAX_WATER = 2000;
    /** The most ingredient slots any batch organ has; a kind may have fewer. */
    public static final int MAX_SLOTS = 9;

    public static final Codec<BatchOrganContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ItemStack.OPTIONAL_CODEC
                            .listOf(0, MAX_SLOTS)
                            .fieldOf("ingredients")
                            .forGetter(BatchOrganContents::ingredients),
                    ItemStack.OPTIONAL_CODEC.fieldOf("output").forGetter(BatchOrganContents::output),
                    Codec.intRange(0, MAX_WATER).fieldOf("water").forGetter(BatchOrganContents::waterMb),
                    BiomassCodecs.TANK.fieldOf("biomass").forGetter(BatchOrganContents::biomass),
                    OrganCodecs.HISTORY.fieldOf("history").forGetter(BatchOrganContents::history),
                    OrganBatch.CODEC.optionalFieldOf("batch").forGetter(BatchOrganContents::batch),
                    UUIDUtil.CODEC.optionalFieldOf("feeder").forGetter(BatchOrganContents::feeder),
                    Codec.floatRange(0, MAX_EXPERIENCE)
                            .optionalFieldOf("experience", 0f)
                            .forGetter(BatchOrganContents::experience))
            .apply(instance, BatchOrganContents::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, BatchOrganContents> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);
}
