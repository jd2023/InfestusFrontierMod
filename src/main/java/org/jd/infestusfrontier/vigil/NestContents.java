package org.jd.infestusfrontier.vigil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.core.vigil.Territory;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;

record NestContents(BiomassTank tank, int mask, int paid, boolean enabled, boolean egg, Optional<UUID> keeper) {
    static final int CAPACITY = 16000;
    static final NestContents EMPTY = new NestContents(
            BiomassTank.empty(Biomass.of(CAPACITY)), Territory.CENTRE, 0, false, false, Optional.empty());
    static final MapCodec<NestContents> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    BiomassCodecs.TANK.fieldOf("tank").forGetter(NestContents::tank),
                    Codec.intRange(1, Territory.ALL).fieldOf("mask").forGetter(NestContents::mask),
                    Codec.intRange(0, Territory.PAYMENT_TICKS).fieldOf("paid").forGetter(NestContents::paid),
                    Codec.BOOL.fieldOf("enabled").forGetter(NestContents::enabled),
                    Codec.BOOL.fieldOf("egg").forGetter(NestContents::egg),
                    UUIDUtil.CODEC.optionalFieldOf("keeper").forGetter(NestContents::keeper))
            .apply(i, NestContents::new));

    Optional<String> problem() {
        if ((mask & Territory.CENTRE) == 0
                || tank.capacity().units() != CAPACITY
                || !tank.reserved().isZero()) return Optional.of("invalid nest stores or territory");
        if (enabled != keeper.isPresent() || keeper.isPresent() && !egg || !enabled && paid != 0)
            return Optional.of("keeper without active nest");
        return Optional.empty();
    }
}
