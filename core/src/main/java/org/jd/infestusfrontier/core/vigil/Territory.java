package org.jd.infestusfrontier.core.vigil;

/** A fixed, player-selected three-by-three territory; bit four is the nest's chunk. */
public record Territory(int mask) {
    public static final int CENTRE = 1 << 4;
    public static final int ALL = (1 << 9) - 1;
    public static final int DIMENSION_CHUNKS = 27;
    public static final int SERVER_CHUNKS = 54;
    public static final int PAYMENT_TICKS = 200;
    public static final int UPKEEP_PER_CHUNK = 10;
    public static final Territory HOME = new Territory(CENTRE);

    public Territory {
        if ((mask & ~ALL) != 0 || (mask & CENTRE) == 0) throw new IllegalArgumentException("territory " + mask);
    }

    public int chunks() {
        return Integer.bitCount(mask);
    }

    public int payment() {
        return Math.multiplyExact(chunks(), UPKEEP_PER_CHUNK);
    }

    public boolean includes(int index) {
        return index >= 0 && index < 9 && (mask & (1 << index)) != 0;
    }

    public Territory toggle(int index) {
        if (index < 0 || index >= 9 || index == 4) return this;
        return new Territory(mask ^ (1 << index));
    }

    public static int capacity(int feeders) {
        if (feeders < 0 || feeders > 4) throw new IllegalArgumentException("feeders " + feeders);
        return Math.addExact(1, Math.multiplyExact(2, feeders));
    }

    public boolean fits(int dimensionUsed, int serverUsed) {
        return dimensionUsed >= 0
                && serverUsed >= 0
                && dimensionUsed <= DIMENSION_CHUNKS - chunks()
                && serverUsed <= SERVER_CHUNKS - chunks();
    }
}
