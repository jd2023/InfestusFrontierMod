package org.jd.infestusfrontier.testmod;

import static org.jd.infestusfrontier.testmod.PalmNet.chest;
import static org.jd.infestusfrontier.testmod.PalmNet.count;
import static org.jd.infestusfrontier.testmod.PalmNet.mouth;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.MouthBlock;
import org.jd.infestusfrontier.logistics.MouthMenu;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.logistics.RoutingCounts;
import org.jd.infestusfrontier.logistics.RoutingCounts.Step;
import org.jd.infestusfrontier.logistics.api.ItemAccess;
import org.jd.infestusfrontier.logistics.api.LogisticsCapabilities;

/**
 * The work item routing does, counted where it is done ({@link RoutingCounts}). An organ's Mouth is a Mouth in Both
 * mode with no store, called through {@link ItemAccess} in one tick so that Mouth pulses cannot fall inside a measured
 * call. The far Mouths take only (Intake), so they never pulse. Each test has its own batch: the counts are global.
 * What every admitted call may do on a settled network: look at {@link ItemAccess#CANDIDATES} far Mouths at most, and
 * walk, rank and build nothing. A call the shared admission refuses does no routing work at all. Counts only; no
 * timing is measured here.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class RoutingWorkTests {
    private RoutingWorkTests() {}

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final int Y = 1;
    private static final int SETTLE = 10;
    private static final String STICKS = "{filter:[{id:\"minecraft:stick\",count:1}]}";

    /** The counts of one stretch of work. */
    private record Work(long nodes, long lookups, long ranked, long builds, long inventory) {
        static Work now() {
            return new Work(
                    RoutingCounts.get(Step.NODES),
                    RoutingCounts.get(Step.LOOKUPS),
                    RoutingCounts.get(Step.RANKED),
                    RoutingCounts.get(Step.BUILDS),
                    RoutingCounts.get(Step.INVENTORY));
        }

        Work since(Work before) {
            return new Work(
                    nodes - before.nodes,
                    lookups - before.lookups,
                    ranked - before.ranked,
                    builds - before.builds,
                    inventory - before.inventory);
        }

        Work plus(Work other) {
            return new Work(
                    nodes + other.nodes,
                    lookups + other.lookups,
                    ranked + other.ranked,
                    builds + other.builds,
                    inventory + other.inventory);
        }

        boolean none() {
            return equals(new Work(0, 0, 0, 0, 0));
        }

        @Override
        public String toString() {
            return nodes + " nodes, " + lookups + " lookups, " + ranked + " ranked, " + builds + " builds, " + inventory
                    + " inventory ops";
        }
    }

    /** The outcome of the calls of one measured stretch. */
    private static final class Tally {
        int admitted;
        int refused;
        int moved;
        int items;
        int worst;
        Work total = new Work(0, 0, 0, 0, 0);

        /** The counts of the stretch, in the server log: counts only, no timing. */
        void report(String test) {
            LOGGER.info(
                    "routing-work {}: {} admitted, {} refused, {} moved something ({} items), at most {} lookups a call; total {}",
                    test,
                    admitted,
                    refused,
                    moved,
                    items,
                    worst,
                    total);
        }

        /** Records one call that left {@code spent} behind: an admitted call may only look, a refused one nothing. */
        void call(GameTestHelper h, boolean admitted, Work spent, int movedItems) {
            total = total.plus(spent);
            if (!admitted) {
                refused++;
                h.assertTrue(spent.none(), "a refused call does no routing work, saw " + spent);
                return;
            }
            this.admitted++;
            h.assertTrue(
                    spent.nodes() == 0 && spent.ranked() == 0 && spent.builds() == 0,
                    "a settled network is neither walked, ranked nor rebuilt, saw " + spent);
            h.assertTrue(
                    spent.lookups() <= ItemAccess.CANDIDATES,
                    "at most " + ItemAccess.CANDIDATES + " far Mouths looked up a call, saw " + spent.lookups());
            h.assertTrue(
                    spent.inventory() <= 3 * ItemAccess.CANDIDATES,
                    "a simulated and a real insert for the Mouths looked at, saw " + spent.inventory());
            worst = Math.max(worst, (int) spent.lookups());
            if (movedItems > 0) {
                moved++;
                items += movedItems;
            }
        }
    }

    private static ItemAccess access(GameTestHelper h, BlockPos cell) {
        return h.getLevel().getCapability(LogisticsCapabilities.ITEM_ACCESS, h.absolutePos(cell), null);
    }

    private static boolean admitted(ItemAccess.Sent sent) {
        return sent.consulted() && !sent.why().equals(java.util.Optional.of(ItemAccess.BUSY));
    }

    /** One push of {@code count} of {@code item} by the Mouth at {@code door}, tallied. */
    private static void push(GameTestHelper h, Tally tally, ItemAccess door, Item item, int count, boolean simulate) {
        ItemStack stack = new ItemStack(item, count);
        Work before = Work.now();
        ItemAccess.Sent sent = door.push(List.of(new ItemAccess.Offer(stack, count)), simulate);
        Work spent = Work.now().since(before);
        int moved = count - sent.rests().getFirst().getCount();
        tally.call(h, admitted(sent), spent, moved);
    }

    private static void pull(GameTestHelper h, Tally tally, ItemAccess door, Item item, int max) {
        ItemStack kind = new ItemStack(item);
        Work before = Work.now();
        ItemAccess.Drawn drawn = door.pull(List.of(new ItemAccess.Want(kind, max, 0)));
        Work spent = Work.now().since(before);
        tally.call(
                h,
                drawn.consulted() && !drawn.why().equals(java.util.Optional.of(ItemAccess.BUSY)),
                spent,
                drawn.count());
    }

    /**
     * A block of {@code width} by {@code depth} Mouth cells at y=1, joined edge to edge: the Mouths in {@code doors}
     * are Both with no store, every other one is Intake with a chest on it.
     */
    private static List<BlockPos> block(GameTestHelper h, int width, int depth, List<BlockPos> doors) {
        List<BlockPos> far = new ArrayList<>();
        for (int x = 1; x <= width; x++)
            for (int z = 1; z <= depth; z++) {
                BlockPos at = new BlockPos(x, Y, z);
                if (doors.contains(at)) {
                    mouth(h, at, MouthMode.BOTH, null);
                } else {
                    mouth(h, at, MouthMode.INTAKE, null);
                    chest(h, at);
                    far.add(at);
                }
            }
        return far;
    }

    private static void settings(GameTestHelper h, BlockPos cell, String settings) {
        try {
            h.getBlockEntity(cell)
                    .loadCustomOnly(
                            net.minecraft.nbt.TagParser.parseTag(settings),
                            h.getLevel().registryAccess());
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            h.fail("bad Mouth settings " + settings + ": " + e.getMessage());
        }
    }

    private static int stored(GameTestHelper h, List<BlockPos> cells, Item item) {
        int total = 0;
        for (BlockPos cell : cells) total += count(h, cell, item);
        return total;
    }

    /** Waits until the Mouth at {@code door} reads its network and has its endpoints, then lets the first pulses pass. */
    private static net.minecraft.gametest.framework.GameTestSequence settled(GameTestHelper h, BlockPos door) {
        ItemAccess access = access(h, door);
        return h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(access.push(List.of(), true).consulted(), "the network is read"))
                .thenIdle(SETTLE);
    }

    private static final List<BlockPos> DOORS =
            List.of(new BlockPos(1, Y, 1), new BlockPos(8, Y, 1), new BlockPos(1, Y, 5), new BlockPos(8, Y, 5));

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "routing_work_push")
    public static void aSettledNetworkOfFortyMouthsPushesWithinSixteenLookupsACall(GameTestHelper h) {
        List<BlockPos> far = block(h, 8, 5, DOORS);
        h.assertValueEqual(far.size(), 36, "thirty-six far Mouths and four doors share the network");
        settled(h, DOORS.getFirst())
                .thenExecute(() -> {
                    Tally tally = new Tally();
                    // four organs' Mouths, three calls each, in one tick: twelve admissions of the tick
                    for (int round = 0; round < 3; round++)
                        for (BlockPos door : DOORS) push(h, tally, access(h, door), Items.STICK, 4, false);
                    h.assertValueEqual(tally.admitted, 12, "every call was admitted");
                    h.assertValueEqual(tally.moved, 12, "every call moved its items");
                    h.assertValueEqual(stored(h, far, Items.STICK), 48, "all of it arrived, nothing was made or lost");
                    h.assertTrue(tally.worst > 0, "the calls looked at far Mouths");
                    h.assertTrue(tally.total.inventory() > 0, "the calls used stores, saw " + tally.total);
                    tally.report("push, 40 Mouths, 4 organ Mouths x 3 calls");
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "routing_work_pull")
    public static void aSettledNetworkOfFortyMouthsPullsWithinSixteenLookupsACall(GameTestHelper h) {
        // one Intake organ Mouth without a store, and thirty-nine Output Mouths over chests of sticks
        BlockPos door = new BlockPos(1, Y, 1);
        for (int x = 1; x <= 8; x++)
            for (int z = 1; z <= 5; z++) {
                BlockPos at = new BlockPos(x, Y, z);
                if (at.equals(door)) {
                    mouth(h, at, MouthMode.INTAKE, null);
                } else {
                    mouth(h, at, MouthMode.OUTPUT, null);
                    chest(h, at, new ItemStack(Items.STICK, 64));
                }
            }
        ItemAccess access = access(h, door);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(access.pull(List.of()).consulted(), "the network is read"))
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    Tally tally = new Tally();
                    int before = 0;
                    for (int x = 1; x <= 8; x++)
                        for (int z = 1; z <= 5; z++)
                            if (x != 1 || z != 1) before += count(h, new BlockPos(x, Y, z), Items.STICK);
                    for (int call = 0; call < 20; call++) pull(h, tally, access, Items.STICK, 8);
                    int after = 0;
                    for (int x = 1; x <= 8; x++)
                        for (int z = 1; z <= 5; z++)
                            if (x != 1 || z != 1) after += count(h, new BlockPos(x, Y, z), Items.STICK);
                    h.assertTrue(tally.admitted > 0, "calls were admitted");
                    h.assertTrue(tally.moved > 0, "calls drew items");
                    h.assertValueEqual(before - after, tally.items, "what the stores lost is what the calls drew");
                    h.assertTrue(tally.total.inventory() > 0, "the calls used stores, saw " + tally.total);
                    tally.report("pull, 40 Mouths, 20 calls");
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "routing_work_small")
    public static void aSmallNetworkIsLookedAtWholeAndNoMore(GameTestHelper h) {
        BlockPos door = new BlockPos(1, Y, 1);
        List<BlockPos> far = block(h, 5, 1, List.of(door));
        settled(h, door)
                .thenExecute(() -> {
                    Tally tally = new Tally();
                    for (int call = 0; call < 10; call++) push(h, tally, access(h, door), Items.STICK, 2, false);
                    h.assertValueEqual(tally.admitted, 10, "every call was admitted");
                    h.assertValueEqual(stored(h, far, Items.STICK), 20, "all of it arrived");
                    h.assertTrue(tally.worst <= far.size(), "no more lookups than far Mouths, saw " + tally.worst);
                    h.assertTrue(tally.worst > 0, "the far Mouths were looked at");
                    tally.report("push, 5 Mouths, 10 calls");
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "routing_work_refuse")
    public static void refusedPushesLookAtSixteenAndReachTheMouthThatAllowsTheItemWithinThreeCalls(GameTestHelper h) {
        List<BlockPos> far = block(h, 8, 5, List.of(DOORS.getFirst()));
        BlockPos allowing = far.getLast();
        for (BlockPos cell : far) if (!cell.equals(allowing)) settings(h, cell, STICKS);
        ItemAccess access = access(h, DOORS.getFirst());
        settled(h, DOORS.getFirst())
                .thenExecute(() -> {
                    // the bone is refused by thirty-eight Mouths on the way: no call looks at more than sixteen
                    Tally tally = new Tally();
                    int calls = 0;
                    while (tally.moved == 0 && calls++ < 3) push(h, tally, access, Items.BONE, 4, false);
                    h.assertValueEqual(tally.moved, 1, "the Mouth that allows bones was reached within three calls");
                    h.assertValueEqual(count(h, allowing, Items.BONE), 4, "and got them");
                    h.assertTrue(tally.total.lookups() > 0, "the refused calls looked at Mouths");
                    // now it refuses too: every call is turned away, looks at sixteen and moves nothing
                    settings(h, allowing, STICKS);
                    Tally all = new Tally();
                    for (int call = 0; call < 8; call++) push(h, all, access, Items.BONE, 4, false);
                    h.assertValueEqual(all.admitted, 8, "every call was admitted");
                    h.assertValueEqual(all.moved, 0, "nothing was taken");
                    h.assertValueEqual(all.worst, ItemAccess.CANDIDATES, "each refused call looked at sixteen Mouths");
                    h.assertValueEqual(
                            all.total.inventory(), 0L, "no store was asked: the filters turned the bone away");
                    h.assertValueEqual(stored(h, far, Items.BONE), 4, "no bone was made or lost");
                    tally.report("push refused then delivered, 40 Mouths");
                    all.report("push refused by every Mouth, 40 Mouths, 8 calls");
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "routing_work_admission")
    public static void callsPastTheTicksAdmissionsAreRefusedBeforeAnyRoutingWork(GameTestHelper h) {
        List<BlockPos> far = block(h, 8, 5, DOORS);
        settled(h, DOORS.getFirst())
                .thenExecute(() -> {
                    Tally tally = new Tally();
                    for (int call = 0; call < 100; call++)
                        push(h, tally, access(h, DOORS.get(call % DOORS.size())), Items.STICK, 1, true);
                    h.assertTrue(tally.refused > 0, "the tick's admissions ran out");
                    h.assertTrue(tally.admitted >= 50, "the admissions of the tick were used, saw " + tally.admitted);
                    h.assertValueEqual(tally.admitted + tally.refused, 100, "every call was answered");
                    h.assertTrue(tally.moved > 0, "the admitted simulated pushes would have moved items");
                    h.assertValueEqual(stored(h, far, Items.STICK), 0, "a simulated push moves nothing");
                    h.assertTrue(
                            tally.total.lookups() <= (long) tally.admitted * ItemAccess.CANDIDATES,
                            "lookups follow the admitted calls only, saw " + tally.total);
                    tally.report("simulated push, 40 Mouths, 100 calls in one tick");
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "routing_work_priority")
    public static void aPriorityChangeRebuildsTheIndexOnceAndTheNewOrderIsUsed(GameTestHelper h) {
        List<BlockPos> far = block(h, 8, 5, List.of(DOORS.getFirst()));
        BlockPos chosen = far.get(30);
        ItemAccess access = access(h, DOORS.getFirst());
        settled(h, DOORS.getFirst())
                .thenExecute(() -> {
                    ShapingPalmTests.MenuPlayer player = ShapingPalmTests.open(h, chosen);
                    for (int step = 0; step < 2; step++)
                        player.containerMenu.clickMenuButton(player, MouthMenu.PRIORITY_UP);
                    player.closeContainer();
                    Tally tally = new Tally();
                    Work before = Work.now();
                    ItemAccess.Sent first =
                            access.push(List.of(new ItemAccess.Offer(new ItemStack(Items.STICK, 4), 4)), false);
                    Work rebuilt = Work.now().since(before);
                    h.assertValueEqual(rebuilt.builds(), 1L, "the next call rebuilt the index once, saw " + rebuilt);
                    h.assertValueEqual(rebuilt.nodes(), 40L, "and walked the forty cells of the network once");
                    h.assertTrue(
                            rebuilt.lookups() <= 40 + ItemAccess.CANDIDATES,
                            "its lookups are the build's and one call's");
                    h.assertValueEqual(first.rests().getFirst().getCount(), 0, "it moved the items");
                    h.assertValueEqual(count(h, chosen, Items.STICK), 4, "to the Mouth of the higher priority");
                    for (int call = 0; call < 5; call++) push(h, tally, access, Items.STICK, 4, false);
                    h.assertValueEqual(tally.total.builds(), 0L, "later calls reuse it");
                    tally.report("push after a priority change, 5 calls; rebuild " + rebuilt);
                    h.assertValueEqual(count(h, chosen, Items.STICK), 24, "and all of them go to the same Mouth");
                    // a Mouth that stops taking leaves the order (closed, so its chest is not emptied by its own
                    // pulses)
                    h.setBlock(
                            chosen,
                            LogisticsModule.MOUTH
                                    .get()
                                    .defaultBlockState()
                                    .setValue(MouthBlock.MODE, MouthMode.CLOSED));
                })
                .thenWaitUntil(
                        () -> h.assertTrue(access.push(List.of(), true).consulted(), "the network is read again"))
                .thenExecute(() -> {
                    ItemAccess.Sent next =
                            access.push(List.of(new ItemAccess.Offer(new ItemStack(Items.STICK, 4), 4)), false);
                    h.assertValueEqual(next.rests().getFirst().getCount(), 0, "the items still found a taker");
                    h.assertValueEqual(count(h, chosen, Items.STICK), 24, "which is not the Mouth that stopped taking");
                })
                .thenSucceed();
    }

    /** One call a tick over a 32 by 32 block of Mouths: the largest network, 1,024 cells. */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "routing_work_limit",
            timeoutTicks = 400)
    public static void aNetworkOfTheLimitIsBuiltOnceAndEveryCallLooksAtSixteen(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos base = new BlockPos(410000, 80, 410000);
        for (int x = 0; x < 3; x++)
            for (int z = 0; z < 3; z++)
                TestChunks.force(level, new ChunkPos((base.getX() >> 4) + x, (base.getZ() >> 4) + z));
        for (int x = 0; x < 32; x++)
            for (int z = 0; z < 32; z++)
                level.setBlockAndUpdate(
                        base.offset(x, 0, z),
                        LogisticsModule.MOUTH
                                .get()
                                .defaultBlockState()
                                .setValue(MouthBlock.MODE, x == 0 && z == 0 ? MouthMode.BOTH : MouthMode.INTAKE));
        BlockPos usable = base.offset(31, 0, 31);
        level.setBlockAndUpdate(usable.above(), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        ItemAccess access = level.getCapability(LogisticsCapabilities.ITEM_ACCESS, base, null);
        Tally steady = new Tally();
        h.startSequence()
                .thenExecute(RoutingCounts::reset)
                .thenWaitUntil(() -> h.assertTrue(access.push(List.of(), true).consulted(), "the network is read"))
                .thenExecute(() -> {
                    long builds = RoutingCounts.get(Step.BUILDS);
                    h.assertTrue(builds >= 1, "the endpoints were built");
                    h.assertValueEqual(
                            RoutingCounts.get(Step.NODES), builds * 1024, "each build walked the 1,024 cells");
                    h.assertValueEqual(RoutingCounts.get(Step.LOOKUPS), builds * 1024, "and read each Mouth once");
                })
                .thenIdle(SETTLE)
                .thenWaitUntil(() -> {
                    // forty calls a tick, until the bone reaches the one Mouth that has a store
                    for (int call = 0; call < 40 && steady.moved == 0; call++)
                        push(h, steady, access, Items.BONE, 4, false);
                    h.assertTrue(
                            steady.moved > 0,
                            "the bone was found within ceil(1023 / 16) = 64 calls, saw " + steady.admitted);
                })
                .thenExecute(() -> {
                    h.assertTrue(
                            steady.admitted <= 80, "within the 64 calls and the pulses' share, saw " + steady.admitted);
                    h.assertValueEqual(steady.total.builds(), 0L, "no rebuild while calls were made");
                    h.assertValueEqual(steady.total.nodes(), 0L, "no network walk while calls were made");
                    h.assertValueEqual(steady.total.ranked(), 0L, "no ordering while calls were made");
                    h.assertTrue(steady.refused == 0 || steady.admitted > 0, "calls were admitted");
                    h.assertTrue(steady.worst > 0, "far Mouths were looked at");
                    h.assertValueEqual(
                            ((net.minecraft.world.level.block.entity.ChestBlockEntity)
                                            level.getBlockEntity(usable.above()))
                                    .countItem(Items.BONE),
                            4,
                            "the bone is in the chest, once");
                    steady.report("push, 1,024 Mouths, until the bone arrived");
                })
                .thenExecute(() -> {
                    for (int x = 0; x < 3; x++)
                        for (int z = 0; z < 3; z++)
                            level.setChunkForced((base.getX() >> 4) + x, (base.getZ() >> 4) + z, false);
                })
                .thenSucceed();
    }
}
