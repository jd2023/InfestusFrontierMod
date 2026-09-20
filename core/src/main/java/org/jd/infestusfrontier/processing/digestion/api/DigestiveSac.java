package org.jd.infestusfrontier.processing.digestion.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jd.infestusfrontier.organ.api.OrganHistory;
import org.jd.infestusfrontier.storage.api.BiomassTransfer;
import org.jd.infestusfrontier.storage.api.QuantityStore;

/** One loaded, reserved digestion batch with a finite retained biomass output. */
public final class DigestiveSac {
    public static final int SNAPSHOT_SCHEMA = 1;
    public static final int STARTER_CAPACITY = 1_000;
    private static final Map<String, Recipe> RECIPES = recipes();

    private final CompletionAdmission completionAdmission;
    private QuantityStore quantities;
    private OrganHistory history;
    private long revision;
    private long nextBatchId = 1;
    private ActiveBatch activeBatch;

    private DigestiveSac(QuantityStore quantities, OrganHistory history, CompletionAdmission completionAdmission) {
        this.quantities = quantities;
        this.history = history;
        this.completionAdmission = Objects.requireNonNull(completionAdmission, "completionAdmission");
    }

    public static DigestiveSac create(int capacity, CompletionAdmission completionAdmission) {
        if (capacity < 1) throw new IllegalArgumentException("Sac capacity must be positive");
        return new DigestiveSac(
                new QuantityStore(List.of(QuantityStore.ItemSlot.empty(64)),
                        List.of(QuantityStore.Tank.empty(capacity))),
                new OrganHistory(), completionAdmission);
    }

    public static DigestiveSac restore(State state, CompletionAdmission completionAdmission) {
        Objects.requireNonNull(state, "state");
        if (state.schema() != SNAPSHOT_SCHEMA || state.revision() < 0 || state.revision() == Long.MAX_VALUE
                || state.nextBatchId() < 1) {
            throw new IllegalArgumentException("Invalid Digestive Sac counters");
        }
        var store = QuantityStore.restore(state.quantities());
        if (state.quantities().itemSlots().size() != 1 || state.quantities().tanks().size() != 1) {
            throw new IllegalArgumentException("Invalid Digestive Sac store shape");
        }
        var history = OrganHistory.restore(state.history());
        var restored = new DigestiveSac(store, history, completionAdmission);
        restored.revision = state.revision();
        restored.nextBatchId = state.nextBatchId();
        restored.activeBatch = state.activeBatch();
        restored.validateState();
        return restored;
    }

    public static Recipe recipe(String item) {
        var recipe = RECIPES.get(item);
        if (recipe == null) throw new IllegalArgumentException("Unsupported digestive feed: " + item);
        return recipe;
    }

    public FeedResult feed(String item) {
        var recipe = RECIPES.get(item);
        if (recipe == null) return new FeedRefused(FeedRefusal.UNSUPPORTED, snapshot());
        if (activeBatch != null) return new FeedRefused(FeedRefusal.ACTIVE_BATCH, snapshot());
        if (revision > Long.MAX_VALUE - 2L - recipe.workUnits() || nextBatchId == Long.MAX_VALUE) {
            return new FeedRefused(FeedRefusal.EXHAUSTED, snapshot());
        }

        var candidate = QuantityStore.restore(quantities.snapshot());
        if (!candidate.insertItem(item, 1, 64)) {
            return new FeedRefused(FeedRefusal.INPUT_HELD, snapshot());
        }
        var request = new QuantityStore.ReservationRequest(
                Map.of(item, 1), Map.of(), Map.of(), Map.of(),
                Map.of(BiomassTransfer.BIOMASS, recipe.biomass()));
        var preview = candidate.preview(request, candidate.revision());
        if (preview instanceof QuantityStore.PreviewRefused) {
            return new FeedRefused(FeedRefusal.OUTPUT_FULL, snapshot());
        }
        var reserved = candidate.reserve(request, candidate.revision());
        if (!(reserved instanceof QuantityStore.Reserved accepted)) {
            return new FeedRefused(FeedRefusal.OUTPUT_FULL, snapshot());
        }
        quantities = candidate;
        long batchId = nextBatchId++;
        activeBatch = new ActiveBatch(
                batchId, item, accepted.reservationId(), 0, recipe.workUnits());
        revision++;
        return new Fed(batchId, snapshot());
    }

