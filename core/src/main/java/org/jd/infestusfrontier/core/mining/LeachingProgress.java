package org.jd.infestusfrontier.core.mining;

/**
 * Where a Leaching Pod is in its finite work: layers already worked, charges left, and loaded ticks waited towards the
 * next layer. A layer is worked whole or not at all, so this is all a pod needs to save. It never exceeds its limits:
 * a value outside them does not construct, so unreadable saved numbers are refused instead of repaired.
 */
public record LeachingProgress(int layers, int charges, int ticks) {
    public static final LeachingProgress START = new LeachingProgress(0, Leaching.MAX_CHARGES, 0);

    public LeachingProgress {
        if (layers < 0 || layers > Leaching.MAX_LAYERS) throw new IllegalArgumentException("Layers: " + layers);
        if (charges < 0 || charges > Leaching.MAX_CHARGES) throw new IllegalArgumentException("Charges: " + charges);
        if (ticks < 0 || ticks > Leaching.LAYER_TICKS) throw new IllegalArgumentException("Ticks: " + ticks);
    }

    /** True once the layers or the charges are used up: the pod's work is complete. */
    public boolean finished() {
        return layers == Leaching.MAX_LAYERS || charges == 0;
    }

    /** True when the next layer may be worked. */
    public boolean due() {
        return ticks == Leaching.LAYER_TICKS;
    }

    /** One more loaded tick of waiting, never past the point where a layer is due. */
    public LeachingProgress waited() {
        return ticks == Leaching.LAYER_TICKS ? this : new LeachingProgress(layers, charges, ticks + 1);
    }

    /** The layer could not be worked, so the wait starts over; nothing was converted or paid. */
    public LeachingProgress blocked() {
        return new LeachingProgress(layers, charges, 0);
    }

    /** The layer was worked and converted {@code converted} blocks. A layer that converted none costs no charge. */
    public LeachingProgress worked(int converted) {
        if (converted < 0 || converted > Leaching.MAX_BLOCKS_PER_LAYER)
            throw new IllegalArgumentException("Converted: " + converted);
        return new LeachingProgress(layers + 1, converted > 0 ? charges - 1 : charges, 0);
    }
}
