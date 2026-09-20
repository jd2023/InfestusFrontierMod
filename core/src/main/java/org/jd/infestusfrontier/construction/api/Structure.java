package org.jd.infestusfrontier.construction.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jd.infestusfrontier.foundation.TickQuota;

/** Bounded, resumable inspection of one explicit structure rule. */
public final class Structure {
    public static final int CELLS_PER_CALL = 64;
    public static final int CELLS_PER_SERVER_TICK = 256;
    public static final int MAX_STRUCTURE_CELLS = 4096;

    private final Map<String, Rule> rules;
    private final LoadedView view;

    public Structure(List<Rule> rules, LoadedView view) {
        this.view = Objects.requireNonNull(view, "view");
        var indexed = new LinkedHashMap<String, Rule>();
        for (Rule rule : List.copyOf(rules)) {
            if (indexed.putIfAbsent(rule.id(), rule) != null) {
                throw new IllegalArgumentException("Duplicate structure rule: " + rule.id());
            }
        }
        this.rules = Map.copyOf(indexed);
    }

    /**
     * Continues one inspection carried by {@code budget}. Only the rule's finite plan is read;
     * adjacency is never traversed. Reuse the same budget after a budget-related deferral.
     */
    public Inspection inspect(CellPos origin, String ruleId, Budget budget) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(budget, "budget");
        Rule rule = rules.get(ruleId);
        if (rule == null) return new Invalid(InvalidReason.UNKNOWN_RULE, 0, null);
        budget.bind(origin, ruleId);
        long revision = view.revision();
        if (budget.revision != null && budget.revision != revision) {
            budget.reset(revision);
            return new Deferred(DeferredReason.WORLD_CHANGED, 0, 0);
        }
        budget.revision = revision;
        if (budget.terminal != null) return budget.terminal;

