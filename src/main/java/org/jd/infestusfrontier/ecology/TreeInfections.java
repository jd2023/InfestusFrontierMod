package org.jd.infestusfrontier.ecology;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import org.jd.infestusfrontier.core.ecology.TreeDiscovery;
import org.jd.infestusfrontier.core.foundation.TickQuota;
import org.jd.infestusfrontier.ecology.api.TreeInfectionSnapshot;
import org.slf4j.Logger;

/**
 * One dimension's tree infection work (VEGETATION_INFECTION.md, "Tree boundaries"): at most {@value #JOBS} jobs,
 * each discovering one rooted tree and then converting it, log by log and leaf by leaf, out of budgets every
 * dimension shares, {@value #READS_PER_TICK} discovery reads and {@value #REPLACEMENTS_PER_TICK} replacements a
 * server tick, held on the overworld's instance. A tree touched while a job already claims its root is not a second
 * job. A tree touched while the jobs are full is refused, and the refusal is shown to the players near it; there is
 * no queue, and the next wave or culture asks again. A job pauses while a block it must convert is in no loaded
 * chunk, asks again {@value #UNLOADED_RETRY_TICKS} ticks later and loads nothing. Every target is read again before
 * it is replaced, so a block a player changed is left alone.
 *
 * <p>Saved: each job's root, species, and the logs and leaves still to convert; a job still discovering saves only
 * its root and species and discovers again on load. A saved list that cannot be read is kept as it was and saved
 * back unchanged. Limits: ENGINEERING.md, "Tree infection".
 */
final class TreeInfections extends SavedData {
    static final String NAME = "infestusfrontier_tree_infections";
    static final int JOBS = 16;
    static final int READS_PER_TICK = 128;
    static final int REPLACEMENTS_PER_TICK = 16;
    static final int UNLOADED_RETRY_TICKS = 100;
    /** How far from a tree a player hears about it. */
    static final int FEEDBACK_RANGE = 32;

    private static final int REFUSALS_KEPT = 16;
    private static final int REFUSAL_QUIET_TICKS = 200;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String JOBS_TAG = "jobs";
    private static final String KEPT = "unreadable_jobs";
    private static final SavedData.Factory<TreeInfections> FACTORY =
            new SavedData.Factory<>(TreeInfections::new, TreeInfections::load);
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int STEP_PARTICLES = 5;

