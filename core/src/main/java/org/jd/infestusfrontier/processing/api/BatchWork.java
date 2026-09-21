package org.jd.infestusfrontier.processing.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jd.infestusfrontier.organ.api.OrganHistory;
import org.jd.infestusfrontier.storage.api.QuantityStore;

/** Server-owned, one-batch Culture Bowl state machine. Call it only for loaded server work. */
public final class BatchWork {
    public static final int SNAPSHOT_SCHEMA = 1;

    private final CompletionAdmission completionAdmission;
    private final BatchRecipeCatalog recipes;
    private final QuantityStore quantities;
    private final OrganHistory history;
    private long revision;
    private long nextBatchId;
    private ActiveBatch activeBatch;

    private BatchWork(
            long revision,
            long nextBatchId,
            ActiveBatch activeBatch,
            QuantityStore quantities,
            OrganHistory history,
            BatchRecipeCatalog recipes,
            CompletionAdmission completionAdmission) {
        this.revision = revision;
        this.nextBatchId = nextBatchId;
        this.activeBatch = activeBatch;
        this.quantities = quantities;
        this.history = history;
        this.recipes = Objects.requireNonNull(recipes, "recipes");
        this.completionAdmission = Objects.requireNonNull(completionAdmission, "completionAdmission");
    }

    public static BatchWork create(QuantityStore initialQuantities, CompletionAdmission completionAdmission) {
        return create(initialQuantities, CultureBowlRecipes.catalog(), completionAdmission);
    }

    public static BatchWork create(QuantityStore initialQuantities, BatchRecipeCatalog recipes,
            CompletionAdmission completionAdmission) {
        return create(initialQuantities, new OrganHistory().snapshot(), recipes, completionAdmission);
    }

    public static BatchWork create(
            QuantityStore initialQuantities,
            OrganHistory.Snapshot initialHistory,
            CompletionAdmission completionAdmission) {
        return create(initialQuantities, initialHistory, CultureBowlRecipes.catalog(), completionAdmission);
    }

    public static BatchWork create(
            QuantityStore initialQuantities,
            OrganHistory.Snapshot initialHistory,
            BatchRecipeCatalog recipes,
            CompletionAdmission completionAdmission) {
        Objects.requireNonNull(initialQuantities, "initialQuantities");
        Objects.requireNonNull(initialHistory, "initialHistory");
        if (!initialQuantities.snapshot().reservations().isEmpty()) throw new IllegalArgumentException("New Bowl must be idle");
        long nextBatchId = initialHistory.lastCompletedBatchId() == Long.MAX_VALUE
                ? Long.MAX_VALUE
                : initialHistory.lastCompletedBatchId() + 1;
        return new BatchWork(
                0,
                Math.max(1, nextBatchId),
                null,
                QuantityStore.restore(initialQuantities.snapshot()),
                OrganHistory.restore(initialHistory),
                recipes,
                completionAdmission);
    }

    public static BatchWork restore(State state, CompletionAdmission completionAdmission) {
        return restore(state, CultureBowlRecipes.catalog(), completionAdmission);
    }

    public static BatchWork restore(State state, BatchRecipeCatalog recipes, CompletionAdmission completionAdmission) {
        Objects.requireNonNull(state, "state");
        if (state.schema() != SNAPSHOT_SCHEMA) throw new IllegalArgumentException("Unsupported BatchWork schema");
        if (state.revision() < 0 || state.revision() == Long.MAX_VALUE || state.nextBatchId() < 1) {
            throw new IllegalArgumentException("Invalid batch counters");
        }
        var quantities = QuantityStore.restore(state.quantities());
        var history = OrganHistory.restore(state.history());
        var active = state.activeBatch();
        if (active == null) {
            if (!quantities.snapshot().reservations().isEmpty()) {
                throw new IllegalArgumentException("Idle Bowl retains a reservation");
            }
            if (state.nextBatchId() <= history.snapshot().lastCompletedBatchId()) {
                throw new IllegalArgumentException("Next Bowl batch identifier is stale");
            }
        } else {
            if (recipes.alternatives(active.recipeId(), history.snapshot()).isEmpty()) {
                throw new IllegalArgumentException("Active Bowl recipe is unsupported");
            }
            if (active.batchId() >= state.nextBatchId() || active.batchId() <= history.snapshot().lastCompletedBatchId()) {
                throw new IllegalArgumentException("Active Bowl batch identifier is inconsistent");
            }
            int remaining = active.requiredWorkUnits() - active.completedWorkUnits();
            // Each remaining work unit may advance separately; deferred completion needs one more revision.
            if (state.revision() > Long.MAX_VALUE - 2L - remaining || quantities.revision() >= Long.MAX_VALUE - 1) {
                throw new IllegalArgumentException("Active Bowl has exhausted revision capacity");
            }
            if (!quantities.hasReservation(active.reservationId())
                    || quantities.snapshot().reservations().size() != 1) {
                throw new IllegalArgumentException("Active Bowl reservation is missing");
            }
        }
        var restored = new BatchWork(
                state.revision(), state.nextBatchId(), active, quantities, history, recipes, completionAdmission);
        if (active != null) restored.validateActiveRecipe();
        return restored;
    }