    public State advance(int workUnits) {
        if (workUnits < 0) throw new IllegalArgumentException("Work units cannot be negative");
        if (activeBatch == null) return snapshot();
        int completed = activeBatch.completedWorkUnits()
                + Math.min(workUnits, activeBatch.requiredWorkUnits() - activeBatch.completedWorkUnits());
        if (completed != activeBatch.completedWorkUnits()) {
            activeBatch = new ActiveBatch(activeBatch.batchId(), activeBatch.feed(),
                    activeBatch.reservationId(), completed, activeBatch.requiredWorkUnits());
            revision++;
        }
        if (completed < activeBatch.requiredWorkUnits() || !completionAdmission.take()) return snapshot();
        if (!history.canComplete(activeBatch.batchId())
                || quantities.commit(activeBatch.reservationId()) != QuantityStore.CommitResult.COMMITTED
                || history.completeBatch(activeBatch.batchId()) != OrganHistory.CompletionResult.COMPLETED) {
            throw new IllegalStateException("Digestive Sac completion lost its reservation");
        }
        activeBatch = null;
        revision++;
        return snapshot();
    }

    public QuantityStore quantities() {
        return quantities;
    }

    public State snapshot() {
        return new State(SNAPSHOT_SCHEMA, revision, nextBatchId, activeBatch,
                quantities.snapshot(), history.snapshot());
    }

    private void validateState() {
        if (nextBatchId <= history.snapshot().lastCompletedBatchId()) {
            throw new IllegalArgumentException("Digestive Sac next batch must follow its completed history");
        }
        var reservations = quantities.snapshot().reservations();
        if (activeBatch == null) {
            if (!reservations.isEmpty() || !quantities.snapshot().itemSlots().getFirst().isEmpty()) {
                throw new IllegalArgumentException("Idle Digestive Sac retains batch state");
            }
            return;
        }
        if (activeBatch.batchId() >= nextBatchId || activeBatch.batchId() <= history.snapshot().lastCompletedBatchId()
                || reservations.size() != 1 || !quantities.hasReservation(activeBatch.reservationId())) {
            throw new IllegalArgumentException("Digestive Sac active batch identity is invalid");
        }
        if (revision > Long.MAX_VALUE - 2L
                - (activeBatch.requiredWorkUnits() - activeBatch.completedWorkUnits())) {
            throw new IllegalArgumentException("Digestive Sac lacks revision capacity for its batch");
        }
        var recipe = recipe(activeBatch.feed());
        var reservation = reservations.getFirst();
        var inputs = new LinkedHashMap<String, Integer>();
        var outputs = new LinkedHashMap<String, Integer>();
        reservation.inputItems().forEach(a -> inputs.merge(a.resource(), a.amount(), Math::addExact));
        reservation.outputFluids().forEach(a -> outputs.merge(a.resource(), a.amount(), Math::addExact));
        if (!inputs.equals(Map.of(activeBatch.feed(), 1))
                || !outputs.equals(Map.of(BiomassTransfer.BIOMASS, recipe.biomass()))
                || !reservation.inputFluids().isEmpty() || !reservation.itemOutputs().isEmpty()
                || !reservation.returnedContainers().isEmpty()
                || activeBatch.requiredWorkUnits() != recipe.workUnits()) {
            throw new IllegalArgumentException("Digestive Sac reservation does not match its recipe");
        }
    }

    private static Map<String, Recipe> recipes() {
        var recipes = new LinkedHashMap<String, Recipe>();
        recipes.put("rotten_flesh", new Recipe(50, 8 * 20));
        recipes.put("wheat", new Recipe(100, 2 * 20));
        return Map.copyOf(recipes);
    }

    @FunctionalInterface
    public interface CompletionAdmission { boolean take(); }

    public sealed interface FeedResult permits Fed, FeedRefused { State state(); }
    public record Fed(long batchId, State state) implements FeedResult {}
    public record FeedRefused(FeedRefusal reason, State state) implements FeedResult {}
    public enum FeedRefusal { UNSUPPORTED, ACTIVE_BATCH, OUTPUT_FULL, INPUT_HELD, EXHAUSTED }
    public record Recipe(int biomass, int workUnits) {
        public Recipe {
            if (biomass < 1 || workUnits < 1) throw new IllegalArgumentException("Recipe values must be positive");
        }
    }
    public record ActiveBatch(long batchId, String feed, long reservationId,
            int completedWorkUnits, int requiredWorkUnits) {
        public ActiveBatch {
            if (batchId < 1 || reservationId < 1 || feed == null || feed.isBlank()
                    || completedWorkUnits < 0 || requiredWorkUnits < 1
                    || completedWorkUnits > requiredWorkUnits) {
                throw new IllegalArgumentException("Invalid Digestive Sac batch");
            }
        }
    }
    public record State(int schema, long revision, long nextBatchId, ActiveBatch activeBatch,
            QuantityStore.Snapshot quantities, OrganHistory.Snapshot history) {
        public State {
            Objects.requireNonNull(quantities, "quantities");
            Objects.requireNonNull(history, "history");
        }
        public int biomass() { return quantities.fluidAmount(BiomassTransfer.BIOMASS); }
    }
}
