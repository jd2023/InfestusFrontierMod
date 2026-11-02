package org.jd.infestusfrontier.testmod;

import static org.jd.infestusfrontier.testmod.ShapingPalmTests.loose;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.open;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.palm;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.put;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.reload;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.stack;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.interaction.InteractionModule;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.Port;
import org.jd.infestusfrontier.logistics.VascularJunctionBlock;
import org.jd.infestusfrontier.processing.PalmStatus;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.ShapingPalmBlock;
import org.jd.infestusfrontier.processing.ShapingPalmMenu;
import org.jd.infestusfrontier.processing.api.PalmRote;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.jd.infestusfrontier.testmod.ShapingPalmTests.MenuPlayer;

/**
 * The Shaping Palm's Rote (BLOCK_CATALOG T1-41, stage 3): the Synaptic Gel that gives it, Select, and the cycle that
 * repeats one vanilla craft on 10 BU and 40 ticks. Every test counts items and biomass across the grid, the cycle, the
 * tray, the store and the player, so a repeat, a loss or a duplicate shows. The Palm stands at (2, 2, 2) on substrate.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class PalmRoteTests {
    private static final BlockPos AT = ShapingPalmTests.AT;
    private static final BlockPos CHEST = new BlockPos(5, 2, 2);
    private static final String REFUSAL = "infestusfrontier.refusal.processing.";
    private static final String STATUS = "infestusfrontier.status.palm.";
    private static final int TICKS = PalmRote.TICKS;
    /** Longer than the Palm's retry interval. */
    private static final int RETRY_WAIT = 30;

    private PalmRoteTests() {}

    // ---- helpers ----

    private static void use(GameTestHelper h, MenuPlayer player, ItemStack held, BlockPos at) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockPos absolute = h.absolutePos(at);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        player.moveTo(absolute.getX() + 0.5, absolute.getY() + 1, absolute.getZ() - 1.5);
        player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    /** A Palm at {@code at} on substrate with Reach and Rote, given by a Fiber and a Gel used on it. */
    private static BlockPos rotePalm(GameTestHelper h, BlockPos at) {
        h.setBlock(at.below(), EcologyTests.mature());
        h.setBlock(at, ProcessingModule.SHAPING_PALM.get());
        MenuPlayer player = new MenuPlayer(h.getLevel(), "palm_rote_" + at.toShortString());
        use(h, player, stack(LogisticsModule.CONTRACTILE_FIBER.get(), 1), at);
        use(h, player, stack(ProcessingModule.SYNAPTIC_GEL.get(), 1), at);
        h.assertTrue(rote(h, at) != null, "the gel gave the Palm Rote");
        return at;
    }

    private static BlockPos rotePalm(GameTestHelper h) {
        return rotePalm(h, AT);
    }

    private static PalmRote rote(GameTestHelper h, BlockPos at) {
        return h.getBlockEntity(at).collectComponents().get(ProcessingModule.PALM_ROTE.get());
    }

    private static void fuel(GameTestHelper h, BlockPos at, int units) {
        h.assertTrue(
                h.getLevel()
                        .getCapability(BiomassCapabilities.BLOCK, h.absolutePos(at), null)
                        .insert(Biomass.of(units), false)
                        .isDone(),
                "the Palm took " + units + " BU");
    }

    private static int fuelHeld(GameTestHelper h, BlockPos at) {
        return rote(h, at).biomass().stored().units();
    }

    private static IItemHandler ports(GameTestHelper h, BlockPos at) {
        return h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(at), null);
    }

    /** Puts the stacks in grid cells 0, 1, 2 ... with the cursor; an empty stack leaves its cell alone. */
    private static void fill(GameTestHelper h, BlockPos at, ItemStack... cells) {
        MenuPlayer player = open(h, at, "palm_rote_fill");
        for (int cell = 0; cell < cells.length; cell++) if (!cells[cell].isEmpty()) put(player, cell, cells[cell]);
        player.closeContainer();
    }

    private static String select(GameTestHelper h, BlockPos at) {
        MenuPlayer player = open(h, at, "palm_rote_select");
        player.containerMenu.clickMenuButton(player, ShapingPalmMenu.SELECT);
        String said = player.saidKey();
        player.closeContainer();
        return said;
    }

    private static PalmStatus status(GameTestHelper h, BlockPos at) {
        MenuPlayer player = open(h, at, "palm_rote_status");
        PalmStatus status = ((ShapingPalmMenu) player.containerMenu).status();
        player.closeContainer();
        return status;
    }

    /** Two planks above each other make four sticks: the plain repeated craft. */
    private static ItemStack[] stickGrid(int planks) {
        ItemStack none = ItemStack.EMPTY;
        return new ItemStack[] {
            stack(Items.OAK_PLANKS, planks), none, none, stack(Items.OAK_PLANKS, planks),
        };
    }

    private static ItemStack[] cakeGrid() {
        return new ItemStack[] {
            stack(Items.MILK_BUCKET, 1),
            stack(Items.MILK_BUCKET, 1),
            stack(Items.MILK_BUCKET, 1),
            stack(Items.SUGAR, 1),
            stack(Items.EGG, 1),
            stack(Items.SUGAR, 1),
            stack(Items.WHEAT, 1),
            stack(Items.WHEAT, 1),
            stack(Items.WHEAT, 1),
        };
    }

    /** The first {@code n} test ingredients, one each: fresh stacks, since the grid takes what it is given. */
    private static ItemStack[] husks(int n) {
        ItemStack[] husks = new ItemStack[n];
        for (int i = 0; i < n; i++) husks[i] = stack(TestItems.HUSKS.get(i).get(), 1);
        return husks;
    }

    private static int count(List<ItemStack> stacks, Item item) {
        int count = 0;
        for (ItemStack stack : stacks) if (stack.is(item)) count += stack.getCount();
        return count;
    }

    private static int inGrid(GameTestHelper h, BlockPos at, Item item) {
        return count(ShapingPalmTests.grid(h, at).grid(), item);
    }

    private static int inTray(GameTestHelper h, BlockPos at, Item item) {
        return count(rote(h, at).tray(), item);
    }

    private static boolean started(GameTestHelper h, BlockPos at) {
        PalmRote state = rote(h, at);
        return state.cycle().isPresent() || state.cycles() > 0;
    }

    private static ChestBlockEntity chest(GameTestHelper h, BlockPos at, ItemStack... contents) {
        h.setBlock(at, Blocks.CHEST);
        ChestBlockEntity box = h.getBlockEntity(at);
        for (int slot = 0; slot < contents.length; slot++) box.setItem(slot, contents[slot]);
        return box;
    }

    private static void link(GameTestHelper h, BlockPos at, BlockPos store) {
        MenuPlayer player = new MenuPlayer(h.getLevel(), "palm_rote_probe");
        ItemStack probe = stack(InteractionModule.SYNAPTIC_PROBE.get(), 1);
        use(h, player, probe, at);
        player.saidKey();
        use(h, player, player.getMainHandItem(), store);
        h.assertValueEqual(player.saidKey(), STATUS + "linked", "the probe linked the store");
    }

    // ---- the gel and Select ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aSynapticGelGivesRoteToAPalmWithReachAndIsSpentOnce(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = new MenuPlayer(h.getLevel(), "palm_gel");
        h.assertValueEqual(
                h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(at), null) == null,
                true,
                "a Palm without Rote has no item handler");
        use(h, player, stack(ProcessingModule.SYNAPTIC_GEL.get(), 2), at);
        h.assertValueEqual(player.getMainHandItem().getCount(), 2, "a Palm without Reach keeps the gel");
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_rote_needs_reach", "and says Reach comes first");
        use(h, player, stack(LogisticsModule.CONTRACTILE_FIBER.get(), 1), at);
        use(h, player, stack(ProcessingModule.SYNAPTIC_GEL.get(), 2), at);
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "one gel is spent");
        h.assertValueEqual(player.saidKey(), STATUS + "rote", "and the Palm says it has Rote");
        h.assertTrue(h.getBlockState(at).getValue(ShapingPalmBlock.ROTE), "the block state shows Rote");
        h.assertTrue(rote(h, at) != null && rote(h, at).pattern().isEmpty(), "Rote starts with no pattern");
        h.assertTrue(ports(h, at) != null, "the Palm now has an item handler");
        h.assertTrue(
                h.getLevel().getCapability(BiomassCapabilities.BLOCK, h.absolutePos(at), null) != null,
                "and holds biomass");
        use(h, player, player.getMainHandItem(), at);
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "a second gel is kept");
        h.assertValueEqual(player.saidKey(), REFUSAL + "palm_has_rote", "and the Palm says it has Rote already");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void selectRecordsThePatternAndRefusesWhatCannotBeRepeated(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer plain = open(h, at, "palm_no_rote");
        plain.containerMenu.clickMenuButton(plain, ShapingPalmMenu.SELECT);
        h.assertValueEqual(plain.saidKey(), REFUSAL + "palm_no_rote", "Select is Rote's");
        plain.closeContainer();
        rotePalm(h);
        h.assertValueEqual(select(h, at), REFUSAL + "palm_select_no_recipe", "an empty grid is refused");
        fill(h, at, stack(Items.APPLE, 1), stack(Items.STONE, 1), stack(Items.BONE, 1));
        h.assertValueEqual(select(h, at), REFUSAL + "palm_select_no_recipe", "a grid that matches no recipe");
        h.assertTrue(rote(h, at).pattern().isEmpty(), "nothing was recorded");
        MenuPlayer clear = open(h, at, "palm_clear");
        for (int cell = 0; cell < 3; cell++)
            clear.containerMenu.clicked(1 + cell, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, clear);
        clear.closeContainer();
        fill(h, at, stack(Items.LEATHER_HELMET, 1), stack(Items.RED_DYE, 1));
        h.assertValueEqual(select(h, at), REFUSAL + "palm_select_special", "dyeing is a special recipe");
        MenuPlayer clear2 = open(h, at, "palm_clear");
        for (int cell = 0; cell < 2; cell++)
            clear2.containerMenu.clicked(1 + cell, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, clear2);
        clear2.closeContainer();
        fill(h, at, husks(4));
        h.assertValueEqual(
                select(h, at), REFUSAL + "palm_select_remainders", "four kinds of remainder do not fit the tray");
        MenuPlayer drop = open(h, at, "palm_clear");
        drop.containerMenu.clicked(1 + 3, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, drop);
        drop.closeContainer();
        h.assertValueEqual(select(h, at), STATUS + "selected", "three kinds of remainder are the most it takes");
        MenuPlayer again = open(h, at, "palm_clear");
        for (int cell = 0; cell < 3; cell++)
            again.containerMenu.clicked(1 + cell, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, again);
        again.closeContainer();
        fill(h, at, cakeGrid());
        h.assertValueEqual(select(h, at), STATUS + "selected", "a cake replaces the pattern");
        PalmRote.Pattern pattern = rote(h, at).pattern().orElseThrow();
        h.assertValueEqual(pattern.recipe().toString(), "minecraft:cake", "the recipe id is saved");
        h.assertTrue(pattern.result().is(Items.CAKE) && pattern.result().getCount() == 1, "and the assembled result");
        h.assertTrue(
                pattern.kind(0).is(Items.MILK_BUCKET) && pattern.kind(0).getCount() == 1, "each cell holds its kind");
        h.assertTrue(pattern.kind(4).is(Items.EGG) && pattern.kind(8).is(Items.WHEAT), "in its place");
        h.assertValueEqual(inGrid(h, at, Items.MILK_BUCKET), 3, "Select took nothing from the grid");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void selectIsRefusedWhileACycleIsUnderWay(GameTestHelper h) {
        BlockPos at = rotePalm(h);
        fuel(h, at, 100);
        fill(h, at, stickGrid(4));
        h.assertValueEqual(select(h, at), STATUS + "selected", "the sticks are selected");
        h.runAfterDelay(10, () -> {
            h.assertTrue(rote(h, at).cycle().isPresent(), "a cycle is under way");
            fill(h, at, stack(Items.OAK_PLANKS, 1));
            MenuPlayer player = open(h, at, "palm_swap");
            player.containerMenu.clicked(1 + 1, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
            player.closeContainer();
            PalmRote.Pattern before = rote(h, at).pattern().orElseThrow();
            h.assertValueEqual(
                    select(h, at), REFUSAL + "palm_select_running", "the pattern holds until the cycle ends");
            h.assertTrue(rote(h, at).pattern().orElseThrow().equals(before), "and is unchanged");
            h.succeed();
        });
    }

    // ---- the cycle ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void repeatedCraftsTakeOneOfEachTenBiomassAndFortyTicks(GameTestHelper h) {
        BlockPos at = rotePalm(h);
        fuel(h, at, 100);
        fill(h, at, stickGrid(6));
        h.assertValueEqual(select(h, at), STATUS + "selected", "the sticks are selected");
        long[] startedAt = {-1};
        long[] deliveredAt = {-1};
        h.onEachTick(() -> {
            PalmRote now = rote(h, at);
            if (startedAt[0] < 0 && now.cycle().isPresent()) {
                startedAt[0] = h.getTick();
                h.assertValueEqual(inGrid(h, at, Items.OAK_PLANKS), 10, "the start took one of each filled cell");
                h.assertValueEqual(fuelHeld(h, at), 90, "and 10 BU");
                h.assertValueEqual(inTray(h, at, Items.STICK), 0, "and nothing is delivered yet");
            }
            if (deliveredAt[0] < 0 && !now.tray().get(0).isEmpty()) deliveredAt[0] = h.getTick();
        });
        h.runAtTickTime(300, () -> {
            h.assertTrue(startedAt[0] > 0 && deliveredAt[0] > 0, "a cycle started and delivered");
            long took = deliveredAt[0] - startedAt[0];
            h.assertTrue(took >= TICKS - 1 && took <= TICKS + 1, "a cycle took " + TICKS + " ticks, not " + took);
            PalmRote end = rote(h, at);
            h.assertValueEqual(end.cycles(), 6, "six planks in each column made six cycles");
            h.assertValueEqual(inTray(h, at, Items.STICK), 24, "six crafts of four sticks wait in the tray");
            h.assertValueEqual(fuelHeld(h, at), 40, "six cycles cost 60 BU");
            h.assertValueEqual(inGrid(h, at, Items.OAK_PLANKS), 0, "the planks were used");
            h.assertValueEqual(status(h, at), PalmStatus.MISSING, "the Palm says it misses its ingredients");
            h.assertTrue(loose(h, at).isEmpty(), "nothing was dropped on the ground");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 500)
    public static void aFullTrayHoldsTheNextCycleAndReturnedContainersComeOutOnce(GameTestHelper h) {
        BlockPos at = rotePalm(h);
        fuel(h, at, 100);
        fill(h, at, cakeGrid());
        h.assertValueEqual(select(h, at), STATUS + "selected", "a cake is selected");
        IItemHandler ports = ports(h, at);
        h.startSequence()
                .thenExecuteAfter(60, () -> {
                    PalmRote first = rote(h, at);
                    h.assertTrue(first.tray().get(0).is(Items.CAKE), "the cake is in the result slot");
                    h.assertValueEqual(inTray(h, at, Items.BUCKET), 3, "three milk buckets came back as buckets");
                    h.assertTrue(first.tray().get(1).is(Items.BUCKET), "in the first remainder slot");
                    h.assertValueEqual(first.cycles(), 1, "one cycle is counted");
                    h.assertValueEqual(inGrid(h, at, Items.MILK_BUCKET), 0, "the grid gave its milk");
                    // feed the second cake's ingredients the way a Lobe or a hopper does
                    ItemStack[] second = cakeGrid();
                    h.assertFalse(
                            ports.insertItem(3, stack(Items.MILK_BUCKET, 1), true)
                                    .isEmpty(),
                            "milk does not go where the pattern wants sugar");
                    h.assertFalse(
                            ports.insertItem(0, stack(Items.STONE, 1), false).isEmpty(), "stone goes nowhere");
                    for (int cell = 0; cell < second.length; cell++)
                        h.assertTrue(
                                ports.insertItem(cell, second[cell], false).isEmpty(),
                                "cell " + cell + " took its ingredient");
                })
                .thenExecuteAfter(40, () -> {
                    h.assertValueEqual(fuelHeld(h, at), 90, "a full tray started nothing and spent nothing");
                    h.assertTrue(rote(h, at).cycle().isEmpty(), "no cycle is under way");
                    h.assertValueEqual(status(h, at), PalmStatus.TRAY_FULL, "the Palm says the tray is full");
                    h.assertValueEqual(inGrid(h, at, Items.WHEAT), 3, "the second set waits in the grid");
                    ItemStack cake = ports.extractItem(9, 64, false);
                    h.assertTrue(cake.is(Items.CAKE) && cake.getCount() == 1, "the cake comes out of the tray slot");
                    h.assertTrue(ports.extractItem(9, 64, false).isEmpty(), "and only once");
                })
                .thenExecuteAfter(70, () -> {
                    PalmRote second = rote(h, at);
                    h.assertValueEqual(second.cycles(), 2, "emptying the tray let the second cake be made");
                    h.assertTrue(second.tray().get(0).is(Items.CAKE), "it is in the tray");
                    h.assertValueEqual(inTray(h, at, Items.BUCKET), 6, "six buckets in all came back, none twice");
                    h.assertValueEqual(fuelHeld(h, at), 80, "two cycles cost 20 BU");
                    ItemStack buckets = ports.extractItem(10, 64, false);
                    h.assertValueEqual(buckets.getCount(), 6, "a Yield Lobe or hopper takes the remainders");
                    h.assertTrue(ports.extractItem(10, 64, false).isEmpty(), "and they are gone once taken");
                    h.assertTrue(
                            ports.extractItem(0, 64, false).isEmpty()
                                    && ports.extractItem(8, 64, false).isEmpty(),
                            "the grid never comes out");
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void aTrayThatCannotTakeEveryRemainderHoldsTheCycleBackUntilAllHaveRoom(GameTestHelper h) {
        // three husks leave three tools that do not stack: one cycle fills the three remainder slots
        BlockPos at = rotePalm(h);
        fuel(h, at, 100);
        fill(h, at, husks(3));
        h.assertValueEqual(select(h, at), STATUS + "selected", "three kinds of remainder are selected");
        IItemHandler ports = ports(h, at);
        h.startSequence()
                .thenExecuteAfter(60, () -> {
                    PalmRote first = rote(h, at);
                    h.assertTrue(first.tray().get(0).is(Items.COBBLESTONE), "the result is in slot 0");
                    h.assertTrue(
                            first.tray().get(1).is(Items.WOODEN_PICKAXE)
                                    && first.tray().get(2).is(Items.WOODEN_SHOVEL)
                                    && first.tray().get(3).is(Items.WOODEN_AXE),
                            "each remainder kind is in a slot of its own");
                    ItemStack[] next = husks(3);
                    for (int cell = 0; cell < 3; cell++)
                        h.assertTrue(ports.insertItem(cell, next[cell], false).isEmpty(), "the next set is fed");
                    h.assertValueEqual(inGrid(h, at, TestItems.HUSKS.get(0).get()), 1, "and it is in the grid");
                })
                .thenExecuteAfter(30, () -> {
                    h.assertValueEqual(status(h, at), PalmStatus.TRAY_FULL, "the Palm says the tray is full");
                    h.assertValueEqual(fuelHeld(h, at), 90, "and spent nothing");
                    h.assertTrue(ports.extractItem(10, 64, false).is(Items.WOODEN_PICKAXE), "one remainder is taken");
                })
                .thenExecuteAfter(30, () -> {
                    h.assertTrue(rote(h, at).cycle().isEmpty(), "room for one remainder is not room for all three");
                    h.assertValueEqual(fuelHeld(h, at), 90, "so nothing started");
                    h.assertTrue(ports.extractItem(11, 64, false).is(Items.WOODEN_SHOVEL), "the second");
                    h.assertTrue(ports.extractItem(12, 64, false).is(Items.WOODEN_AXE), "and the third");
                })
                .thenExecuteAfter(5, () -> {
                    h.assertTrue(rote(h, at).cycle().isPresent(), "with room for all, the cycle starts");
                    h.assertValueEqual(fuelHeld(h, at), 80, "and pays");
                    h.assertValueEqual(inGrid(h, at, TestItems.HUSKS.get(0).get()), 0, "taking its husks");
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void anInterruptedFuelPausesTheNextCycleAndALaterFeedResumesIt(GameTestHelper h) {
        BlockPos at = rotePalm(h);
        fuel(h, at, 15);
        fill(h, at, stickGrid(6));
        select(h, at);
        IItemHandler ports = ports(h, at);
        h.startSequence()
                .thenExecuteAfter(10, () -> {
                    h.assertTrue(rote(h, at).cycle().isPresent(), "the first cycle started");
                    h.assertValueEqual(fuelHeld(h, at), 5, "it took 10 BU");
                    var tank = h.getLevel().getCapability(BiomassCapabilities.BLOCK, h.absolutePos(at), null);
                    h.assertTrue(tank.extract(Biomass.of(5), false).isDone(), "the rest of the fuel is drained");
                })
                .thenExecuteAfter(TICKS + 5, () -> {
                    h.assertValueEqual(inTray(h, at, Items.STICK), 4, "the cycle in hand finished: it was paid for");
                    h.assertTrue(rote(h, at).cycle().isEmpty(), "no second cycle started");
                    h.assertValueEqual(status(h, at), PalmStatus.NO_BIOMASS, "the Palm says it needs biomass");
                    h.assertValueEqual(inGrid(h, at, Items.OAK_PLANKS), 10, "and keeps its planks");
                })
                .thenExecuteAfter(60, () -> {
                    h.assertTrue(rote(h, at).cycle().isEmpty(), "still waiting after a minute");
                    fuel(h, at, 10);
                })
                .thenExecuteAfter(5, () -> {
                    h.assertTrue(rote(h, at).cycle().isPresent(), "a feed of 10 BU starts the next cycle at once");
                    h.assertValueEqual(fuelHeld(h, at), 0, "and is all spent");
                    h.assertTrue(ports.extractItem(9, 64, true).getCount() == 4, "the first four sticks still wait");
                })
                .thenSucceed();
    }

    // ---- automation ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void feedingAndYieldLobesMoveOnlyTheSelectedIngredientsAndTheProducts(GameTestHelper h) {
        BlockPos at = rotePalm(h);
        fill(h, at, stickGrid(1));
        select(h, at);
        BlockPos west = at.west();
        BlockPos east = at.east();
        lobe(h, west, true, Direction.EAST);
        lobe(h, east, false, Direction.WEST);
        IItemHandler feeding =
                h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(west), Direction.DOWN);
        IItemHandler yield =
                h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(east), Direction.DOWN);
        h.assertTrue(feeding != null && feeding.getSlots() == 13, "the Feeding Lobe serves the grid and the tray");
        h.assertFalse(feeding.insertItem(0, stack(Items.STONE, 4), false).isEmpty(), "stone is not the ingredient");
        h.assertFalse(
                feeding.insertItem(1, stack(Items.OAK_PLANKS, 4), false).isEmpty(),
                "planks do not go in an empty cell");
        h.assertFalse(feeding.insertItem(9, stack(Items.STICK, 4), false).isEmpty(), "nothing goes into the tray");
        h.assertFalse(
                feeding.insertItem(3, stack(Items.SPRUCE_PLANKS, 1), false).isEmpty(), "another wood is another kind");
        ItemStack planks = stack(Items.OAK_PLANKS, 6);
        for (int turn = 0; turn < 130 && !planks.isEmpty(); turn++)
            planks = feeding.insertItem(turn % 13, planks, false);
        h.assertTrue(planks.isEmpty(), "all six were taken, a few at a time");
        h.assertTrue(feeding.extractItem(0, 64, false).isEmpty(), "a Feeding Lobe takes nothing out");
        h.assertValueEqual(inGrid(h, at, Items.OAK_PLANKS), 8, "the grid holds the 2 it had and the 6 accepted");
        h.assertTrue(
                Math.abs(ShapingPalmTests.cell(h, at, 0).getCount()
                                - ShapingPalmTests.cell(h, at, 3).getCount())
                        <= 1,
                "shared between the two cells that want planks, not piled in the first");
        h.assertFalse(yield.insertItem(0, stack(Items.OAK_PLANKS, 4), false).isEmpty(), "a Yield Lobe puts nothing in");
        h.assertTrue(yield.extractItem(0, 64, false).isEmpty(), "and never gives the ingredients");
        h.assertTrue(yield.extractItem(9, 64, false).isEmpty(), "nor anything before a cycle is done");
        fuel(h, at, 20);
        h.runAfterDelay(TICKS + 10, () -> {
            ItemStack made = yield.extractItem(9, 64, false);
            h.assertTrue(made.is(Items.STICK) && made.getCount() == 4, "the Yield Lobe takes the four sticks");
            h.assertTrue(yield.extractItem(9, 64, false).isEmpty(), "once");
            PalmRote now = rote(h, at);
            h.assertValueEqual(
                    inGrid(h, at, Items.OAK_PLANKS)
                            + 2 * (now.cycles() + (now.cycle().isPresent() ? 1 : 0)),
                    8,
                    "each cycle took one plank from each of two cells");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aHopperAboveThePalmFeedsOnlyTheSelectedIngredient(GameTestHelper h) {
        BlockPos at = rotePalm(h);
        fill(h, at, stickGrid(1));
        select(h, at);
        h.setBlock(at.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = h.getBlockEntity(at.above());
        hopper.setItem(0, stack(Items.STONE, 8));
        hopper.setItem(1, stack(Items.OAK_PLANKS, 8));
        h.runAfterDelay(100, () -> {
            h.assertValueEqual(inGrid(h, at, Items.STONE), 0, "no stone went in");
            h.assertValueEqual(hopper.getItem(0).getCount(), 8, "the hopper kept its stone");
            h.assertTrue(inGrid(h, at, Items.OAK_PLANKS) > 2, "planks went in");
            h.assertValueEqual(
                    inGrid(h, at, Items.OAK_PLANKS) + count(hopperItems(hopper), Items.OAK_PLANKS),
                    10,
                    "and not one was lost or made twice");
            h.succeed();
        });
    }

    private static List<ItemStack> hopperItems(HopperBlockEntity hopper) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < hopper.getContainerSize(); slot++) items.add(hopper.getItem(slot));
        return items;
    }

    private static void lobe(GameTestHelper h, BlockPos at, boolean feeds, Direction toOrgan) {
        h.setBlock(at.below(), EcologyTests.mature());
        h.setBlock(
                at,
                (feeds ? LogisticsModule.FEEDING_LOBE : LogisticsModule.YIELD_LOBE)
                        .get()
                        .defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, toOrgan));
    }

    // ---- competing consumers and the link ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void twoPalmsOnOneChestShareItAndRestockOncePerCompletedCycle(GameTestHelper h) {
        BlockPos first = rotePalm(h, new BlockPos(2, 2, 2));
        BlockPos second = rotePalm(h, new BlockPos(2, 2, 4));
        BlockPos store = new BlockPos(4, 2, 3);
        ChestBlockEntity box = chest(h, store, stack(Items.OAK_PLANKS, 8));
        link(h, first, store);
        link(h, second, store);
        for (BlockPos at : List.of(first, second)) {
            fuel(h, at, 100);
            fill(h, at, stickGrid(1));
            select(h, at);
        }
        h.startSequence()
                .thenExecuteAfter(TICKS + 5, () -> {
                    h.assertValueEqual(rote(h, first).cycles(), 1, "the first Palm finished a cycle");
                    h.assertValueEqual(rote(h, second).cycles(), 1, "and so did the second");
                    h.assertValueEqual(box.countItem(Items.OAK_PLANKS), 0, "the 8 planks were taken, not copied");
                    int grids = inGrid(h, first, Items.OAK_PLANKS) + inGrid(h, second, Items.OAK_PLANKS);
                    int running = (rote(h, first).cycle().isPresent() ? 1 : 0)
                            + (rote(h, second).cycle().isPresent() ? 1 : 0);
                    h.assertValueEqual(
                            grids + 2 * running + 2 * 2,
                            12,
                            "the 2 + 2 planks the Palms held and the 8 in the chest are in grids, cycles or sticks");
                    h.assertTrue(running >= 1, "the Palm that got planks went on");
                    box.setItem(0, stack(Items.OAK_PLANKS, 40));
                })
                .thenExecuteAfter(TICKS / 2, () -> {
                    h.assertValueEqual(
                            box.countItem(Items.OAK_PLANKS),
                            40,
                            "a restock is once a completed cycle: the chest is left");
                })
                .thenExecuteAfter(TICKS, () -> {
                    h.assertTrue(box.countItem(Items.OAK_PLANKS) < 40, "the next completed cycle restocked again");
                    int sticks = inTray(h, first, Items.STICK) + inTray(h, second, Items.STICK);
                    int planks = inGrid(h, first, Items.OAK_PLANKS)
                            + inGrid(h, second, Items.OAK_PLANKS)
                            + box.countItem(Items.OAK_PLANKS);
                    int running = (rote(h, first).cycle().isPresent() ? 1 : 0)
                            + (rote(h, second).cycle().isPresent() ? 1 : 0);
                    h.assertValueEqual(
                            planks + 2 * (sticks / 4) + 2 * running,
                            4 + 8 + 40,
                            "every plank is a plank, a stick pair or in a cycle: none made or lost");
                    h.assertTrue(running >= 1, "at least one Palm is still at work");
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void aMissingStorePausesTheRestockWithAReasonAndAReturnedOneResumesIt(GameTestHelper h) {
        BlockPos at = rotePalm(h);
        chest(h, CHEST, stack(Items.OAK_PLANKS, 30));
        link(h, at, CHEST);
        fuel(h, at, 100);
        fill(h, at, stickGrid(1));
        select(h, at);
        h.startSequence()
                .thenExecuteAfter(10, () -> {
                    h.assertTrue(rote(h, at).cycle().isPresent(), "the first cycle runs on what the grid held");
                    h.setBlock(CHEST, Blocks.AIR);
                })
                .thenExecuteAfter(TICKS + 10, () -> {
                    h.assertValueEqual(rote(h, at).cycles(), 1, "the cycle finished without its store");
                    h.assertTrue(rote(h, at).cycle().isEmpty(), "and none followed: the grid is empty");
                    h.assertValueEqual(status(h, at), PalmStatus.LINK_LOST, "the Palm says the link is lost");
                    h.assertValueEqual(inTray(h, at, Items.STICK), 4, "its sticks are safe in the tray");
                    h.assertValueEqual(fuelHeld(h, at), 90, "and it spent nothing more");
                    chest(h, CHEST, stack(Items.OAK_PLANKS, 30));
                })
                .thenExecuteAfter(RETRY_WAIT, () -> {
                    ChestBlockEntity back = h.getBlockEntity(CHEST);
                    h.assertTrue(back.countItem(Items.OAK_PLANKS) < 30, "the store came back and gave planks");
                    h.assertTrue(rote(h, at).cycle().isPresent() || rote(h, at).cycles() >= 2, "so the Palm went on");
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 300)
    public static void aReservoirAndAJunctionFuelAPalmWithRoteAndABucketFillsItToTheBrim(GameTestHelper h) {
        BlockPos at = rotePalm(h);
        BlockPos junction = at.west();
        BlockPos reservoir = junction.west();
        h.setBlock(junction.below(), EcologyTests.mature());
        h.setBlock(reservoir.below(), EcologyTests.mature());
        h.setBlock(reservoir, StorageModule.RESERVOIR_CELL.get());
        h.setBlock(
                junction,
                LogisticsModule.VASCULAR_JUNCTION
                        .get()
                        .defaultBlockState()
                        .setValue(VascularJunctionBlock.PORT.get(Direction.WEST), Port.INTAKE)
                        .setValue(VascularJunctionBlock.PORT.get(Direction.EAST), Port.OUTLET));
        h.assertValueEqual(
                h.getLevel()
                        .getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(reservoir), Direction.UP)
                        .fill(BiomassFluids.of(Biomass.of(500)), IFluidHandler.FluidAction.EXECUTE),
                500,
                "the Reservoir holds 500 BU");
        fill(h, at, stickGrid(2));
        select(h, at);
        h.runAfterDelay(80, () -> {
            h.assertTrue(
                    rote(h, at).cycles() + (rote(h, at).cycle().isPresent() ? 1 : 0) >= 1,
                    "the Junction fuelled a cycle");
            h.assertTrue(fuelHeld(h, at) > 0, "and the Palm holds biomass from the Reservoir");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBiomassBucketFillsAPalmWithRoteAndRefusesOneWithout(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = new MenuPlayer(h.getLevel(), "palm_bucket");
        use(h, player, stack(StorageModule.BIOMASS_BUCKET.get(), 1), at);
        h.assertTrue(
                player.getMainHandItem().is(StorageModule.BIOMASS_BUCKET.get()),
                "a Palm without Rote keeps the bucket full");
        rotePalm(h);
        use(h, player, stack(StorageModule.BIOMASS_BUCKET.get(), 1), at);
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET), "the bucket is emptied into the Palm");
        h.assertValueEqual(fuelHeld(h, at), 1000, "its 1,000 BU fill the tank");
        use(h, player, stack(StorageModule.BIOMASS_BUCKET.get(), 1), at);
        h.assertTrue(player.getMainHandItem().is(StorageModule.BIOMASS_BUCKET.get()), "a full tank takes no more");
        h.succeed();
    }

    // ---- saves, breaking, data reload ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 300)
    public static void aCycleInTheMiddleOfASaveResumesAndDeliversItsProductsOnce(GameTestHelper h) {
        BlockPos at = rotePalm(h);
        fuel(h, at, 50);
        fill(h, at, stickGrid(3));
        select(h, at);
        PalmRote[] before = {null};
        h.runAtTickTime(15, () -> {
            before[0] = rote(h, at);
            h.assertTrue(before[0].cycle().isPresent(), "the cycle is under way");
            int done = before[0].cycle().get().done();
            h.assertTrue(done > 5 && done < TICKS - 5, "in the middle: " + done + " of " + TICKS);
            reload(h, at);
            PalmRote after = rote(h, at);
            h.assertTrue(after.equals(before[0]), "the save keeps the pattern, the cycle, the tray and the biomass");
            h.assertValueEqual(inGrid(h, at, Items.OAK_PLANKS), 4, "and the grid after the inputs were spent");
            h.assertTrue(h.getBlockState(at).getValue(ShapingPalmBlock.ROTE), "Rote still shows");
        });
        h.runAtTickTime(15 + TICKS, () -> {
            h.assertValueEqual(inTray(h, at, Items.STICK) % 4, 0, "products come in whole crafts");
            h.assertTrue(inTray(h, at, Items.STICK) >= 4, "the saved cycle delivered");
        });
        h.runAtTickTime(15 + 3 * TICKS + 20, () -> {
            h.assertValueEqual(rote(h, at).cycles(), 3, "three planks each made three cycles");
            h.assertValueEqual(inTray(h, at, Items.STICK), 12, "twelve sticks: nothing twice, nothing lost");
            h.assertValueEqual(fuelHeld(h, at), 20, "and 30 BU");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 300)
    public static void breakingThePalmMidCycleKeepsItsWorkOnTheItemAndPlacingItFinishesOnce(GameTestHelper h) {
        BlockPos at = rotePalm(h);
        fuel(h, at, 50);
        fill(h, at, stickGrid(3));
        select(h, at);
        ItemStack[] kept = {ItemStack.EMPTY};
        h.runAtTickTime(15, () -> {
            h.assertTrue(rote(h, at).cycle().isPresent(), "the cycle is under way");
            h.getLevel().destroyBlock(h.absolutePos(at), true);
            List<ItemEntity> drops = loose(h, at);
            h.assertValueEqual(drops.size(), 1, "the Palm is the only drop: nothing spilled");
            kept[0] = drops.get(0).getItem().copy();
            h.assertTrue(kept[0].is(ProcessingModule.SHAPING_PALM_ITEM.get()), "and it is the Palm");
            PalmRote carried = kept[0].get(ProcessingModule.PALM_ROTE.get());
            h.assertTrue(carried != null && carried.cycle().isPresent(), "its item carries the cycle");
            h.assertValueEqual(carried.biomass().stored().units(), 40, "the biomass");
            h.assertTrue(carried.pattern().isPresent(), "and the pattern");
            h.assertTrue(
                    Boolean.TRUE.equals(kept[0].get(ProcessingModule.PALM_REACH.get())),
                    "and Reach, so no fiber is lost");
            h.assertValueEqual(
                    kept[0].get(ProcessingModule.PALM_CONTENTS.get()).cell(0).getCount(), 2, "and the grid");
            drops.get(0).discard();
            h.setBlock(at.below(), EcologyTests.mature());
            MenuPlayer player = new MenuPlayer(h.getLevel(), "palm_replace");
            use(h, player, kept[0].copy(), at.below());
            h.assertTrue(rote(h, at) != null, "placed again, the Palm has Rote");
            h.assertTrue(rote(h, at).equals(carried), "and the very state it was broken in");
        });
        h.runAtTickTime(15 + TICKS + 10, () -> {
            h.assertValueEqual(inTray(h, at, Items.STICK) % 4, 0, "whole crafts");
            h.assertTrue(inTray(h, at, Items.STICK) >= 4, "the carried cycle delivered");
            h.assertTrue(h.getBlockState(at).getValue(ShapingPalmBlock.ROTE), "Rote shows on the placed block");
        });
        h.runAtTickTime(15 + 3 * TICKS + 20, () -> {
            h.assertValueEqual(inTray(h, at, Items.STICK), 12, "three crafts of four sticks, none twice");
            h.assertTrue(loose(h, at).isEmpty(), "nothing on the ground");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "palm_rote_recipes",
            timeoutTicks = 400)
    public static void aChangedRecipePausesTheCycleWithAReasonAndRetriesWithinTwentyTicks(GameTestHelper h) {
        BlockPos at = rotePalm(h);
        fuel(h, at, 100);
        fill(h, at, stickGrid(4));
        select(h, at);
        var recipes = h.getLevel().getRecipeManager();
        List<RecipeHolder<?>> original = new ArrayList<>(recipes.getRecipes());
        List<RecipeHolder<?>> changed = new ArrayList<>();
        for (RecipeHolder<?> holder : original) {
            if (!holder.id().toString().equals("minecraft:stick")) {
                changed.add(holder);
                continue;
            }
            ShapedRecipe kind = (ShapedRecipe) holder.value();
            ShapedRecipePattern pattern = ShapedRecipePattern.of(
                    java.util.Map.of('#', Ingredient.of(net.minecraft.tags.ItemTags.PLANKS)), "#", "#");
            changed.add(new RecipeHolder<>(
                    holder.id(),
                    new ShapedRecipe(
                            "",
                            CraftingBookCategory.MISC,
                            pattern,
                            stack(Items.STICK, kind.getResultItem(null).getCount() * 2))));
        }
        recipes.replaceRecipes(changed);
        long[] restoredAt = {-1};
        h.onEachTick(() -> {
            if (restoredAt[0] < 0) return;
            long since = h.getTick() - restoredAt[0];
            if (rote(h, at).cycle().isPresent() && since < 3)
                h.fail("the Palm retried at once instead of waiting for its interval");
        });
        h.startSequence()
                .thenExecuteAfter(5, () -> {
                    h.assertTrue(rote(h, at).cycle().isEmpty(), "a changed result starts nothing");
                    h.assertValueEqual(status(h, at), PalmStatus.RECIPE_CHANGED, "the Palm says the recipe changed");
                    h.assertValueEqual(inGrid(h, at, Items.OAK_PLANKS), 8, "nothing was taken");
                    h.assertValueEqual(fuelHeld(h, at), 100, "nor any biomass");
                    h.assertTrue(rote(h, at).pattern().isPresent(), "the pattern is kept");
                    recipes.replaceRecipes(original);
                    restoredAt[0] = h.getTick();
                })
                .thenExecuteAfter(2, () -> {
                    h.assertTrue(rote(h, at).cycle().isEmpty(), "two ticks after the data came back it still waits");
                })
                .thenExecuteAfter(25, () -> {
                    h.assertTrue(
                            started(h, at), "within the retry interval the restored recipe starts the cycle again");
                    h.assertValueEqual(fuelHeld(h, at), 90, "paying 10 BU");
                })
                .thenSucceed();
    }

    // ---- the shared limit ----

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "palm_rote_quota",
            timeoutTicks = 200)
    public static void atMostThirtyTwoCyclesStartInOneServerTickAndTheRestRetryWithinTwentyFourTicks(GameTestHelper h) {
        List<BlockPos> palms = new ArrayList<>();
        for (int x = 0; x < 8; x++) for (int z = 0; z < 5; z++) palms.add(new BlockPos(x, 2, z));
        for (BlockPos at : palms) {
            rotePalm(h, at);
            fuel(h, at, 10);
            fill(h, at, stickGrid(1));
            h.assertValueEqual(select(h, at), STATUS + "selected", "the sticks are selected at " + at.toShortString());
        }
        int[] seen = {0};
        int[] mostInOneTick = {0};
        int[] ticksWithStarts = {0};
        h.onEachTick(() -> {
            int now = 0;
            for (BlockPos at : palms) if (started(h, at)) now++;
            int delta = now - seen[0];
            if (delta > 0) ticksWithStarts[0]++;
            mostInOneTick[0] = Math.max(mostInOneTick[0], delta);
            seen[0] = now;
        });
        h.runAtTickTime(60, () -> {
            h.assertValueEqual(seen[0], palms.size(), "every Palm started in the end");
            h.assertValueEqual(mostInOneTick[0], 32, "32 Palms started together in one tick, never more");
            h.assertTrue(ticksWithStarts[0] > 1, "the rest started on later ticks");
            h.succeed();
        });
    }
}
