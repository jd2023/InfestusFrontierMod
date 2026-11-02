package org.jd.infestusfrontier.testmod;

import static org.jd.infestusfrontier.testmod.ShapingPalmTests.RESULT;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.cell;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.fillInventory;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.grid;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.held;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.in;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.loose;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.open;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.palm;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.put;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.reload;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.result;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.stack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.interaction.InteractionModule;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.ShapingPalmMenu;
import org.jd.infestusfrontier.storage.api.ItemStore;
import org.jd.infestusfrontier.testmod.ShapingPalmTests.MenuPlayer;

/**
 * The Shaping Palm's supply link (BLOCK_CATALOG T1-41, stage 2). The Palm gets Reach from a Contractile Fiber, the
 * probe links it to one store, and its screen's three buttons act on that store alone. Every test counts items across
 * the grid, the store and the player, so a refused, partial or competing action shows a lost or duplicated item.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class PalmReachTests {
    private static final BlockPos CHEST = new BlockPos(5, 2, 2);
    private static final String REFUSAL = "infestusfrontier.refusal.processing.";
    private static final String STATUS = "infestusfrontier.status.palm.";

    private PalmReachTests() {}

    // ---- helpers ----

    private static void use(GameTestHelper h, MenuPlayer player, ItemStack held, BlockPos at) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockPos absolute = h.absolutePos(at);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        player.moveTo(absolute.getX() + 0.5, absolute.getY() + 1, absolute.getZ() - 1.5);
        player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    /** Gives the Palm at {@code at} Reach with one fiber used on it. */
    private static void reach(GameTestHelper h, BlockPos at) {
        MenuPlayer player = new MenuPlayer(h.getLevel(), "palm_fiber");
        use(h, player, stack(LogisticsModule.CONTRACTILE_FIBER.get(), 1), at);
        h.assertTrue(
                h.getBlockEntity(at).collectComponents().getOrDefault(ProcessingModule.PALM_REACH.get(), false)
                        == Boolean.TRUE,
                "the fiber gave the Palm Reach");
    }

    /** The probe on the Palm and then on {@code store}; returns what the second use said. */
    private static String link(GameTestHelper h, BlockPos at, BlockPos store) {
        MenuPlayer player = new MenuPlayer(h.getLevel(), "palm_probe");
        ItemStack probe = stack(InteractionModule.SYNAPTIC_PROBE.get(), 1);
        use(h, player, probe, at);
        player.saidKey();
        use(h, player, player.getMainHandItem(), store);
        return player.saidKey();
    }

    private static BlockPos chest(GameTestHelper h, BlockPos at, ItemStack... contents) {
        h.setBlock(at, Blocks.CHEST);
        ChestBlockEntity box = h.getBlockEntity(at);
        for (int slot = 0; slot < contents.length; slot++) box.setItem(slot, contents[slot]);
        return at;
    }

    private static int count(GameTestHelper h, BlockPos chest, Item item) {
        ChestBlockEntity box = h.getBlockEntity(chest);
        int count = 0;
        for (int slot = 0; slot < box.getContainerSize(); slot++)
            if (box.getItem(slot).is(item)) count += box.getItem(slot).getCount();
        return count;
    }

    private static int inGrid(GameTestHelper h, BlockPos at, Item item) {
        int count = 0;
        for (ItemStack stack : grid(h, at).grid()) if (stack.is(item)) count += stack.getCount();
        return count;
    }

    /** A Palm with Reach, linked to a chest holding {@code contents}. */
    private static BlockPos linked(GameTestHelper h, ItemStack... contents) {
        BlockPos at = palm(h);
        chest(h, CHEST, contents);
        reach(h, at);
        h.assertValueEqual(link(h, at, CHEST), STATUS + "linked", "the probe linked the chest");
        return at;
    }

    private static boolean press(MenuPlayer player, int button) {
        return player.containerMenu.clickMenuButton(player, button);
    }

    private static BlockPos larderAt() {
        return LarderTests.ROOT;
    }

    /** A lone Larder of 60 iron ingots keeping {@code reserve}, and a Palm with Reach linked to it. */
    private static BlockPos palmAndLarder(GameTestHelper h, int reserve) {
        BlockPos larder = LarderTests.aloneLarder(h, 0);
        LarderTests.label(h, larder, 0, Items.IRON_INGOT);
        ItemStore store = (ItemStore) LarderTests.items(h, larder);
        h.assertTrue(store.insert(stack(Items.IRON_INGOT, 60), false).isEmpty(), "the Larder took 60 iron");
        LarderTests.reserve(h, larder, 0, reserve);
        BlockPos at = larder.west(3);
        h.setBlock(at.below(), EcologyTests.mature());
        h.setBlock(at, ProcessingModule.SHAPING_PALM.get());
        reach(h, at);
        h.assertValueEqual(link(h, at, larder), STATUS + "linked", "the probe linked the Larder");
        return at;
    }

    private static int stock(GameTestHelper h, BlockPos larder, ItemStack like) {
        return ((ItemStore) LarderTests.items(h, larder)).count(like);
    }

    // ---- Reach and the link ----

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
        h.setBlock(CHEST, Blocks.STONE);
        ItemStack fiber = stack(LogisticsModule.CONTRACTILE_FIBER.get(), 1);
        use(h, player, fiber, CHEST);
        h.assertValueEqual(fiber.getCount(), 1, "stone does not take the fiber");
        h.assertTrue(
                h.getBlockEntity(at).collectComponents().get(ProcessingModule.PALM_REACH.get()) == null,
                "the Palm was not touched");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void reachIsCarriedByTheBrokenPalmButTheLinkIsNot(GameTestHelper h) {
        BlockPos at = linked(h, stack(Items.OAK_PLANKS, 8));
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.OAK_PLANKS, 2));
        player.closeContainer();
        h.getLevel().destroyBlock(h.absolutePos(at), true);
        java.util.List<ItemEntity> drops = loose(h, at);
        h.assertValueEqual(drops.size(), 1, "one drop, the Palm");
        ItemStack dropped = drops.get(0).getItem();
        h.assertTrue(
                dropped.getOrDefault(ProcessingModule.PALM_REACH.get(), false), "the item carries Reach, as it was");
        h.assertTrue(
                dropped.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA) == null
                        || !dropped.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA)
                                .copyTag()
                                .contains("link"),
                "no link rides on the item");
        drops.get(0).discard();
        MenuPlayer placer = new MenuPlayer(h.getLevel(), "palm_placer");
        placer.setItemInHand(InteractionHand.MAIN_HAND, dropped.copy());
        h.placeAt(placer, placer.getMainHandItem(), at.below(), Direction.UP);
        h.assertBlockPresent(ProcessingModule.SHAPING_PALM.get(), at);
        h.assertValueEqual(inGrid(h, at, Items.OAK_PLANKS), 2, "its grid came back");
        MenuPlayer viewer = open(h, at);
        h.assertValueEqual(((ShapingPalmMenu) viewer.containerMenu).reachState(), 1, "with Reach and no link");
        h.assertFalse(press(viewer, ShapingPalmMenu.RESTOCK), "no link, no restock");
        h.assertValueEqual(viewer.saidKey(), REFUSAL + "palm_not_linked", "and it says why");
        h.assertValueEqual(count(h, CHEST, Items.OAK_PLANKS), 8, "the chest was not touched");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theLinkIsAPositionThatSurvivesSaveAndReloadAndShowsOnTheScreen(GameTestHelper h) {
        BlockPos at = linked(h);
        reload(h, at);
        MenuPlayer player = open(h, at);
        ShapingPalmMenu menu = (ShapingPalmMenu) player.containerMenu;
        h.assertValueEqual(menu.reachState(), 2, "Reach and the link are back");
        h.assertValueEqual(
                java.util.List.of(menu.linkOffset(0), menu.linkOffset(1), menu.linkOffset(2)),
                java.util.List.of(CHEST.getX() - at.getX(), 0, 0),
                "the screen shows the store's offset");
        CompoundTag saved =
                h.getBlockEntity(at).saveWithFullMetadata(h.getLevel().registryAccess());
        h.assertTrue(saved.getBoolean("reach"), "Reach is saved");
        h.assertValueEqual(
                NbtUtils.readBlockPos(saved, "link").orElseThrow(),
                h.absolutePos(CHEST),
                "the link is the one position, saved as is");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aSavedLinkBeyondRangeIsDroppedWhenTheOrganLoads(GameTestHelper h) {
        BlockPos at = linked(h);
        var registries = h.getLevel().registryAccess();
        CompoundTag tag = h.getBlockEntity(at).saveWithFullMetadata(registries);
        tag.put("link", NbtUtils.writeBlockPos(h.absolutePos(at).offset(9, 0, 0)));
        h.getBlockEntity(at).loadWithComponents(tag, registries);
        MenuPlayer player = open(h, at);
        h.assertValueEqual(((ShapingPalmMenu) player.containerMenu).reachState(), 1, "Reach stays, the link is gone");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theProbeRefusesAStoreBeyondEightBlocksAnHandlerlessBlockAndAPalmWithoutReach(GameTestHelper h) {
        BlockPos at = palm(h);
        h.setBlock(CHEST, Blocks.STONE);
        MenuPlayer player = new MenuPlayer(h.getLevel(), "palm_probe");
        use(h, player, stack(InteractionModule.SYNAPTIC_PROBE.get(), 1), at);
        h.assertValueEqual(
                player.saidKey(), "", "a Palm without Reach is not picked: the probe leaves it to its screen");
        h.assertTrue(!player.getMainHandItem().has(InteractionModule.PALM_SOURCE.get()), "so the probe holds no Palm");
        reach(h, at);
        use(h, player, player.getMainHandItem(), at);
        h.assertValueEqual(player.saidKey(), STATUS + "picked", "with Reach it is picked");
        use(h, player, player.getMainHandItem(), CHEST);
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_link_no_store", "stone holds nothing");
        BlockPos far = at.offset(9, 0, 0);
        h.setBlock(far.below(), Blocks.STONE);
        chest(h, far, stack(Items.APPLE, 1));
        use(h, player, player.getMainHandItem(), far);
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_link_range", "a chest 9 blocks off is too far");
        h.assertTrue(player.getMainHandItem().has(InteractionModule.PALM_SOURCE.get()), "the pick is kept to retry");
        use(h, player, player.getMainHandItem(), at.above(3));
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_link_no_store", "so is the air");
        h.assertValueEqual(((ShapingPalmMenu) open(h, at).containerMenu).reachState(), 1, "no refusal made a link");
        chest(h, CHEST, stack(Items.APPLE, 1));
        use(h, player, player.getMainHandItem(), CHEST);
        h.assertValueEqual(player.saidKey(), STATUS + "linked", "a chest within range links");
        h.assertTrue(!player.getMainHandItem().has(InteractionModule.PALM_SOURCE.get()), "and the pick is spent");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aPickedPalmInAnotherDimensionOrGoneRefusesAndLetsGoOnTheAir(GameTestHelper h) {
        BlockPos at = palm(h);
        reach(h, at);
        chest(h, CHEST, stack(Items.APPLE, 1));
        MenuPlayer player = new MenuPlayer(h.getLevel(), "palm_probe");
        ItemStack probe = stack(InteractionModule.SYNAPTIC_PROBE.get(), 1);
        probe.set(InteractionModule.PALM_SOURCE.get(), GlobalPos.of(Level.NETHER, h.absolutePos(at)));
        use(h, player, probe, CHEST);
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_link_dimension", "another dimension is refused");
        probe.set(
                InteractionModule.PALM_SOURCE.get(),
                GlobalPos.of(h.getLevel().dimension(), h.absolutePos(at.south(4))));
        use(h, player, probe, CHEST);
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_link_gone", "a Palm that is not there is refused");
        h.assertTrue(!probe.has(InteractionModule.PALM_SOURCE.get()), "and the pick is let go");
        probe.set(InteractionModule.PALM_SOURCE.get(), GlobalPos.of(h.getLevel().dimension(), h.absolutePos(at)));
        probe.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(!probe.has(InteractionModule.PALM_SOURCE.get()), "a use on the air lets the Palm go");
        h.assertValueEqual(player.saidKey(), STATUS + "let_go", "and says so");
        h.succeed();
    }

    // ---- Restock ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void restockTopsEveryFilledCellUpFromTheStoreAndLeavesEmptyCellsEmpty(GameTestHelper h) {
        BlockPos at = linked(h, stack(Items.OAK_PLANKS, 40), stack(Items.STICK, 10), stack(Items.APPLE, 5));
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.OAK_PLANKS, 60));
        put(player, 4, stack(Items.STICK, 3));
        h.assertTrue(press(player, ShapingPalmMenu.RESTOCK), "the restock goes");
        h.assertValueEqual(player.saidKey(), STATUS + "restocked", "and says so");
        h.assertValueEqual(cell(h, at, 0).getCount(), 64, "a cell tops up to a full stack");
        h.assertValueEqual(cell(h, at, 4).getCount(), 13, "or takes all the store has: 3 + 10");
        h.assertTrue(cell(h, at, 8).isEmpty(), "an empty cell stays empty: the Palm does not guess a recipe");
        h.assertValueEqual(count(h, CHEST, Items.OAK_PLANKS), 36, "40 + 60 planks, 64 in the grid");
        h.assertValueEqual(count(h, CHEST, Items.STICK), 0, "every stick left the chest");
        h.assertValueEqual(count(h, CHEST, Items.APPLE), 5, "an item the grid does not hold is not touched");
        h.assertValueEqual(
                inGrid(h, at, Items.OAK_PLANKS) + count(h, CHEST, Items.OAK_PLANKS), 100, "no plank lost or made");
        h.assertValueEqual(inGrid(h, at, Items.STICK) + count(h, CHEST, Items.STICK), 13, "no stick lost or made");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void restockWithNothingToGiveRefusesReadablyAndChangesNothing(GameTestHelper h) {
        BlockPos at = linked(h, stack(Items.APPLE, 5));
        MenuPlayer player = open(h, at);
        h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "an empty grid has nothing to restock");
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_grid_empty", "and says so");
        put(player, 0, stack(Items.OAK_PLANKS, 5));
        h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "a store without the kind gives nothing");
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_store_short", "and says the store is short");
        h.assertValueEqual(cell(h, at, 0).getCount(), 5, "the grid is as it was");
        h.assertValueEqual(count(h, CHEST, Items.APPLE), 5, "and so is the store");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aFullGridRefusesRestockAndTheStoreKeepsEverything(GameTestHelper h) {
        BlockPos at = linked(h, stack(Items.OAK_PLANKS, 40));
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.OAK_PLANKS, 64));
        h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "a full stack wants nothing");
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_grid_full", "and says it is full");
        h.assertValueEqual(count(h, CHEST, Items.OAK_PLANKS), 40, "the chest is untouched");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void restockMatchesComponentsAndLeavesALookalikeAlone(GameTestHelper h) {
        ItemStack named = stack(Items.IRON_INGOT, 6);
        named.set(
                net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal("Tin"));
        BlockPos at = linked(h, stack(Items.IRON_INGOT, 20), named.copy());
        MenuPlayer player = open(h, at);
        put(player, 0, named.copyWithCount(1));
        put(player, 1, stack(Items.IRON_INGOT, 1));
        h.assertTrue(press(player, ShapingPalmMenu.RESTOCK), "the restock goes");
        h.assertValueEqual(cell(h, at, 0).getCount(), 7, "the named cell took the 6 named ingots only");
        h.assertTrue(
                net.minecraft.world.item.ItemStack.isSameItemSameComponents(cell(h, at, 0), named),
                "and is still the named kind");
        h.assertValueEqual(cell(h, at, 1).getCount(), 21, "the plain cell took the 20 plain ingots only");
        h.assertValueEqual(count(h, CHEST, Items.IRON_INGOT), 0, "both kinds left the chest, each to its own cell");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void manualInputsStayUsableWithALinkAndCraftWithoutOne(GameTestHelper h) {
        BlockPos at = linked(h, stack(Items.OAK_PLANKS, 8));
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.OAK_PLANKS, 1));
        put(player, 3, stack(Items.OAK_PLANKS, 1));
        h.assertTrue(result(player).is(Items.STICK), "two planks in a column make sticks by hand");
        player.containerMenu.clicked(RESULT, 0, ClickType.PICKUP, player);
        h.assertTrue(player.containerMenu.getCarried().is(Items.STICK), "the manual craft works as before");
        h.assertValueEqual(count(h, CHEST, Items.OAK_PLANKS), 8, "and used no stock of the store");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aLarderReserveStaysInTheLarderWhateverTheGridAsks(GameTestHelper h) {
        BlockPos at = palmAndLarder(h, 16);
        ItemStack iron = stack(Items.IRON_INGOT, 1);
        MenuPlayer player = open(h, at);
        put(player, 0, iron.copy());
        h.assertTrue(press(player, ShapingPalmMenu.RESTOCK), "the free part is given");
        h.assertValueEqual(cell(h, at, 0).getCount(), 45, "1 held + the 44 above the reserve of 16");
        h.assertValueEqual(stock(h, larderAt(), iron), 16, "the reserve is still in the Larder");
        h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "a second restock finds nothing it may take");
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_store_short", "protected stock reads as short");
        h.assertValueEqual(stock(h, larderAt(), iron), 16, "still 16");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aReserveRaisedBetweenSimulationAndRestockIsHonoured(GameTestHelper h) {
        BlockPos at = palmAndLarder(h, 16);
        ItemStack iron = stack(Items.IRON_INGOT, 1);
        IItemHandler handler = LarderTests.items(h, larderAt());
        h.assertValueEqual(handler.extractItem(0, 64, true).getCount(), 44, "the simulation sees the free 44");
        LarderTests.reserve(h, larderAt(), 0, 32);
        MenuPlayer player = open(h, at);
        put(player, 0, iron.copy());
        h.assertTrue(press(player, ShapingPalmMenu.RESTOCK), "the restock goes");
        h.assertValueEqual(cell(h, at, 0).getCount(), 29, "it took 28, what the new reserve left, not 44");
        h.assertValueEqual(stock(h, larderAt(), iron), 32, "the Larder keeps its new reserve");
        LarderTests.reserve(h, larderAt(), 0, 64);
        h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "a reserve above the stock gives nothing");
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_store_short", "and reads as short");
        h.assertValueEqual(stock(h, larderAt(), iron), 32, "nothing moved");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCompetingConsumerTakingFromTheStoreFirstLeavesTheRestockTheRest(GameTestHelper h) {
        BlockPos at = linked(h, stack(Items.OAK_PLANKS, 30));
        MenuPlayer first = open(h, at, "palm_first");
        MenuPlayer second = open(h, at, "palm_second");
        put(first, 0, stack(Items.OAK_PLANKS, 1));
        IItemHandler handler = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(CHEST), null);
        ItemStack stolen = handler.extractItem(0, 20, false);
        h.assertValueEqual(stolen.getCount(), 20, "a hopper took 20 first");
        h.assertTrue(press(second, ShapingPalmMenu.RESTOCK), "the second viewer's restock goes");
        h.assertValueEqual(cell(h, at, 0).getCount(), 11, "it got the 10 that were left");
        h.assertFalse(press(first, ShapingPalmMenu.RESTOCK), "the first viewer finds nothing more");
        h.assertValueEqual(first.saidKey(), REFUSAL + "palm_store_short", "and is told");
        h.assertValueEqual(
                inGrid(h, at, Items.OAK_PLANKS) + count(h, CHEST, Items.OAK_PLANKS) + stolen.getCount(),
                31,
                "every plank is in exactly one place");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void twoViewersRestockingOneStoreShareItWithoutDuplication(GameTestHelper h) {
        BlockPos at = linked(h, stack(Items.COBBLESTONE, 40));
        MenuPlayer one = open(h, at, "palm_one");
        MenuPlayer two = open(h, at, "palm_two");
        put(one, 0, stack(Items.COBBLESTONE, 40));
        put(one, 1, stack(Items.COBBLESTONE, 40));
        h.assertTrue(press(one, ShapingPalmMenu.RESTOCK), "the first restock goes");
        h.assertTrue(in(two, 0).getCount() == cell(h, at, 0).getCount(), "the other viewer sees the new grid");
        h.assertFalse(press(two, ShapingPalmMenu.RESTOCK), "the second finds the store empty");
        h.assertValueEqual(count(h, CHEST, Items.COBBLESTONE), 0, "the store is empty");
        h.assertValueEqual(inGrid(h, at, Items.COBBLESTONE), 120, "80 + 40 cobblestone, none made, none lost");
        h.succeed();
    }

    // ---- Return ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void returnMovesTheWholeGridIntoTheStore(GameTestHelper h) {
        BlockPos at = linked(h, stack(Items.OAK_PLANKS, 10));
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.OAK_PLANKS, 30));
        put(player, 5, stack(Items.MILK_BUCKET, 1));
        h.assertTrue(press(player, ShapingPalmMenu.RETURN), "the return goes");
        h.assertValueEqual(player.saidKey(), STATUS + "returned", "and says so");
        h.assertTrue(grid(h, at).isEmpty(), "the grid is empty");
        h.assertValueEqual(count(h, CHEST, Items.OAK_PLANKS), 40, "every plank is in the chest");
        h.assertValueEqual(count(h, CHEST, Items.MILK_BUCKET), 1, "and the bucket");
        h.assertFalse(press(player, ShapingPalmMenu.RETURN), "an empty grid has nothing to return");
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_grid_empty", "and says so");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aFullStoreTakesWhatFitsAndKeepsTheRestInTheGrid(GameTestHelper h) {
        ItemStack[] full = new ItemStack[27];
        for (int slot = 0; slot < full.length; slot++) full[slot] = stack(Items.DIRT, 64);
        full[26] = stack(Items.OAK_PLANKS, 60);
        BlockPos at = linked(h, full);
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.OAK_PLANKS, 10));
        put(player, 1, stack(Items.STICK, 5));
        h.assertTrue(press(player, ShapingPalmMenu.RETURN), "what fits goes");
        h.assertValueEqual(count(h, CHEST, Items.OAK_PLANKS), 64, "the chest took 4 planks to fill its stack");
        h.assertValueEqual(cell(h, at, 0).getCount(), 6, "the other 6 stay in their cell");
        h.assertValueEqual(cell(h, at, 1).getCount(), 5, "the sticks, with no room, stay too");
        h.assertValueEqual(
                inGrid(h, at, Items.OAK_PLANKS) + count(h, CHEST, Items.OAK_PLANKS), 70, "no plank lost or made");
        h.assertFalse(press(player, ShapingPalmMenu.RETURN), "now nothing fits");
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_store_full", "the refusal says the store is full");
        h.assertValueEqual(cell(h, at, 0).getCount(), 6, "and the grid is as it was");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aReturnToALarderGoesThroughItsOwnInsertionRules(GameTestHelper h) {
        BlockPos at = palmAndLarder(h, 16);
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.IRON_INGOT, 3));
        put(player, 1, stack(Items.APPLE, 7));
        h.assertTrue(press(player, ShapingPalmMenu.RETURN), "the return goes");
        h.assertValueEqual(stock(h, larderAt(), stack(Items.IRON_INGOT, 1)), 63, "the iron joined its label: 60 + 3");
        h.assertValueEqual(stock(h, larderAt(), stack(Items.APPLE, 1)), 7, "the apples took a free compartment");
        h.assertTrue(grid(h, at).isEmpty(), "the grid is empty: every item is in the Larder once");
        h.succeed();
    }

    // ---- Store result ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void storeResultCraftsOnceIntoTheStoreAndKeepsRemainders(GameTestHelper h) {
        BlockPos at = linked(h);
        MenuPlayer player = open(h, at);
        for (int cell : new int[] {0, 1, 2}) put(player, cell, stack(Items.MILK_BUCKET, 1));
        put(player, 3, stack(Items.SUGAR, 2));
        put(player, 4, stack(Items.EGG, 2));
        put(player, 5, stack(Items.SUGAR, 2));
        for (int cell : new int[] {6, 7, 8}) put(player, cell, stack(Items.WHEAT, 2));
        h.assertTrue(result(player).is(Items.CAKE), "the grid makes a cake");
        h.assertTrue(press(player, ShapingPalmMenu.STORE_RESULT), "the store takes it");
        h.assertValueEqual(player.saidKey(), STATUS + "stored", "and says so");
        h.assertValueEqual(count(h, CHEST, Items.CAKE), 1, "one cake in the chest");
        h.assertValueEqual(held(player, Items.CAKE), 0, "none in the player's inventory");
        h.assertValueEqual(player.crafted(Items.CAKE), 1, "it counts as the player's craft");
        h.assertValueEqual(inGrid(h, at, Items.BUCKET), 3, "the three buckets went back to their emptied cells");
        h.assertValueEqual(cell(h, at, 4).getCount(), 1, "one egg left");
        h.assertTrue(result(player).isEmpty(), "and the grid no longer makes a cake");
        h.assertTrue(loose(h, at).isEmpty(), "nothing fell on the ground");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void storeResultIntoAFullStoreRefusesWholeAndTheGridKeepsEverything(GameTestHelper h) {
        ItemStack[] full = new ItemStack[27];
        for (int slot = 0; slot < full.length; slot++) full[slot] = stack(Items.DIRT, 64);
        BlockPos at = linked(h, full);
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.OAK_PLANKS, 4));
        put(player, 3, stack(Items.OAK_PLANKS, 4));
        h.assertTrue(result(player).is(Items.STICK), "two planks in a column make sticks");
        h.assertFalse(press(player, ShapingPalmMenu.STORE_RESULT), "a store with no room refuses");
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_store_full", "and says so");
        h.assertValueEqual(inGrid(h, at, Items.OAK_PLANKS), 8, "no plank was taken");
        h.assertValueEqual(player.crafted(Items.STICK), 0, "no craft was counted");

        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void storeResultWithNoResultRefusesAndSoDoesARemainderWithNowhereToGo(GameTestHelper h) {
        BlockPos at = linked(h);
        MenuPlayer player = open(h, at);
        h.assertFalse(press(player, ShapingPalmMenu.STORE_RESULT), "an empty grid has no result");
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_no_result", "and says so");
        for (int cell : new int[] {0, 1, 3, 4}) put(player, cell, stack(Items.HONEY_BOTTLE, 2));
        h.assertTrue(result(player).is(Items.HONEY_BLOCK), "four honey bottles make a honey block");
        fillInventory(player);
        h.assertFalse(press(player, ShapingPalmMenu.STORE_RESULT), "a glass bottle with nowhere to go refuses");
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_no_room", "and says there is no room");
        h.assertValueEqual(inGrid(h, at, Items.HONEY_BOTTLE), 8, "no bottle was taken");
        h.assertValueEqual(count(h, CHEST, Items.HONEY_BLOCK), 0, "nothing was stored");
        h.assertValueEqual(player.crafted(Items.HONEY_BLOCK), 0, "and no craft was counted");
        h.succeed();
    }

    // ---- invalidation, unloading, automation ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBrokenStoreRefusesEveryActionAndTheLinkWorksAgainWhenAStoreReturns(GameTestHelper h) {
        BlockPos at = linked(h, stack(Items.OAK_PLANKS, 20));
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.OAK_PLANKS, 1));
        h.setBlock(CHEST, Blocks.AIR);
        for (int button : new int[] {ShapingPalmMenu.RESTOCK, ShapingPalmMenu.RETURN, ShapingPalmMenu.STORE_RESULT}) {
            h.assertFalse(press(player, button), "button " + button + " finds no store");
            h.assertValueEqual(player.saidKey(), REFUSAL + "palm_link_no_store", "and says there is none");
        }
        h.assertValueEqual(cell(h, at, 0).getCount(), 1, "the grid is untouched");
        h.assertValueEqual(((ShapingPalmMenu) player.containerMenu).reachState(), 2, "the link is kept");
        chest(h, CHEST, stack(Items.OAK_PLANKS, 20));
        h.assertTrue(press(player, ShapingPalmMenu.RESTOCK), "a new chest at the position is the store again");
        h.assertValueEqual(cell(h, at, 0).getCount(), 21, "and gives its planks");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void linkingAgainReplacesTheOldLinkAndTheOldStoreIsLeftAlone(GameTestHelper h) {
        BlockPos at = linked(h, stack(Items.OAK_PLANKS, 20));
        BlockPos other = new BlockPos(2, 2, 6);
        chest(h, other, stack(Items.OAK_PLANKS, 5));
        h.assertValueEqual(link(h, at, other), STATUS + "linked", "the Palm links to the other chest");
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.OAK_PLANKS, 1));
        h.assertTrue(press(player, ShapingPalmMenu.RESTOCK), "the restock goes");
        h.assertValueEqual(cell(h, at, 0).getCount(), 6, "it used the new store alone");
        h.assertValueEqual(count(h, CHEST, Items.OAK_PLANKS), 20, "the old store kept its planks");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aPalmWithNoReachOrNoFoundationOrAnUnreadableSaveActsOnNothing(GameTestHelper h) {
        BlockPos at = palm(h);
        chest(h, CHEST, stack(Items.OAK_PLANKS, 20));
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.OAK_PLANKS, 1));
        h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "no Reach");
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_no_reach", "says so");
        reach(h, at);
        h.assertValueEqual(link(h, at, CHEST), STATUS + "linked", "linked");
        h.setBlock(at.below(), Blocks.STONE);
        h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "no foundation");
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_no_foundation", "says so");
        h.setBlock(at.below(), EcologyTests.mature());
        var registries = h.getLevel().registryAccess();
        CompoundTag tag = h.getBlockEntity(at).saveWithFullMetadata(registries);
        tag.getCompound("contents").getList("grid", 10).getCompound(0).putInt("count", 99);
        h.getBlockEntity(at).loadWithComponents(tag, registries);
        MenuPlayer broken = open(h, at, "palm_broken");
        h.assertFalse(press(broken, ShapingPalmMenu.RESTOCK), "an unreadable save");
        h.assertValueEqual(broken.saidKey(), "infestusfrontier.refusal.organ.unreadable_save", "says so");
        h.assertValueEqual(count(h, CHEST, Items.OAK_PLANKS), 20, "the chest was never touched");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aLinkStartsNoWorkOfItsOwnAndEachActionStopsAfterOnePass(GameTestHelper h) {
        BlockPos at = linked(h, stack(Items.OAK_PLANKS, 64), stack(Items.STICK, 64), stack(Items.COBBLESTONE, 64));
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.OAK_PLANKS, 1));
        put(player, 3, stack(Items.OAK_PLANKS, 1));
        h.assertTrue(press(player, ShapingPalmMenu.RESTOCK), "one restock");
        ItemStack gridBefore = cell(h, at, 0);
        h.assertValueEqual(gridBefore.getCount(), 64, "it topped the cells up");
        h.assertTrue(result(player).is(Items.STICK), "the grid makes sticks");
        h.assertValueEqual(player.crafted(Items.STICK), 0, "restocking crafted nothing");
        h.assertTrue(press(player, ShapingPalmMenu.STORE_RESULT), "one store-result craft");
        h.assertValueEqual(player.crafted(Items.STICK), 4, "exactly one craft: its four sticks");
        int sticks = count(h, CHEST, Items.STICK);
        h.runAfterDelay(120, () -> {
            h.assertValueEqual(count(h, CHEST, Items.STICK), sticks, "nothing was crafted since");
            h.assertValueEqual(player.crafted(Items.STICK), 4, "still the one craft");
            h.assertValueEqual(cell(h, at, 0).getCount(), 63, "the grid is where the one craft left it");
            h.assertTrue(
                    h.getLevel()
                            .getEntitiesOfClass(
                                    ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(at)).inflate(8))
                            .isEmpty(),
                    "and nothing was spilled");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "palm_reach_unload",
            timeoutTicks = 1200)
    public static void aStoreInAnUnloadedChunkRefusesWithoutLoadingItAndAReloadKeepsTheLink(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        int baseX = (origin.getX() & ~15) + 248000;
        int z = (origin.getZ() & ~15) + 248008;
        BlockPos at = new BlockPos(baseX + 15, 80, z);
        BlockPos store = at.east(3);
        ChunkPos palmChunk = new ChunkPos(at);
        ChunkPos storeChunk = new ChunkPos(store);
        TestChunks.force(level, palmChunk);
        TestChunks.force(level, storeChunk);
        level.setBlockAndUpdate(at.below(), EcologyTests.mature());
        level.setBlockAndUpdate(at, ProcessingModule.SHAPING_PALM.get().defaultBlockState());
        level.setBlockAndUpdate(store, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) level.getBlockEntity(store)).setItem(0, stack(Items.OAK_PLANKS, 30));
        MenuPlayer player = new MenuPlayer(level, "palm_unload");
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        use(level, player, stack(LogisticsModule.CONTRACTILE_FIBER.get(), 1), at);
        use(level, player, stack(InteractionModule.SYNAPTIC_PROBE.get(), 1), at);
        use(level, player, player.getMainHandItem(), store);
        h.assertValueEqual(player.saidKey(), STATUS + "linked", "linked across the chunk border");
        player.openMenu((net.minecraft.world.MenuProvider) level.getBlockEntity(at), at);
        put(player, 0, stack(Items.OAK_PLANKS, 1));
        h.startSequence()
                .thenExecute(() -> {
                    level.setChunkForced(storeChunk.x, storeChunk.z, false);
                    level.setChunkForced(palmChunk.x, palmChunk.z, false);
                    level.getChunkSource().addRegionTicket(TicketType.FORCED, palmChunk, 0, palmChunk);
                })
                .thenWaitUntil(() -> h.assertFalse(level.hasChunkAt(store), "the store's chunk unloaded"))
                .thenExecute(() -> {
                    h.assertTrue(level.hasChunkAt(at), "the Palm's chunk is still there");
                    h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "an unloaded store refuses");
                    h.assertValueEqual(player.saidKey(), REFUSAL + "palm_link_unloaded", "and says it is not loaded");
                    h.assertFalse(press(player, ShapingPalmMenu.RETURN), "return refuses too");
                    h.assertFalse(level.hasChunkAt(store), "asking did not load the chunk");
                    h.assertValueEqual(((ShapingPalmMenu) player.containerMenu).reachState(), 2, "the link is kept");
                    level.getChunkSource().removeRegionTicket(TicketType.FORCED, palmChunk, 0, palmChunk);
                    level.setChunkForced(palmChunk.x, palmChunk.z, false);
                    player.closeContainer();
                })
                .thenWaitUntil(() -> h.assertFalse(level.hasChunkAt(at), "the Palm's chunk unloaded too"))
                .thenExecute(() -> {
                    TestChunks.force(level, palmChunk);
                    TestChunks.force(level, storeChunk);
                })
                .thenWaitUntil(() -> h.assertTrue(level.hasChunkAt(store) && level.hasChunkAt(at), "both are back"))
                .thenExecute(() -> {
                    player.openMenu((net.minecraft.world.MenuProvider) level.getBlockEntity(at), at);
                    h.assertValueEqual(
                            ((ShapingPalmMenu) player.containerMenu).reachState(),
                            2,
                            "the chunk's save kept Reach and the link");
                    h.assertTrue(press(player, ShapingPalmMenu.RESTOCK), "the restock works again");
                    h.assertValueEqual(
                            ((ChestBlockEntity) level.getBlockEntity(store))
                                    .getItem(0)
                                    .getCount(),
                            0,
                            "the chest gave all 30 planks");
                    h.assertValueEqual(
                            ((ShapingPalmMenu) player.containerMenu)
                                    .getSlot(ShapingPalmMenu.FIRST_CELL)
                                    .getItem()
                                    .getCount(),
                            31,
                            "and the cell holds 1 + 30, none made, none lost");
                    level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                    level.setChunkForced(palmChunk.x, palmChunk.z, false);
                    level.setChunkForced(storeChunk.x, storeChunk.z, false);
                })
                .thenSucceed();
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "palm_reach_unload",
            timeoutTicks = 1200)
    public static void aDoubleChestWhoseOtherHalfIsUnloadedRefusesWithoutLoadingIt(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        int baseX = (origin.getX() & ~15) + 248064;
        int z = (origin.getZ() & ~15) + 248064;
        BlockPos at = new BlockPos(baseX + 12, 80, z);
        BlockPos near = new BlockPos(baseX + 15, 80, z);
        BlockPos far = near.east();
        ChunkPos palmChunk = new ChunkPos(at);
        ChunkPos farChunk = new ChunkPos(far);
        h.assertFalse(palmChunk.equals(farChunk), "the halves are in two chunks");
        TestChunks.force(level, palmChunk);
        TestChunks.force(level, farChunk);
        level.setBlockAndUpdate(at.below(), EcologyTests.mature());
        level.setBlockAndUpdate(at, ProcessingModule.SHAPING_PALM.get().defaultBlockState());
        level.setBlockAndUpdate(
                near,
                Blocks.CHEST
                        .defaultBlockState()
                        .setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.NORTH)
                        .setValue(
                                net.minecraft.world.level.block.ChestBlock.TYPE,
                                net.minecraft.world.level.block.state.properties.ChestType.LEFT));
        level.setBlockAndUpdate(
                far,
                Blocks.CHEST
                        .defaultBlockState()
                        .setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.NORTH)
                        .setValue(
                                net.minecraft.world.level.block.ChestBlock.TYPE,
                                net.minecraft.world.level.block.state.properties.ChestType.RIGHT));
        ((ChestBlockEntity) level.getBlockEntity(near)).setItem(0, stack(Items.OAK_PLANKS, 30));
        MenuPlayer player = new MenuPlayer(level, "palm_double");
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        use(level, player, stack(LogisticsModule.CONTRACTILE_FIBER.get(), 1), at);
        use(level, player, stack(InteractionModule.SYNAPTIC_PROBE.get(), 1), at);
        use(level, player, player.getMainHandItem(), near);
        h.assertValueEqual(player.saidKey(), STATUS + "linked", "linked to the near half while both are loaded");
        player.openMenu((net.minecraft.world.MenuProvider) level.getBlockEntity(at), at);
        put(player, 0, stack(Items.OAK_PLANKS, 1));
        h.startSequence()
                .thenExecute(() -> {
                    level.setChunkForced(farChunk.x, farChunk.z, false);
                    level.setChunkForced(palmChunk.x, palmChunk.z, false);
                    level.getChunkSource().addRegionTicket(TicketType.FORCED, palmChunk, 0, palmChunk);
                })
                .thenWaitUntil(() -> h.assertFalse(level.hasChunkAt(far), "the far half's chunk unloaded"))
                .thenExecute(() -> {
                    h.assertTrue(level.hasChunkAt(near), "the near half's chunk is still there");
                    h.assertFalse(press(player, ShapingPalmMenu.RESTOCK), "a half-unloaded double chest refuses");
                    h.assertValueEqual(player.saidKey(), REFUSAL + "palm_link_no_store", "and says there is no store");
                    h.assertFalse(level.hasChunkAt(far), "asking did not load the far half's chunk");
                    h.assertValueEqual(
                            ((ShapingPalmMenu) player.containerMenu)
                                    .getSlot(ShapingPalmMenu.FIRST_CELL)
                                    .getItem()
                                    .getCount(),
                            1,
                            "nothing moved");
                    h.assertValueEqual(
                            ((ChestBlockEntity) level.getBlockEntity(near))
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

    private static void use(ServerLevel level, MenuPlayer player, ItemStack held, BlockPos at) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false);
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }
}
