package org.jd.infestusfrontier.mining;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import org.jd.infestusfrontier.core.mining.MinePlan;
import org.jd.infestusfrontier.core.mining.PlanShape;
import org.jd.infestusfrontier.core.mining.Steer;
import org.jd.infestusfrontier.core.mining.Survey;
import org.jd.infestusfrontier.core.mining.Turn;

/**
 * How a Surveyed Tissue marker saves its {@link Survey}: the plan's numbers, whether it is approved and how many of its
 * steps are built. Impossible numbers are a decode error, never an exception, so the marker keeps them unread. A
 * marker saved before routes could be wide reads as one column wide; one saved when lights were a choice keeps its plan
 * and is lit, as every plan now is.
 */
final class SurveyContents {
    private record Raw(
            int shape,
            int length,
            int firstWay,
            int firstAt,
            int secondWay,
            int secondAt,
            boolean approved,
            int built,
            boolean wide) {}

    static final MapCodec<Survey> CODEC = RecordCodecBuilder.<Raw>mapCodec(instance -> instance.group(
                            Codec.INT.fieldOf("shape").forGetter(Raw::shape),
                            Codec.INT.fieldOf("length").forGetter(Raw::length),
                            Codec.INT.fieldOf("first_way").forGetter(Raw::firstWay),
                            Codec.INT.fieldOf("first_at").forGetter(Raw::firstAt),
                            Codec.INT.fieldOf("second_way").forGetter(Raw::secondWay),
                            Codec.INT.fieldOf("second_at").forGetter(Raw::secondAt),
                            Codec.BOOL.fieldOf("approved").forGetter(Raw::approved),
                            Codec.INT.fieldOf("built").forGetter(Raw::built),
                            Codec.BOOL.optionalFieldOf("wide", false).forGetter(Raw::wide))
                    .apply(instance, Raw::new))
            .flatXmap(SurveyContents::read, survey -> DataResult.success(raw(survey)));

    private SurveyContents() {}

    private static Raw raw(Survey survey) {
        MinePlan plan = survey.plan();
        return new Raw(
                plan.shape().ordinal(),
                plan.length(),
                plan.first().way().ordinal(),
                plan.first().at(),
                plan.second().way().ordinal(),
                plan.second().at(),
                survey.approved(),
                survey.built(),
                plan.wide());
    }

    private static DataResult<Survey> read(Raw raw) {
        Optional<PlanShape> shape = PlanShape.at(raw.shape());
        Optional<Steer> first = Steer.at(raw.firstWay());
        Optional<Steer> second = Steer.at(raw.secondWay());
        if (shape.isEmpty() || first.isEmpty() || second.isEmpty()) return DataResult.error(() -> "plan " + raw);
        try {
            return DataResult.success(new Survey(
                    new MinePlan(
                            shape.get(),
                            raw.length(),
                            new Turn(first.get(), raw.firstAt()),
                            new Turn(second.get(), raw.secondAt()),
                            raw.wide()),
                    raw.approved(),
                    raw.built()));
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "plan " + raw + ": " + e.getMessage());
        }
    }
}
