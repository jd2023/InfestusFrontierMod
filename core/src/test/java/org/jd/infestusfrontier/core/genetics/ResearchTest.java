package org.jd.infestusfrontier.core.genetics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ResearchTest {
    private static final String PIG = "minecraft:pig";

    @Test
    void aNewBankHasNoResearchAndAdmitsAnySpecies() {
        assertEquals(0, Research.NONE.points(PIG));
        assertFalse(Research.NONE.complete(PIG));
        assertEquals(Optional.empty(), Research.NONE.refusal(PIG));
    }

    @Test
    void samplesAddTheirPointsUntilTheGenomeIsCompleteAndThenItRefusesMore() {
        Research research = Research.NONE;
        int samples = 0;
        while (!research.complete(PIG)) {
            assertEquals(Optional.empty(), research.refusal(PIG));
            int before = research.points(PIG);
            research = research.extracted(PIG, 10);
            assertEquals(before + 10, research.points(PIG), "each sample adds its points once");
            samples++;
        }
        assertEquals(Genetics.COMPLETE / 10, samples);
        assertEquals(Optional.of(Genetics.GENOME_COMPLETE), research.refusal(PIG));
        Research complete = research;
        assertThrows(IllegalStateException.class, () -> complete.extracted(PIG, 10), "no extraction past complete");
    }

    @Test
    void theLastSampleCompletesTheGenomeWithoutPassingIt() {
        Research research = Research.NONE.extracted(PIG, 95).extracted(PIG, 10);
        assertEquals(Genetics.COMPLETE, research.points(PIG));
        assertTrue(research.complete(PIG));
    }

    @Test
    void aCompleteGenomeStaysCompleteWhileOtherSpeciesAreResearched() {
        Research research = Research.NONE.extracted(PIG, Genetics.COMPLETE).extracted("minecraft:cow", 10);
        assertTrue(research.complete(PIG));
        assertEquals(10, research.points("minecraft:cow"));
        assertEquals(
                List.of(PIG, "minecraft:cow"),
                research.genomes().stream().map(Genome::species).toList());
    }

    @Test
    void aBankResearchesAtMostItsLimitOfSpeciesAndRefusesAnotherWithoutChange() {
        Research research = Research.NONE;
        for (int n = 0; n < Genetics.MAX_GENOMES; n++) research = research.extracted("test:species" + n, 10);
        assertEquals(Optional.of(Genetics.RESEARCH_FULL), research.refusal(PIG));
        assertEquals(Optional.empty(), research.refusal("test:species0"), "a known species still adds");
        Research full = research;
        assertThrows(IllegalStateException.class, () -> full.extracted(PIG, 10));
        assertEquals(Genetics.MAX_GENOMES, full.genomes().size());
    }

    @Test
    void impossibleResearchDoesNotConstruct() {
        assertTrue(Research.problem(List.of(new Genome(PIG, 1), new Genome(PIG, 2)))
                .isPresent());
        List<Genome> tooMany = new java.util.ArrayList<>();
        for (int n = 0; n <= Genetics.MAX_GENOMES; n++) tooMany.add(new Genome("test:species" + n, 1));
        assertTrue(Research.problem(tooMany).isPresent());
        assertThrows(IllegalArgumentException.class, () -> new Research(tooMany));
        assertThrows(IllegalArgumentException.class, () -> new Genome(PIG, 0));
        assertThrows(IllegalArgumentException.class, () -> new Genome(PIG, Genetics.COMPLETE + 1));
        assertThrows(IllegalArgumentException.class, () -> new Genome(" ", 1));
        assertThrows(IllegalArgumentException.class, () -> Research.NONE.extracted(PIG, 0));
    }
}
