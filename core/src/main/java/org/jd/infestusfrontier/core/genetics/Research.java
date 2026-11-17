package org.jd.infestusfrontier.core.genetics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jd.infestusfrontier.core.foundation.Refusal;

/**
 * Everything one DNA Bank has researched: at most {@value Genetics#MAX_GENOMES} genomes, one per species, in the order
 * their first sample was extracted. Research only grows: a genome's points rise to {@value Genetics#COMPLETE} and a
 * complete genome stays complete. A value outside the limits does not construct.
 */
public record Research(List<Genome> genomes) {
    public static final Research NONE = new Research(List.of());

    public Research {
        genomes = List.copyOf(genomes);
        Optional<String> problem = problem(genomes);
        if (problem.isPresent()) throw new IllegalArgumentException(problem.get());
    }

    /** Why {@code genomes} cannot be one bank's research, or empty when they can. Never throws on a bad list. */
    public static Optional<String> problem(List<Genome> genomes) {
        if (genomes.size() > Genetics.MAX_GENOMES) return Optional.of("genomes " + genomes.size());
        Set<String> seen = new HashSet<>();
        for (Genome genome : genomes)
            if (!seen.add(genome.species())) return Optional.of("species twice " + genome.species());
        return Optional.empty();
    }

    /** The points {@code species} has here, 0 when it has none. */
    public int points(String species) {
        return genome(species).map(Genome::points).orElse(0);
    }

    public boolean complete(String species) {
        return genome(species).map(Genome::complete).orElse(false);
    }

    /** Why a sample of {@code species} cannot be extracted here now, or empty when it can. */
    public Optional<Refusal> refusal(String species) {
        Optional<Genome> genome = genome(species);
        if (genome.isPresent())
            return genome.get().complete() ? Optional.of(Genetics.GENOME_COMPLETE) : Optional.empty();
        return genomes.size() >= Genetics.MAX_GENOMES ? Optional.of(Genetics.RESEARCH_FULL) : Optional.empty();
    }

    /**
     * The research after an extraction of {@code species} added {@code points}: a new genome is recorded last, and a
     * genome never passes {@value Genetics#COMPLETE}. Only for a species {@link #refusal} admits.
     */
    public Research extracted(String species, int points) {
        if (points < 1 || points > Genetics.COMPLETE) throw new IllegalArgumentException("Points: " + points);
        Optional<Refusal> refused = refusal(species);
        if (refused.isPresent()) throw new IllegalStateException(refused.get().key() + " for " + species);
        List<Genome> after = new ArrayList<>(genomes.size() + 1);
        boolean found = false;
        for (Genome genome : genomes)
            if (genome.species().equals(species)) {
                after.add(new Genome(species, Math.min(Genetics.COMPLETE, Math.addExact(genome.points(), points))));
                found = true;
            } else after.add(genome);
        if (!found) after.add(new Genome(species, points));
        return new Research(after);
    }

    private Optional<Genome> genome(String species) {
        return genomes.stream()
                .filter(genome -> genome.species().equals(species))
                .findFirst();
    }
}
