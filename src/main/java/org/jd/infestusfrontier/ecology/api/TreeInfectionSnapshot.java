package org.jd.infestusfrontier.ecology.api;

import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * One dimension's tree infection work as it stands: the jobs at work (discovering a tree or converting an admitted
 * one) and the trees refused lately, each with why. An immutable copy; nothing in it is live.
 */
public record TreeInfectionSnapshot(List<Job> jobs, List<Refusal> refusals) {
    public TreeInfectionSnapshot {
        jobs = List.copyOf(jobs);
        refusals = List.copyOf(refusals);
    }

    public enum Phase {
        DISCOVERING,
        CONVERTING
    }

    /** Why a tree was refused: the dimension holds its 16 jobs, or discovery found the tree too large, joined to another species or partly unloaded. */
    public enum Reason {
        CAPACITY,
        OVERSIZED,
        AMBIGUOUS,
        UNLOADED
    }

    /** A job on the tree rooted at {@code root}; the counts are what is still to convert, 0 while discovering. */
    public record Job(BlockPos root, String species, Phase phase, int logsLeft, int leavesLeft) {}

    public record Refusal(BlockPos root, Reason reason) {}

    /** The job rooted at {@code root}, if one is at work. */
    public boolean works(BlockPos root) {
        return jobs.stream().anyMatch(job -> job.root().equals(root));
    }
}