    private record SavedJob(
            BlockPos root, TreeSource species, boolean discovering, List<BlockPos> logs, List<BlockPos> leaves) {
        static final Codec<SavedJob> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        BlockPos.CODEC.fieldOf("root").forGetter(SavedJob::root),
                        StringRepresentable.fromEnum(TreeSource::values)
                                .fieldOf("species")
                                .forGetter(SavedJob::species),
                        Codec.BOOL.fieldOf("discovering").forGetter(SavedJob::discovering),
                        BlockPos.CODEC
                                .listOf(0, TreeDiscovery.Limits.DEFAULT.logs())
                                .fieldOf("logs")
                                .forGetter(SavedJob::logs),
                        BlockPos.CODEC
                                .listOf(0, TreeDiscovery.Limits.DEFAULT.leaves())
                                .fieldOf("leaves")
                                .forGetter(SavedJob::leaves))
                .apply(instance, SavedJob::new));
    }

    private static final class Job {
        final BlockPos root;
        final TreeSource species;
        boolean discovering;
        Optional<TreeDiscovery> discovery = Optional.empty();
        final ArrayDeque<BlockPos> logs = new ArrayDeque<>();
        final ArrayDeque<BlockPos> leaves = new ArrayDeque<>();
        /**
         * The logs the job admitted, kept until the job ends, for telling a second touch or a second discovery of
         * the same tree from a new one.
         */
        final Set<BlockPos> claimed = new HashSet<>();

        long pausedUntil = Long.MIN_VALUE;

        Job(BlockPos root, TreeSource species, boolean discovering) {
            this.root = root;
            this.species = species;
            this.discovering = discovering;
        }

        boolean claims(BlockPos pos) {
            return root.equals(pos)
                    || claimed.contains(pos)
                    || discovery.map(d -> d.claims(TreeInfection.position(pos))).orElse(false);
        }

        SavedJob saved() {
            return new SavedJob(root, species, discovering, List.copyOf(logs), List.copyOf(leaves));
        }
    }

    private record Refusal(BlockPos root, TreeInfectionSnapshot.Reason reason, long tick) {}

    private final List<Job> jobs = new ArrayList<>();
    private final ArrayDeque<Refusal> refusals = new ArrayDeque<>();
    private Optional<Tag> unreadable = Optional.empty();
    private int turn;
    /** The server-wide budgets; read from the overworld's instance only. */
    private final TickQuota reads = new TickQuota(READS_PER_TICK);

    private final TickQuota replacements = new TickQuota(REPLACEMENTS_PER_TICK);

    static TreeInfections get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    static TreeInfections load(CompoundTag tag, HolderLookup.Provider registries) {
        TreeInfections loaded = new TreeInfections();
        if (tag.contains(KEPT)) loaded.unreadable = Optional.of(tag.get(KEPT).copy());
        if (!tag.contains(JOBS_TAG)) return loaded;
        DataResult<List<SavedJob>> read = SavedJob.CODEC
                .listOf(0, JOBS)
                .parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get(JOBS_TAG));
        read.ifSuccess(saved -> {
            for (SavedJob job : saved) {
                Job restored = new Job(job.root(), job.species(), job.discovering());
                restored.logs.addAll(job.logs());
                restored.leaves.addAll(job.leaves());
                restored.claimed.addAll(job.logs());
                loaded.jobs.add(restored);
            }
        });
        read.ifError(error -> {
            LOGGER.warn("Tree infections keep an unreadable job list: {}", error.message());
            loaded.unreadable = Optional.of(tag.get(JOBS_TAG).copy());
        });
        return loaded;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        Tag encoded = SavedJob.CODEC
                .listOf()
                .encodeStart(
                        registries.createSerializationContext(NbtOps.INSTANCE),
                        jobs.stream().map(Job::saved).toList())
                .getOrThrow();
        tag.put(JOBS_TAG, encoded);
        unreadable.ifPresent(kept -> tag.put(KEPT, kept.copy()));
        return tag;
    }

    /**
     * Starts a job on the tree rooted at {@code root}, unless a job already claims that log, or the dimension holds
     * its {@value #JOBS} jobs, in which case the players nearby are told and nothing is kept.
     */
    void request(ServerLevel level, TreeInfection.Root root) {
        for (Job job : jobs) if (job.claims(root.pos())) return;
        long now = level.getServer().getTickCount();
        if (jobs.size() >= JOBS) {
            refuse(level, root.pos(), root.species(), TreeInfectionSnapshot.Reason.CAPACITY, now);
            return;
        }
        jobs.add(new Job(root.pos(), root.species(), true));
        setDirty();
    }

    /** One server tick of this dimension's work, out of the server-wide budgets. */
    void advance(ServerLevel level) {
        if (jobs.isEmpty()) return;
        long now = level.getServer().getTickCount();
        TreeInfections server = get(level.getServer().overworld());
        turn++;
        for (Job job : rotation()) {
            if (!job.discovering) continue;
            if (job.discovery.isEmpty())
                job.discovery =
                        Optional.of(new TreeDiscovery(TreeInfection.position(job.root), TreeDiscovery.Limits.DEFAULT));
            TreeDiscovery discovery = job.discovery.get();
            int wanted = Math.max(1, TreeDiscovery.Limits.DEFAULT.examined() - discovery.examined());
            int granted = server.reads.grant(now, wanted);
            if (granted == 0) break;
            discovery.advance(granted, TreeInfection.terrain(level, job.species));
            if (discovery.done()) settle(level, job, discovery.outcome().orElseThrow(), now);
        }
        boolean progressed = true;
        while (progressed) {
            progressed = false;
            for (Job job : rotation()) {
                if (job.discovering || job.pausedUntil > now) continue;
                if (!server.replacements.take(now)) return;
                progressed |= replaceOne(level, job, now);
            }
        }
    }

    private List<Job> rotation() {
        List<Job> order = new ArrayList<>(jobs.size());
        if (jobs.isEmpty()) return order;
        int start = Math.floorMod(turn, jobs.size());
        for (int i = 0; i < jobs.size(); i++) order.add(jobs.get((start + i) % jobs.size()));
        return order;
    }

    private void settle(ServerLevel level, Job job, TreeDiscovery.Outcome outcome, long now) {
        switch (outcome) {
            case TreeDiscovery.Refused refused -> {
                jobs.remove(job);
                setDirty();
                switch (refused.reason()) {
                    case MISSING -> {}
                    case OVERSIZED -> refuse(level, job.root, job.species, TreeInfectionSnapshot.Reason.OVERSIZED, now);
                    case AMBIGUOUS -> refuse(level, job.root, job.species, TreeInfectionSnapshot.Reason.AMBIGUOUS, now);
                    case UNLOADED -> refuse(level, job.root, job.species, TreeInfectionSnapshot.Reason.UNLOADED, now);
                }
            }
            case TreeDiscovery.Admitted admitted -> {
                List<BlockPos> logs =
                        admitted.logs().stream().map(TreeInfection::pos).toList();
                for (Job other : jobs) {
                    if (other == job || other.discovering) continue;
                    if (logs.stream().anyMatch(other.claimed::contains)) {
                        jobs.remove(job);
                        setDirty();
                        return;
                    }
                }
                job.discovering = false;
                job.discovery = Optional.empty();
                job.logs.addAll(logs);
                job.claimed.addAll(logs);
                admitted.leaves().stream().map(TreeInfection::pos).forEach(job.leaves::add);
                setDirty();
                tell(
                        level,
                        job.root,
                        Component.translatable(
                                "infestusfrontier.tree_infection.admitted",
                                species(job.species),
                                job.logs.size(),
                                job.leaves.size()));
            }
        }
    }

    /**
     * Converts the job's next block if it still is the log or leaf discovery saw; drops it from the job either way.
     * False when the job is paused on an unloaded chunk or has nothing left.
     */
    private boolean replaceOne(ServerLevel level, Job job, long now) {
        ArrayDeque<BlockPos> queue = job.logs.isEmpty() ? job.leaves : job.logs;
        if (queue.isEmpty()) {
            jobs.remove(job);
            setDirty();
            return false;
        }
        BlockPos target = queue.peek();
        if (!level.hasChunkAt(target)) {
            job.pausedUntil = now + UNLOADED_RETRY_TICKS;
            return false;
        }
        BlockState state = level.getBlockState(target);
        queue.poll();
        if (job.logs.isEmpty() && job.leaves.isEmpty()) jobs.remove(job);
        setDirty();
        Optional<BlockState> infected = TreeInfection.infected(state, job.species);
        if (infected.isEmpty()) return true;
        level.setBlock(target, infected.get(), QUIET);
        if (infected.get().is(EcologyModule.INFECTED_LOG.get()))
            level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target, STEP_PARTICLES);
        return true;
    }

    /** Remembers the refusal and tells the players nearby, once per tree and reason in {@value #REFUSAL_QUIET_TICKS} ticks. */
    private void refuse(
            ServerLevel level, BlockPos root, TreeSource species, TreeInfectionSnapshot.Reason reason, long now) {
        for (Refusal recent : refusals)
            if (recent.root().equals(root) && recent.reason() == reason && now - recent.tick() < REFUSAL_QUIET_TICKS)
                return;
        if (refusals.size() == REFUSALS_KEPT) refusals.removeFirst();
        refusals.addLast(new Refusal(root, reason, now));
        String key = "infestusfrontier.tree_infection." + reason.name().toLowerCase(java.util.Locale.ROOT);
        Component message = reason == TreeInfectionSnapshot.Reason.CAPACITY
                ? Component.translatable(key, JOBS)
                : Component.translatable(key, species(species), root.getX(), root.getY(), root.getZ());
        tell(level, root, message);
    }

    private static Component species(TreeSource species) {
        return Component.translatable("infestusfrontier.tree_infection.species." + species.getSerializedName());
    }

    private static void tell(ServerLevel level, BlockPos at, Component message) {
        for (ServerPlayer player : level.players())
            if (player.blockPosition().closerThan(at, FEEDBACK_RANGE)) player.displayClientMessage(message, true);
    }

    TreeInfectionSnapshot snapshot() {
        List<TreeInfectionSnapshot.Job> work = new ArrayList<>(jobs.size());
        for (Job job : jobs)
            work.add(new TreeInfectionSnapshot.Job(
                    job.root,
                    job.species.getSerializedName(),
                    job.discovering ? TreeInfectionSnapshot.Phase.DISCOVERING : TreeInfectionSnapshot.Phase.CONVERTING,
                    job.logs.size(),
                    job.leaves.size()));
        List<TreeInfectionSnapshot.Refusal> refused = new ArrayList<>(refusals.size());
        for (Refusal refusal : refusals)
            refused.add(new TreeInfectionSnapshot.Refusal(refusal.root(), refusal.reason()));
        return new TreeInfectionSnapshot(work, refused);
    }
}
