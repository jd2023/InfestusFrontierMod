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

record NestContents(
        BiomassTank tank,
        int mask,
        int paid,
        boolean enabled,
        boolean egg,
        Optional<UUID> keeper,
        float health,
        int hungry) {
    static final int CAPACITY = 16000;
    static final int GROWTH_COST = 8000;
    static final float FULL_HEALTH = 30;
    static final int HUNGER_TICKETS = 800;
    static final int DAMAGE_INTERVAL = 20;
    static final NestContents EMPTY = new NestContents(
            BiomassTank.empty(Biomass.of(CAPACITY)),
            Territory.CENTRE,
            0,
            false,
            false,
            Optional.empty(),
            FULL_HEALTH,
            0);
    static final MapCodec<NestContents> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    BiomassCodecs.TANK.fieldOf("tank").forGetter(NestContents::tank),
                    Codec.intRange(1, Territory.ALL).fieldOf("mask").forGetter(NestContents::mask),
                    Codec.intRange(0, Territory.PAYMENT_TICKS).fieldOf("paid").forGetter(NestContents::paid),
                    Codec.BOOL.fieldOf("enabled").forGetter(NestContents::enabled),
                    Codec.BOOL.fieldOf("egg").forGetter(NestContents::egg),
                    UUIDUtil.CODEC.optionalFieldOf("keeper").forGetter(NestContents::keeper),
                    Codec.floatRange(Float.MIN_VALUE, Float.MAX_VALUE)
                            .optionalFieldOf("health", FULL_HEALTH)
                            .forGetter(NestContents::health),
                    Codec.intRange(0, HUNGER_TICKETS)
                            .optionalFieldOf("hungry", 0)
                            .forGetter(NestContents::hungry))
            .apply(i, NestContents::new));

    // Health is authoritative only while sheltered. Awake health belongs to the real entity.
    NestContents withTank(BiomassTank next) {
        return new NestContents(next, mask, paid, enabled, egg, keeper, health, hungry);
    }

    Optional<String> problem() {
        if (!Float.isFinite(health) || health <= 0) return Optional.of("invalid keeper health");
        if ((mask & Territory.CENTRE) == 0
                || tank.capacity().units() != CAPACITY
                || !tank.reserved().isZero()) return Optional.of("invalid nest stores or territory");
        if (enabled != keeper.isPresent() || keeper.isPresent() && !egg || !enabled && paid != 0)
            return Optional.of("keeper without active nest");
        return Optional.empty();
    }
}
