package org.jd.infestusfrontier.testmod;

import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.done;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.discovery.PendingCredits;
import org.jd.infestusfrontier.processing.ShapingPalmMenu;
import org.jd.infestusfrontier.testmod.WorkshopAdvancementTests.Worker;

/**
 * The Workshop steps exhibit of the default demo ({@code captures/demo.json}, south of row C at x 84 to 112): an ore row
 * of a Gizzard, a Kidney and a Bio-Furnace beside the chests that supply them, a chest and a Capsule Tree each joined by
 * Item Veins to a Reach Palm, and signs that name each step. Every test builds it from the demo's own commands and earns
 * the steps the way a visitor does, from the exhibit's own supplies, with no grant and no command.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class WorkshopStepsExhibitTests {
    private static final BlockPos MIN = new BlockPos(84, -62, -23);
    private static final BlockPos MAX = new BlockPos(113, -55, -14);
    private static final int ENTITIES = DemoExhibit.ENTITIES_LOAD_TICKS;
    private static final int SETTLE = 40;

    private static final BlockPos GIZZARD = new BlockPos(86, -60, -16);
    private static final BlockPos KIDNEY = new BlockPos(90, -60, -16);
    private static final BlockPos FURNACE = new BlockPos(94, -60, -16);
    private static final BlockPos RAW_CHEST = new BlockPos(88, -60, -16);
    private static final BlockPos TAILINGS_CHEST = new BlockPos(92, -60, -16);
    private static final BlockPos DUST_CHEST = new BlockPos(96, -60, -16);
    private static final BlockPos LOG_CHEST = new BlockPos(100, -60, -16);
    private static final BlockPos CHEST_PALM = new BlockPos(104, -60, -16);
    private static final BlockPos TREE_ROOT = new BlockPos(108, -60, -16);
    private static final BlockPos TREE_PALM = new BlockPos(112, -60, -16);
    private static final BlockPos STOCK_CHEST = new BlockPos(106, -60, -18);

    private WorkshopStepsExhibitTests() {}

    private static DemoExhibit exhibit(int slot) {
        return new DemoExhibit(MIN, MAX, slot);
    }

    /** What a sign says, lines joined. */
    private static String text(ServerLevel level, BlockPos absolute) {
        SignBlockEntity sign = (SignBlockEntity) level.getBlockEntity(absolute);
        StringBuilder text = new StringBuilder();
        for (Component line : sign.getFrontText().getMessages(false))
            text.append(line.getString()).append(' ');
        return text.toString();
    }

    private static ItemStack take(ServerLevel level, BlockPos chest, int slot, int count) {
        return ((ChestBlockEntity) level.getBlockEntity(chest)).removeItem(slot, count);
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_exhibit",
            timeoutTicks = 480)
    public static void theOreRowEarnsThreeStepsAndTheGoalFromItsOwnChests(GameTestHelper h) {
        DemoExhibit yard = exhibit(30);
        yard.force(h);
        ServerLevel level = h.getLevel();
        Worker visitor = new Worker(h, "ore_visitor");
        PendingCredits credits = PendingCredits.get(level);
        h.startSequence()
                .thenIdle(ENTITIES)
                .thenExecute(() -> {
                    yard.build(h);
                    for (String step : List.of("raw_to_dust", "washed_tailings", "dust_ingot", "ore_workshop"))
                        h.assertFalse(done(h, visitor, "contact/" + step), step + " is not granted by the exhibit");
                    String title = text(level, yard.at(new BlockPos(91, -60, -22)));
                    h.assertTrue(title.contains("WORKSHOP STEPS") && title.contains("ore"), "the row is titled");
                    for (String step : List.of("1 Raw Ore to", "2 One More Dust", "3 Dust to Ingot")) {
                        int x = step.startsWith("1") ? 86 : step.startsWith("2") ? 90 : 94;
                        h.assertTrue(
                                text(level, yard.at(new BlockPos(x, -60, -19))).contains(step), "a sign says " + step);
                    }
                    visitor.useAbsolute(yard.at(GIZZARD), take(level, yard.at(RAW_CHEST), 2, 1)); // raw copper
                    visitor.useAbsolute(yard.at(KIDNEY), take(level, yard.at(TAILINGS_CHEST), 1, 1)); // gold Tailings
                    for (int n = 0; n < 4; n++)
                        visitor.useAbsolute(yard.at(FURNACE), take(level, yard.at(DUST_CHEST), 0, 1));
                })
                .thenIdle(340)
                .thenExecute(() -> {
                    h.assertFalse(done(h, visitor, "contact/raw_to_dust"), "not earned while the visitor is away");
                    credits.award(visitor);
                    for (String step : List.of("raw_to_dust", "washed_tailings", "dust_ingot"))
                        h.assertTrue(done(h, visitor, "contact/" + step), step + " earned from the exhibit's supplies");
                    CriteriaTriggers.TICK.trigger(visitor);
                    h.assertTrue(done(h, visitor, "contact/ore_workshop"), "and the goal");
                    yard.tidy(h);
                    yard.forget(h);
                })
                .thenSucceed();
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_palm_tree_exhibit",
            timeoutTicks = 200)
    public static void theChestAndTheTreeEachSupplyAPalmAndTheVisitorsEarnTheirStepsApart(GameTestHelper h) {
        DemoExhibit yard = exhibit(31);
        yard.force(h);
        ServerLevel level = h.getLevel();
        Worker storer = new Worker(h, "tree_visitor");
        Worker chestCrafter = new Worker(h, "chest_visitor");
        Worker treeCrafter = new Worker(h, "tree_crafter");
        h.startSequence()
                .thenIdle(ENTITIES)
                .thenExecute(() -> yard.build(h))
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    List<String> signs = new ArrayList<>();
                    for (BlockPos at : List.of(
                            new BlockPos(102, -60, -22),
                            new BlockPos(104, -60, -19),
                            new BlockPos(108, -60, -19),
                            new BlockPos(112, -60, -19))) signs.add(text(level, yard.at(at)));
                    h.assertTrue(signs.get(0).contains("WORKSHOP STEPS"), "the storage row is titled");
                    h.assertTrue(signs.get(1).contains("Restock"), "the chest's Palm says to Restock");
                    h.assertTrue(signs.get(2).contains("Capsule Tree"), "the tree is named");
                    h.assertTrue(signs.get(3).contains("Reach Palm"), "so is the tree's Palm");
                    for (Worker visitor : List.of(storer, chestCrafter, treeCrafter))
                        for (String step : List.of("tree_storage", "palm_from_storage"))
                            h.assertFalse(done(h, visitor, "contact/" + step), step + " is not granted");
                    // the tree: put a stack of the stock chest in by the list on the trunk
                    ItemStack stock = take(level, yard.at(STOCK_CHEST), 0, 16);
                    h.assertTrue(stock.is(Items.COPPER_INGOT), "the stock chest holds copper to store");
                    AbstractContainerMenu ledger = storer.openAbsolute(yard.at(TREE_ROOT));
                    ledger.setCarried(stock);
                    int open = ((org.jd.infestusfrontier.storage.ItemCapsuleMenu) ledger)
                            .kinds(); // the entry after the last kind
                    ledger.clicked(open, 0, ClickType.PICKUP, storer);
                    h.assertTrue(done(h, storer, "contact/tree_storage"), "a stack stored in the tree earns its step");
                    // the chest's Palm: its one log is restocked from the chest and the craft is taken
                    AbstractContainerMenu palm = chestCrafter.openAbsolute(yard.at(CHEST_PALM));
                    h.assertTrue(
                            palm.clickMenuButton(chestCrafter, ShapingPalmMenu.RESTOCK),
                            "the chest's logs are in reach");
                    palm.clicked(ShapingPalmMenu.RESULT, 0, ClickType.PICKUP, chestCrafter);
                    h.assertTrue(palm.getCarried().is(Items.OAK_PLANKS), "the craft makes planks");
                    h.assertTrue(done(h, chestCrafter, "contact/palm_from_storage"), "a chest's craft earns the step");
                    h.assertFalse(done(h, chestCrafter, "contact/tree_storage"), "which is not the tree's step");
                    // the tree's Palm: six ingots restocked from the tree make iron bars
                    AbstractContainerMenu bars = treeCrafter.openAbsolute(yard.at(TREE_PALM));
                    h.assertTrue(
                            bars.clickMenuButton(treeCrafter, ShapingPalmMenu.RESTOCK), "the tree's iron is in reach");
                    bars.clicked(ShapingPalmMenu.RESULT, 0, ClickType.PICKUP, treeCrafter);
                    h.assertTrue(bars.getCarried().is(Items.IRON_BARS), "the craft makes iron bars");
                    h.assertTrue(done(h, treeCrafter, "contact/palm_from_storage"), "a tree's craft earns the step");
                    h.assertFalse(done(h, treeCrafter, "contact/tree_storage"), "without storing anything in it");
                    h.assertFalse(done(h, storer, "contact/palm_from_storage"), "storing is not crafting");
                    yard.tidy(h);
                    yard.forget(h);
                })
                .thenSucceed();
    }
}
