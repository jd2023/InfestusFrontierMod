package org.jd.infestusfrontier.core.storage;

import java.util.Optional;

/**
 * What a Capsule Stem pod is (BLOCK_CATALOG T1-09): a bulk pod, one compartment of 8, 16 or 32 stacks of its kind at
 * grade 0, 1 or 2, or a Mixed Craw, {@value #CRAW_COMPARTMENTS} compartments of one kind each at a quarter of that
 * depth, so its total is half a bulk pod's. A stack is the kind's own stack size.
 */
public enum PodKind {
    BULK("bulk", 1, 8),
    CRAW("craw", 4, 1);

    /** The compartments of a Mixed Craw: the most kinds a pod holds at once. */
    public static final int CRAW_COMPARTMENTS = 4;

    private final String id;
    private final int compartments;
    private final int youngStacks;

    PodKind(String id, int compartments, int youngStacks) {
        this.id = id;
        this.compartments = compartments;
        this.youngStacks = youngStacks;
    }

    /** The name a save and a language file use. */
    public String id() {
        return id;
    }

    /** How many kinds the pod holds at once. */
    public int compartments() {
        return compartments;
    }

    /** Stacks of its kind one compartment holds at {@code grade}: doubled by each grade. */
    public int stacks(int grade) {
        if (grade < 0 || grade > 30) throw new IllegalArgumentException("grade " + grade);
        return youngStacks << grade;
    }

    /** Stacks in all the compartments of a pod at {@code grade}. */
    public int totalStacks(int grade) {
        return compartments * stacks(grade);
    }

    public static Optional<PodKind> byId(String id) {
        for (PodKind kind : values()) if (kind.id.equals(id)) return Optional.of(kind);
        return Optional.empty();
    }
}
