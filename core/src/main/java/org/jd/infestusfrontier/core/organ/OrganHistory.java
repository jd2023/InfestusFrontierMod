package org.jd.infestusfrontier.core.organ;

/** What one organ has done. Travels with the organ when it is broken and placed again. */
public record OrganHistory(int completedBatches) {
    public static final OrganHistory NONE = new OrganHistory(0);

    public OrganHistory {
        if (completedBatches < 0) throw new IllegalArgumentException("Negative count");
    }

    public OrganHistory completed() {
        return new OrganHistory(Math.incrementExact(completedBatches));
    }
}
