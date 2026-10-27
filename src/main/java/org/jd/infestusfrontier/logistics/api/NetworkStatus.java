package org.jd.infestusfrontier.logistics.api;

/** Immutable probe reading of network discovery. No inventory state is owned by this snapshot. */
public record NetworkStatus(String state, int cells, int limit) {}
