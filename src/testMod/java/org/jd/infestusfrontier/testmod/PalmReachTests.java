package org.jd.infestusfrontier.testmod;

import static org.jd.infestusfrontier.testmod.PalmNet.ACCESS;
import static org.jd.infestusfrontier.testmod.PalmNet.PALM;
import static org.jd.infestusfrontier.testmod.PalmNet.REFUSAL;
import static org.jd.infestusfrontier.testmod.PalmNet.SETTLE;
import static org.jd.infestusfrontier.testmod.PalmNet.STATUS;
import static org.jd.infestusfrontier.testmod.PalmNet.chest;
import static org.jd.infestusfrontier.testmod.PalmNet.count;
import static org.jd.infestusfrontier.testmod.PalmNet.mouth;
import static org.jd.infestusfrontier.testmod.PalmNet.reach;
import static org.jd.infestusfrontier.testmod.PalmNet.rig;
import static org.jd.infestusfrontier.testmod.PalmNet.use;
import static org.jd.infestusfrontier.testmod.PalmNet.veins;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.cell;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.fillInventory;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.grid;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.held;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.loose;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.open;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.palm;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.put;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.result;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.stack;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.interaction.InteractionModule;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.logistics.api.ItemAccess;
import org.jd.infestusfrontier.logistics.api.LogisticsCapabilities;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.ShapingPalmMenu;
import org.jd.infestusfrontier.storage.api.ItemStore;
import org.jd.infestusfrontier.testmod.ShapingPalmTests.MenuPlayer;

