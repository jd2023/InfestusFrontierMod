package org.jd.infestusfrontier.construction.api;

import java.util.LinkedHashMap;
import java.util.Map;

/** Finite planting-stock store. A player-selected reserve applies to each stored type. */
public final class SeedPouch {
    public static final int TYPE_CAPACITY = 4;
    public static final int COUNT_CAPACITY = 64;
    private int reserve = 1;

    public int reserve() { return reserve; }
    public boolean setReserve(int amount) {
        if (amount < 0 || amount > COUNT_CAPACITY) return false;
        reserve = amount;
        return true;
    }

    private final LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();

    public InsertResult insert(String resource, int requested) {
        if (!valid(resource) || requested < 1) return new InsertResult(InsertStatus.INVALID, 0);
        int present = counts.getOrDefault(resource, 0);
        if (present == 0 && counts.size() == TYPE_CAPACITY) {
            return new InsertResult(InsertStatus.TYPE_CAPACITY, 0);
        }
        int accepted = Math.min(requested, COUNT_CAPACITY - present);
        if (accepted == 0) return new InsertResult(InsertStatus.COUNT_CAPACITY, 0);
        counts.put(resource, present + accepted);
        return new InsertResult(accepted == requested ? InsertStatus.ACCEPTED : InsertStatus.COUNT_CAPACITY, accepted);
    }

    public int takeSurplus(String resource, int requested) {
        if (!valid(resource) || requested < 1) return 0;
        int present = counts.getOrDefault(resource, 0);
        int taken = Math.min(requested, Math.max(0, present - reserve));
        if (taken == present) counts.remove(resource);
        else if (taken > 0) counts.put(resource, present - taken);
        return taken;
    }

    /** Player dismantling/explicit recovery may take the retained planting item too. */
    public int takeAll(String resource) {
        if (!valid(resource)) return 0;
        return counts.getOrDefault(resource, 0) == 0 ? 0 : counts.remove(resource);
    }

    public int count(String resource) { return counts.getOrDefault(resource, 0); }
    public Map<String, Integer> snapshot() {
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(counts));
    }

    public enum InsertStatus { ACCEPTED, COUNT_CAPACITY, TYPE_CAPACITY, INVALID }
    public record InsertResult(InsertStatus status, int accepted) {}

    private static boolean valid(String resource) {
        return resource != null && !resource.isBlank() && resource.length() <= 256;
    }
}
