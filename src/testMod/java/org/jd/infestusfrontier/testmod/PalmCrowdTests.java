package org.jd.infestusfrontier.testmod;

import static org.jd.infestusfrontier.testmod.PalmNet.PALM;
import static org.jd.infestusfrontier.testmod.PalmNet.SETTLE;
import static org.jd.infestusfrontier.testmod.PalmNet.count;
import static org.jd.infestusfrontier.testmod.PalmNet.mouth;
import static org.jd.infestusfrontier.testmod.PalmNet.veins;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.cell;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.open;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.put;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.result;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.stack;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.logistics.api.ItemAccess;
import org.jd.infestusfrontier.logistics.api.LogisticsCapabilities;
import org.jd.infestusfrontier.processing.ShapingPalmMenu;
import org.jd.infestusfrontier.testmod.ShapingPalmTests.MenuPlayer;

/**
 * A Reach-only Palm (no item handler, so no automatic delivery) on a settled network of {@link #ENDPOINTS} far Mouths:
 * more than the {@link ItemAccess#CANDIDATES} one call looks at. Repeated Restock and Return must reach the one usable
 * endpoint behind sixteen that refuse, within {@link #ATTEMPTS} admitted presses ({@code ceil(17 / 16)}), although no
 * earlier press moved anything. A mixed-priority prefix puts the usable endpoint last by priority; the equal-priority
 * sweeps make each of the 17 endpoints the usable one in turn, whatever order the network found them in. Each test has
 * its own batch, so another test's Mouth pulses cannot spend the tick's network admissions and pass for starvation.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class PalmCrowdTests {
    private PalmCrowdTests() {}

    private static final int ENDPOINTS = 17;
    private static final int ATTEMPTS = 2;
    private static final String STICKS = ",filter:[{id:\"minecraft:stick\",count:1}]";

    /** {@link #ENDPOINTS} far Mouth cells on two spines of veins joined beside the Palm's Mouth, two on every second column. */
    private static List<BlockPos> endpoints(GameTestHelper h) {
        veins(h, new BlockPos(3, 1, 2), new BlockPos(14, 1, 2));
        veins(h, new BlockPos(3, 1, 8), new BlockPos(14, 1, 8));
        veins(h, new BlockPos(3, 1, 3), new BlockPos(3, 1, 7));
        List<BlockPos> cells = new ArrayList<>();
        for (int z : new int[] {1, 3, 7, 9}) for (int x = 4; x <= 14; x += 2) cells.add(new BlockPos(x, 1, z));
        return cells.subList(0, ENDPOINTS);
    }

    /** Fills every slot of the chest on {@code cell} with {@code fill}, or empties it. */
    private static void load(GameTestHelper h, BlockPos cell, ItemStack fill) {
        h.setBlock(cell.above(), Blocks.CHEST);
        ChestBlockEntity box = h.getBlockEntity(cell.above());
        for (int slot = 0; slot < box.getContainerSize(); slot++) box.setItem(slot, fill.copy());
    }

    private static void endpoint(GameTestHelper h, BlockPos cell, MouthMode mode, String settings, ItemStack fill) {
        mouth(h, cell, mode, settings);
        load(h, cell, fill);
    }

    private static void stock(GameTestHelper h, BlockPos cell, ItemStack stack) {
        load(h, cell, ItemStack.EMPTY);
        ((ChestBlockEntity) h.getBlockEntity(cell.above())).setItem(0, stack);
    }

    /** Waits until the Palm's Mouth reads its network, and lets the Mouths' first pulses pass. */
    private static GameTestSequence settled(GameTestHelper h, BlockPos door) {
        ItemAccess access = h.getLevel().getCapability(LogisticsCapabilities.ITEM_ACCESS, h.absolutePos(door), null);
        return h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(access.pull(List.of()).consulted(), "the network is read"))
                .thenIdle(SETTLE);
    }

    private static boolean press(MenuPlayer player, int button) {
        return player.containerMenu.clickMenuButton(player, button);
    }

    /** Presses {@code button} until it moves something, at most {@link #ATTEMPTS} times. */
    private static boolean pressed(MenuPlayer player, int button) {
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) if (press(player, button)) return true;
        return false;
    }

    private static int planks(GameTestHelper h, List<BlockPos> cells) {
        int total = 0;
        for (BlockPos cell : cells) total += count(h, cell, Items.OAK_PLANKS);
        return total;
    }

    // ---- Restock ----

    private static void restockBehindSixteen(GameTestHelper h, String refuser, ItemStack refuserStock) {
        BlockPos door = PalmNet.palm(h, PALM, MouthMode.BOTH).below();
        List<BlockPos> cells = endpoints(h);
        for (BlockPos cell : cells.subList(0, 16))
            endpoint(h, cell, MouthMode.OUTPUT, "{priority:1" + refuser + "}", refuserStock);
        BlockPos usable = cells.get(16);
        endpoint(h, usable, MouthMode.OUTPUT, "{priority:0}", ItemStack.EMPTY);
        stock(h, usable, stack(Items.OAK_PLANKS, 20));
        MenuPlayer[] player = new MenuPlayer[1];
        settled(h, door)
                .thenExecute(() -> {
                    player[0] = open(h, PALM);
                    put(player[0], 0, stack(Items.OAK_PLANKS, 1));
                    int before = planks(h, cells);
                    h.assertTrue(
                            pressed(player[0], ShapingPalmMenu.RESTOCK),
                            "the stock behind sixteen refusing higher-priority sources is reached");
                    h.assertValueEqual(cell(h, PALM, 0).getCount(), 21, "all twenty came");
                    h.assertValueEqual(count(h, usable, Items.OAK_PLANKS), 0, "from the seventeenth");
                    h.assertValueEqual(
                            planks(h, cells) + cell(h, PALM, 0).getCount() - 1, before, "nothing was lost or made");
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "palm_crowd_restock_empty")
    public static void restockReachesStockBehindSixteenEmptyHigherPrioritySources(GameTestHelper h) {
        restockBehindSixteen(h, "", ItemStack.EMPTY);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "palm_crowd_restock_filter")
    public static void restockReachesStockBehindSixteenFilteringHigherPrioritySources(GameTestHelper h) {
        restockBehindSixteen(h, STICKS, stack(Items.OAK_PLANKS, 5));
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "palm_crowd_restock_equal",
            timeoutTicks = 400)
    public static void restockReachesWhicheverOfSeventeenEqualSourcesHasStock(GameTestHelper h) {
        BlockPos door = PalmNet.palm(h, PALM, MouthMode.BOTH).below();
        List<BlockPos> cells = endpoints(h);
        for (BlockPos cell : cells) endpoint(h, cell, MouthMode.OUTPUT, "{priority:0}", ItemStack.EMPTY);
        MenuPlayer[] player = new MenuPlayer[1];
        GameTestSequence run = settled(h, door).thenExecute(() -> player[0] = open(h, PALM));
        for (int round = 0; round < 2 * ENDPOINTS; round++) {
            BlockPos usable = cells.get(round < ENDPOINTS ? round : 2 * ENDPOINTS - 1 - round);
            run.thenExecuteAfter(1, () -> {
                stock(h, usable, stack(Items.OAK_PLANKS, 20));
                put(player[0], 0, ItemStack.EMPTY); // lifts what the last round left in the cell
                put(player[0], 0, stack(Items.OAK_PLANKS, 1));
                h.assertTrue(pressed(player[0], ShapingPalmMenu.RESTOCK), "the one source with stock is reached");
                h.assertValueEqual(cell(h, PALM, 0).getCount(), 21, "all twenty came");
                h.assertValueEqual(planks(h, cells), 0, "no plank stayed behind or was made");
            });
        }
        run.thenSucceed();
    }

    // ---- Return ----

    private static void returnBehindSixteen(GameTestHelper h, String refuser, ItemStack refuserStock) {
        BlockPos door = PalmNet.palm(h, PALM, MouthMode.BOTH).below();
        List<BlockPos> cells = endpoints(h);
        for (BlockPos cell : cells.subList(0, 16))
            endpoint(h, cell, MouthMode.INTAKE, "{priority:1" + refuser + "}", refuserStock);
        BlockPos usable = cells.get(16);
        endpoint(h, usable, MouthMode.INTAKE, "{priority:0}", ItemStack.EMPTY);
        MenuPlayer[] player = new MenuPlayer[1];
        settled(h, door)
                .thenExecute(() -> {
                    player[0] = open(h, PALM);
                    put(player[0], 0, stack(Items.OAK_PLANKS, 4));
                    h.assertTrue(
                            pressed(player[0], ShapingPalmMenu.RETURN),
                            "the room behind sixteen refusing higher-priority destinations is reached");
                    h.assertTrue(cell(h, PALM, 0).isEmpty(), "the grid gave its planks");
                    h.assertValueEqual(count(h, usable, Items.OAK_PLANKS), 4, "to the seventeenth, once");
                    h.assertValueEqual(planks(h, cells), 4, "and to no other");
                })
                .thenSucceed();
    }

    private static ItemStack cobblestone() {
        return stack(Items.COBBLESTONE, 64);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "palm_crowd_return_full")
    public static void returnReachesRoomBehindSixteenFullHigherPriorityDestinations(GameTestHelper h) {
        returnBehindSixteen(h, "", cobblestone());
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "palm_crowd_return_filter")
    public static void returnReachesRoomBehindSixteenFilteringHigherPriorityDestinations(GameTestHelper h) {
        returnBehindSixteen(h, STICKS, ItemStack.EMPTY);
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "palm_crowd_return_equal",
            timeoutTicks = 400)
    public static void returnReachesWhicheverOfSeventeenEqualDestinationsHasRoom(GameTestHelper h) {
        BlockPos door = PalmNet.palm(h, PALM, MouthMode.BOTH).below();
        List<BlockPos> cells = endpoints(h);
        for (BlockPos cell : cells) endpoint(h, cell, MouthMode.INTAKE, "{priority:0}", cobblestone());
        MenuPlayer[] player = new MenuPlayer[1];
        GameTestSequence run = settled(h, door).thenExecute(() -> player[0] = open(h, PALM));
        for (int round = 0; round < 2 * ENDPOINTS; round++) {
            BlockPos usable = cells.get(round < ENDPOINTS ? round : 2 * ENDPOINTS - 1 - round);
            run.thenExecuteAfter(1, () -> {
                load(h, usable, ItemStack.EMPTY);
                put(player[0], 0, stack(Items.OAK_PLANKS, 4));
                h.assertTrue(pressed(player[0], ShapingPalmMenu.RETURN), "the one destination with room is reached");
                h.assertTrue(cell(h, PALM, 0).isEmpty(), "the grid gave its planks");
                h.assertValueEqual(planks(h, cells), 4, "once, and nothing was lost");
                load(h, usable, cobblestone());
            });
        }
        run.thenSucceed();
    }

    /** The simulation that refuses on the first press passes its candidates; the second press simulates and stores. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "palm_crowd_store_result")
    public static void storeResultReachesRoomBehindSixteenFullHigherPriorityDestinations(GameTestHelper h) {
        BlockPos door = PalmNet.palm(h, PALM, MouthMode.BOTH).below();
        List<BlockPos> cells = endpoints(h);
        for (BlockPos cell : cells.subList(0, 16)) endpoint(h, cell, MouthMode.INTAKE, "{priority:1}", cobblestone());
        BlockPos usable = cells.get(16);
        endpoint(h, usable, MouthMode.INTAKE, "{priority:0}", ItemStack.EMPTY);
        settled(h, door)
                .thenExecute(() -> {
                    MenuPlayer player = open(h, PALM);
                    put(player, 0, stack(Items.OAK_PLANKS, 4));
                    put(player, 3, stack(Items.OAK_PLANKS, 4));
                    h.assertTrue(result(player).is(Items.STICK), "the grid makes sticks");
                    h.assertTrue(
                            pressed(player, ShapingPalmMenu.STORE_RESULT),
                            "the room behind sixteen refusing destinations is reached");
                    h.assertValueEqual(player.crafted(Items.STICK), 4, "the craft was counted once: its four sticks");
                    h.assertValueEqual(count(h, usable, Items.STICK), 4, "its sticks are in the seventeenth");
                    h.assertValueEqual(planks(h, cells), 0, "no plank was stored");
                    h.assertValueEqual(
                            cell(h, PALM, 0).getCount() + cell(h, PALM, 3).getCount(), 6, "two planks were spent");
                })
                .thenSucceed();
    }
}
