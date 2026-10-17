package org.jd.infestusfrontier.processing.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.organ.api.OrganCodecs;
import org.jd.infestusfrontier.processing.OrganBatch;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;

/** The saved and carried form of a Culture Bowl, including the batch it is running. */
public record CultureBowlContents(
        List<ItemStack> ingredients,
        ItemStack output,
        int waterMb,
        BiomassTank biomass,
        OrganHistory history,
        Optional<OrganBatch> batch) {
    public static final int WATER_CAPACITY = 2000;
    public static final Codec<CultureBowlContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ItemStack.OPTIONAL_CODEC
                            .listOf()
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
