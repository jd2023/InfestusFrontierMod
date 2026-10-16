package org.jd.infestusfrontier.core.processing;

/** One reserved unit of work. Saved progress in ticks, never wall time, drives recovery. */
public record Batch(String recipe, int ticksRequired, int ticksDone) {
    public Batch {
        if (recipe.isBlank()) throw new IllegalArgumentException("Blank recipe");
        if (ticksRequired < 1) throw new IllegalArgumentException("A batch needs at least one tick");
        if (ticksDone < 0 || ticksDone > ticksRequired) throw new IllegalArgumentException("Progress out of range");
    }

    public static Batch start(String recipe, int ticksRequired) {
        return new Batch(recipe, ticksRequired, 0);
    }

    /** One loaded tick of work. A finished batch stays finished. */
    public Batch advance() {
        return isDone() ? this : new Batch(recipe, ticksRequired, ticksDone + 1);
    }

    public boolean isDone() {
        return ticksDone == ticksRequired;
    }

    /** Progress in whole percent, 0 to 100. */
    public int percent() {
        return (int) (100L * ticksDone / ticksRequired);
    }
}
