package org.jd.infestusfrontier.ecology;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
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
 * it is replaced and must still be the block and state discovery read there, its leaf distance aside, so a block a
 * player changed is left alone. Discovery never takes a root that is no longer the log contact found.
 *
 * <p>Saved: each job's root, the kind of its logs and the root's state as contact found it; for a converting job the
 * logs and leaves still to convert, each with the state discovery read there, as an index into a table of the job's
 * distinct states. A job still discovering saves no targets and discovers again on load. A job an older save names by
 * one of six species reads as that species' vanilla log and leaves, with no states: it checks its targets by block, as
 * that save did. A record is read in exactly one format; a saved list that cannot be read is kept as it was and saved
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

    /**
     * The six species an older save names, and the vanilla log and leaves each stood for: read so that a job saved
     * before trees were told apart by block keeps converting exactly those blocks. Never written.
     */
    private static final Map<String, LegacySpecies> LEGACY_SPECIES = Map.of(
            "oak", new LegacySpecies(Blocks.OAK_LOG, Blocks.OAK_LEAVES),
            "birch", new LegacySpecies(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES),
            "spruce", new LegacySpecies(Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES),
            "jungle", new LegacySpecies(Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES),
            "acacia", new LegacySpecies(Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES),
            "dark_oak", new LegacySpecies(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES));

    private record LegacySpecies(Block log, Block leaves) {}

    /** The most distinct block states one job's targets hold: one a target at most. */
    private static final int STATES = TreeDiscovery.Limits.DEFAULT.logs() + TreeDiscovery.Limits.DEFAULT.leaves();

    /** The keys only a job of the current format has; a record with any of them is never read as a legacy one. */
    private static final List<String> CURRENT_KEYS =
            List.of("log", "root_state", "states", "log_states", "leaf_states", "identity_only");

    /**
     * A saved block state read exactly: vanilla's codec puts a default in place of a property it cannot read, so the
     * state it decodes must write back the very properties the record holds, names, types and values, or the record
     * is an error. A guessed state would let a job convert a block it never saw.
     */
    private static final Codec<BlockState> EXACT_STATE = new Codec<>() {
        private static final Codec<Map<String, String>> PROPERTIES = Codec.unboundedMap(Codec.STRING, Codec.STRING);

        @Override
        public <T> DataResult<Pair<BlockState, T>> decode(DynamicOps<T> ops, T input) {
            return BlockState.CODEC
                    .decode(ops, input)
                    .flatMap(decoded -> ops.getMap(input)
                            .flatMap(map -> {
                                T given = map.get("Properties");
                                return given == null
                                        ? DataResult.success(Map.<String, String>of())
                                        : PROPERTIES.parse(ops, given);
                            })
                            .flatMap(given -> given.equals(properties(decoded.getFirst()))
                                    ? DataResult.success(decoded)
                                    : DataResult.error(
                                            () -> "a state whose properties do not read exactly: " + input)));
        }

        @Override
        public <T> DataResult<T> encode(BlockState state, DynamicOps<T> ops, T prefix) {
            return BlockState.CODEC.encode(state, ops, prefix);
        }
    };

    /** The properties {@code state} is written with, by name. */
    private static Map<String, String> properties(BlockState state) {
        Map<String, String> written = new HashMap<>();
        for (Property<?> property : state.getProperties()) written.put(property.getName(), valueName(state, property));
        return written;
    }

    private static <V extends Comparable<V>> String valueName(BlockState state, Property<V> property) {
        return property.getName(state.getValue(property));
    }

    /** A log or leaf still to convert and the index, in the job's states, of what it was when discovery read it. */
    private record Target(BlockPos pos, int state) {}

    /**
     * A job as saved: the root, the kind of its logs and, unless the job came from an older save, the root's state as
     * contact saw it; for a converting job the logs and leaves still to convert, each with the index of its expected
     * state in {@code states}. {@code identityOnly} marks targets an older save left without states: those are checked
     * by block alone, as that save did.
     */
    private record SavedJob(
            BlockPos root,
            ResourceLocation log,
            Optional<BlockState> rootState,
            boolean discovering,
            List<BlockState> states,
            List<BlockPos> logs,
            List<Integer> logStates,
            List<BlockPos> leaves,
            List<Integer> leafStates,
            boolean identityOnly) {
        static final Codec<SavedJob> CURRENT = RecordCodecBuilder.<SavedJob>create(instance -> instance.group(
                                BlockPos.CODEC.fieldOf("root").forGetter(SavedJob::root),
                                ResourceLocation.CODEC.fieldOf("log").forGetter(SavedJob::log),
                                EXACT_STATE.optionalFieldOf("root_state").forGetter(SavedJob::rootState),
                                Codec.BOOL.fieldOf("discovering").forGetter(SavedJob::discovering),
                                EXACT_STATE.listOf(0, STATES).fieldOf("states").forGetter(SavedJob::states),
                                BlockPos.CODEC
                                        .listOf(0, TreeDiscovery.Limits.DEFAULT.logs())
                                        .fieldOf("logs")
                                        .forGetter(SavedJob::logs),
                                Codec.INT
                                        .listOf(0, TreeDiscovery.Limits.DEFAULT.logs())
                                        .fieldOf("log_states")
                                        .forGetter(SavedJob::logStates),
                                BlockPos.CODEC
                                        .listOf(0, TreeDiscovery.Limits.DEFAULT.leaves())
                                        .fieldOf("leaves")
                                        .forGetter(SavedJob::leaves),
                                Codec.INT
                                        .listOf(0, TreeDiscovery.Limits.DEFAULT.leaves())
                                        .fieldOf("leaf_states")
                                        .forGetter(SavedJob::leafStates),
                                Codec.BOOL.fieldOf("identity_only").forGetter(SavedJob::identityOnly))
                        .apply(instance, SavedJob::new))
                .validate(SavedJob::consistent);

        /** The format before trees were told apart by block: one of six species names for logs and leaves. */
        static final Codec<SavedJob> LEGACY = RecordCodecBuilder.<SavedJob>create(instance -> instance.group(
                        BlockPos.CODEC.fieldOf("root").forGetter(SavedJob::root),
                        Codec.STRING
                                .validate(name -> LEGACY_SPECIES.containsKey(name)
                                        ? DataResult.success(name)
                                        : DataResult.error(() -> "unknown species " + name))
                                .fieldOf("species")
                                .forGetter(job -> ""),
                        Codec.BOOL.fieldOf("discovering").forGetter(SavedJob::discovering),
                        BlockPos.CODEC
                                .listOf(0, TreeDiscovery.Limits.DEFAULT.logs())
                                .fieldOf("logs")
                                .forGetter(SavedJob::logs),
                        BlockPos.CODEC
                                .listOf(0, TreeDiscovery.Limits.DEFAULT.leaves())
                                .fieldOf("leaves")
                                .forGetter(SavedJob::leaves))
                .apply(instance, (root, species, discovering, logs, leaves) -> {
                    LegacySpecies blocks = LEGACY_SPECIES.get(species);
                    // no states were saved: the targets are the species' blocks, checked by block as that save did
                    return new SavedJob(
                            root,
                            BuiltInRegistries.BLOCK.getKey(blocks.log()),
                            Optional.empty(),
                            discovering,
                            List.of(
                                    blocks.log().defaultBlockState(),
                                    blocks.leaves().defaultBlockState()),
                            logs,
                            Collections.nCopies(logs.size(), 0),
                            leaves,
                            Collections.nCopies(leaves.size(), 1),
                            true);
                }));

        /**
         * Read as {@link #LEGACY} when the record names a species and nothing of the current format, as
         * {@link #CURRENT} when it names a log and no species; anything else, a mixture included, is an error. A record
         * that fails its format is never tried as the other. Written as {@link #CURRENT}.
         */
        static final Codec<SavedJob> CODEC = new Codec<>() {
            @Override
            public <T> DataResult<Pair<SavedJob, T>> decode(DynamicOps<T> ops, T input) {
                return ops.getMap(input).flatMap(map -> {
                    boolean species = map.get("species") != null;
                    boolean current = CURRENT_KEYS.stream().anyMatch(key -> map.get(key) != null);
                    if (species && current) return DataResult.error(() -> "a job of two formats: " + input);
                    if (species) return LEGACY.decode(ops, input);
                    if (current) return CURRENT.decode(ops, input);
                    return DataResult.error(() -> "a job of no known format: " + input);
                });
            }

            @Override
            public <T> DataResult<T> encode(SavedJob job, DynamicOps<T> ops, T prefix) {
                return CURRENT.encode(job, ops, prefix);
            }
        };

        /** The indices point into the states, the states of the logs are the job's log, and so is the root's. */
        private static DataResult<SavedJob> consistent(SavedJob job) {
            if (job.logStates().size() != job.logs().size()
                    || job.leafStates().size() != job.leaves().size())
                return DataResult.error(() -> "target states do not match the targets");
            for (int state : job.logStates())
                if (state < 0
                        || state >= job.states().size()
                        || !kindIs(job.states().get(state), job.log()))
                    return DataResult.error(() -> "a log state that is not " + job.log());
            for (int state : job.leafStates())
                if (state < 0 || state >= job.states().size()) return DataResult.error(() -> "no state " + state);
            if (job.rootState().isPresent() && !kindIs(job.rootState().get(), job.log()))
                return DataResult.error(() -> "a root that is not " + job.log());
            return DataResult.success(job);
        }

        private static boolean kindIs(BlockState state, ResourceLocation kind) {
            return TreeInfection.kind(state).equals(kind);
        }
    }

    private static final class Job {
        final BlockPos root;
        /** The kind of log contact found at the root; discovery never takes another. */
        final ResourceLocation log;
        /** The root as contact found it; empty only for a job an older save left, checked by kind and axis. */
        final Optional<BlockState> rootState;

        boolean discovering;
        Optional<TreeDiscovery> discovery = Optional.empty();
        /** What discovery read at each log and leaf it met, for the targets it admits; empty once it settles. */
        final Map<BlockPos, BlockState> seen = new HashMap<>();
        /** The distinct states the targets expect, indexed by {@link Target#state}. */
        final List<BlockState> states = new ArrayList<>();
        /** True for targets an older save left without states: they are checked by block alone. */
        boolean identityOnly;

        final ArrayDeque<Target> logs = new ArrayDeque<>();
        final ArrayDeque<Target> leaves = new ArrayDeque<>();
        /**
         * The logs the job admitted, kept until the job ends, for telling a second touch or a second discovery of
         * the same tree from a new one.
         */
        final Set<BlockPos> claimed = new HashSet<>();

        long pausedUntil = Long.MIN_VALUE;

        Job(BlockPos root, ResourceLocation log, Optional<BlockState> rootState, boolean discovering) {
            this.root = root;
            this.log = log;
            this.rootState = rootState;
            this.discovering = discovering;
        }

        boolean claims(BlockPos pos) {
            return root.equals(pos)
                    || claimed.contains(pos)
                    || discovery.map(d -> d.claims(TreeInfection.position(pos))).orElse(false);
        }

        /** True while the root is still the log contact found. */
        boolean rootStands(BlockState now) {
            return rootState
                    .map(state -> state == now)
                    .orElseGet(() -> TreeInfection.kind(now).equals(log)
                            && now.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y);
        }

        SavedJob saved() {
            return new SavedJob(
                    root,
                    log,
                    rootState,
                    discovering,
                    List.copyOf(states),
                    logs.stream().map(Target::pos).toList(),
                    logs.stream().map(Target::state).toList(),
                    leaves.stream().map(Target::pos).toList(),
                    leaves.stream().map(Target::state).toList(),
                    identityOnly);
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
                Job restored = new Job(job.root(), job.log(), job.rootState(), job.discovering());
                restored.states.addAll(job.states());
                restored.identityOnly = job.identityOnly();
                for (int i = 0; i < job.logs().size(); i++)
                    restored.logs.add(
                            new Target(job.logs().get(i), job.logStates().get(i)));
                for (int i = 0; i < job.leaves().size(); i++)
                    restored.leaves.add(
                            new Target(job.leaves().get(i), job.leafStates().get(i)));
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
            refuse(level, root.pos(), root.log(), TreeInfectionSnapshot.Reason.CAPACITY, now);
            return;
        }
        jobs.add(new Job(root.pos(), root.log(), Optional.of(root.state()), true));
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
            discovery.advance(granted, TreeInfection.terrain(level, job.root, job::rootStands, job.seen::put));
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
                    case OVERSIZED -> refuse(level, job.root, job.log, TreeInfectionSnapshot.Reason.OVERSIZED, now);
                    case AMBIGUOUS -> refuse(level, job.root, job.log, TreeInfectionSnapshot.Reason.AMBIGUOUS, now);
                    case UNLOADED -> refuse(level, job.root, job.log, TreeInfectionSnapshot.Reason.UNLOADED, now);
                }
            }
            case TreeDiscovery.Admitted admitted -> {
                List<BlockPos> logs = admitted.logs().stream()
                        .map(part -> TreeInfection.pos(part.position()))
                        .toList();
                for (Job other : jobs) {
                    if (other == job || other.discovering) continue;
                    if (logs.stream().anyMatch(other.claimed::contains)) {
                        jobs.remove(job);
                        setDirty();
                        return;
                    }
                }
                // the targets and their states are built anew from what this discovery read, whatever the job held
                job.discovering = false;
                job.discovery = Optional.empty();
                job.states.clear();
                job.logs.clear();
                job.leaves.clear();
                job.identityOnly = false;
                Map<BlockState, Integer> index = new HashMap<>();
                for (TreeDiscovery.Part part : admitted.logs())
                    target(job, index, part).ifPresent(job.logs::add);
                for (TreeDiscovery.Part part : admitted.leaves())
                    target(job, index, part).ifPresent(job.leaves::add);
                job.seen.clear();
                job.claimed.addAll(logs);
                setDirty();
                tell(
                        level,
                        job.root,
                        Component.translatable(
                                "infestusfrontier.tree_infection.admitted",
                                name(job.log),
                                job.logs.size(),
                                job.leaves.size()));
            }
        }
    }

    /** The admitted part as a target expecting the state discovery read there; the state joins the job's table. */
    private static Optional<Target> target(Job job, Map<BlockState, Integer> index, TreeDiscovery.Part part) {
        BlockPos pos = TreeInfection.pos(part.position());
        BlockState read = job.seen.get(pos);
        if (read == null) return Optional.empty();
        int state = index.computeIfAbsent(read, added -> {
            job.states.add(added);
            return job.states.size() - 1;
        });
        return Optional.of(new Target(pos, state));
    }

    /**
     * Converts the job's next block if it still is the log or leaf discovery saw, in block and state; drops it from
     * the job either way. False when the job is paused on an unloaded chunk or has nothing left.
     */
    private boolean replaceOne(ServerLevel level, Job job, long now) {
        boolean log = !job.logs.isEmpty();
        if (!log && job.leaves.isEmpty()) {
            jobs.remove(job);
            setDirty();
            return false;
        }
        Target target = log ? job.logs.peek() : job.leaves.peek();
        if (!level.hasChunkAt(target.pos())) {
            job.pausedUntil = now + UNLOADED_RETRY_TICKS;
            return false;
        }
        BlockState state = level.getBlockState(target.pos());
        BlockState expected = job.states.get(target.state());
        Optional<BlockState> infected = log
                ? TreeInfection.infectedLog(level, target.pos(), state, expected, job.identityOnly)
                : TreeInfection.blightedLeaves(state, expected, job.identityOnly);
        (log ? job.logs : job.leaves).poll();
        if (job.logs.isEmpty() && job.leaves.isEmpty()) jobs.remove(job);
        setDirty();
        if (infected.isEmpty()) return true;
        level.setBlock(target.pos(), infected.get(), QUIET);
        if (log) level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target.pos(), STEP_PARTICLES);
        return true;
    }

    /** Remembers the refusal and tells the players nearby, once per tree and reason in {@value #REFUSAL_QUIET_TICKS} ticks. */
    private void refuse(
            ServerLevel level, BlockPos root, ResourceLocation log, TreeInfectionSnapshot.Reason reason, long now) {
        for (Refusal recent : refusals)
            if (recent.root().equals(root) && recent.reason() == reason && now - recent.tick() < REFUSAL_QUIET_TICKS)
                return;
        if (refusals.size() == REFUSALS_KEPT) refusals.removeFirst();
        refusals.addLast(new Refusal(root, reason, now));
        String key = "infestusfrontier.tree_infection." + reason.name().toLowerCase(java.util.Locale.ROOT);
        Component message = reason == TreeInfectionSnapshot.Reason.CAPACITY
                ? Component.translatable(key, JOBS)
                : Component.translatable(key, name(log), root.getX(), root.getY(), root.getZ());
        tell(level, root, message);
    }

    /** The name of the log a tree is made of, as a player reads it. */
    private static Component name(ResourceLocation log) {
        return BuiltInRegistries.BLOCK.get(log).getName();
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
                    job.log.toString(),
                    job.discovering ? TreeInfectionSnapshot.Phase.DISCOVERING : TreeInfectionSnapshot.Phase.CONVERTING,
                    job.logs.size(),
                    job.leaves.size()));
        List<TreeInfectionSnapshot.Refusal> refused = new ArrayList<>(refusals.size());
        for (Refusal refusal : refusals)
            refused.add(new TreeInfectionSnapshot.Refusal(refusal.root(), refusal.reason()));
        return new TreeInfectionSnapshot(work, refused);
    }
}
