package org.jd.infestusfrontier.mining;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.mining.Collecting;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;

/**
 * Everything Collection Cilia own: their biomass and their history, the items they have sent into the network. They
 * hold no items: what they sweep goes straight from the bed to a far store. Saved on the block entity and carried on
 * the item when the cilia are broken.
 */
record CollectionCiliaContents(BiomassTank tank, int sent) {
    static final CollectionCiliaContents EMPTY =
            new CollectionCiliaContents(BiomassTank.empty(Biomass.of(Collecting.BIOMASS_CAPACITY_BU)), 0);

    private static final MapCodec<CollectionCiliaContents> RAW = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BiomassCodecs.TANK.fieldOf("tank").forGetter(CollectionCiliaContents::tank),
                    Codec.INT.fieldOf("sent").forGetter(CollectionCiliaContents::sent))
            .apply(instance, CollectionCiliaContents::new));

    /** Impossible saved contents are a decode error, never an exception. */
    static final MapCodec<CollectionCiliaContents> CODEC = RAW.flatXmap(
            contents -> contents.problem()
                    .<DataResult<CollectionCiliaContents>>map(problem -> DataResult.error(() -> problem))
                    .orElseGet(() -> DataResult.success(contents)),
            DataResult::success);

    /** Why these contents cannot be the cilia's, or empty when they can. */
    Optional<String> problem() {
        if (tank.capacity().units() != Collecting.BIOMASS_CAPACITY_BU
                || !tank.reserved().isZero()) return Optional.of("tank " + tank);
        if (sent < 0) return Optional.of("sent " + sent);
        return Optional.empty();
    }
}
