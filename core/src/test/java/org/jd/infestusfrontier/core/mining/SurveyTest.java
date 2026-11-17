package org.jd.infestusfrontier.core.mining;

import static org.junit.jupiter.api.Assertions.*;

import org.jd.infestusfrontier.core.foundation.Outcome;
import org.junit.jupiter.api.Test;

class SurveyTest {
    private static Survey done(Outcome<Survey> outcome) {
        return ((Outcome.Done<Survey>) outcome).value();
    }

    @Test
    void aDraftIsEditedThenApprovedAndThenNoLongerChanges() {
        Survey draft = done(Survey.START.edit(PlanEdit.LONGER, false));
        assertEquals(9, draft.plan().length());
        assertFalse(draft.approved());
        Survey approved = done(draft.approve());
        assertTrue(approved.approved());
        assertEquals(new Outcome.Refused<Survey>(Surveying.APPROVED), approved.edit(PlanEdit.LONGER, false));
        assertEquals(new Outcome.Refused<Survey>(Surveying.APPROVED), approved.approve());
    }

    @Test
    void stepsAreBuiltInOrderUntilThePlanIsFinished() {
        Survey survey =
                done(new Survey(new MinePlan(PlanShape.STAIRCASE, 2, Turn.NONE, Turn.NONE, false), false, 0).approve());
        assertThrows(IllegalStateException.class, () -> Survey.START.next(), "a draft is not built");
        assertEquals(1, survey.next().number());
        survey = survey.grown();
        assertEquals(2, survey.next().number());
        survey = survey.grown().grown();
        assertTrue(survey.finished(), "two treads and the landing");
        assertThrows(IllegalStateException.class, survey::next);
        assertThrows(IllegalStateException.class, survey::grown);
    }

    @Test
    void impossibleSavedNumbersDoNotConstruct() {
        MinePlan plan = new MinePlan(PlanShape.ROUTE, 4, Turn.NONE, Turn.NONE, false);
        assertThrows(IllegalArgumentException.class, () -> new Survey(plan, true, 5));
        assertThrows(IllegalArgumentException.class, () -> new Survey(plan, true, -1));
        assertThrows(IllegalArgumentException.class, () -> new Survey(plan, false, 1), "nothing built on a draft");
        assertDoesNotThrow(() -> new Survey(plan, true, 4));
    }
}
