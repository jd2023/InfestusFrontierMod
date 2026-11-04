package org.jd.infestusfrontier.organ.api;

/** An organ that works for a player without one standing by: it keeps a {@link Steward}. */
public interface Stewarded {
    Steward steward();
}
