package org.jd.infestusfrontier.core.genetics;

/**
 * One species' research in a DNA Bank: the species id and the points its extracted samples have added, at most
 * {@value Genetics#COMPLETE}. A genome is only recorded once it has points. Points never go down.
 */
public record Genome(String species, int points) {
    public Genome {
        if (species.isBlank()) throw new IllegalArgumentException("Blank species");
        if (points < 1 || points > Genetics.COMPLETE) throw new IllegalArgumentException("Points: " + points);
    }

    public boolean complete() {
        return points == Genetics.COMPLETE;
    }
}
