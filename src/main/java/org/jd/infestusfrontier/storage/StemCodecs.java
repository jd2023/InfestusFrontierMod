package org.jd.infestusfrontier.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import org.jd.infestusfrontier.core.storage.StemPlan;

/** How a Capsule Stem's plan is saved: its seed, then its cells and pods as offsets from the root in growth order. */
final class StemCodecs {
    /** A plan grows at most 7 + 1 + 6 cells; pressed pods may pass {@link StemPlan#MAX_PODS}, never 3 a trunk cell. */
    static final int MOST_CELLS = 2 * StemPlan.MAX_HEIGHT + 2;

    static final int MOST_PODS = 3 * MOST_CELLS;

    private StemCodecs() {}

    private static final Codec<StemPlan.Part> PART = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(StemPlan.Part.valueOf(name.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "no stem part " + name);
                }
            },
            part -> part.name().toLowerCase(Locale.ROOT));

    private static final Codec<StemPlan.Cell> CELL = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("x").forGetter(StemPlan.Cell::x),
                    Codec.INT.fieldOf("y").forGetter(StemPlan.Cell::y),
                    Codec.INT.fieldOf("z").forGetter(StemPlan.Cell::z),
                    PART.fieldOf("part").forGetter(StemPlan.Cell::part),
                    Codec.INT.fieldOf("side").forGetter(StemPlan.Cell::side),
                    Codec.INT.fieldOf("parent").forGetter(StemPlan.Cell::parent),
                    Codec.INT.fieldOf("trunk").forGetter(StemPlan.Cell::trunk),
                    Codec.INT.fieldOf("tier").forGetter(StemPlan.Cell::tier))
            .apply(instance, StemPlan.Cell::new));

    private static final Codec<StemPlan.Pod> POD = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("x").forGetter(StemPlan.Pod::x),
                    Codec.INT.fieldOf("y").forGetter(StemPlan.Pod::y),
                    Codec.INT.fieldOf("z").forGetter(StemPlan.Pod::z),
                    Codec.INT.fieldOf("side").forGetter(StemPlan.Pod::side),
                    Codec.INT.fieldOf("cell").forGetter(StemPlan.Pod::cell))
            .apply(instance, StemPlan.Pod::new));

    private record Raw(long seed, List<StemPlan.Cell> cells, List<StemPlan.Pod> pods) {}

    private static final Codec<Raw> RAW = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.fieldOf("seed").forGetter(Raw::seed),
                    CELL.listOf(1, MOST_CELLS).fieldOf("cells").forGetter(Raw::cells),
                    POD.listOf(0, MOST_PODS).optionalFieldOf("pods", List.of()).forGetter(Raw::pods))
            .apply(instance, Raw::new));

    /** A plan that breaks its own rules, as a cell grown from a later one, does not decode. */
    static final Codec<StemPlan> PLAN = RAW.comapFlatMap(
            raw -> {
                try {
                    return DataResult.success(new StemPlan(raw.seed(), raw.cells(), raw.pods()));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(e::getMessage);
                }
            },
            plan -> new Raw(plan.seed(), plan.cells(), plan.pods()));
}
