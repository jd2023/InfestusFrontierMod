package org.jd.infestusfrontier.mining;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.mining.Fracturing;
import org.jd.infestusfrontier.core.mining.JawProgress;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;

/**
 * Everything a Fracture Jaw owns: its biomass and its progress, whose cuts are its history. Saved on the block entity
 * and carried on the item when the jaw is broken.
 */
record FractureJawContents(BiomassTank tank, JawProgress progress) {
    static final FractureJawContents EMPTY =
            new FractureJawContents(BiomassTank.empty(Biomass.of(Fracturing.BIOMASS_CAPACITY_BU)), JawProgress.START);

    /** The saved numbers before they are judged; impossible ones are a decode error, never an exception. */
    private record Raw(BiomassTank tank, int cuts, int ticks) {}

    static final MapCodec<FractureJawContents> CODEC = RecordCodecBuilder.<Raw>mapCodec(instance -> instance.group(
                            BiomassCodecs.TANK.fieldOf("tank").forGetter(Raw::tank),
                            Codec.INT.fieldOf("cuts").forGetter(Raw::cuts),
                            Codec.INT.fieldOf("ticks").forGetter(Raw::ticks))
                    .apply(instance, Raw::new))
            .flatXmap(FractureJawContents::read, contents -> DataResult.success(contents.raw()));

    private Raw raw() {
        return new Raw(tank, progress.cuts(), progress.ticks());
    }

    private static DataResult<FractureJawContents> read(Raw raw) {
        if (raw.cuts() < 0) return DataResult.error(() -> "cuts " + raw.cuts());
        if (raw.ticks() < 0 || raw.ticks() > Fracturing.CUT_TICKS)
            return DataResult.error(() -> "ticks " + raw.ticks());
        FractureJawContents contents = new FractureJawContents(raw.tank(), new JawProgress(raw.cuts(), raw.ticks()));
        Optional<String> problem = contents.problem();
        return problem.isPresent() ? DataResult.error(problem::get) : DataResult.success(contents);
    }

    /** Why these contents cannot be a jaw's, or empty when they can. */
    Optional<String> problem() {
        if (tank.capacity().units() != Fracturing.BIOMASS_CAPACITY_BU
                || !tank.reserved().isZero()) return Optional.of("tank " + tank);
        return Optional.empty();
    }
}
