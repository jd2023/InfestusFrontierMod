package org.jd.infestusfrontier.core.foundation;

/** A checked amount of biomass in biomass units (BU). One BU is one millibucket. Never negative, never overflows. */
public record Biomass(int units) implements Comparable<Biomass> {
    public static final Biomass ZERO = new Biomass(0);
    public static final Biomass BUCKET = new Biomass(1000);

    public Biomass {
        if (units < 0) throw new IllegalArgumentException("Negative biomass: " + units);
    }

    public static Biomass of(int units) {
        return new Biomass(units);
    }

    public Biomass plus(Biomass other) {
        return new Biomass(Math.addExact(units, other.units));
    }

    /** Fails when the result would be negative. Callers compare first. */
    public Biomass minus(Biomass other) {
        return new Biomass(Math.subtractExact(units, other.units));
    }

    public boolean isZero() {
        return units == 0;
    }

    public boolean atLeast(Biomass other) {
        return units >= other.units;
    }

    public Biomass min(Biomass other) {
        return units <= other.units ? this : other;
    }

    @Override
    public int compareTo(Biomass other) {
        return Integer.compare(units, other.units);
    }
}
