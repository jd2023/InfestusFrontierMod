package org.jd.infestusfrontier.storage.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.storage.BiomassTank;

/** Serialization of biomass values. A tank over capacity is a decode error, never an exception. */
public final class BiomassCodecs {
    public static final Codec<Biomass> BIOMASS =
            Codec.intRange(0, Integer.MAX_VALUE).xmap(Biomass::of, Biomass::units);
    public static final Codec<BiomassTank> TANK = RecordCodecBuilder.<RawTank>create(instance -> instance.group(
                            BIOMASS.fieldOf("stored").forGetter(RawTank::stored),
                            BIOMASS.optionalFieldOf("reserved", Biomass.ZERO).forGetter(RawTank::reserved),
                            BIOMASS.fieldOf("capacity").forGetter(RawTank::capacity))
                    .apply(instance, RawTank::new))
            .flatXmap(RawTank::toTank, tank -> DataResult.success(RawTank.of(tank)));
    public static final StreamCodec<ByteBuf, BiomassTank> TANK_STREAM = ByteBufCodecs.fromCodec(TANK);

    private BiomassCodecs() {}

    private record RawTank(Biomass stored, Biomass reserved, Biomass capacity) {
        static RawTank of(BiomassTank tank) {
            return new RawTank(tank.stored(), tank.reserved(), tank.capacity());
        }

        DataResult<BiomassTank> toTank() {
            if ((long) stored.units() + reserved.units() > capacity.units())
                return DataResult.error(() -> "Biomass tank over capacity");
            return DataResult.success(new BiomassTank(stored, reserved, capacity));
        }
    }
}