    private void validateActiveRecipe() {
        var reservation = quantities.snapshot().reservations().getFirst();
        var inputs = new LinkedHashMap<String, Integer>();
        var fluids = new LinkedHashMap<String, Integer>();
        var outputs = new LinkedHashMap<String, Integer>();
        var returned = new LinkedHashMap<String, Integer>();
        reservation.inputItems().forEach(a -> inputs.merge(a.resource(), a.amount(), Math::addExact));
        reservation.inputFluids().forEach(a -> fluids.merge(a.resource(), a.amount(), Math::addExact));
        reservation.itemOutputs().forEach(a -> outputs.merge(a.resource(), a.amount(), Math::addExact));
        reservation.returnedContainers().forEach(a -> returned.merge(a.resource(), a.amount(), Math::addExact));
        boolean matches = recipes.alternatives(activeBatch.recipeId(), history.snapshot()).stream().anyMatch(recipe ->
                recipe.itemInputs().equals(inputs) && recipe.fluidInputs().equals(fluids)
                        && recipe.outputs().equals(outputs) && recipe.returnedContainers().equals(returned)
                        && recipe.workUnits() == activeBatch.requiredWorkUnits());
        if (!matches) {
            throw new IllegalArgumentException("Active reservation does not match its earned recipe");
        }
    }

    public boolean insertItem(String resource, int amount, int capacity, long expectedRevision) {
        if (!canTransfer(expectedRevision) || !quantities.insertItem(resource, amount, capacity)) return false;
        incrementRevision();
        return true;
    }

    public boolean insertWater(int amount, long expectedRevision) {
        return insertFluid(0, "water", amount, expectedRevision);
    }

    public boolean insertFluid(String resource, int amount, long expectedRevision) {
        if (!canTransfer(expectedRevision) || !quantities.insertFluid(resource, amount)) return false;
        incrementRevision();
        return true;
    }

    public boolean insertFluid(int tankIndex, String resource, int amount, long expectedRevision) {
        if (!canTransfer(expectedRevision) || !quantities.insertFluid(tankIndex, resource, amount)) return false;
        incrementRevision();
        return true;
    }

    public boolean extractItemSlot(int index, long expectedRevision) {
        if (!canTransfer(expectedRevision) || !quantities.extractItemSlot(index)) return false;
        incrementRevision();
        return true;
    }

    private boolean canTransfer(long expectedRevision) {
        return expectedRevision == revision && activeBatch == null && revision < Long.MAX_VALUE - 1;
    }

    public boolean isActive() { return activeBatch != null; }

    public long revision() {
        return revision;
    }

    public State state() {
        return new State(
                SNAPSHOT_SCHEMA,
                revision,
                nextBatchId,
                activeBatch,
                quantities.snapshot(),
                history.snapshot());
    }

    public State snapshot() {
        return state();
    }

