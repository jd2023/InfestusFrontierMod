package org.jd.infestusfrontier.core.genetics;

/**
 * The sample a DNA Bank is extracting: its species, the points it will add, the loaded ticks it takes and the ticks
 * done. The sample and its biomass were paid when it started and its numbers were fixed then, so neither a reload of
 * the recipes nor a restart changes what it adds. A value outside the limits does not construct.
 */
public record Extraction(String species, int points, int ticks, int elapsed) {
    public Extraction {
        if (species.isBlank()) throw new IllegalArgumentException("Blank species");
        if (points < 1 || points > Genetics.COMPLETE) throw new IllegalArgumentException("Points: " + points);
        if (ticks < 1 || ticks > Genetics.MAX_EXTRACTION_TICKS) throw new IllegalArgumentException("Ticks: " + ticks);
        if (elapsed < 0 || elapsed > ticks) throw new IllegalArgumentException("Elapsed: " + elapsed);
    }

    /** A new extraction with no tick done yet. */
    public static Extraction start(String species, int points, int ticks) {
        return new Extraction(species, points, ticks, 0);
    }

    /** True when the extraction has had all its ticks and its points may be added. */
    public boolean done() {
        return elapsed == ticks;
    }

    /** One more loaded tick, never past the end. */
    public Extraction waited() {
        return done() ? this : new Extraction(species, points, ticks, elapsed + 1);
    }

    public int ticksLeft() {
        return ticks - elapsed;
    }
}
