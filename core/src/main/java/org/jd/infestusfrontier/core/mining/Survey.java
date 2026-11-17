package org.jd.infestusfrontier.core.mining;

import org.jd.infestusfrontier.core.foundation.Outcome;

/**
 * A marker's plan and how far it is built: a draft the player edits, then, once approved, a fixed plan whose steps are
 * built in order. {@code built} counts the finished steps. A value outside the limits does not construct.
 */
public record Survey(MinePlan plan, boolean approved, int built) {
    public static final Survey START = new Survey(MinePlan.START, false, 0);

    public Survey {
        if (built < 0 || built > plan.steps().size()) throw new IllegalArgumentException("Built: " + built);
        if (built > 0 && !approved) throw new IllegalArgumentException("Built on a draft");
    }

    /** The draft changed as asked, or a refusal while the plan is approved. */
    public Outcome<Survey> edit(PlanEdit edit, boolean far) {
        if (approved) return Outcome.refused(Surveying.APPROVED);
        return Outcome.done(new Survey(plan.edit(edit, far), false, 0));
    }

    /** The draft approved, or a refusal when it already is. Whether it fits the world is the marker's to judge. */
    public Outcome<Survey> approve() {
        if (approved) return Outcome.refused(Surveying.APPROVED);
        return Outcome.done(new Survey(plan, true, 0));
    }

    /** True when every step of an approved plan is built. */
    public boolean finished() {
        return approved && built == plan.steps().size();
    }

    /** The next step to build. Only for an approved plan that is not finished. */
    public PlanStep next() {
        if (!approved || finished()) throw new IllegalStateException("No step to build");
        return plan.steps().get(built);
    }

    /** One more step built. */
    public Survey grown() {
        if (!approved || finished()) throw new IllegalStateException("No step to build");
        return new Survey(plan, true, built + 1);
    }
}
