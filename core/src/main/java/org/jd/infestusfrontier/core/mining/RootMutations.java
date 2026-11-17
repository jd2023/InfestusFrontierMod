package org.jd.infestusfrontier.core.mining;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.jd.infestusfrontier.core.foundation.Refusal;

/**
 * The mutations installed on one Tunnel Root head, in their fixed order. Installation is permanent: nothing removes
 * one, and each is installed once. A head takes {@link RootMutation#WIDER_SECTION} only once it is a Rootstock. A
 * value that breaks these rules does not construct, so impossible saved mutations are refused, not repaired.
 */
public record RootMutations(List<RootMutation> installed) {
    public static final RootMutations NONE = new RootMutations(List.of());

    public RootMutations {
        installed = List.copyOf(installed);
        Optional<String> problem = problem(installed);
        if (problem.isPresent()) throw new IllegalArgumentException(problem.get());
    }

    /** Why {@code installed} cannot be one head's mutations, or empty when it can. Never throws. */
    public static Optional<String> problem(List<RootMutation> installed) {
        EnumSet<RootMutation> seen = EnumSet.noneOf(RootMutation.class);
        for (RootMutation mutation : installed) {
            if (!seen.add(mutation)) return Optional.of("twice " + mutation.id());
            Optional<RootMutation> needed = mutation.requires();
            if (needed.isPresent() && !seen.contains(needed.get()))
                return Optional.of(mutation.id() + " without " + needed.get().id());
        }
        return Optional.empty();
    }

    public boolean has(RootMutation mutation) {
        return installed.contains(mutation);
    }

    /** True for a Descending Rootstock: a head with {@link RootMutation#STAIRS}. */
    public boolean rootstock() {
        return has(RootMutation.STAIRS);
    }

    /** Why this head cannot take {@code mutation} now, or empty when it can. Reads, changes nothing. */
    public Optional<Refusal> refusal(RootMutation mutation) {
        if (has(mutation)) return Optional.of(Tunnelling.MUTATION_INSTALLED);
        Optional<RootMutation> needed = mutation.requires();
        if (needed.isPresent() && !has(needed.get())) return Optional.of(Tunnelling.NOT_ROOTSTOCK);
        return Optional.empty();
    }

    /** These mutations and {@code mutation}. Only for a mutation {@link #refusal} admits. */
    public RootMutations with(RootMutation mutation) {
        Optional<Refusal> refused = refusal(mutation);
        if (refused.isPresent()) throw new IllegalStateException(refused.get().key() + " for " + mutation.id());
        List<RootMutation> after = new ArrayList<>(installed);
        after.add(mutation);
        return new RootMutations(after);
    }

    /** One bit per mutation by its ordinal, for a synced int. */
    public int bits() {
        int bits = 0;
        for (RootMutation mutation : installed) bits |= 1 << mutation.ordinal();
        return bits;
    }

    /** The mutations {@code bits} name, in their fixed order; bits no mutation has are ignored. */
    public static List<RootMutation> fromBits(int bits) {
        List<RootMutation> named = new ArrayList<>();
        for (RootMutation mutation : RootMutation.values())
            if ((bits & (1 << mutation.ordinal())) != 0) named.add(mutation);
        return named;
    }
}
