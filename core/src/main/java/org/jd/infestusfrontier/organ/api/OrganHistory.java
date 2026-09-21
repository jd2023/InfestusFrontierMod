package org.jd.infestusfrontier.organ.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Owns one organ core's completed-batch count and its three earned growth choices. */
public final class OrganHistory {
    public static final long L1_THRESHOLD = 32;
    public static final long L2_THRESHOLD = 128;
    public static final long L3_THRESHOLD = 512;
    public static final int MAX_CHOICES = 3;

    private long completedBatches;
    private long lastCompletedBatchId;
    private final List<GrowthChoice> choices;

    public OrganHistory() {
        choices = new ArrayList<>(MAX_CHOICES);
    }

    private OrganHistory(Snapshot snapshot) {
        completedBatches = snapshot.completedBatches();
        lastCompletedBatchId = snapshot.lastCompletedBatchId();
        choices = new ArrayList<>(snapshot.choices());
    }

    public static OrganHistory restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.completedBatches() < 0 || snapshot.lastCompletedBatchId() < 0) {
            throw new IllegalArgumentException("Organ history counters cannot be negative");
        }
        if (snapshot.completedBatches() > snapshot.lastCompletedBatchId()) {
            throw new IllegalArgumentException("Completed count cannot exceed the latest batch identifier");
        }
        if (snapshot.choices().size() > MAX_CHOICES) {
            throw new IllegalArgumentException("An organ can retain at most three choices");
        }
        if (snapshot.choices().size() > levelFor(snapshot.completedBatches()).choiceSlots()) {
            throw new IllegalArgumentException("Persisted choices exceed the organ's earned level");
        }
        for (var choice : snapshot.choices()) Objects.requireNonNull(choice, "choice");
        return new OrganHistory(snapshot);
    }

    public boolean canComplete(long batchId) {
        return batchId > lastCompletedBatchId && completedBatches < Long.MAX_VALUE;
    }

    public CompletionResult completeBatch(long batchId) {
        if (batchId < 1) throw new IllegalArgumentException("Batch identifier must be positive");
        if (batchId <= lastCompletedBatchId) return CompletionResult.ALREADY_COMPLETED;
        if (completedBatches == Long.MAX_VALUE) return CompletionResult.COUNT_EXHAUSTED;
        completedBatches++;
        lastCompletedBatchId = batchId;
        return CompletionResult.COMPLETED;
    }

    public ChoiceResult choose(GrowthChoice choice) {
        Objects.requireNonNull(choice, "choice");
        if (choices.size() == MAX_CHOICES) return ChoiceResult.CHOICE_CAP_REACHED;
        if (choices.size() >= levelFor(completedBatches).choiceSlots()) {
            return completedBatches < L1_THRESHOLD
                    ? ChoiceResult.LEVEL_REQUIRED
                    : ChoiceResult.LEVEL_CHOICE_ALREADY_USED;
        }
        choices.add(choice);
        return ChoiceResult.CHOSEN;
    }

    public Snapshot snapshot() {
        return new Snapshot(completedBatches, lastCompletedBatchId, choices);
    }

    public enum GrowthChoice {
        INCUBATION,
        WATER_ECONOMY
    }

    public enum CompletionResult {
        COMPLETED,
        ALREADY_COMPLETED,
        COUNT_EXHAUSTED
    }

    public enum ChoiceResult {
        CHOSEN,
        LEVEL_REQUIRED,
        LEVEL_CHOICE_ALREADY_USED,
        CHOICE_CAP_REACHED
    }

    public enum Level {
        L0(0),
        L1(1),
        L2(2),
        L3(3);

        private final int choiceSlots;

        Level(int choiceSlots) {
            this.choiceSlots = choiceSlots;
        }

        public int choiceSlots() {
            return choiceSlots;
        }
    }

    public record Snapshot(long completedBatches, long lastCompletedBatchId, List<GrowthChoice> choices) {
        public Snapshot {
            if (choices.size() > MAX_CHOICES) throw new IllegalArgumentException("Too many growth choices");
            choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
        }

        public Level level() {
            return levelFor(completedBatches);
        }

        public int choiceCount(GrowthChoice choice) {
            int count = 0;
            for (var selected : choices) if (selected == choice) count++;
            return count;
        }
    }

    private static Level levelFor(long completedBatches) {
        if (completedBatches >= L3_THRESHOLD) return Level.L3;
        if (completedBatches >= L2_THRESHOLD) return Level.L2;
        if (completedBatches >= L1_THRESHOLD) return Level.L1;
        return Level.L0;
    }
}