        int inspectedThisCall = 0;
        while (budget.cursor < rule.expectations().size()) {
            if (inspectedThisCall == CELLS_PER_CALL) {
                return new Deferred(DeferredReason.CALL_BUDGET, budget.cursor, inspectedThisCall);
            }
            Expectation expected = rule.expectations().get(budget.cursor);
            CellPos absolute;
            try {
                absolute = origin.offset(expected.offset());
            } catch (ArithmeticException overflow) {
                return budget.finish(new Invalid(InvalidReason.POSITION_OVERFLOW, budget.cursor, null));
            }
            if (!view.isLoaded(absolute)) {
                return new Deferred(DeferredReason.UNLOADED, budget.cursor, inspectedThisCall);
            }
            if (!budget.shared.take(budget.tick)) {
                return new Deferred(DeferredReason.SERVER_TICK_BUDGET, budget.cursor, inspectedThisCall);
            }
            Cell actual = Objects.requireNonNull(view.read(absolute), "loaded view returned null");
            inspectedThisCall++;
            budget.inspected++;
            if (actual.role() == Role.CORE && ++budget.cores > 1) {
                return budget.finish(new Invalid(InvalidReason.MULTIPLE_CORES, budget.inspected, absolute));
            }
            if (!expected.matches(actual)) {
                return budget.finish(new Invalid(InvalidReason.PART_MISMATCH, budget.inspected, absolute));
            }
            budget.cursor++;
        }
        return budget.finish(new Valid(budget.inspected));
    }

    public sealed interface Inspection permits Valid, Invalid, Deferred {}
    public record Valid(int inspectedCells) implements Inspection {}
    public record Invalid(InvalidReason reason, int inspectedCells, CellPos position) implements Inspection {}
    public record Deferred(DeferredReason reason, int nextCell, int inspectedCells) implements Inspection {}
    public enum InvalidReason { UNKNOWN_RULE, PART_MISMATCH, MULTIPLE_CORES, POSITION_OVERFLOW }
    public enum DeferredReason { UNLOADED, CALL_BUDGET, SERVER_TICK_BUDGET, WORLD_CHANGED }

    /** Shared admission for all structure inspections belonging to one server. */
    public static final class ServerBudget {
        private final TickQuota quota = new TickQuota(CELLS_PER_SERVER_TICK);
        public Budget request(long serverTick) { return new Budget(quota, serverTick); }
    }

    /** Per-request continuation; obtain it from one server-identity {@link ServerBudget}. */
    public static final class Budget {
        private final TickQuota shared;
        private long tick;
        private CellPos origin;
        private String ruleId;
        private int cursor;
        private int inspected;
        private int cores;
        private Inspection terminal;
        private Long revision;

        private void reset(long currentRevision) {
            revision = currentRevision;
            cursor = inspected = cores = 0;
            terminal = null;
        }

        private Budget(TickQuota shared, long serverTick) {
            this.shared = Objects.requireNonNull(shared, "shared");
            this.tick = serverTick;
        }

        /** Resumes the same request on a later server tick after shared admission deferral. */
        public Budget atTick(long serverTick) {
            tick = serverTick;
            return this;
        }

        private void bind(CellPos requestedOrigin, String requestedRule) {
            if (origin == null) {
                origin = requestedOrigin;
                ruleId = requestedRule;
            } else if (!origin.equals(requestedOrigin) || !ruleId.equals(requestedRule)) {
                throw new IllegalArgumentException("An inspection budget cannot be rebound");
            }
        }

        private <T extends Inspection> T finish(T result) {
            terminal = result;
            return result;
        }
    }

    public record Rule(String id, List<Expectation> expectations) {
        public Rule {
            requireName(id, "rule id");
            expectations = List.copyOf(expectations);
            if (expectations.isEmpty() || expectations.size() > MAX_STRUCTURE_CELLS) {
                throw new IllegalArgumentException("Structure rules require 1.." + MAX_STRUCTURE_CELLS + " cells");
            }
            long unique = expectations.stream().map(Expectation::offset).distinct().count();
            if (unique != expectations.size()) throw new IllegalArgumentException("Structure rule positions must be unique");
            if (expectations.stream().filter(e -> e.role == Role.CORE).count() > 1) {
                throw new IllegalArgumentException("One structure rule cannot own multiple cores");
            }
        }
    }

    public record Expectation(CellPos offset, Role role, String part) {
        public Expectation {
            Objects.requireNonNull(offset, "offset");
            Objects.requireNonNull(role, "role");
            if (role == Role.PART) requireName(part, "part");
            if (role != Role.PART && part != null) throw new IllegalArgumentException("Only part expectations name a part");
        }

        public static Expectation core(CellPos offset) { return new Expectation(offset, Role.CORE, null); }
        public static Expectation part(CellPos offset, String part) { return new Expectation(offset, Role.PART, part); }
        public static Expectation any(CellPos offset) { return new Expectation(offset, Role.ANY, null); }

        private boolean matches(Cell cell) {
            return role == Role.ANY || role == cell.role() && (role != Role.PART || part.equals(cell.part()));
        }
    }

    public enum Role { CORE, PART, EMPTY, ANY }

    public record Cell(Role role, String part, String coreId) {
        public Cell {
            Objects.requireNonNull(role, "role");
            if (role == Role.ANY) throw new IllegalArgumentException("ANY is not a cell role");
            if (role == Role.CORE) requireName(coreId, "core id");
            else if (coreId != null) throw new IllegalArgumentException("Passive cells cannot own a core identity");
            if (role == Role.PART) requireName(part, "part");
            else if (part != null) throw new IllegalArgumentException("Only part cells name a part");
        }

        public static Cell core(String id) { return new Cell(Role.CORE, null, id); }
        public static Cell part(String part) { return new Cell(Role.PART, part, null); }
        public static Cell empty() { return new Cell(Role.EMPTY, null, null); }
    }

    public record CellPos(int x, int y, int z) {
        public CellPos offset(CellPos relative) {
            return new CellPos(Math.addExact(x, relative.x), Math.addExact(y, relative.y), Math.addExact(z, relative.z));
        }
    }

    /**
     * Server-thread view. Its monotonic revision must change before any relevant cell state,
     * core identity or loaded status changes. A world-wide revision is conservative and valid.
     * Calls are synchronous: the view must not mutate during one inspection invocation.
     */
    public interface LoadedView {
        long revision();
        boolean isLoaded(CellPos pos);
        Cell read(CellPos pos);
    }

    private static void requireName(String value, String label) {
        if (value == null || value.isBlank() || value.length() > 256) {
            throw new IllegalArgumentException(label + " must contain 1..256 characters");
        }
    }
}
