package org.jd.infestusfrontier.core.storage;

import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;

/**
 * One biomass balance. {@code stored} is real biomass, {@code reserved} is room promised to a running batch. The sum
 * never exceeds {@code capacity}. Every change is whole or nothing and returns a new tank.
 */
public record BiomassTank(Biomass stored, Biomass reserved, Biomass capacity) {
    public static final Refusal FULL = new Refusal("storage.full");
    public static final Refusal EMPTY = new Refusal("storage.empty");

    public BiomassTank {
        if (!capacity.atLeast(stored.plus(reserved))) throw new IllegalArgumentException("Tank over capacity");
    }

    public static BiomassTank empty(Biomass capacity) {
        return new BiomassTank(Biomass.ZERO, Biomass.ZERO, capacity);
    }

    /** Capacity not stored and not promised. */
    public Biomass room() {
        return capacity.minus(stored).minus(reserved);
    }

    public boolean isFull() {
        return room().isZero();
    }

    public Outcome<BiomassTank> fill(Biomass amount) {
        if (!room().atLeast(amount)) return Outcome.refused(FULL);
        return Outcome.done(new BiomassTank(stored.plus(amount), reserved, capacity));
    }

    public Outcome<BiomassTank> drain(Biomass amount) {
        if (!stored.atLeast(amount)) return Outcome.refused(EMPTY);
        return Outcome.done(new BiomassTank(stored.minus(amount), reserved, capacity));
    }

    /** Promises room to a batch before it starts, so its output can never be refused. */
    public Outcome<BiomassTank> reserve(Biomass amount) {
        if (!room().atLeast(amount)) return Outcome.refused(FULL);
        return Outcome.done(new BiomassTank(stored, reserved.plus(amount), capacity));
    }

    /** Turns a promise into stored biomass when the batch completes. Fails on a promise never made. */
    public BiomassTank commit(Biomass amount) {
        return new BiomassTank(stored.plus(amount), reserved.minus(amount), capacity);
    }

    /** Gives a promise back when the batch is cancelled. Fails on a promise never made. */
    public BiomassTank release(Biomass amount) {
        return new BiomassTank(stored, reserved.minus(amount), capacity);
    }
}
