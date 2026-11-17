package org.jd.infestusfrontier.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.jd.infestusfrontier.core.mining.Offset;
import org.jd.infestusfrontier.core.mining.PlanStep;
import org.jd.infestusfrontier.core.mining.Survey;

/**
 * What a marker's preview shows: the steps of its plan not built yet, as boxes relative to the marker. Each step is
 * its root cell, its tread, its clear passage and its light. Drawn from the plan's numbers alone; nothing here reads
 * the ground, so the preview shows the shape that will be cleared and built, never what lies inside it.
 */
final class SurveyPreview {
    /** The part of a step a box shows. */
    enum Part {
        ROOT,
        TREAD,
        PASSAGE,
        LIGHT
    }

    /** One box, relative to the marker's own block, and whether its plan is approved. */
    record Box(AABB box, Part part, boolean approved) {}

    /** A light is drawn as a small box in the middle of its block. */
    private static final double LIGHT_INSET = 0.35;

    private SurveyPreview() {}

    /** The boxes of {@code marker}'s plan still to build; none while it is unreadable or finished. */
    static List<Box> of(SurveyedTissueBlockEntity marker) {
        Optional<Survey> read = marker.survey();
        if (read.isEmpty()) return List.of();
        Survey survey = read.get();
        List<PlanStep> steps = survey.plan().steps();
        List<Box> boxes = new ArrayList<>();
        for (PlanStep step : steps.subList(survey.built(), steps.size())) {
            boxes.add(new Box(cell(marker, step.root()), Part.ROOT, survey.approved()));
            step.tread().ifPresent(tread -> boxes.add(new Box(cell(marker, tread), Part.TREAD, survey.approved())));
            AABB passage = cell(marker, step.passage().get(0))
                    .minmax(cell(marker, step.passage().get(step.passage().size() - 1)));
            boxes.add(new Box(passage, Part.PASSAGE, survey.approved()));
            step.light()
                    .ifPresent(light -> boxes.add(
                            new Box(cell(marker, light).deflate(LIGHT_INSET), Part.LIGHT, survey.approved())));
        }
        return boxes;
    }

    /** The block at {@code offset} of the plan, relative to the marker. */
    private static AABB cell(SurveyedTissueBlockEntity marker, Offset offset) {
        BlockPos at = marker.at(offset).subtract(marker.getBlockPos());
        return new AABB(at);
    }
}
