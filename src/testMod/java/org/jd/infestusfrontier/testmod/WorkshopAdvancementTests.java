package org.jd.infestusfrontier.testmod;

import static org.jd.infestusfrontier.testmod.PalmNet.SETTLE;
import static org.jd.infestusfrontier.testmod.PalmNet.rig;
import static org.jd.infestusfrontier.testmod.ShapingPalmTests.stack;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.done;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.holder;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.id;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.mineral.Metal;
import org.jd.infestusfrontier.core.mineral.Parcel;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.core.processing.Batch;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.discovery.PendingCredits;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.mineral.MineralModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.organ.api.Stewarded;
import org.jd.infestusfrontier.organ.api.WorkshopActionEvent;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.ShapingPalmMenu;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.processing.api.OrganBatch;
import org.jd.infestusfrontier.processing.api.PalmRote;
import org.jd.infestusfrontier.storage.ItemCapsuleMenu;
import org.jd.infestusfrontier.storage.ItemCapsulePodMenu;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.ItemStore;
import org.jd.infestusfrontier.storage.api.ItemStores;
import org.jd.infestusfrontier.testmod.ShapingPalmTests.MenuPlayer;

/**
 * The storage and ore-processing milestones of the advancement tab. Every milestone is earned by a successful action
 * of one named player at a real screen or a real organ: a stack stored in a standing Capsule Tree, a Shaping Palm craft
 * that spent an item the Palm's Mouth fetched, and the Gizzard, Kidney and Bio-Furnace batches of their own recipes.
 * A refused, blocked, unused or hand-only action earns nothing. The players are real server players the server does not
 * list, so an organ's credit waits in {@link PendingCredits} and is awarded on login, as for the starter milestones;
 * the real client capture covers the online path.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class WorkshopAdvancementTests {
    private static final String TREE = "contact/tree_storage";
    private static final String PALM_MILESTONE = "contact/palm_from_storage";
    private static final String GROUND = "contact/raw_to_dust";
    private static final String WASHED = "contact/washed_tailings";
    private static final String INGOT = "contact/dust_ingot";
    private static final String GOAL = "contact/ore_workshop";
    private static final BlockPos GIZZARD = new BlockPos(1, 2, 1);
    private static final BlockPos KIDNEY = new BlockPos(3, 2, 1);
    private static final BlockPos FURNACE = new BlockPos(5, 2, 1);
    private static final BlockPos ROTE = new BlockPos(2, 2, 6);
    private static final int GRIND_TICKS = 160;
    private static final int SMELT_TICKS = 320;

    private WorkshopAdvancementTests() {}

    // ---- players and fixtures ----

    /** A real server player with a wire that goes nowhere and a screen that opens without a packet. */
    static final class Worker extends ServerPlayer {
        Worker(GameTestHelper h, String name) {
            super(
                    h.getLevel().getServer(),
                    h.getLevel(),
                    new GameProfile(UUID.randomUUID(), name),
                    ClientInformation.createDefault());
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(connection);
            new ServerGamePacketListenerImpl(
                    h.getLevel().getServer(),
                    connection,
                    this,
                    CommonListenerCookie.createInitial(getGameProfile(), false));
        }

        @Override
        public OptionalInt openMenu(MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extra) {
            containerMenu = provider.createMenu(1, getInventory(), this);
            return OptionalInt.of(1);
        }

        /** Stands by {@code at} and opens its screen, as a right-click does. */
        AbstractContainerMenu open(GameTestHelper h, BlockPos at) {
            BlockPos absolute = h.absolutePos(at);
            moveTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() - 1.5);
            openMenu((MenuProvider) h.getBlockEntity(at), absolute);
            containerMenu.broadcastChanges();
            return containerMenu;
        }

        /** Stands by the block at {@code absolute} and opens its screen. */
        AbstractContainerMenu openAbsolute(BlockPos absolute) {
            moveTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() - 1.5);
            openMenu((MenuProvider) serverLevel().getBlockEntity(absolute), absolute);
            containerMenu.broadcastChanges();
            return containerMenu;
        }

        /** Right-clicks the block at {@code absolute} holding {@code held}, as a player loads an organ by hand. */
        void useAbsolute(BlockPos absolute, ItemStack held) {
            moveTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() - 1.5);
            setItemInHand(InteractionHand.MAIN_HAND, held);
            gameMode.useItemOn(
                    this,
                    serverLevel(),
                    held,
                    InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(
                            net.minecraft.world.phys.Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
        }

        /** Right-clicks {@code at} holding {@code held}, as a player loads an organ by hand. */
        void use(GameTestHelper h, BlockPos at, ItemStack held) {
            BlockPos absolute = h.absolutePos(at);
            moveTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() - 1.5);
            setItemInHand(InteractionHand.MAIN_HAND, held);
            EcologyTests.use(h, this, at, Direction.UP);
        }
    }

    /** Puts {@code stack} in menu slot {@code slot} with the cursor, as a click on the well does. */
    private static void put(AbstractContainerMenu menu, Worker player, int slot, ItemStack stack) {
        stash(menu, player);
        menu.setCarried(stack);
        menu.clicked(slot, 0, ClickType.PICKUP, player);
    }

    /** What the cursor holds goes to the bag, as a player puts a taken stack away. */
    private static void stash(AbstractContainerMenu menu, Worker player) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) return;
        player.getInventory().add(carried.copy());
        menu.setCarried(ItemStack.EMPTY);
    }

    private static ItemStack named(Item item, int count, String name) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    // ---- the definitions ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "workshop_advancements")
    public static void theBranchesHangUnderTheStarterWorkshopWithItemIconsAndNoReward(GameTestHelper h) {
        record Step(String path, String parent, Item icon, String frame) {}
        for (Step step : List.of(
                new Step("tree_storage", "membrane_sheet", StorageModule.ITEM_CAPSULE_ITEM.get(), "task"),
                new Step("palm_from_storage", "tree_storage", ProcessingModule.SHAPING_PALM_ITEM.get(), "task"),
                new Step("raw_to_dust", "bone_plate", ProcessingModule.MINERAL_GIZZARD_ITEM.get(), "task"),
                new Step("washed_tailings", "raw_to_dust", ProcessingModule.WASHING_KIDNEY_ITEM.get(), "task"),
                new Step("dust_ingot", "raw_to_dust", Items.IRON_INGOT, "task"),
                new Step("ore_workshop", "dust_ingot", MineralModule.IRON_DUST.get(), "goal"))) {
            var advancement = holder(h, "contact/" + step.path()).value();
            h.assertValueEqual(advancement.parent().orElseThrow(), id("contact/" + step.parent()), "parent");
            var display = advancement.display().orElseThrow();
            h.assertValueEqual(display.getIcon().getItem(), step.icon(), step.path() + " icon");
            h.assertValueEqual(display.getType().getSerializedName(), step.frame(), step.path() + " frame");
            h.assertTrue(
                    advancement.rewards().recipes().isEmpty()
                            && advancement.rewards().loot().isEmpty()
                            && advancement.rewards().experience() == 0,
                    step.path() + " rewards nothing");
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "workshop_advancements")
    public static void theOreGoalNeedsTheThreeProcessingMilestonesAndNothingElse(GameTestHelper h) {
        ServerPlayer player = new Worker(h, "goal");
        CriteriaTriggers.TICK.trigger(player);
        h.assertFalse(done(h, player, GOAL), "nothing earned, no goal");
        for (String path : List.of(GROUND, WASHED)) {
            award(h, player, path);
            CriteriaTriggers.TICK.trigger(player);
            h.assertFalse(done(h, player, GOAL), "two of the three do not make the goal after " + path);
        }
        award(h, player, TREE);
        award(h, player, PALM_MILESTONE);
        CriteriaTriggers.TICK.trigger(player);
        h.assertFalse(done(h, player, GOAL), "the storage milestones are not ore processing");
        award(h, player, INGOT);
        CriteriaTriggers.TICK.trigger(player);
        h.assertTrue(done(h, player, GOAL), "the third makes the goal on the next tick");
        h.succeed();
    }

    private static void award(GameTestHelper h, ServerPlayer player, String path) {
        var holder = holder(h, path);
        for (String criterion : holder.value().criteria().keySet())
            player.getAdvancements().award(holder, criterion);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "workshop_advancements")
    public static void theGuideEntriesOfTheStepsNameTheirMilestones(GameTestHelper h) {
        var book = com.klikli_dev.modonomicon.data.BookDataManager.get()
                .getBook(ResourceLocation.fromNamespaceAndPath("infestusfrontier", "the_waking_genome"));
        for (String entry : List.of("item_capsule", "shaping_palm", "mineral_gizzard", "washing_kidney")) {
            var page =
                    book.getCategory(id("circulation")).getEntries().get(id("circulation/" + entry)).getPages().stream()
                            .filter(candidate -> candidate instanceof com.klikli_dev.modonomicon.book.page.BookTextPage)
                            .map(candidate -> (com.klikli_dev.modonomicon.book.page.BookTextPage) candidate)
                            .filter(candidate ->
                                    candidate.getTitle() instanceof com.klikli_dev.modonomicon.book.BookTextHolder title
                                            && title.getKey().contains(".milestone"))
                            .findFirst();
            h.assertTrue(page.isPresent(), entry + " has a page for its milestone");
        }
        h.succeed();
    }

    // ---- storing in a tree ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "workshop_advancements")
    public static void aPlayerWhoStoresInATreeByTheListOrByAPodEarnsTheTreeMilestoneAndNobodyElseDoes(
            GameTestHelper h) {
        LarderTests.plant(h, LarderTests.FORKING);
        LarderTests.growAll(h);
        BlockPos pod = LarderTests.pods(h).get(0);
        Worker byList = new Worker(h, "list");
        Worker byPod = new Worker(h, "pod");
        Worker bystander = new Worker(h, "bystander");
        Worker byShift = new Worker(h, "shift");
        h.assertFalse(done(h, byList, TREE), "growing and standing by a tree earns nothing");
        AbstractContainerMenu ledger = byList.open(h, LarderTests.ROOT);
        h.assertFalse(done(h, byList, TREE), "opening the list earns nothing");
        put(ledger, byList, 0, stack(Items.OAK_PLANKS, 5));
        h.assertTrue(
                LarderTests.store(h, LarderTests.ROOT).count(stack(Items.OAK_PLANKS, 1)) >= 5, "the tree took them");
        h.assertTrue(done(h, byList, TREE), "a stack put in the tree's list earns the milestone");
        AbstractContainerMenu podMenu = byPod.open(h, pod);
        put(podMenu, byPod, ItemCapsulePodMenu.COMPARTMENT, stack(Items.COBBLESTONE, 3));
        h.assertTrue(done(h, byPod, TREE), "a stack put in a pod of the tree earns it too");
        AbstractContainerMenu shift = byShift.open(h, LarderTests.ROOT);
        byShift.getInventory().items.set(0, stack(Items.STICK, 4));
        shift.quickMoveStack(byShift, shift.slots.size() - 9);
        h.assertTrue(done(h, byShift, TREE), "a shift-click from the inventory into the list earns it");
        h.assertFalse(done(h, bystander, TREE), "a player who stored nothing does not earn it");
        h.assertFalse(done(h, byList, PALM_MILESTONE), "and storing is not crafting");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "workshop_advancements")
    public static void aHopperAMouthAFullTreeAndALonePodEarnNoTreeMilestone(GameTestHelper h) {
        LarderTests.plant(h, LarderTests.FORKING);
        LarderTests.growAll(h);
        Worker full = new Worker(h, "full");
        List<WorkshopActionEvent> seen = new ArrayList<>();
        Consumer<WorkshopActionEvent> listener = event -> {
            if (event.player() == full) seen.add(event);
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, WorkshopActionEvent.class, listener);
        ItemStore tree = LarderTests.store(h, LarderTests.ROOT);
        h.assertTrue(tree.insert(stack(Items.OAK_PLANKS, 20), false).isEmpty(), "a Mouth fills the tree");
        IItemHandler hopper = LarderTests.items(h, LarderTests.ROOT);
        hopper.insertItem(0, stack(Items.STICK, 8), false);
        // fill every compartment with one kind, then try to store more of it by hand
        int room = tree.insert(stack(Items.DIRT, 64 * 1000), false).getCount();
        h.assertTrue(room > 0, "the tree fills to its limit");
        AbstractContainerMenu ledger = full.open(h, LarderTests.ROOT);
        int before = tree.count(stack(Items.DIRT, 1));
        ledger.setCarried(stack(Items.DIRT, 5));
        int dirtCell = -1;
        for (int cell = 0; cell < ItemCapsuleMenu.VIEW && dirtCell < 0; cell++)
            if (ledger.getSlot(cell).getItem().is(Items.DIRT)) dirtCell = cell;
        h.assertTrue(dirtCell >= 0, "the list shows the dirt");
        ledger.clicked(dirtCell, 0, ClickType.PICKUP, full);
        h.assertValueEqual(tree.count(stack(Items.DIRT, 1)), before, "a full kind takes no more");
        h.assertFalse(done(h, full, TREE), "a store that took nothing earns nothing");
        NeoForge.EVENT_BUS.unregister(listener);
        h.assertTrue(seen.isEmpty(), "and posted nothing for the player either");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "workshop_advancements")
    public static void aPodOnItsOwnIsNotATreeAndAFakePlayerEarnsNothing(GameTestHelper h) {
        BlockPos alone = LarderTests.aloneLarder(h, 0);
        Worker player = new Worker(h, "alone");
        AbstractContainerMenu menu = player.open(h, alone);
        put(menu, player, ItemCapsulePodMenu.COMPARTMENT, stack(Items.OAK_PLANKS, 3));
        h.assertValueEqual(LarderTests.store(h, alone).count(stack(Items.OAK_PLANKS, 1)), 3, "the pod took them");
        h.assertFalse(done(h, player, TREE), "a pod that hangs on no stem is no tree");
        LarderTests.plant(h, LarderTests.FORKING);
        LarderTests.growAll(h);
        MenuPlayer fake = new MenuPlayer(h.getLevel(), "fake_storer");
        List<WorkshopActionEvent> seen = new ArrayList<>();
        Consumer<WorkshopActionEvent> listener = event -> {
            if (event.player().getUUID().equals(fake.getUUID())) seen.add(event);
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, WorkshopActionEvent.class, listener);
        BlockPos root = h.absolutePos(LarderTests.ROOT);
        fake.moveTo(root.getX() + 0.5, root.getY(), root.getZ() - 1.5);
        fake.openMenu((MenuProvider) h.getBlockEntity(LarderTests.ROOT), root);
        fake.containerMenu.setCarried(stack(Items.OAK_PLANKS, 2));
        fake.containerMenu.clicked(0, 0, ClickType.PICKUP, fake);
        NeoForge.EVENT_BUS.unregister(listener);
        h.assertTrue(
                LarderTests.store(h, LarderTests.ROOT).count(stack(Items.OAK_PLANKS, 1)) >= 2,
                "the fake player stored in the tree");
        h.assertTrue(seen.isEmpty(), "but a fake player earns no milestone");
        h.succeed();
    }

    // ---- crafting from storage ----

    private static final int CELL = ShapingPalmMenu.FIRST_CELL;

    private static ItemStack log(int count) {
        return stack(Items.OAK_LOG, count);
    }

    private static int planks(Worker player) {
        int total = player.containerMenu.getCarried().is(Items.OAK_PLANKS)
                ? player.containerMenu.getCarried().getCount()
                : 0;
        for (ItemStack stack : player.getInventory().items) if (stack.is(Items.OAK_PLANKS)) total += stack.getCount();
        return total;
    }

    /** Takes the craft in the result well with the cursor, as a click does; what the cursor held goes to the bag. */
    private static void takeResult(Worker player) {
        stash(player.containerMenu, player);
        player.containerMenu.clicked(ShapingPalmMenu.RESULT, 0, ClickType.PICKUP, player);
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_palm_chest",
            timeoutTicks = 100)
    public static void aCraftFromAGridTheChestRestockedEarnsThePalmMilestone(GameTestHelper h) {
        PalmNet.Rig rig = rig(h, 12, MouthMode.BOTH, MouthMode.OUTPUT, log(64), log(64));
        Worker player = new Worker(h, "chest_palm");
        h.runAfterDelay(SETTLE, () -> {
            AbstractContainerMenu menu = player.open(h, rig.palm());
            put(menu, player, CELL, log(1));
            takeResult(player);
            h.assertValueEqual(planks(player), 4, "a craft on the hand's own log");
            h.assertFalse(done(h, player, PALM_MILESTONE), "a manual craft beside a linked route earns nothing");
            put(menu, player, CELL, log(1));
            h.assertTrue(menu.clickMenuButton(player, ShapingPalmMenu.RESTOCK), "the restock draws from the chest");
            h.assertFalse(done(h, player, PALM_MILESTONE), "a restock alone is not a craft");
            takeResult(player);
            h.assertValueEqual(planks(player), 8, "the craft on a restocked grid is the same craft");
            h.assertTrue(done(h, player, PALM_MILESTONE), "and it earns the milestone");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_palm_tree",
            timeoutTicks = 100)
    public static void aCraftFromAGridACapsuleTreeRestockedEarnsItToo(GameTestHelper h) {
        LarderTests.plant(h, LarderTests.FORKING);
        PalmNet.mouth(h, LarderTests.ROOT.below(), MouthMode.BOTH, null);
        LarderTests.growAll(h);
        h.assertTrue(
                LarderTests.store(h, LarderTests.ROOT).insert(log(30), false).isEmpty(), "the tree holds 30 logs");
        BlockPos at = new BlockPos(2, 2, 8);
        PalmNet.palm(h, at, MouthMode.BOTH);
        PalmNet.veins(h, at.below().east(), LarderTests.ROOT.below().west());
        Worker player = new Worker(h, "tree_palm");
        h.runAfterDelay(SETTLE, () -> {
            AbstractContainerMenu menu = player.open(h, at);
            put(menu, player, CELL, log(1));
            h.assertTrue(menu.clickMenuButton(player, ShapingPalmMenu.RESTOCK), "the restock draws from the tree");
            takeResult(player);
            h.assertTrue(done(h, player, PALM_MILESTONE), "a craft on what the tree gave earns the milestone");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_palm_store",
            timeoutTicks = 100)
    public static void storeResultOnARestockedGridIsACraftFromStorage(GameTestHelper h) {
        PalmNet.Rig rig = rig(h, 12, MouthMode.BOTH, MouthMode.BOTH, log(20));
        Worker player = new Worker(h, "store_palm");
        h.runAfterDelay(SETTLE, () -> {
            AbstractContainerMenu menu = player.open(h, rig.palm());
            put(menu, player, CELL, log(1));
            h.assertTrue(menu.clickMenuButton(player, ShapingPalmMenu.RESTOCK), "restock");
            h.assertTrue(menu.clickMenuButton(player, ShapingPalmMenu.STORE_RESULT), "the craft goes to the chest");
            h.assertTrue(done(h, player, PALM_MILESTONE), "Store result is a craft taken by the player");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_palm_unused",
            timeoutTicks = 100)
    public static void aRouteNobodyUsesAMismatchedKindAndASwappedGridEarnNothing(GameTestHelper h) {
        PalmNet.Rig rig = rig(h, 12, MouthMode.BOTH, MouthMode.OUTPUT, log(64), log(64));
        Worker idle = new Worker(h, "idle");
        Worker named = new Worker(h, "named");
        Worker swapper = new Worker(h, "swapper");
        Worker dropper = new Worker(h, "dropper");
        h.runAfterDelay(SETTLE, () -> {
            // the route is linked and stocked, but the player crafts from the hand's own logs
            AbstractContainerMenu menu = idle.open(h, rig.palm());
            put(menu, idle, CELL, log(8));
            for (int n = 0; n < 8; n++) takeResult(idle);
            h.assertValueEqual(planks(idle), 32, "eight crafts by hand");
            h.assertFalse(done(h, idle, PALM_MILESTONE), "a linked route that is not used earns nothing");
            // a named log in the grid: the chest's plain logs are another kind, so there is nothing to restock
            menu = named.open(h, rig.palm());
            put(menu, named, CELL, named(Items.OAK_LOG, 1, "keep"));
            h.assertFalse(menu.clickMenuButton(named, ShapingPalmMenu.RESTOCK), "a named log is another kind");
            takeResult(named);
            h.assertValueEqual(planks(named), 4, "the named log crafted by hand");
            h.assertFalse(done(h, named, PALM_MILESTONE), "the kind is the item and its components");
            // restock, then swap the whole stack for the hand's own birch logs: the fetched logs are gone
            menu = swapper.open(h, rig.palm());
            put(menu, swapper, CELL, log(1));
            h.assertTrue(menu.clickMenuButton(swapper, ShapingPalmMenu.RESTOCK), "restock");
            put(menu, swapper, CELL, stack(Items.BIRCH_LOG, 64)); // the click swaps: the oak logs go to the cursor
            takeResult(swapper);
            h.assertTrue(
                    swapper.containerMenu.getCarried().is(Items.BIRCH_PLANKS), "the swapped grid crafted birch planks");
            h.assertFalse(done(h, swapper, PALM_MILESTONE), "a grid of the hand's own items earns nothing");
            // restock, take every log out, put one back by hand
            menu = dropper.open(h, rig.palm());
            put(menu, dropper, CELL, log(1));
            h.assertTrue(menu.clickMenuButton(dropper, ShapingPalmMenu.RESTOCK), "restock");
            menu.setCarried(ItemStack.EMPTY);
            menu.clicked(CELL, 0, ClickType.PICKUP, dropper); // the whole stack leaves with the hand
            put(menu, dropper, CELL, log(1));
            takeResult(dropper);
            h.assertFalse(done(h, dropper, PALM_MILESTONE), "the logs the chest gave left with the hand");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_palm_taker",
            timeoutTicks = 100)
    public static void theCraftIsCreditedToThePlayerWhoTookItNotToTheOneWhoRestocked(GameTestHelper h) {
        PalmNet.Rig rig = rig(h, 12, MouthMode.BOTH, MouthMode.OUTPUT, log(64), log(64));
        Worker loader = new Worker(h, "loader");
        Worker taker = new Worker(h, "taker");
        h.runAfterDelay(SETTLE, () -> {
            AbstractContainerMenu menu = loader.open(h, rig.palm());
            put(menu, loader, CELL, log(1));
            h.assertTrue(menu.clickMenuButton(loader, ShapingPalmMenu.RESTOCK), "restock");
            loader.closeContainer();
            taker.open(h, rig.palm());
            takeResult(taker);
            h.assertTrue(done(h, taker, PALM_MILESTONE), "the player who took the craft earns it");
            h.assertFalse(done(h, loader, PALM_MILESTONE), "the player who only restocked does not");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_palm_fake",
            timeoutTicks = 100)
    public static void aFakePlayerCraftingFromStorageEarnsNothing(GameTestHelper h) {
        PalmNet.Rig rig = rig(h, 12, MouthMode.BOTH, MouthMode.OUTPUT, log(64), log(64));
        MenuPlayer fake = new MenuPlayer(h.getLevel(), "fake_crafter");
        List<WorkshopActionEvent> seen = new ArrayList<>();
        Consumer<WorkshopActionEvent> listener = event -> {
            if (event.player().getUUID().equals(fake.getUUID())) seen.add(event);
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, WorkshopActionEvent.class, listener);
        h.runAfterDelay(SETTLE, () -> {
            BlockPos root = h.absolutePos(rig.palm());
            fake.moveTo(root.getX() + 0.5, root.getY(), root.getZ() - 1.5);
            fake.openMenu((MenuProvider) h.getBlockEntity(rig.palm()), root);
            fake.containerMenu.setCarried(log(1));
            fake.containerMenu.clicked(CELL, 0, ClickType.PICKUP, fake);
            fake.containerMenu.clickMenuButton(fake, ShapingPalmMenu.RESTOCK);
            fake.containerMenu.setCarried(ItemStack.EMPTY);
            fake.containerMenu.clicked(ShapingPalmMenu.RESULT, 0, ClickType.PICKUP, fake);
            NeoForge.EVENT_BUS.unregister(listener);
            h.assertTrue(fake.containerMenu.getCarried().is(Items.OAK_PLANKS), "the craft itself worked");
            h.assertTrue(seen.isEmpty(), "but a fake player earns nothing");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_palm_saved",
            timeoutTicks = 100)
    public static void whatTheMouthFetchedSurvivesASaveAndIsForgottenWhenThePalmIsBroken(GameTestHelper h) {
        PalmNet.Rig rig = rig(h, 12, MouthMode.BOTH, MouthMode.OUTPUT, log(80));
        Worker saved = new Worker(h, "saved");
        Worker carried = new Worker(h, "carried");
        h.runAfterDelay(SETTLE, () -> {
            AbstractContainerMenu menu = saved.open(h, rig.palm());
            put(menu, saved, CELL, log(1));
            h.assertTrue(menu.clickMenuButton(saved, ShapingPalmMenu.RESTOCK), "restock");
            saved.closeContainer();
            ShapingPalmTests.reload(h, rig.palm());
            saved.open(h, rig.palm());
            takeResult(saved);
            h.assertTrue(done(h, saved, PALM_MILESTONE), "what the Mouth fetched is still fetched after a save");
            saved.closeContainer();
            // break the Palm with 63 logs in it and place it again: the item keeps the grid, not where it came from
            h.getLevel().destroyBlock(h.absolutePos(rig.palm()), true);
            ItemStack dropped = ShapingPalmTests.loose(h, rig.palm()).get(0).getItem();
            ShapingPalmTests.loose(h, rig.palm()).forEach(drop -> drop.discard());
            carried.setItemInHand(InteractionHand.MAIN_HAND, dropped.copy());
            h.placeAt(carried, carried.getMainHandItem(), rig.palm().below(), Direction.UP);
            h.assertBlockPresent(ProcessingModule.SHAPING_PALM.get(), rig.palm());
            AbstractContainerMenu again = carried.open(h, rig.palm());
            h.assertTrue(again.getSlot(CELL).getItem().getCount() > 1, "the grid came back with the item");
            takeResult(carried);
            h.assertFalse(done(h, carried, PALM_MILESTONE), "the item carries the grid, not what the Mouth fetched");
            h.succeed();
        });
    }

    // ---- a Palm with Rote ----

    private static BlockPos rote(GameTestHelper h, Worker steward, boolean placed) {
        h.setBlock(ROTE.below(), EcologyTests.mature());
        BlockState state = ProcessingModule.SHAPING_PALM.get().defaultBlockState();
        h.setBlock(ROTE, state);
        if (placed) state.getBlock().setPlacedBy(h.getLevel(), h.absolutePos(ROTE), state, steward, ItemStack.EMPTY);
        steward.use(h, ROTE, stack(LogisticsModule.CONTRACTILE_FIBER.get(), 1));
        steward.use(h, ROTE, stack(ProcessingModule.SYNAPTIC_GEL.get(), 1));
        h.assertTrue(rote(h) != null, "the gel gave the Palm Rote");
        fuel(h);
        AbstractContainerMenu menu = steward.open(h, ROTE);
        put(menu, steward, CELL, log(1));
        h.assertTrue(menu.clickMenuButton(steward, ShapingPalmMenu.SELECT), "the log pattern is selected");
        steward.closeContainer();
        return ROTE;
    }

    private static void fuel(GameTestHelper h) {
        h.getLevel()
                .getCapability(BiomassCapabilities.BLOCK, h.absolutePos(ROTE), null)
                .insert(Biomass.of(200), false);
    }

    private static PalmRote rote(GameTestHelper h) {
        return h.getBlockEntity(ROTE).collectComponents().get(ProcessingModule.PALM_ROTE.get());
    }

    private static IItemHandler ports(GameTestHelper h) {
        return h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(ROTE), null);
    }

    private static Optional<UUID> stewardOf(GameTestHelper h) {
        return ((Stewarded) h.getBlockEntity(ROTE)).steward().player();
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_advancements",
            timeoutTicks = 200)
    public static void aCycleCreditsItsStewardOnlyWhenAMouthSuppliedItsIngredientAndTheCreditWaitsForLogin(
            GameTestHelper h) {
        Worker steward = new Worker(h, "steward");
        Worker bystander = new Worker(h, "rote_bystander");
        PendingCredits credits = PendingCredits.get(h.getLevel());
        rote(h, steward, true);
        h.assertValueEqual(stewardOf(h), Optional.of(steward.getUUID()), "the player who placed and tended it");
        h.runAtTickTime(50, () -> {
            h.assertValueEqual(rote(h).cycles(), 1, "the first cycle ran on the hand's own log");
            h.assertFalse(credits.holdsAutomatic(steward.getUUID(), id("shaping_palm")), "which earns nothing");
            ports(h).insertItem(0, log(1), false); // a hopper's way in
        });
        h.runAtTickTime(100, () -> {
            h.assertValueEqual(rote(h).cycles(), 2, "the second cycle ran on a hopper's log");
            h.assertFalse(credits.holdsAutomatic(steward.getUUID(), id("shaping_palm")), "a hopper is not the network");
            h.assertTrue(ItemStores.insert(ports(h), log(1), false).isEmpty(), "a Mouth's way in");
        });
        h.runAtTickTime(150, () -> {
            h.assertValueEqual(rote(h).cycles(), 3, "the third cycle ran on a Mouth's log");
            h.assertTrue(credits.holdsAutomatic(steward.getUUID(), id("shaping_palm")), "its steward waits");
            h.assertFalse(credits.holdsAutomatic(bystander.getUUID(), id("shaping_palm")), "nobody else");
            h.assertFalse(done(h, steward, PALM_MILESTONE), "not earned while away");
            credits.award(bystander);
            h.assertFalse(done(h, bystander, PALM_MILESTONE), "not the bystander's");
            credits.award(steward);
            h.assertTrue(done(h, steward, PALM_MILESTONE), "earned on login");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_advancements",
            timeoutTicks = 200)
    public static void aCycleSavedMidwayKeepsItsSupplyAndItsSteward(GameTestHelper h) {
        Worker steward = new Worker(h, "midway");
        PendingCredits credits = PendingCredits.get(h.getLevel());
        rote(h, steward, false);
        h.runAtTickTime(50, () -> {
            h.assertValueEqual(rote(h).cycles(), 1, "the first cycle ran");
            h.assertTrue(ItemStores.insert(ports(h), log(1), false).isEmpty(), "a Mouth supplies the next");
        });
        h.runAtTickTime(70, () -> {
            h.assertTrue(rote(h).cycle().isPresent(), "a cycle is under way");
            h.assertTrue(rote(h).cycle().get().supplied(), "and it knows its log came from the network");
            ShapingPalmTests.reload(h, ROTE);
            h.assertTrue(rote(h).cycle().get().supplied(), "a save and a read keep that");
            h.assertValueEqual(stewardOf(h), Optional.of(steward.getUUID()), "and the steward");
        });
        h.runAtTickTime(130, () -> {
            h.assertValueEqual(rote(h).cycles(), 2, "the cycle finished once");
            h.assertTrue(credits.holdsAutomatic(steward.getUUID(), id("shaping_palm")), "credited after the reload");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_advancements",
            timeoutTicks = 200)
    public static void aPalmNobodyTendedCreditsNobodyAndTheFirstToOpenItClaimsOnlyTheFuture(GameTestHelper h) {
        Worker maker = new Worker(h, "maker");
        Worker opener = new Worker(h, "late_opener");
        PendingCredits credits = PendingCredits.get(h.getLevel());
        h.setBlock(ROTE.below(), EcologyTests.mature());
        h.setBlock(ROTE, ProcessingModule.SHAPING_PALM.get());
        maker.use(h, ROTE, stack(LogisticsModule.CONTRACTILE_FIBER.get(), 1));
        maker.use(h, ROTE, stack(ProcessingModule.SYNAPTIC_GEL.get(), 1));
        fuel(h);
        MenuPlayer fake = ShapingPalmTests.open(h, ROTE, "untended");
        ShapingPalmTests.put(fake, 0, log(1));
        h.assertTrue(fake.containerMenu.clickMenuButton(fake, ShapingPalmMenu.SELECT), "the pattern is selected");
        h.assertTrue(
                stewardOf(h).isEmpty(),
                "handing a Fiber and a Gel to a Palm does not make a steward, nor does a fake player");
        h.runAtTickTime(50, () -> {
            h.assertTrue(ItemStores.insert(ports(h), log(1), false).isEmpty(), "a Mouth supplies a log");
        });
        h.runAtTickTime(100, () -> {
            h.assertValueEqual(rote(h).cycles(), 2, "both cycles ran");
            h.assertFalse(
                    credits.holdsAutomatic(maker.getUUID(), id("shaping_palm")), "an untended Palm credits nobody");
            Organs.openScreen(h.getLevel(), h.absolutePos(ROTE), opener);
            h.assertValueEqual(
                    stewardOf(h), Optional.of(opener.getUUID()), "the first to open an untended Palm claims it");
            h.assertFalse(credits.holdsAutomatic(opener.getUUID(), id("shaping_palm")), "the past earns nothing");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_advancements",
            timeoutTicks = 120)
    public static void aFinishedCycleWithNoRoomInItsTrayEarnsNothingUntilItDelivers(GameTestHelper h) {
        Worker steward = new Worker(h, "tray");
        PendingCredits credits = PendingCredits.get(h.getLevel());
        h.setBlock(ROTE.below(), EcologyTests.mature());
        BlockState state = ProcessingModule.SHAPING_PALM.get().defaultBlockState();
        h.setBlock(ROTE, state);
        state.getBlock().setPlacedBy(h.getLevel(), h.absolutePos(ROTE), state, steward, ItemStack.EMPTY);
        PalmRote waiting = new PalmRote(
                Optional.empty(),
                Optional.of(new PalmRote.Cycle(PalmRote.TICKS, new ItemStack(Items.OAK_PLANKS, 4), List.of(), true)),
                List.of(new ItemStack(Items.OAK_PLANKS, 64)),
                BiomassTank.empty(PalmRote.CAPACITY),
                0,
                false);
        h.getBlockEntity(ROTE)
                .applyComponents(
                        DataComponentMap.builder()
                                .set(ProcessingModule.PALM_ROTE.get(), waiting)
                                .build(),
                        DataComponentPatch.EMPTY);
        h.runAtTickTime(40, () -> {
            h.assertTrue(rote(h).cycle().isPresent(), "the finished cycle waits for room");
            h.assertFalse(
                    credits.holdsAutomatic(steward.getUUID(), id("shaping_palm")),
                    "a cycle that cannot deliver earns nothing");
            ports(h).extractItem(9, 64, false); // the tray's result slot
        });
        h.runAtTickTime(60, () -> {
            h.assertTrue(rote(h).cycle().isEmpty(), "it delivers once there is room");
            h.assertValueEqual(rote(h).cycles(), 1, "once");
            h.assertTrue(credits.holdsAutomatic(steward.getUUID(), id("shaping_palm")), "and then it counts");
            h.succeed();
        });
    }

    // ---- the ore line ----

    private static BlockPos organ(GameTestHelper h, BlockPos at, Block block, int biomass) {
        h.setBlock(at.below(), EcologyTests.mature());
        h.setBlock(at, block);
        if (biomass > 0) fuel(h, at, biomass);
        return at;
    }

    private static void fuel(GameTestHelper h, BlockPos at, int units) {
        h.getLevel()
                .getCapability(BiomassCapabilities.BLOCK, h.absolutePos(at), null)
                .insert(Biomass.of(units), false);
    }

    private static void water(GameTestHelper h, BlockPos at, int mb) {
        h.getLevel()
                .getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(at), Direction.UP)
                .fill(new FluidStack(Fluids.WATER, mb), IFluidHandler.FluidAction.EXECUTE);
    }

    private static ItemStack tailings(Metal metal, int units) {
        return MineralModule.tailings(new Parcel(metal, units));
    }

    private static BatchOrganContents contents(GameTestHelper h, BlockPos at) {
        return h.getBlockEntity(at).collectComponents().get(ProcessingModule.BATCH_CONTENTS.get());
    }

    private static IItemHandler ports(GameTestHelper h, BlockPos at) {
        return h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(at), null);
    }

    private static List<ItemStack> trays(int count) {
        List<ItemStack> list = new ArrayList<>();
        for (int n = 0; n < count; n++) list.add(ItemStack.EMPTY);
        return list;
    }

    /** An organ one tick from finishing a batch of {@code recipe} that {@code starter} started, with {@code trays} set. */
    private static void finishing(
            GameTestHelper h,
            BlockPos at,
            int capacity,
            List<ItemStack> trays,
            String recipe,
            ItemStack result,
            int slot,
            UUID starter) {
        OrganBatch batch =
                new OrganBatch(new Batch(recipe, 100, 99), result, List.of(), slot, Optional.of(starter), 0f);
        h.getBlockEntity(at)
                .applyComponents(
                        DataComponentMap.builder()
                                .set(
                                        ProcessingModule.BATCH_CONTENTS.get(),
                                        new BatchOrganContents(
                                                List.of(ItemStack.EMPTY),
                                                trays,
                                                0,
                                                BiomassTank.empty(Biomass.of(capacity)),
                                                OrganHistory.NONE,
                                                Optional.of(batch),
                                                Optional.of(starter),
                                                0f))
                                .build(),
                        DataComponentPatch.EMPTY);
    }

    private static ResourceLocation recipe(String path) {
        return id(path);
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_advancements",
            timeoutTicks = 400)
    public static void eachStepOfTheLineEarnsItsMilestoneAndTheGoalNeedsAllThree(GameTestHelper h) {
        Worker player = new Worker(h, "miner");
        Worker bystander = new Worker(h, "ore_bystander");
        PendingCredits credits = PendingCredits.get(h.getLevel());
        organ(h, GIZZARD, ProcessingModule.MINERAL_GIZZARD.get(), 200);
        organ(h, KIDNEY, ProcessingModule.WASHING_KIDNEY.get(), 200);
        water(h, KIDNEY, 1000);
        organ(h, FURNACE, ProcessingModule.BIO_FURNACE.get(), 400);
        player.use(h, GIZZARD, stack(Items.RAW_COPPER, 1));
        player.use(h, KIDNEY, tailings(Metal.GOLD, 4));
        for (int n = 0; n < 4; n++) player.use(h, FURNACE, stack(MineralModule.IRON_DUST.get(), 1));
        h.runAtTickTime(GRIND_TICKS - 20, () -> {
            for (BlockPos organ : List.of(GIZZARD, KIDNEY, FURNACE))
                h.assertTrue(contents(h, organ).batch().isPresent(), "a batch runs at " + organ);
            h.assertFalse(credits.holds(player.getUUID(), id("mineral_gizzard")), "the grind has not finished");
        });
        h.runAtTickTime(GRIND_TICKS + 12, () -> {
            h.assertTrue(
                    credits.holds(player.getUUID(), id("mineral_gizzard"), recipe("grinding/copper")),
                    "the Gizzard's grind of copper waits for its feeder");
            h.assertTrue(
                    credits.holds(player.getUUID(), id("washing_kidney"), recipe("washing/gold")),
                    "so does the Kidney's wash of gold");
            h.assertFalse(credits.holds(player.getUUID(), id("bio_furnace")), "the furnace is still smelting");
            h.assertFalse(credits.holds(bystander.getUUID(), id("mineral_gizzard")), "the bystander has none");
        });
        h.runAtTickTime(SMELT_TICKS + 12, () -> {
            h.assertTrue(
                    credits.holds(player.getUUID(), id("bio_furnace"), recipe("dust_smelting/iron")),
                    "the furnace's dust waits too");
            for (String path : List.of(GROUND, WASHED, INGOT)) h.assertFalse(done(h, player, path), path + " not yet");
            // the credits survive a save of the world's data and fire from the copy that is read back
            var registries = h.getLevel().registryAccess();
            PendingCredits reloaded = PendingCredits.load(credits.save(new CompoundTag(), registries), registries);
            reloaded.award(bystander);
            for (String path : List.of(GROUND, WASHED, INGOT))
                h.assertFalse(done(h, bystander, path), path + " is not the bystander's");
            reloaded.award(player);
            for (String path : List.of(GROUND, WASHED, INGOT)) h.assertTrue(done(h, player, path), path + " earned");
            h.assertFalse(done(h, player, TREE), "the storage branch is not touched");
            CriteriaTriggers.TICK.trigger(player);
            h.assertTrue(done(h, player, GOAL), "all three make the goal");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_advancements",
            timeoutTicks = 60)
    public static void everyMetalEarnsItsStepFromItsOwnRecipe(GameTestHelper h) {
        PendingCredits credits = PendingCredits.get(h.getLevel());
        List<Worker> players = new ArrayList<>();
        int x = 1;
        for (Metal metal : Metal.values()) {
            Worker who = new Worker(h, "metal_" + metal.id());
            players.add(who);
            BlockPos grind = organ(h, new BlockPos(x, 2, 1), ProcessingModule.MINERAL_GIZZARD.get(), 0);
            BlockPos wash = organ(h, new BlockPos(x, 2, 3), ProcessingModule.WASHING_KIDNEY.get(), 0);
            BlockPos smelt = organ(h, new BlockPos(x, 2, 5), ProcessingModule.BIO_FURNACE.get(), 0);
            ItemStack dust = new ItemStack(MineralModule.dust(metal), 1);
            finishing(
                    h,
                    grind,
                    1000,
                    trays(3),
                    "infestusfrontier:grinding/" + metal.id(),
                    dust.copyWithCount(4),
                    0,
                    who.getUUID());
            finishing(h, wash, 1000, trays(3), "infestusfrontier:washing/" + metal.id(), dust, 0, who.getUUID());
            finishing(
                    h,
                    smelt,
                    2000,
                    trays(1),
                    "infestusfrontier:dust_smelting/" + metal.id(),
                    new ItemStack(Items.IRON_INGOT),
                    0,
                    who.getUUID());
            x += 2;
        }
        h.runAtTickTime(30, () -> {
            for (int n = 0; n < players.size(); n++) {
                Worker who = players.get(n);
                String metal = Metal.values()[n].id();
                h.assertTrue(
                        credits.holds(who.getUUID(), id("mineral_gizzard"), recipe("grinding/" + metal)),
                        metal + " grinding credited");
                h.assertTrue(
                        credits.holds(who.getUUID(), id("washing_kidney"), recipe("washing/" + metal)),
                        metal + " washing credited");
                h.assertTrue(
                        credits.holds(who.getUUID(), id("bio_furnace"), recipe("dust_smelting/" + metal)),
                        metal + " dust smelting credited");
                credits.award(who);
                for (String path : List.of(GROUND, WASHED, INGOT))
                    h.assertTrue(done(h, who, path), metal + " earns " + path);
            }
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_advancements",
            timeoutTicks = 60)
    public static void settlingRawSmeltingAndAnUntaggedCreditEarnNoOreStep(GameTestHelper h) {
        Worker player = new Worker(h, "other_batches");
        Worker legacy = new Worker(h, "legacy");
        PendingCredits credits = PendingCredits.get(h.getLevel());
        organ(h, KIDNEY, ProcessingModule.WASHING_KIDNEY.get(), 0);
        organ(h, FURNACE, ProcessingModule.BIO_FURNACE.get(), 0);
        finishing(
                h,
                KIDNEY,
                1000,
                trays(3),
                "infestusfrontier:settling",
                new ItemStack(MineralModule.ROCK_RESIDUE.get()),
                2,
                player.getUUID());
        finishing(
                h,
                FURNACE,
                2000,
                trays(1),
                "minecraft:iron_ingot_from_smelting_raw_iron",
                new ItemStack(Items.IRON_INGOT),
                0,
                player.getUUID());
        for (String organ : List.of("mineral_gizzard", "washing_kidney", "bio_furnace"))
            credits.remember(legacy.getUUID(), id(organ));
        h.runAtTickTime(30, () -> {
            h.assertTrue(credits.holds(player.getUUID(), id("washing_kidney"), recipe("settling")), "settled");
            credits.award(player);
            credits.award(legacy);
            h.assertTrue(done(h, player, "contact/bio_furnace"), "raw smelting is still the furnace's old step");
            h.assertTrue(done(h, legacy, "contact/bio_furnace"), "a credit with no recipe still earns that");
            for (Worker who : List.of(player, legacy))
                for (String path : List.of(GROUND, WASHED, INGOT))
                    h.assertFalse(done(h, who, path), path + " needs the batch of its own recipe");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_advancements",
            timeoutTicks = 100)
    public static void unstartedAndBlockedBatchesEarnNothingUntilTheyDeliver(GameTestHelper h) {
        Worker player = new Worker(h, "blocked");
        PendingCredits credits = PendingCredits.get(h.getLevel());
        BlockPos waiting = new BlockPos(1, 2, 3);
        organ(h, GIZZARD, ProcessingModule.MINERAL_GIZZARD.get(), 0); // no biomass
        organ(h, KIDNEY, ProcessingModule.WASHING_KIDNEY.get(), 200); // no water
        organ(h, FURNACE, ProcessingModule.BIO_FURNACE.get(), 400); // three dust are not enough
        organ(h, waiting, ProcessingModule.MINERAL_GIZZARD.get(), 0);
        player.use(h, GIZZARD, stack(Items.RAW_IRON, 1));
        player.use(h, KIDNEY, tailings(Metal.IRON, 4));
        for (int n = 0; n < 3; n++) player.use(h, FURNACE, stack(MineralModule.IRON_DUST.get(), 1));
        // a finished grind whose dust tray is full waits for room
        finishing(
                h,
                waiting,
                1000,
                List.of(new ItemStack(MineralModule.IRON_DUST.get(), 64), ItemStack.EMPTY, ItemStack.EMPTY),
                "infestusfrontier:grinding/iron",
                new ItemStack(MineralModule.IRON_DUST.get(), 4),
                0,
                player.getUUID());
        h.runAtTickTime(40, () -> {
            for (BlockPos organ : List.of(GIZZARD, KIDNEY, FURNACE))
                h.assertTrue(contents(h, organ).batch().isEmpty(), "nothing starts at " + organ);
            h.assertTrue(contents(h, waiting).batch().isPresent(), "a finished batch waits for room in its tray");
            h.assertFalse(
                    credits.holds(player.getUUID(), id("mineral_gizzard")), "no biomass, no full tray, no credit");
            h.assertFalse(credits.holds(player.getUUID(), id("washing_kidney")), "no water, no credit");
            h.assertFalse(credits.holds(player.getUUID(), id("bio_furnace")), "three dust, no credit");
            fuel(h, GIZZARD, 200);
            water(h, KIDNEY, 500);
            player.use(h, FURNACE, stack(MineralModule.IRON_DUST.get(), 1));
        });
        h.runAtTickTime(50, () -> {
            for (BlockPos organ : List.of(GIZZARD, KIDNEY, FURNACE))
                h.assertTrue(contents(h, organ).batch().isPresent(), "the batch starts once supplied at " + organ);
            h.assertFalse(credits.holds(player.getUUID(), id("mineral_gizzard")), "started is not finished");
            ports(h, waiting).extractItem(1, 64, false); // the dust tray is port slot 1
            h.assertTrue(contents(h, waiting).batch().isPresent(), "emptying the tray is not a finish");
        });
        h.runAtTickTime(70, () -> {
            h.assertTrue(contents(h, waiting).batch().isEmpty(), "the waiting batch delivers once there is room");
            h.assertTrue(
                    credits.holds(player.getUUID(), id("mineral_gizzard"), recipe("grinding/iron")),
                    "and only then it counts");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_advancements",
            timeoutTicks = 100)
    public static void theKidneyAndTheFurnaceJudgeTheComponentsAndTheMetalOfWhatIsFed(GameTestHelper h) {
        Worker player = new Worker(h, "components");
        organ(h, KIDNEY, ProcessingModule.WASHING_KIDNEY.get(), 200);
        water(h, KIDNEY, 1000);
        organ(h, FURNACE, ProcessingModule.BIO_FURNACE.get(), 400);
        h.assertFalse(
                ports(h, KIDNEY).insertItem(0, tailings(Metal.IRON, 3), true).isEmpty(),
                "a washed Tailings of 3 units is refused");
        h.assertFalse(
                ports(h, KIDNEY)
                        .insertItem(0, new ItemStack(MineralModule.MINERAL_TAILINGS.get()), true)
                        .isEmpty(),
                "a Tailings with no parcel is refused");
        player.use(h, KIDNEY, tailings(Metal.COPPER, 4));
        for (int n = 0; n < 3; n++) player.use(h, FURNACE, stack(MineralModule.IRON_DUST.get(), 1));
        h.assertFalse(
                ports(h, FURNACE)
                        .insertItem(0, stack(MineralModule.GOLD_DUST.get(), 1), true)
                        .isEmpty(),
                "another metal's dust cannot share a batch with iron dust");
        h.runAtTickTime(10, () -> {
            h.assertValueEqual(
                    contents(h, KIDNEY).batch().orElseThrow().batch().recipe(),
                    "infestusfrontier:washing/copper",
                    "the copper Tailings runs the copper wash");
            h.assertTrue(contents(h, FURNACE).batch().isEmpty(), "three dust of iron wait");
            player.use(h, FURNACE, stack(MineralModule.IRON_DUST.get(), 1));
        });
        h.runAtTickTime(40, () -> {
            h.assertValueEqual(
                    contents(h, FURNACE).batch().orElseThrow().batch().recipe(),
                    "infestusfrontier:dust_smelting/iron",
                    "the fourth iron dust starts the iron smelt");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "workshop_advancements",
            timeoutTicks = 60)
    public static void eachGizzardCreditsThePlayerWhoLoadedItAndNobodyElse(GameTestHelper h) {
        Worker first = new Worker(h, "first_loader");
        Worker second = new Worker(h, "second_loader");
        Worker bystander = new Worker(h, "ore_watcher");
        PendingCredits credits = PendingCredits.get(h.getLevel());
        BlockPos other = new BlockPos(3, 2, 1);
        organ(h, GIZZARD, ProcessingModule.MINERAL_GIZZARD.get(), 0);
        organ(h, other, ProcessingModule.MINERAL_GIZZARD.get(), 0);
        ItemStack dust = new ItemStack(MineralModule.IRON_DUST.get(), 4);
        finishing(h, GIZZARD, 1000, trays(3), "infestusfrontier:grinding/iron", dust, 0, first.getUUID());
        finishing(h, other, 1000, trays(3), "infestusfrontier:grinding/iron", dust, 0, second.getUUID());
        h.runAtTickTime(30, () -> {
            credits.award(second);
            h.assertTrue(done(h, second, GROUND), "the player of the second Gizzard earns it");
            h.assertFalse(done(h, first, GROUND), "not yet the first Gizzard's player, who is away");
            credits.award(first);
            h.assertTrue(done(h, first, GROUND), "each in turn");
            credits.award(bystander);
            h.assertFalse(done(h, bystander, GROUND), "never the one who loaded neither");
            h.succeed();
        });
    }
}
