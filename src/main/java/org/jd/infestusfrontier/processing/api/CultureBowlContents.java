package org.jd.infestusfrontier.processing.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.organ.api.OrganCodecs;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;

/**
 * The saved and carried form of a Culture Bowl, including the batch it is running. A value: the stacks are copied
 * in and copied out, and two contents are equal when every stack matches item for item.
 */
public record CultureBowlContents(
        List<ItemStack> ingredients,
        ItemStack output,
        int waterMb,
        BiomassTank biomass,
        OrganHistory history,
        Optional<OrganBatch> batch) {
    public CultureBowlContents {
        ingredients = List.copyOf(ingredients.stream().map(ItemStack::copy).toList());
        output = output.copy();
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
        return other instanceof CultureBowlContents that
                && ItemStack.listMatches(ingredients, that.ingredients)
                && ItemStack.matches(output, that.output)
                && waterMb == that.waterMb
                && biomass.equals(that.biomass)
                && history.equals(that.history)
                && batch.equals(that.batch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                ItemStack.hashStackList(ingredients),
                ItemStack.hashItemAndComponents(output),
                waterMb,
                biomass,
                history,
                batch);
    }

    public static final int WATER_CAPACITY = 2000;
    public static final int INGREDIENT_SLOTS = 9;
    public static final Codec<CultureBowlContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ItemStack.OPTIONAL_CODEC
                            .listOf(0, INGREDIENT_SLOTS)
                            .fieldOf("ingredients")
                            .forGetter(CultureBowlContents::ingredients),
                    ItemStack.OPTIONAL_CODEC.fieldOf("output").forGetter(CultureBowlContents::output),
                    Codec.intRange(0, WATER_CAPACITY).fieldOf("water").forGetter(CultureBowlContents::waterMb),
                    BiomassCodecs.TANK.fieldOf("biomass").forGetter(CultureBowlContents::biomass),
                    OrganCodecs.HISTORY.fieldOf("history").forGetter(CultureBowlContents::history),
                    OrganBatch.CODEC.optionalFieldOf("batch").forGetter(CultureBowlContents::batch))
            .apply(instance, CultureBowlContents::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CultureBowlContents> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);
}