    public StartResult start(StartRequest request, long expectedRevision) {
        Objects.requireNonNull(request, "request");
        if (expectedRevision != revision) return new Refused(StartRefusal.STALE_REVISION, state());
        if (activeBatch != null) return new Refused(StartRefusal.ACTIVE_BATCH, state());
        if (revision == Long.MAX_VALUE - 1) return new Refused(StartRefusal.REVISION_EXHAUSTED, state());
        var alternatives = recipes.alternatives(request.recipeId(), history.snapshot());
        if (alternatives.isEmpty()) return new Refused(StartRefusal.UNKNOWN_RECIPE, state());
        var recipe = selectRecipe(alternatives);
        // Reserve the start, every work tick and a separate completion after quota refusal.
        if (revision > Long.MAX_VALUE - 3L - recipe.workUnits()) {
            return new Refused(StartRefusal.REVISION_EXHAUSTED, state());
        }
        if (nextBatchId == Long.MAX_VALUE) return new Refused(StartRefusal.IDENTIFIER_EXHAUSTED, state());

        var reserved = quantities.reserve(
                new QuantityStore.ReservationRequest(
                        recipe.itemInputs(), recipe.fluidInputs(), recipe.outputs(), recipe.returnedContainers()),
                quantities.revision());
        if (reserved instanceof QuantityStore.ReservationRefused refused) {
            return new Refused(mapRefusal(refused.reason()), state());
        }

        var reservation = (QuantityStore.Reserved) reserved;
        long batchId = nextBatchId++;
        activeBatch = new ActiveBatch(
                batchId,
                request.recipeId(),
                reservation.reservationId(),
                0,
                recipe.workUnits());
        incrementRevision();
        return new Started(batchId, state());
    }

    /** Advances loaded work only; reaching completion may pause at the shared admission boundary. */
    public State advance(int workUnits) {
        if (workUnits < 0) throw new IllegalArgumentException("Work units cannot be negative");
        if (activeBatch == null) return state();

        int remaining = activeBatch.requiredWorkUnits() - activeBatch.completedWorkUnits();
        int accepted = Math.min(remaining, workUnits);
        if (revision == Long.MAX_VALUE - 1 && (accepted > 0 || remaining == 0)) {
            throw new IllegalStateException("Batch revision is exhausted");
        }
        if (accepted > 0) {
            activeBatch = new ActiveBatch(
                    activeBatch.batchId(),
                    activeBatch.recipeId(),
                    activeBatch.reservationId(),
                    activeBatch.completedWorkUnits() + accepted,
                    activeBatch.requiredWorkUnits());
        }
        if (activeBatch.completedWorkUnits() != activeBatch.requiredWorkUnits()) {
            if (accepted > 0) incrementRevision();
            return state();
        }
        if (!completionAdmission.take()) {
            if (accepted > 0) incrementRevision();
            return state();
        }
        if (!history.canComplete(activeBatch.batchId())) {
            throw new IllegalStateException("Batch history cannot accept the active completion");
        }
        if (quantities.commit(activeBatch.reservationId()) != QuantityStore.CommitResult.COMMITTED) {
            throw new IllegalStateException("Active batch reservation disappeared");
        }
        if (history.completeBatch(activeBatch.batchId()) != OrganHistory.CompletionResult.COMPLETED) {
            throw new IllegalStateException("Active batch completion was not counted");
        }
        activeBatch = null;
        incrementRevision();
        return state();
    }

    public CancelResult cancel(long expectedRevision) {
        if (expectedRevision != revision) return CancelResult.STALE_REVISION;
        if (activeBatch == null) return CancelResult.IDLE;
        if (revision == Long.MAX_VALUE - 1) throw new IllegalStateException("Batch revision is exhausted");
        if (quantities.release(activeBatch.reservationId()) != QuantityStore.ReleaseResult.RELEASED) {
            throw new IllegalStateException("Active batch reservation disappeared");
        }
        activeBatch = null;
        incrementRevision();
        return CancelResult.CANCELLED;
    }

    public GrowthResult choose(OrganHistory.GrowthChoice choice, long expectedRevision) {
        if (expectedRevision != revision) return GrowthResult.STALE_REVISION;
        if (activeBatch != null) return GrowthResult.ACTIVE_BATCH;
        if (revision == Long.MAX_VALUE - 1) return GrowthResult.REVISION_EXHAUSTED;
        var result = history.choose(choice);
        if (result == OrganHistory.ChoiceResult.CHOSEN) incrementRevision();
        return switch (result) {
            case CHOSEN -> GrowthResult.CHOSEN;
            case LEVEL_REQUIRED -> GrowthResult.LEVEL_REQUIRED;
            case LEVEL_CHOICE_ALREADY_USED -> GrowthResult.LEVEL_CHOICE_ALREADY_USED;
            case CHOICE_CAP_REACHED -> GrowthResult.CHOICE_CAP_REACHED;
        };
    }

