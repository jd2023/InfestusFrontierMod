package org.jd.infestusfrontier.construction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jd.infestusfrontier.construction.api.Structure;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class StructureTest {
    private static final Structure.CellPos ZERO = new Structure.CellPos(0, 0, 0);
    private static final Structure.Rule TWO_CELL = new Structure.Rule("room", List.of(
            Structure.Expectation.core(ZERO),
            Structure.Expectation.part(new Structure.CellPos(1, 0, 0), "wall")));

    @Test
    void missingCellDefersWithoutReadingOrLoadingIt() {
        var view = new RecordingView(Map.of(ZERO, Structure.Cell.core("a")), List.of(ZERO));
        var structure = new Structure(List.of(TWO_CELL), view);

        var result = structure.inspect(ZERO, "room", new Structure.ServerBudget().request(1));

        var deferred = assertInstanceOf(Structure.Deferred.class, result);
        assertEquals(Structure.DeferredReason.UNLOADED, deferred.reason());
        assertEquals(List.of(ZERO, new Structure.CellPos(1, 0, 0)), view.loadedChecks);
        assertEquals(List.of(ZERO), view.reads);
    }

    @Test
    void explicitPlanDoesNotWalkIntoConnectedStructureAndWallRemovalCreatesNoCore() {
        var cells = new HashMap<Structure.CellPos, Structure.Cell>();
        cells.put(ZERO, Structure.Cell.core("left"));
        cells.put(new Structure.CellPos(1, 0, 0), Structure.Cell.part("wall"));
        cells.put(new Structure.CellPos(2, 0, 0), Structure.Cell.core("right"));
        var view = new RecordingView(cells, List.copyOf(cells.keySet()));
        var structure = new Structure(List.of(TWO_CELL), view);

        assertInstanceOf(Structure.Valid.class,
                structure.inspect(ZERO, "room", new Structure.ServerBudget().request(2)));
        assertEquals(List.of(ZERO, new Structure.CellPos(1, 0, 0)), view.reads);

        cells.remove(new Structure.CellPos(1, 0, 0));
        view.reads.clear();
        var result = structure.inspect(ZERO, "room", new Structure.ServerBudget().request(3));
        assertInstanceOf(Structure.Invalid.class, result);
        assertEquals(2, cells.values().stream().filter(cell -> cell.role() == Structure.Role.CORE).count());

        var overreaching = new Structure.Rule("overreaching", List.of(
                Structure.Expectation.core(ZERO),
                Structure.Expectation.part(new Structure.CellPos(1, 0, 0), "wall"),
                Structure.Expectation.any(new Structure.CellPos(2, 0, 0))));
        cells.put(new Structure.CellPos(1, 0, 0), Structure.Cell.part("wall"));
        var invalid = assertInstanceOf(Structure.Invalid.class, new Structure(List.of(overreaching), view)
                .inspect(ZERO, "overreaching", new Structure.ServerBudget().request(4)));
        assertEquals(Structure.InvalidReason.MULTIPLE_CORES, invalid.reason());
    }

    @Test
    void oneCallAndSharedTickBudgetsAreHardBoundedAndRulesAreFinite() {
        var expectations = java.util.stream.IntStream.range(0, 65)
                .mapToObj(x -> Structure.Expectation.part(new Structure.CellPos(x, 0, 0), "wall"))
                .toList();
        var cells = new HashMap<Structure.CellPos, Structure.Cell>();
        expectations.forEach(expectation -> cells.put(expectation.offset(), Structure.Cell.part("wall")));
        var structure = new Structure(List.of(new Structure.Rule("long", expectations)),
                new RecordingView(cells, expectations.stream().map(Structure.Expectation::offset).toList()));
        var budget = new Structure.ServerBudget().request(7);

        var first = assertInstanceOf(Structure.Deferred.class, structure.inspect(ZERO, "long", budget));
        assertEquals(Structure.DeferredReason.CALL_BUDGET, first.reason());
        assertEquals(64, first.inspectedCells());
        assertInstanceOf(Structure.Valid.class, structure.inspect(ZERO, "long", budget));

        assertThrows(IllegalArgumentException.class, () -> new Structure.Rule("too_large",
                java.util.stream.IntStream.rangeClosed(0, 4096)
                        .mapToObj(x -> Structure.Expectation.any(new Structure.CellPos(x, 0, 0))).toList()));
    }

    @Test
    void sharedBudgetStopsAtTwoHundredFiftySixAndResumesNextTick() {
        var expectations = java.util.stream.IntStream.range(0, 257)
                .mapToObj(x -> Structure.Expectation.any(new Structure.CellPos(x, 0, 0))).toList();
        var loaded = expectations.stream().map(Structure.Expectation::offset).toList();
        var structure = new Structure(List.of(new Structure.Rule("ceiling", expectations)),
                new RecordingView(Map.of(), loaded));
        var request = new Structure.ServerBudget().request(11);
        for (int call = 0; call < 4; call++) {
            assertEquals(Structure.DeferredReason.CALL_BUDGET,
                    assertInstanceOf(Structure.Deferred.class, structure.inspect(ZERO, "ceiling", request)).reason());
        }
        assertEquals(Structure.DeferredReason.SERVER_TICK_BUDGET,
                assertInstanceOf(Structure.Deferred.class, structure.inspect(ZERO, "ceiling", request)).reason());
        assertInstanceOf(Structure.Valid.class, structure.inspect(ZERO, "ceiling", request.atTick(12)));
    }

    private static final class RecordingView implements Structure.LoadedView {
        private final Map<Structure.CellPos, Structure.Cell> cells;
        private final List<Structure.CellPos> loaded;
        private final java.util.ArrayList<Structure.CellPos> loadedChecks = new java.util.ArrayList<>();
        private final java.util.ArrayList<Structure.CellPos> reads = new java.util.ArrayList<>();

        RecordingView(Map<Structure.CellPos, Structure.Cell> cells, List<Structure.CellPos> loaded) {
            this.cells = cells;
            this.loaded = loaded;
        }

        @Override public boolean isLoaded(Structure.CellPos pos) { loadedChecks.add(pos); return loaded.contains(pos); }
        @Override public Structure.Cell read(Structure.CellPos pos) { reads.add(pos); return cells.getOrDefault(pos, Structure.Cell.empty()); }
    }
}
