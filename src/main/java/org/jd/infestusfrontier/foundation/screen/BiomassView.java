package org.jd.infestusfrontier.foundation.screen;

/** The biomass balance a menu syncs to its screen. Capacity is at least 1. */
public interface BiomassView {
    int stored();

    int capacity();
}