    private BatchRecipeCatalog.ResolvedRecipe selectRecipe(List<BatchRecipeCatalog.ResolvedRecipe> alternatives) {
        for (var alternative : alternatives) {
            boolean available = true;
            for (var input : alternative.itemInputs().entrySet()) {
                if (quantities.availableItem(input.getKey()) < input.getValue()) {
                    available = false;
                    break;
                }
            }
            if (available) return alternative;
        }
        return alternatives.getFirst();
    }

    private static StartRefusal mapRefusal(QuantityStore.ReserveRefusal refusal) {
        return switch (refusal) {
            case STALE_REVISION -> StartRefusal.STALE_REVISION;
            case RESERVATION_LIMIT -> StartRefusal.ACTIVE_BATCH;
            case INSUFFICIENT_ITEM -> StartRefusal.INSUFFICIENT_ITEM;
            case INSUFFICIENT_FLUID -> StartRefusal.INSUFFICIENT_FLUID;
            case OUTPUT_FULL -> StartRefusal.OUTPUT_FULL;
            case RETURNED_CONTAINER_FULL -> StartRefusal.RETURNED_CONTAINER_FULL;
            case IDENTIFIER_EXHAUSTED -> StartRefusal.IDENTIFIER_EXHAUSTED;
            case REVISION_EXHAUSTED -> StartRefusal.REVISION_EXHAUSTED;
        };
    }

    private void incrementRevision() {
        if (revision == Long.MAX_VALUE - 1) throw new IllegalStateException("Batch revision is exhausted");
        revision++;
    }

    @FunctionalInterface
    public interface CompletionAdmission {
        /** Implementations should delegate to the server-wide TickQuota for the current server tick. */
        boolean take();
    }

    public sealed interface StartResult permits Started, Refused {
        State state();
    }

    public record Started(long batchId, State state) implements StartResult {}

    public record Refused(StartRefusal reason, State state) implements StartResult {}

    public record StartRequest(String recipeId) {
        public StartRequest {
            if (recipeId == null || recipeId.isBlank()) throw new IllegalArgumentException("Recipe ID is required");
        }
    }

    public record ActiveBatch(
            long batchId,
            String recipeId,
            long reservationId,
            int completedWorkUnits,
            int requiredWorkUnits) {
        public ActiveBatch {
            if (batchId < 1 || reservationId < 1) throw new IllegalArgumentException("Invalid active batch identity");
            if (recipeId == null || recipeId.isBlank()) throw new IllegalArgumentException("Active recipe is required");
            if (requiredWorkUnits < 1 || completedWorkUnits < 0 || completedWorkUnits > requiredWorkUnits) {
                throw new IllegalArgumentException("Invalid active batch progress");
            }
        }
    }

    /** One recovered core stores this state once; no wall or controller needs another history copy. */
    public record State(
            int schema,
            long revision,
            long nextBatchId,
            ActiveBatch activeBatch,
            QuantityStore.Snapshot quantities,
            OrganHistory.Snapshot history) {
        public State {
            Objects.requireNonNull(quantities, "quantities");
            Objects.requireNonNull(history, "history");
        }

        public Status status() {
            if (activeBatch == null) return Status.IDLE;
            return activeBatch.completedWorkUnits() == activeBatch.requiredWorkUnits()
                    ? Status.COMPLETION_BLOCKED
                    : Status.WORKING;
        }
    }

    public enum Status {
        IDLE,
        WORKING,
        COMPLETION_BLOCKED
    }

    public enum StartRefusal {
        STALE_REVISION,
        ACTIVE_BATCH,
        UNKNOWN_RECIPE,
        INSUFFICIENT_ITEM,
        INSUFFICIENT_FLUID,
        OUTPUT_FULL,
        RETURNED_CONTAINER_FULL,
        IDENTIFIER_EXHAUSTED,
        REVISION_EXHAUSTED
    }

    public enum CancelResult {
        CANCELLED,
        IDLE,
        STALE_REVISION
    }

    public enum GrowthResult {
        CHOSEN,
        STALE_REVISION,
        ACTIVE_BATCH,
        LEVEL_REQUIRED,
        LEVEL_CHOICE_ALREADY_USED,
        CHOICE_CAP_REACHED,
        REVISION_EXHAUSTED
    }
}
