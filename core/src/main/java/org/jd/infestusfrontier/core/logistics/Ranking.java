package org.jd.infestusfrontier.core.logistics;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The order an output Mouth offers items to destinations. Highest priority first. Equal-priority destinations keep
 * their input order and take turns: a group starts at {@code turn} modulo its size. Sorted once for a network and
 * reused while its members and priorities do not change; reading the order at a place copies nothing.
 */
public final class Ranking<T> {
    private final List<T> ids;
    /** Where each group of equal priority starts, then the size of the list. */
    private final int[] starts;

    public Ranking(List<Delivery.Destination<T>> destinations) {
        List<Delivery.Destination<T>> sorted = new ArrayList<>(destinations);
        sorted.sort(Comparator.comparingInt(d -> -d.priority()));
        ids = sorted.stream().map(Delivery.Destination::id).toList();
        List<Integer> bounds = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++)
            if (i == 0 || sorted.get(i).priority() != sorted.get(i - 1).priority()) bounds.add(i);
        bounds.add(sorted.size());
        starts = bounds.stream().mapToInt(Integer::intValue).toArray();
    }

    public int size() {
        return ids.size();
    }

    /** The destinations in order for {@code turn}; each read costs a search of the priority groups. */
    public List<T> ordered(int turn) {
        return new AbstractList<>() {
            @Override
            public int size() {
                return ids.size();
            }

            @Override
            public T get(int index) {
                if (index < 0 || index >= ids.size()) throw new IndexOutOfBoundsException(index);
                int group = 0;
                while (starts[group + 1] <= index) group++;
                int start = starts[group];
                int size = starts[group + 1] - start;
                return ids.get(start + (Math.floorMod(turn, size) + index - start) % size);
            }
        };
    }
}