/**
 * The Shaping Palm's reach through its Mouth (BLOCK_CATALOG T1-41, stage 2). The Palm gets Reach from a Contractile
 * Fiber and reaches storage only through the Mouth under it and the Item Veins that join that Mouth to a Mouth on a
 * store. Its screen's three buttons act on that network alone. Every test counts items across the grid and the stores,
 * so a refused, partial or competing action shows a lost or duplicated item. The Palm stands at (2, 2, 2).
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class PalmReachTests {
    private PalmReachTests() {}

    // ---- helpers ----

    private static int inGrid(GameTestHelper h, BlockPos at, net.minecraft.world.item.Item item) {
        int count = 0;
        for (ItemStack stack : grid(h, at).grid()) if (stack.is(item)) count += stack.getCount();
        return count;
    }

    private static boolean press(MenuPlayer player, int button) {
        return player.containerMenu.clickMenuButton(player, button);
    }

    /** Runs {@code check} once the network has settled, then succeeds. */
    private static void later(GameTestHelper h, Runnable check) {
        h.runAfterDelay(SETTLE, () -> {
            check.run();
            h.succeed();
        });
    }

    // ---- Reach ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aContractileFiberGivesAPalmReachOnceAndASecondOneIsKept(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = new MenuPlayer(h.getLevel(), "palm_fiber");
        ItemStack fibers = stack(LogisticsModule.CONTRACTILE_FIBER.get(), 2);
        use(h, player, fibers, at);
        h.assertValueEqual(fibers.getCount(), 1, "one fiber is spent");
        h.assertValueEqual(player.saidKey(), STATUS + "reach", "the player is told");
        use(h, player, fibers, at);
        h.assertValueEqual(fibers.getCount(), 1, "a Palm with Reach does not take another");
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_has_reach", "and says so");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aFiberOnAnotherBlockGivesNothing(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = new MenuPlayer(h.getLevel(), "palm_fiber");
        BlockPos stone = new BlockPos(5, 2, 2);
        h.setBlock(stone, Blocks.STONE);
        ItemStack fiber = stack(LogisticsModule.CONTRACTILE_FIBER.get(), 1);
        use(h, player, fiber, stone);
        h.assertValueEqual(fiber.getCount(), 1, "stone does not take the fiber");
        h.assertTrue(
                h.getBlockEntity(at).collectComponents().get(ProcessingModule.PALM_REACH.get()) == null,
                "the Palm was not touched");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void reachIsCarriedByTheBrokenPalmAndItsGridComesBackWithIt(GameTestHelper h) {
        BlockPos at = PalmNet.palm(h, PALM, MouthMode.BOTH);
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.OAK_PLANKS, 2));
        player.closeContainer();
        h.getLevel().destroyBlock(h.absolutePos(at), true);
        List<ItemEntity> drops = loose(h, at);
        h.assertValueEqual(drops.size(), 1, "one drop, the Palm");
        ItemStack dropped = drops.get(0).getItem();
        h.assertTrue(
                dropped.getOrDefault(ProcessingModule.PALM_REACH.get(), false), "the item carries Reach, as it was");
        CustomData data = dropped.get(DataComponents.BLOCK_ENTITY_DATA);
        h.assertTrue(data == null || !data.copyTag().contains("link"), "no position rides on the item");
        drops.get(0).discard();
        MenuPlayer placer = new MenuPlayer(h.getLevel(), "palm_placer");
        placer.setItemInHand(InteractionHand.MAIN_HAND, dropped.copy());
        h.placeAt(placer, placer.getMainHandItem(), at.below(), Direction.UP);
        h.assertBlockPresent(ProcessingModule.SHAPING_PALM.get(), at);
        h.assertValueEqual(inGrid(h, at, Items.OAK_PLANKS), 2, "its grid came back");
        MenuPlayer viewer = open(h, at);
        h.assertValueEqual(
                ((ShapingPalmMenu) viewer.containerMenu).reachState(),
                ShapingPalmMenu.MOUTH_BOTH,
                "with Reach, and its Mouth under it");
        h.succeed();
    }

    // ---- the network is the only way to a store ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void restockDrawsFromAChestTwelveBlocksAwayOverItemVeinsLeavingEmptyCellsEmpty(GameTestHelper h) {
        PalmNet.Rig rig = rig(
                h,
                12,
                MouthMode.BOTH,
                MouthMode.OUTPUT,
                stack(Items.OAK_PLANKS, 40),
                stack(Items.STICK, 10),
                stack(Items.APPLE, 5));
        later(h, () -> {
            MenuPlayer player = open(h, rig.palm());
            put(player, 0, stack(Items.OAK_PLANKS, 60));
            put(player, 4, stack(Items.STICK, 3));
            h.assertTrue(press(player, ShapingPalmMenu.RESTOCK), "the restock goes");
            h.assertValueEqual(player.saidKey(), STATUS + "restocked", "and says so");
            h.assertValueEqual(cell(h, rig.palm(), 0).getCount(), 64, "a cell tops up to a full stack");
            h.assertValueEqual(cell(h, rig.palm(), 4).getCount(), 13, "or takes all the store has: 3 + 10");
            h.assertTrue(cell(h, rig.palm(), 8).isEmpty(), "an empty cell stays empty: the Palm does not guess");
            h.assertValueEqual(count(h, rig.far(), Items.OAK_PLANKS), 36, "60 + 40 planks, 64 in the grid");
            h.assertValueEqual(count(h, rig.far(), Items.STICK), 0, "every stick left the chest");
            h.assertValueEqual(count(h, rig.far(), Items.APPLE), 5, "an item the grid does not hold is not touched");
            h.assertValueEqual(
                    inGrid(h, rig.palm(), Items.OAK_PLANKS) + count(h, rig.far(), Items.OAK_PLANKS),
                    100,
                    "no plank lost or made");
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aChestBesideThePalmAndAMouthWithoutVeinsAreInaccessible(GameTestHelper h) {
        BlockPos at = PalmNet.palm(h, PALM, MouthMode.BOTH);
        // a vanilla chest touching the Palm, and a Mouth on a chest two blocks away that no vein joins
        h.setBlock(at.east(), Blocks.CHEST);
        ((ChestBlockEntity) h.getBlockEntity(at.east())).setItem(0, stack(Items.OAK_PLANKS, 30));
        BlockPos lone = at.below().east(3);
        mouth(h, lone, MouthMode.OUTPUT, null);
        chest(h, lone, stack(Items.OAK_PLANKS, 30));
        later(h, () -> {
            MenuPlayer player = open(h, at);
            put(player, 0, stack(Items.OAK_PLANKS, 1));
            h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "nothing the network does not join is reached");
            h.assertValueEqual(player.saidKey(), REFUSAL + "palm_store_short", "and the Palm says the stock is short");
            h.assertValueEqual(cell(h, at, 0).getCount(), 1, "the grid is as it was");
            h.assertValueEqual(
                    ((ChestBlockEntity) h.getBlockEntity(at.east())).countItem(Items.OAK_PLANKS),
                    30,
                    "the chest beside kept its planks");
            h.assertValueEqual(count(h, lone, Items.OAK_PLANKS), 30, "so did the chest of the lone Mouth");
            put(player, 1, stack(Items.APPLE, 2));
            h.assertFalse(press(player, ShapingPalmMenu.RETURN), "nothing takes the grid either");
            h.assertValueEqual(player.saidKey(), REFUSAL + "palm_store_full", "and the Palm says so");
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aPalmWithoutAMouthUnderItRefusesEveryActionAndSaysSo(GameTestHelper h) {
        BlockPos at = palm(h);
        reach(h, at);
        chest(h, at.below().east(2), stack(Items.OAK_PLANKS, 20));
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.OAK_PLANKS, 1));
        h.assertValueEqual(
                ((ShapingPalmMenu) player.containerMenu).reachState(), ShapingPalmMenu.NO_MOUTH, "the screen says so");
        for (int button : new int[] {ShapingPalmMenu.RESTOCK, ShapingPalmMenu.RETURN, ShapingPalmMenu.STORE_RESULT}) {
            h.assertFalse(press(player, button), "button " + button + " has no door");
            h.assertValueEqual(player.saidKey(), REFUSAL + "palm_no_mouth", "and says there is no Mouth");
        }
        h.assertValueEqual(cell(h, at, 0).getCount(), 1, "the grid is untouched");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theDirectionOfThePalmsMouthDecidesWhatThePalmMayDo(GameTestHelper h) {
        PalmNet.Rig rig = rig(h, 5, MouthMode.OUTPUT, MouthMode.BOTH, stack(Items.OAK_PLANKS, 20));
        later(h, () -> {
            MenuPlayer player = open(h, rig.palm());
            put(player, 0, stack(Items.OAK_PLANKS, 2));
            h.assertValueEqual(
                    ((ShapingPalmMenu) player.containerMenu).reachState(), ShapingPalmMenu.MOUTH_GIVES, "gives");
            h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "an Output Mouth does not take from the network");
            h.assertValueEqual(player.saidKey(), ACCESS + "access_not_taking", "and says so");
            h.assertTrue(press(player, ShapingPalmMenu.RETURN), "but it gives the grid to it");
            h.assertValueEqual(count(h, rig.far(), Items.OAK_PLANKS), 22, "to the far chest");
            mouth(h, rig.door(), MouthMode.INTAKE, null);
            put(player, 0, stack(Items.OAK_PLANKS, 2));
            h.assertFalse(press(player, ShapingPalmMenu.RETURN), "an Intake Mouth does not give");
            h.assertValueEqual(player.saidKey(), ACCESS + "access_not_giving", "and says so");
            h.assertFalse(press(player, ShapingPalmMenu.STORE_RESULT), "nor does it store a result");
            h.assertValueEqual(player.saidKey(), ACCESS + "access_not_giving", "for the same reason");
            mouth(h, rig.door(), MouthMode.CLOSED, null);
            h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "a closed Mouth is a passage only");
            h.assertValueEqual(player.saidKey(), ACCESS + "access_not_taking", "restock refused");
            h.assertFalse(press(player, ShapingPalmMenu.RETURN), "return refused");
            h.assertValueEqual(player.saidKey(), ACCESS + "access_not_giving", "and so is return");
            h.assertValueEqual(inGrid(h, rig.palm(), Items.OAK_PLANKS), 2, "the grid kept its planks");
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aFarMouthGivesOnlyWhenItGivesAndTakesOnlyWhenItTakes(GameTestHelper h) {
        PalmNet.Rig rig = rig(h, 5, MouthMode.BOTH, MouthMode.INTAKE, stack(Items.OAK_PLANKS, 20));
        later(h, () -> {
            MenuPlayer player = open(h, rig.palm());
            put(player, 0, stack(Items.OAK_PLANKS, 2));
            h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "an Intake far Mouth lends nothing");
            h.assertValueEqual(player.saidKey(), REFUSAL + "palm_store_short", "the stock is short");
            h.assertValueEqual(count(h, rig.far(), Items.OAK_PLANKS), 20, "the chest kept its planks");
            mouth(h, rig.far(), MouthMode.OUTPUT, null);
            chest(h, rig.far(), stack(Items.OAK_PLANKS, 20));
            h.assertFalse(press(player, ShapingPalmMenu.RETURN), "an Output far Mouth takes nothing");
            h.assertValueEqual(player.saidKey(), REFUSAL + "palm_store_full", "no Mouth takes it");
            h.assertValueEqual(inGrid(h, rig.palm(), Items.OAK_PLANKS), 2, "the grid kept its planks");
            mouth(h, rig.far(), MouthMode.CLOSED, null);
            chest(h, rig.far(), stack(Items.OAK_PLANKS, 20));
            h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "a closed far Mouth neither gives");
            h.assertFalse(press(player, ShapingPalmMenu.RETURN), "nor takes");
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aRestockWithAnEmptyOrAFullGridRefusesAndChangesNothing(GameTestHelper h) {
        PalmNet.Rig rig = rig(h, 5, MouthMode.BOTH, MouthMode.OUTPUT, stack(Items.OAK_PLANKS, 20));
        later(h, () -> {
            MenuPlayer player = open(h, rig.palm());
            h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "an empty grid has nothing to restock");
            h.assertValueEqual(player.saidKey(), REFUSAL + "palm_grid_empty", "and says so");
            put(player, 0, stack(Items.OAK_PLANKS, 64));
            h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "a full cell takes no more");
            h.assertValueEqual(player.saidKey(), REFUSAL + "palm_grid_full", "and the Palm says it is full");
            put(player, 1, stack(Items.APPLE, 1));
            h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "a cell the network cannot fill");
            h.assertValueEqual(player.saidKey(), REFUSAL + "palm_store_short", "says the stock is short");
            h.assertValueEqual(count(h, rig.far(), Items.OAK_PLANKS), 20, "the chest kept its planks");
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void restockMatchesComponentsAndLeavesALookalikeAlone(GameTestHelper h) {
        ItemStack named = stack(Items.IRON_INGOT, 6);
        named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Tin"));
        PalmNet.Rig rig = rig(h, 5, MouthMode.BOTH, MouthMode.OUTPUT, stack(Items.IRON_INGOT, 20), named.copy());
        later(h, () -> {
            MenuPlayer player = open(h, rig.palm());
            put(player, 0, named.copyWithCount(1));
            put(player, 1, stack(Items.IRON_INGOT, 1));
            h.assertTrue(press(player, ShapingPalmMenu.RESTOCK), "the restock goes");
            h.assertValueEqual(cell(h, rig.palm(), 0).getCount(), 7, "the named cell took the 6 named ingots");
            h.assertTrue(ItemStack.isSameItemSameComponents(cell(h, rig.palm(), 0), named), "and holds only that kind");
            h.assertValueEqual(cell(h, rig.palm(), 1).getCount(), 21, "the plain cell took the 20 plain ones");
            h.assertTrue(
                    ItemStack.isSameItemSameComponents(cell(h, rig.palm(), 1), stack(Items.IRON_INGOT, 1)),
                    "and holds only plain ingots");
            h.assertValueEqual(count(h, rig.far(), Items.IRON_INGOT), 0, "the chest gave every ingot, once");
        });
    }

    // ---- filters, amounts and priorities ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theFiltersOfBothMouthsApplyToWhatTheNetworkMoves(GameTestHelper h) {
        PalmNet.Rig rig = rig(
                h,
                5,
                MouthMode.BOTH,
                MouthMode.BOTH,
                "{filter:[{id:\"minecraft:stick\",count:1}]}",
                stack(Items.OAK_PLANKS, 20),
                stack(Items.STICK, 20));
        later(h, () -> {
            MenuPlayer player = open(h, rig.palm());
            put(player, 0, stack(Items.OAK_PLANKS, 1));
            h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "the far Mouth's filter lets only sticks out");
            h.assertValueEqual(count(h, rig.far(), Items.OAK_PLANKS), 20, "the planks stay");
            put(player, 1, stack(Items.STICK, 1));
            h.assertTrue(press(player, ShapingPalmMenu.RESTOCK), "the sticks go");
            h.assertValueEqual(cell(h, rig.palm(), 1).getCount(), 21, "all twenty came");
            h.assertFalse(
                    press(player, ShapingPalmMenu.RETURN) && count(h, rig.far(), Items.OAK_PLANKS) != 20,
                    "the far Mouth's filter lets only sticks in: planks do not return");
            h.assertValueEqual(count(h, rig.far(), Items.OAK_PLANKS), 20, "no plank went in");
            h.assertValueEqual(count(h, rig.far(), Items.STICK), 21, "the sticks went back");
            // the Palm's own Mouth filters too: only apples
            PalmNet.mouth(h, rig.door(), MouthMode.BOTH, "{filter:[{id:\"minecraft:apple\",count:1}]}");
            MenuPlayer second = open(h, rig.palm(), "palm_filter");
            put(second, 2, stack(Items.STICK, 1));
            h.assertFalse(press(second, ShapingPalmMenu.RESTOCK), "the Palm's Mouth turns sticks away");
            h.assertValueEqual(second.saidKey(), ACCESS + "access_turned_away", "and says its filter did");
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theAmountsOfBothMouthsLimitWhatMoves(GameTestHelper h) {
        PalmNet.Rig rig = rig(
                h,
                5,
                MouthMode.BOTH,
                MouthMode.BOTH,
                "{amount:8}",
                stack(Items.OAK_PLANKS, 20),
                stack(Items.STICK, 20));
        later(h, () -> {
            MenuPlayer player = open(h, rig.palm());
            put(player, 0, stack(Items.OAK_PLANKS, 1));
            h.assertTrue(press(player, ShapingPalmMenu.RESTOCK), "the restock goes");
            h.assertValueEqual(cell(h, rig.palm(), 0).getCount(), 13, "12 came: the far Mouth keeps 8 of its 20");
            h.assertValueEqual(count(h, rig.far(), Items.OAK_PLANKS), 8, "and stops there");
            // the Palm's Mouth holds at most 16 of a kind in the Palm
            PalmNet.mouth(h, rig.door(), MouthMode.BOTH, "{amount:16}");
            MenuPlayer second = open(h, rig.palm(), "palm_amount");
            put(second, 1, stack(Items.STICK, 10));
            h.assertTrue(press(second, ShapingPalmMenu.RESTOCK), "the second restock goes");
            h.assertValueEqual(cell(h, rig.palm(), 1).getCount(), 16, "the Palm's Mouth holds 16 at most: 6 more came");
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void higherPriorityFarMouthsGiveFirstAndLowerOnesTheRest(GameTestHelper h) {
        BlockPos at = PalmNet.palm(h, PALM, MouthMode.BOTH);
        veins(h, at.below().east(), at.below().east(3));
        BlockPos lowFar = at.below().east(4);
        mouth(h, lowFar, MouthMode.OUTPUT, "{priority:0}");
        chest(h, lowFar, stack(Items.OAK_PLANKS, 10));
        BlockPos highFar = at.below().east(2).south();
        mouth(h, highFar, MouthMode.OUTPUT, "{priority:4}");
        chest(h, highFar, stack(Items.OAK_PLANKS, 3));
        later(h, () -> {
            MenuPlayer player = open(h, at);
            // a cell of 59 wants 5: the Mouth of priority 4 holds 3, so the other gives 2
            put(player, 0, stack(Items.OAK_PLANKS, 59));
            h.assertTrue(press(player, ShapingPalmMenu.RESTOCK), "the restock goes");
            h.assertValueEqual(count(h, highFar, Items.OAK_PLANKS), 0, "the high-priority Mouth was emptied first");
            h.assertValueEqual(count(h, lowFar, Items.OAK_PLANKS), 8, "the low one gave the other 2");
            h.assertValueEqual(cell(h, at, 0).getCount(), 64, "a full stack");
        });
    }

    // ---- Capsule Trees, Mixed Craws, Larders ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aCapsuleTreeWithBulkCrawAndLarderPodsSuppliesThePalmAndKeepsItsReserve(GameTestHelper h) {
        LarderTests.plant(h, LarderTests.FORKING);
        mouth(h, LarderTests.ROOT.below(), MouthMode.BOTH, null);
        LarderTests.growAll(h);
        List<BlockPos> pods = LarderTests.pods(h);
        BlockPos craw = pods.get(1);
        BlockPos larder = pods.get(2);
        LarderTests.treat(h, craw, LarderTests.gel(1)).closeContainer();
        LarderTests.treat(h, larder, LarderTests.gel(1)).closeContainer();
        LarderTests.treat(h, larder, LarderTests.resin(1)).closeContainer();
        LarderTests.label(h, larder, 0, Items.IRON_INGOT);
        ItemStore tree = LarderTests.store(h, LarderTests.ROOT);
        h.assertTrue(tree.insert(stack(Items.IRON_INGOT, 40), false).isEmpty(), "the tree took 40 iron");
        h.assertTrue(tree.insert(stack(Items.OAK_PLANKS, 30), false).isEmpty(), "and 30 planks");
        LarderTests.reserve(h, larder, 0, 16);
        BlockPos at = new BlockPos(2, 2, 8);
        PalmNet.palm(h, at, MouthMode.BOTH);
        veins(h, at.below().east(), LarderTests.ROOT.below().west());
        later(h, () -> {
            MenuPlayer player = open(h, at);
            put(player, 0, stack(Items.IRON_INGOT, 1));
            put(player, 1, stack(Items.OAK_PLANKS, 1));
            h.assertTrue(press(player, ShapingPalmMenu.RESTOCK), "the restock goes");
            h.assertValueEqual(cell(h, at, 0).getCount(), 25, "1 + the 24 above the Larder's reserve");
            h.assertValueEqual(cell(h, at, 1).getCount(), 31, "1 + all 30 planks");
            h.assertValueEqual(tree.count(stack(Items.IRON_INGOT, 1)), 16, "the Larder kept its 16");
            h.assertValueEqual(tree.count(stack(Items.OAK_PLANKS, 1)), 0, "the planks left the tree");
            MenuPlayer second = open(h, at, "palm_tree_second");
            h.assertFalse(
                    press(second, ShapingPalmMenu.RESTOCK) && tree.count(stack(Items.IRON_INGOT, 1)) < 16,
                    "asking again never takes the reserve");
            h.assertValueEqual(tree.count(stack(Items.IRON_INGOT, 1)), 16, "still 16");
            // return goes through the tree's own rules: the iron rejoins its label
            put(second, 2, stack(Items.IRON_INGOT, 3));
            h.assertTrue(press(second, ShapingPalmMenu.RETURN), "the return goes");
            h.assertValueEqual(
                    tree.count(stack(Items.IRON_INGOT, 1)), 16 + 25 + 3, "iron back in the tree: nothing lost");
            h.assertValueEqual(tree.count(stack(Items.OAK_PLANKS, 1)), 31, "planks back in the tree");
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aLarderReserveStaysInTheLarderWhateverTheGridAsks(GameTestHelper h) {
        BlockPos larder = LarderTests.aloneLarder(h, 0);
        mouth(h, larder.below(), MouthMode.BOTH, null);
        LarderTests.label(h, larder, 0, Items.IRON_INGOT);
        ItemStore store = (ItemStore) LarderTests.items(h, larder);
        h.assertTrue(store.insert(stack(Items.IRON_INGOT, 60), false).isEmpty(), "the Larder took 60 iron");
        LarderTests.reserve(h, larder, 0, 16);
        BlockPos at = new BlockPos(2, 2, 8);
        PalmNet.palm(h, at, MouthMode.BOTH);
        veins(h, at.below().east(), larder.below().west());
        later(h, () -> {
            MenuPlayer player = open(h, at);
            for (int cell = 0; cell < 9; cell++) put(player, cell, stack(Items.IRON_INGOT, 1));
            h.assertTrue(press(player, ShapingPalmMenu.RESTOCK), "the restock goes");
            h.assertValueEqual(inGrid(h, at, Items.IRON_INGOT), 9 + 44, "9 + the 44 above the reserve");
            h.assertValueEqual(store.count(stack(Items.IRON_INGOT, 1)), 16, "the reserve stays");
            h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "nothing more is free");
            h.assertValueEqual(store.count(stack(Items.IRON_INGOT, 1)), 16, "and still stays");
            // a return into a Larder goes through its own insertion rules
            h.assertTrue(press(open(h, at, "palm_larder_return"), ShapingPalmMenu.RETURN), "the return goes");
            h.assertValueEqual(
                    store.count(stack(Items.IRON_INGOT, 1)), 16 + 44 + 9, "every iron is in the Larder once");
        });
    }

    // ---- competing Palms ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void twoPalmsRestockingOneChestShareItWithoutDuplication(GameTestHelper h) {
        BlockPos first = PalmNet.palm(h, new BlockPos(2, 2, 2), MouthMode.BOTH);
        BlockPos second = PalmNet.palm(h, new BlockPos(2, 2, 4), MouthMode.BOTH);
        veins(h, new BlockPos(3, 1, 2), new BlockPos(3, 1, 4));
        BlockPos far = new BlockPos(4, 1, 3);
        mouth(h, far, MouthMode.OUTPUT, null);
        chest(h, far, stack(Items.COBBLESTONE, 64), stack(Items.COBBLESTONE, 36));
        later(h, () -> {
            MenuPlayer one = open(h, first, "palm_one");
            MenuPlayer two = open(h, second, "palm_two");
            put(one, 0, stack(Items.COBBLESTONE, 20));
            put(two, 0, stack(Items.COBBLESTONE, 20));
            h.assertTrue(press(one, ShapingPalmMenu.RESTOCK), "the first goes");
            h.assertValueEqual(cell(h, first, 0).getCount(), 64, "it tops up: 44 from the chest");
            h.assertTrue(press(two, ShapingPalmMenu.RESTOCK), "the second goes");
            h.assertValueEqual(cell(h, second, 0).getCount(), 64, "it tops up too: 44 more");
            h.assertValueEqual(count(h, far, Items.COBBLESTONE), 12, "100 - 44 - 44");
            h.assertValueEqual(
                    inGrid(h, first, Items.COBBLESTONE)
                            + inGrid(h, second, Items.COBBLESTONE)
                            + count(h, far, Items.COBBLESTONE),
                    140,
                    "no cobblestone lost or made");
            h.assertFalse(press(one, ShapingPalmMenu.RESTOCK), "a full cell takes no more");
        });
    }

    // ---- Return ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void returnSendsTheWholeGridToAnIntakeMouthsChestOverTheVeins(GameTestHelper h) {
        PalmNet.Rig rig = rig(h, 9, MouthMode.BOTH, MouthMode.INTAKE, stack(Items.OAK_PLANKS, 10));
        later(h, () -> {
            MenuPlayer player = open(h, rig.palm());
            put(player, 0, stack(Items.OAK_PLANKS, 30));
            put(player, 5, stack(Items.MILK_BUCKET, 1));
            h.assertTrue(press(player, ShapingPalmMenu.RETURN), "the return goes");
            h.assertValueEqual(player.saidKey(), STATUS + "returned", "and says so");
            h.assertTrue(grid(h, rig.palm()).isEmpty(), "the grid is empty");
            h.assertValueEqual(count(h, rig.far(), Items.OAK_PLANKS), 40, "every plank is in the chest");
            h.assertValueEqual(count(h, rig.far(), Items.MILK_BUCKET), 1, "and the bucket");
            h.assertFalse(press(player, ShapingPalmMenu.RETURN), "an empty grid has nothing to return");
            h.assertValueEqual(player.saidKey(), REFUSAL + "palm_grid_empty", "and says so");
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aFullDestinationTakesWhatFitsAndKeepsTheRestInTheGrid(GameTestHelper h) {
        ItemStack[] full = new ItemStack[27];
        for (int slot = 0; slot < full.length; slot++) full[slot] = stack(Items.DIRT, 64);
        full[26] = stack(Items.OAK_PLANKS, 60);
        PalmNet.Rig rig = rig(h, 5, MouthMode.BOTH, MouthMode.INTAKE, full);
        later(h, () -> {
            MenuPlayer player = open(h, rig.palm());
            put(player, 0, stack(Items.OAK_PLANKS, 10));
            put(player, 1, stack(Items.STICK, 5));
            h.assertTrue(press(player, ShapingPalmMenu.RETURN), "what fits goes");
            h.assertValueEqual(count(h, rig.far(), Items.OAK_PLANKS), 64, "the chest took 4 planks to fill its stack");
            h.assertValueEqual(cell(h, rig.palm(), 0).getCount(), 6, "the other 6 stay in their cell");
            h.assertValueEqual(cell(h, rig.palm(), 1).getCount(), 5, "the sticks, with no room, stay too");
            h.assertValueEqual(
                    inGrid(h, rig.palm(), Items.OAK_PLANKS) + count(h, rig.far(), Items.OAK_PLANKS),
                    70,
                    "no plank lost or made");
            h.assertFalse(press(player, ShapingPalmMenu.RETURN), "now nothing fits");
            h.assertValueEqual(player.saidKey(), REFUSAL + "palm_store_full", "the refusal says no Mouth takes it");
            h.assertValueEqual(cell(h, rig.palm(), 0).getCount(), 6, "and the grid is as it was");
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void whatTheFirstDestinationCannotTakeGoesToTheNextByPriority(GameTestHelper h) {
        BlockPos at = PalmNet.palm(h, PALM, MouthMode.BOTH);
        veins(h, at.below().east(), at.below().east(3));
        BlockPos nearly = at.below().east(4);
        ItemStack[] filled = new ItemStack[27];
        for (int slot = 0; slot < filled.length; slot++) filled[slot] = stack(Items.DIRT, 64);
        filled[26] = stack(Items.OAK_PLANKS, 60);
        mouth(h, nearly, MouthMode.INTAKE, "{priority:4}");
        chest(h, nearly, filled);
        BlockPos spare = at.below().east(2).south();
        mouth(h, spare, MouthMode.INTAKE, "{priority:0}");
        chest(h, spare);
        later(h, () -> {
            MenuPlayer player = open(h, at);
            put(player, 0, stack(Items.OAK_PLANKS, 10));
            h.assertTrue(press(player, ShapingPalmMenu.RETURN), "the return goes");
            h.assertValueEqual(count(h, nearly, Items.OAK_PLANKS), 64, "the high-priority chest was filled first");
            h.assertValueEqual(count(h, spare, Items.OAK_PLANKS), 6, "the low one took the other 6");
            h.assertTrue(grid(h, at).isEmpty(), "the grid is empty: every plank is somewhere once");
        });
    }

    // ---- Store result ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void storeResultCraftsOnceIntoTheNetworkAndKeepsRemainders(GameTestHelper h) {
        PalmNet.Rig rig = rig(h, 6, MouthMode.BOTH, MouthMode.INTAKE);
        later(h, () -> {
            MenuPlayer player = open(h, rig.palm());
            for (int cell : new int[] {0, 1, 2}) put(player, cell, stack(Items.MILK_BUCKET, 1));
            put(player, 3, stack(Items.SUGAR, 2));
            put(player, 4, stack(Items.EGG, 2));
            put(player, 5, stack(Items.SUGAR, 2));
            for (int cell : new int[] {6, 7, 8}) put(player, cell, stack(Items.WHEAT, 2));
            h.assertTrue(result(player).is(Items.CAKE), "the grid makes a cake");
            h.assertTrue(press(player, ShapingPalmMenu.STORE_RESULT), "the network takes it");
            h.assertValueEqual(player.saidKey(), STATUS + "stored", "and says so");
            h.assertValueEqual(count(h, rig.far(), Items.CAKE), 1, "one cake in the far chest");
            h.assertValueEqual(held(player, Items.CAKE), 0, "none in the player's inventory");
            h.assertValueEqual(player.crafted(Items.CAKE), 1, "it counts as the player's craft");
            h.assertValueEqual(inGrid(h, rig.palm(), Items.BUCKET), 3, "the three buckets went back to their cells");
            h.assertValueEqual(cell(h, rig.palm(), 4).getCount(), 1, "one egg left");
            h.assertTrue(result(player).isEmpty(), "and the grid no longer makes a cake");
            h.assertTrue(loose(h, rig.palm()).isEmpty(), "nothing fell on the ground");
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void storeResultRefusesWholeWhenNoMouthTakesItAndTheGridKeepsEverything(GameTestHelper h) {
        ItemStack[] full = new ItemStack[27];
        for (int slot = 0; slot < full.length; slot++) full[slot] = stack(Items.DIRT, 64);
        PalmNet.Rig rig = rig(h, 5, MouthMode.BOTH, MouthMode.INTAKE, full);
        later(h, () -> {
            MenuPlayer player = open(h, rig.palm());
            put(player, 0, stack(Items.OAK_PLANKS, 4));
            put(player, 3, stack(Items.OAK_PLANKS, 4));
            h.assertTrue(result(player).is(Items.STICK), "two planks in a column make sticks");
            h.assertFalse(press(player, ShapingPalmMenu.STORE_RESULT), "a network with no room refuses");
            h.assertValueEqual(player.saidKey(), REFUSAL + "palm_store_full", "and says so");
            h.assertValueEqual(inGrid(h, rig.palm(), Items.OAK_PLANKS), 8, "no plank was taken");
            h.assertValueEqual(player.crafted(Items.STICK), 0, "no craft was counted");
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void storeResultWithNoResultRefusesAndSoDoesARemainderWithNowhereToGo(GameTestHelper h) {
        PalmNet.Rig rig = rig(h, 5, MouthMode.BOTH, MouthMode.INTAKE);
        later(h, () -> {
            MenuPlayer player = open(h, rig.palm());
            h.assertFalse(press(player, ShapingPalmMenu.STORE_RESULT), "an empty grid has no result");
            h.assertValueEqual(player.saidKey(), REFUSAL + "palm_no_result", "and says so");
            for (int cell : new int[] {0, 1, 3, 4}) put(player, cell, stack(Items.HONEY_BOTTLE, 2));
            h.assertTrue(result(player).is(Items.HONEY_BLOCK), "four honey bottles make a honey block");
            fillInventory(player);
            h.assertFalse(press(player, ShapingPalmMenu.STORE_RESULT), "a glass bottle with nowhere to go refuses");
            h.assertValueEqual(player.saidKey(), REFUSAL + "palm_no_room", "and says there is no room");
            h.assertValueEqual(inGrid(h, rig.palm(), Items.HONEY_BOTTLE), 8, "no bottle was taken");
            h.assertValueEqual(count(h, rig.far(), Items.HONEY_BLOCK), 0, "nothing was stored");
            h.assertValueEqual(player.crafted(Items.HONEY_BLOCK), 0, "and no craft was counted");
        });
    }

    // ---- routes that break, shared work, unloaded endpoints ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aBrokenVeinStopsEveryTransferAndTheReconnectedOneRestoresIt(GameTestHelper h) {
        PalmNet.Rig rig = rig(h, 7, MouthMode.BOTH, MouthMode.BOTH, stack(Items.OAK_PLANKS, 20));
        BlockPos middle = new BlockPos(5, 1, 2);
        MenuPlayer[] player = new MenuPlayer[1];
        h.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    player[0] = open(h, rig.palm());
                    put(player[0], 0, stack(Items.OAK_PLANKS, 40));
                    h.assertTrue(press(player[0], ShapingPalmMenu.RESTOCK), "connected, the restock goes");
                    h.assertValueEqual(cell(h, rig.palm(), 0).getCount(), 60, "all 20 came");
                    h.setBlock(middle, Blocks.AIR);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    chest(h, rig.far(), stack(Items.OAK_PLANKS, 20));
                    h.assertFalse(press(player[0], ShapingPalmMenu.RESTOCK), "a broken vein: nothing arrives");
                    h.assertValueEqual(count(h, rig.far(), Items.OAK_PLANKS), 20, "the chest kept its planks");
                    h.assertFalse(press(player[0], ShapingPalmMenu.RETURN), "and nothing goes");
                    h.assertValueEqual(cell(h, rig.palm(), 0).getCount(), 60, "the grid kept what it held");
                    h.setBlock(middle, LogisticsModule.ITEM_VEIN.get());
                })
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    h.assertTrue(press(player[0], ShapingPalmMenu.RESTOCK), "reconnected, the restock goes again");
                    h.assertValueEqual(cell(h, rig.palm(), 0).getCount(), 64, "topped up from the chest");
                    h.assertValueEqual(count(h, rig.far(), Items.OAK_PLANKS), 16, "20 - 4");
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void whenThisTicksNetworkWorkIsSpentAnActionRefusesAndChangesNothing(GameTestHelper h) {
        PalmNet.Rig rig = rig(h, 5, MouthMode.BOTH, MouthMode.BOTH, stack(Items.OAK_PLANKS, 20));
        later(h, () -> {
            MenuPlayer player = open(h, rig.palm());
            put(player, 0, stack(Items.OAK_PLANKS, 1));
            ItemAccess access =
                    h.getLevel().getCapability(LogisticsCapabilities.ITEM_ACCESS, h.absolutePos(rig.door()), null);
            for (int i = 0; i < 300; i++) access.pull(List.of());
            h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "the tick's work is spent");
            h.assertValueEqual(player.saidKey(), ACCESS + "access_busy", "and the Palm says the network is busy");
            h.assertValueEqual(cell(h, rig.palm(), 0).getCount(), 1, "the grid is as it was");
            h.assertValueEqual(count(h, rig.far(), Items.OAK_PLANKS), 20, "and so is the chest");
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "palm_reach_unload",
            timeoutTicks = 1200)
    public static void aStoreInAnUnloadedChunkGivesNothingLoadsNothingAndComesBackWithItsChunk(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        int baseX = (origin.getX() & ~15) + 248000;
        int z = (origin.getZ() & ~15) + 248008;
        BlockPos at = new BlockPos(baseX + 12, 80, z);
        BlockPos far = new BlockPos(baseX + 17, 79, z);
        ChunkPos palmChunk = new ChunkPos(at);
        ChunkPos farChunk = new ChunkPos(far);
        h.assertFalse(palmChunk.equals(farChunk), "the two are in different chunks");
        TestChunks.force(level, palmChunk);
        TestChunks.force(level, farChunk);
        // the Palm's Mouth, a row of veins across the chunk border, the far Mouth with a chest of planks
        level.setBlockAndUpdate(
                at.below(),
                LogisticsModule.MOUTH
                        .get()
                        .defaultBlockState()
                        .setValue(org.jd.infestusfrontier.logistics.MouthBlock.MODE, MouthMode.BOTH));
        level.setBlockAndUpdate(at, ProcessingModule.SHAPING_PALM.get().defaultBlockState());
        for (BlockPos vein = at.below().east(); vein.getX() < far.getX(); vein = vein.east())
            level.setBlockAndUpdate(vein, LogisticsModule.ITEM_VEIN.get().defaultBlockState());
        level.setBlockAndUpdate(
                far,
                LogisticsModule.MOUTH
                        .get()
                        .defaultBlockState()
                        .setValue(org.jd.infestusfrontier.logistics.MouthBlock.MODE, MouthMode.OUTPUT));
        level.setBlockAndUpdate(far.above(), Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) level.getBlockEntity(far.above())).setItem(0, stack(Items.OAK_PLANKS, 30));
        MenuPlayer player = new MenuPlayer(level, "palm_unload");
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        useAt(level, player, stack(LogisticsModule.CONTRACTILE_FIBER.get(), 1), at);
        h.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    player.openMenu((net.minecraft.world.MenuProvider) level.getBlockEntity(at), at);
                    put(player, 0, stack(Items.OAK_PLANKS, 1));
                    h.assertTrue(press(player, ShapingPalmMenu.RESTOCK), "loaded, the far chest gives");
                    h.assertValueEqual(heldIn(level, at, 0), 31, "all 30 came");
                    ((ChestBlockEntity) level.getBlockEntity(far.above())).setItem(0, stack(Items.OAK_PLANKS, 30));
                    level.setChunkForced(farChunk.x, farChunk.z, false);
                    level.setChunkForced(palmChunk.x, palmChunk.z, false);
                    level.getChunkSource().addRegionTicket(TicketType.FORCED, palmChunk, 0, palmChunk);
                })
                .thenWaitUntil(() -> h.assertFalse(level.hasChunkAt(far), "the far chunk unloaded"))
                .thenExecute(() -> {
                    h.assertTrue(level.hasChunkAt(at), "the Palm's chunk is still there");
                    put(player, 1, stack(Items.OAK_PLANKS, 1));
                    h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "an unloaded endpoint gives nothing");
                    h.assertFalse(press(player, ShapingPalmMenu.RETURN), "and takes nothing");
                    h.assertFalse(level.hasChunkAt(far), "asking did not load the chunk");
                    h.assertValueEqual(heldIn(level, at, 1), 1, "the grid is untouched");
                    level.getChunkSource().removeRegionTicket(TicketType.FORCED, palmChunk, 0, palmChunk);
                    level.setChunkForced(palmChunk.x, palmChunk.z, false);
                    player.closeContainer();
                })
                .thenWaitUntil(() -> h.assertFalse(level.hasChunkAt(at), "the Palm's chunk unloaded too"))
                .thenExecute(() -> {
                    TestChunks.force(level, palmChunk);
                    TestChunks.force(level, farChunk);
                })
                .thenWaitUntil(() -> h.assertTrue(level.hasChunkAt(far) && level.hasChunkAt(at), "both are back"))
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    player.openMenu((net.minecraft.world.MenuProvider) level.getBlockEntity(at), at);
                    h.assertTrue(press(player, ShapingPalmMenu.RESTOCK), "the restock works again");
                    h.assertValueEqual(heldIn(level, at, 1), 31, "the cell holds 1 + 30, none made, none lost");
                    h.assertValueEqual(
                            ((ChestBlockEntity) level.getBlockEntity(far.above()))
                                    .getItem(0)
                                    .getCount(),
                            0,
                            "the chest gave all 30 planks");
                    level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                    level.setChunkForced(palmChunk.x, palmChunk.z, false);
                    level.setChunkForced(farChunk.x, farChunk.z, false);
                })
                .thenSucceed();
    }

    private static int heldIn(ServerLevel level, BlockPos at, int cell) {
        return ((org.jd.infestusfrontier.processing.api.PalmContents) level.getBlockEntity(at)
                        .collectComponents()
                        .getOrDefault(
                                ProcessingModule.PALM_CONTENTS.get(),
                                org.jd.infestusfrontier.processing.api.PalmContents.EMPTY))
                .cell(cell)
                .getCount();
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "palm_reach_unload",
            timeoutTicks = 1200)
    public static void aDoubleChestWhoseOtherHalfIsUnloadedGivesNothingAndLoadsNothing(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        int baseX = (origin.getX() & ~15) + 248064;
        int z = (origin.getZ() & ~15) + 248064;
        BlockPos at = new BlockPos(baseX + 12, 80, z);
        BlockPos near = new BlockPos(baseX + 15, 79, z);
        BlockPos farHalf = near.east();
        ChunkPos palmChunk = new ChunkPos(at);
        ChunkPos farChunk = new ChunkPos(farHalf);
        h.assertFalse(palmChunk.equals(farChunk), "the halves are in two chunks");
        TestChunks.force(level, palmChunk);
        TestChunks.force(level, farChunk);
        level.setBlockAndUpdate(
                at.below(),
                LogisticsModule.MOUTH
                        .get()
                        .defaultBlockState()
                        .setValue(org.jd.infestusfrontier.logistics.MouthBlock.MODE, MouthMode.BOTH));
        level.setBlockAndUpdate(at, ProcessingModule.SHAPING_PALM.get().defaultBlockState());
        for (BlockPos vein = at.below().east(); vein.getX() < near.getX(); vein = vein.east())
            level.setBlockAndUpdate(vein, LogisticsModule.ITEM_VEIN.get().defaultBlockState());
        level.setBlockAndUpdate(
                near,
                LogisticsModule.MOUTH
                        .get()
                        .defaultBlockState()
                        .setValue(org.jd.infestusfrontier.logistics.MouthBlock.MODE, MouthMode.OUTPUT));
        level.setBlockAndUpdate(
                near.above(),
                Blocks.CHEST
                        .defaultBlockState()
                        .setValue(ChestBlock.FACING, Direction.NORTH)
                        .setValue(ChestBlock.TYPE, ChestType.LEFT));
        level.setBlockAndUpdate(
                farHalf.above(),
                Blocks.CHEST
                        .defaultBlockState()
                        .setValue(ChestBlock.FACING, Direction.NORTH)
                        .setValue(ChestBlock.TYPE, ChestType.RIGHT));
        ((ChestBlockEntity) level.getBlockEntity(near.above())).setItem(0, stack(Items.OAK_PLANKS, 30));
        MenuPlayer player = new MenuPlayer(level, "palm_double");
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        useAt(level, player, stack(LogisticsModule.CONTRACTILE_FIBER.get(), 1), at);
        h.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    player.openMenu((net.minecraft.world.MenuProvider) level.getBlockEntity(at), at);
                    put(player, 0, stack(Items.OAK_PLANKS, 1));
                    level.setChunkForced(farChunk.x, farChunk.z, false);
                    level.setChunkForced(palmChunk.x, palmChunk.z, false);
                    level.getChunkSource().addRegionTicket(TicketType.FORCED, palmChunk, 0, palmChunk);
                })
                .thenWaitUntil(() -> h.assertFalse(level.hasChunkAt(farHalf), "the far half's chunk unloaded"))
                .thenExecute(() -> {
                    h.assertTrue(level.hasChunkAt(near), "the near half's chunk is still there");
                    h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "a half-unloaded double chest gives nothing");
                    h.assertFalse(level.hasChunkAt(farHalf), "asking did not load the far half's chunk");
                    h.assertValueEqual(heldIn(level, at, 0), 1, "nothing moved");
                    h.assertValueEqual(
                            ((ChestBlockEntity) level.getBlockEntity(near.above()))
                                    .getItem(0)
                                    .getCount(),
                            30,
                            "and the chest kept its planks");
                    level.getChunkSource().removeRegionTicket(TicketType.FORCED, palmChunk, 0, palmChunk);
                    player.closeContainer();
                    level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                })
                .thenSucceed();
    }

    // ---- saves from before Mouths carried the Palm's access ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aSavedDirectLinkGrantsNoAccessAndIsDroppedWhileTheRestOfThePalmLoads(GameTestHelper h) {
        BlockPos at = PalmNet.palm(h, PALM, MouthMode.BOTH);
        BlockPos chestAt = new BlockPos(5, 2, 2);
        h.setBlock(chestAt, Blocks.CHEST);
        ((ChestBlockEntity) h.getBlockEntity(chestAt)).setItem(0, stack(Items.OAK_PLANKS, 30));
        MenuPlayer filler = open(h, at);
        put(filler, 0, stack(Items.OAK_PLANKS, 5));
        put(filler, 4, stack(Items.STICK, 3));
        filler.closeContainer();
        var registries = h.getLevel().registryAccess();
        CompoundTag old = h.getBlockEntity(at).saveWithFullMetadata(registries);
        old.put("link", NbtUtils.writeBlockPos(h.absolutePos(chestAt)));
        h.getBlockEntity(at).loadWithComponents(old, registries);
        later(h, () -> {
            MenuPlayer player = open(h, at, "palm_old_save");
            h.assertValueEqual(inGrid(h, at, Items.OAK_PLANKS), 5, "the grid loads as it was");
            h.assertValueEqual(inGrid(h, at, Items.STICK), 3, "every cell");
            h.assertValueEqual(
                    ((ShapingPalmMenu) player.containerMenu).reachState(), ShapingPalmMenu.MOUTH_BOTH, "Reach is kept");
            h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "the old position reaches nothing");
            h.assertValueEqual(player.saidKey(), REFUSAL + "palm_store_short", "the Palm sees no stock");
            h.assertValueEqual(
                    ((ChestBlockEntity) h.getBlockEntity(chestAt)).countItem(Items.OAK_PLANKS),
                    30,
                    "the chest is untouched");
            CompoundTag saved = h.getBlockEntity(at).saveWithFullMetadata(registries);
            h.assertFalse(saved.contains("link"), "the position is not saved again");
            h.assertTrue(saved.getBoolean("reach"), "Reach is");
            // the same save as an item's block entity data
            ItemStack item = new ItemStack(ProcessingModule.SHAPING_PALM_ITEM.get());
            item.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(old));
            h.destroyBlock(at);
            MenuPlayer placer = new MenuPlayer(h.getLevel(), "palm_old_item");
            placer.setItemInHand(InteractionHand.MAIN_HAND, item);
            h.placeAt(placer, item, at.below(), Direction.UP);
            h.assertFalse(
                    h.getBlockEntity(at).saveWithFullMetadata(registries).contains("link"),
                    "a link on an old item is dropped too");
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anUnreadablePalmKeepsItsBytesWithoutTheOldLinkAndActsOnNothing(GameTestHelper h) {
        PalmNet.Rig rig = rig(h, 5, MouthMode.BOTH, MouthMode.BOTH, stack(Items.OAK_PLANKS, 20));
        var registries = h.getLevel().registryAccess();
        CompoundTag tag = h.getBlockEntity(rig.palm()).saveWithFullMetadata(registries);
        tag.put("link", NbtUtils.writeBlockPos(h.absolutePos(rig.chest())));
        tag.getCompound("contents").getList("grid", 10).getCompound(0).putInt("count", 99);
        tag.getCompound("contents").getList("grid", 10).getCompound(0).putString("id", "minecraft:oak_planks");
        h.getBlockEntity(rig.palm()).loadWithComponents(tag, registries);
        later(h, () -> {
            MenuPlayer player = open(h, rig.palm());
            h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "an unreadable save");
            h.assertValueEqual(player.saidKey(), "infestusfrontier.refusal.organ.unreadable_save", "says so");
            CompoundTag saved = h.getBlockEntity(rig.palm()).saveWithFullMetadata(registries);
            h.assertFalse(saved.contains("link"), "the old position is not kept");
            h.assertTrue(saved.contains("contents"), "the bytes are");
            h.assertValueEqual(count(h, rig.far(), Items.OAK_PLANKS), 20, "the chest was never touched");
        });
    }

    // ---- the probe pairs nothing ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theProbeOnAPalmOpensItsMouthAndPairsNothing(GameTestHelper h) {
        PalmNet.Rig rig = rig(h, 5, MouthMode.BOTH, MouthMode.BOTH, stack(Items.OAK_PLANKS, 20));
        MenuPlayer player = new MenuPlayer(h.getLevel(), "palm_probe");
        ItemStack probe = stack(InteractionModule.SYNAPTIC_PROBE.get(), 1);
        use(h, player, probe, rig.palm());
        h.assertTrue(
                player.containerMenu instanceof org.jd.infestusfrontier.logistics.MouthMenu,
                "the probe on the Palm opens the Mouth it stands on");
        player.closeContainer();
        use(h, player, player.getMainHandItem(), rig.chest());
        h.assertTrue(
                player.getMainHandItem().getComponentsPatch().isEmpty(),
                "the probe holds nothing: no Palm is picked, no store is offered");
        h.assertValueEqual(cell(h, rig.palm(), 0).getCount(), 0, "and the Palm gained no store from either click");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aPalmWithNoReachOrNoFoundationOrAnUnreadableSaveActsOnNothing(GameTestHelper h) {
        BlockPos at = palm(h);
        mouth(h, at.below(), MouthMode.BOTH, null);
        BlockPos far = at.below().east(3);
        veins(h, at.below().east(), far.west());
        mouth(h, far, MouthMode.BOTH, null);
        chest(h, far, stack(Items.OAK_PLANKS, 20));
        later(h, () -> {
            MenuPlayer player = open(h, at);
            put(player, 0, stack(Items.OAK_PLANKS, 1));
            h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "no Reach");
            h.assertValueEqual(player.saidKey(), REFUSAL + "palm_no_reach", "says so");
            reach(h, at);
            h.setBlock(at.below(), Blocks.STONE);
            h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "no foundation");
            h.assertValueEqual(player.saidKey(), REFUSAL + "palm_no_foundation", "says so");
            mouth(h, at.below(), MouthMode.BOTH, null);
            h.assertValueEqual(count(h, far, Items.OAK_PLANKS), 20, "the chest was never touched");
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void anActionStopsAfterOnePassAndNothingIsCraftedOrSpilled(GameTestHelper h) {
        PalmNet.Rig rig = rig(
                h,
                5,
                MouthMode.BOTH,
                MouthMode.BOTH,
                stack(Items.OAK_PLANKS, 64),
                stack(Items.STICK, 64),
                stack(Items.COBBLESTONE, 64));
        MenuPlayer[] player = new MenuPlayer[1];
        h.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    player[0] = open(h, rig.palm());
                    put(player[0], 0, stack(Items.OAK_PLANKS, 1));
                    put(player[0], 3, stack(Items.OAK_PLANKS, 1));
                    h.assertTrue(press(player[0], ShapingPalmMenu.RESTOCK), "one restock");
                    h.assertValueEqual(cell(h, rig.palm(), 0).getCount(), 33, "it shared the 64 planks: 1 + 32 each");
                    h.assertTrue(result(player[0]).is(Items.STICK), "the grid makes sticks");
                    h.assertValueEqual(player[0].crafted(Items.STICK), 0, "restocking crafted nothing");
                    h.assertTrue(press(player[0], ShapingPalmMenu.STORE_RESULT), "one store-result craft");
                    h.assertValueEqual(player[0].crafted(Items.STICK), 4, "exactly one craft: its four sticks");
                })
                .thenIdle(80)
                .thenExecute(() -> {
                    h.assertValueEqual(player[0].crafted(Items.STICK), 4, "still the one craft");
                    h.assertValueEqual(
                            cell(h, rig.palm(), 0).getCount(), 32, "the grid is where the one craft left it");
                    h.assertTrue(
                            h.getLevel()
                                    .getEntitiesOfClass(
                                            ItemEntity.class,
                                            new net.minecraft.world.phys.AABB(h.absolutePos(rig.palm())).inflate(8))
                                    .isEmpty(),
                            "and nothing was spilled");
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theScreenReadsTheReachStateAndTheMouthsDirection(GameTestHelper h) {
        BlockPos at = palm(h);
        h.assertValueEqual(
                ((ShapingPalmMenu) open(h, at).containerMenu).reachState(), ShapingPalmMenu.NO_REACH, "no Reach");
        reach(h, at);
        h.assertValueEqual(
                ((ShapingPalmMenu) open(h, at).containerMenu).reachState(),
                ShapingPalmMenu.NO_MOUTH,
                "Reach, no Mouth");
        int[] expected = {
            ShapingPalmMenu.MOUTH_TAKES,
            ShapingPalmMenu.MOUTH_GIVES,
            ShapingPalmMenu.MOUTH_BOTH,
            ShapingPalmMenu.MOUTH_CLOSED
        };
        MouthMode[] modes = {MouthMode.INTAKE, MouthMode.OUTPUT, MouthMode.BOTH, MouthMode.CLOSED};
        for (int i = 0; i < modes.length; i++) {
            mouth(h, at.below(), modes[i], null);
            h.assertValueEqual(((ShapingPalmMenu) open(h, at).containerMenu).reachState(), expected[i], "" + modes[i]);
        }
        h.succeed();
    }

    /** A use on an absolute position, for the tests that build far from the test region. */
    private static void useAt(ServerLevel level, MenuPlayer player, ItemStack held, BlockPos at) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false);
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }
}
