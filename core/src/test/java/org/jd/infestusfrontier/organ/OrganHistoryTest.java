package org.jd.infestusfrontier.organ;

import java.util.List;
import org.jd.infestusfrontier.organ.api.OrganHistory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrganHistoryTest {
    @Test
    void levelsPermitExactlyOnePersistentChoiceAtEachThreshold() {
        var history = OrganHistory.restore(new OrganHistory.Snapshot(31, 31, List.of()));
        assertEquals(OrganHistory.Level.L0, history.snapshot().level());
        assertEquals(
                OrganHistory.ChoiceResult.LEVEL_REQUIRED,
                history.choose(OrganHistory.GrowthChoice.INCUBATION));

        assertEquals(OrganHistory.CompletionResult.COMPLETED, history.completeBatch(32));
        assertEquals(OrganHistory.Level.L1, history.snapshot().level());
        assertEquals(
                OrganHistory.ChoiceResult.CHOSEN,
                history.choose(OrganHistory.GrowthChoice.INCUBATION));
        assertEquals(
                OrganHistory.ChoiceResult.LEVEL_CHOICE_ALREADY_USED,
                history.choose(OrganHistory.GrowthChoice.WATER_ECONOMY));

        var levelTwo = OrganHistory.restore(new OrganHistory.Snapshot(
                128, 128, List.of(OrganHistory.GrowthChoice.INCUBATION)));
        assertEquals(OrganHistory.ChoiceResult.CHOSEN, levelTwo.choose(OrganHistory.GrowthChoice.WATER_ECONOMY));
        assertEquals(
                OrganHistory.ChoiceResult.LEVEL_CHOICE_ALREADY_USED,
                levelTwo.choose(OrganHistory.GrowthChoice.INCUBATION));

        var levelThree = OrganHistory.restore(new OrganHistory.Snapshot(
                512,
                512,
                List.of(OrganHistory.GrowthChoice.INCUBATION, OrganHistory.GrowthChoice.WATER_ECONOMY)));
        assertEquals(OrganHistory.ChoiceResult.CHOSEN, levelThree.choose(OrganHistory.GrowthChoice.INCUBATION));
        assertEquals(
                OrganHistory.ChoiceResult.CHOICE_CAP_REACHED,
                levelThree.choose(OrganHistory.GrowthChoice.WATER_ECONOMY));

        var restored = OrganHistory.restore(levelThree.snapshot());
        assertEquals(levelThree.snapshot(), restored.snapshot());
        assertEquals(2, restored.snapshot().choiceCount(OrganHistory.GrowthChoice.INCUBATION));
        assertEquals(1, restored.snapshot().choiceCount(OrganHistory.GrowthChoice.WATER_ECONOMY));
    }

    @Test
    void aBatchIdentifierCanEarnAtMostOneCount() {
        var history = new OrganHistory();
        assertEquals(OrganHistory.CompletionResult.COMPLETED, history.completeBatch(1));
        assertEquals(OrganHistory.CompletionResult.ALREADY_COMPLETED, history.completeBatch(1));
        assertEquals(1, history.snapshot().completedBatches());
    }

    @Test
    void malformedPersistedHistoriesAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> OrganHistory.restore(new OrganHistory.Snapshot(
                        31, 31, List.of(OrganHistory.GrowthChoice.INCUBATION))));
        assertThrows(
                IllegalArgumentException.class,
                () -> OrganHistory.restore(new OrganHistory.Snapshot(
                        512,
                        512,
                        List.of(
                                OrganHistory.GrowthChoice.INCUBATION,
                                OrganHistory.GrowthChoice.INCUBATION,
                                OrganHistory.GrowthChoice.INCUBATION,
                                OrganHistory.GrowthChoice.WATER_ECONOMY))));
    }
}
