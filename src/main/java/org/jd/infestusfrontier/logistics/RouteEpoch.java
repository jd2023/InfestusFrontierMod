package org.jd.infestusfrontier.logistics;

/**
 * Counts changes to the item networks of one dimension: a placed or removed Item Vein or Mouth, a Mouth's mode or
 * priority. A Mouth keeps its found destinations while the count it saw is current. Not saved: a restart starts every
 * cache empty anyway.
 */
final class RouteEpoch {
    private long count;

    long current() {
        return count;
    }

    void changed() {
        count++;
    }
}
