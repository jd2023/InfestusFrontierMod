package org.jd.infestusfrontier.testmod;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.processing.BatchOrganBlock;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.processing.api.PalmContents;
import org.jd.infestusfrontier.testmod.WorkshopAdvancementTests.Worker;

/**
 * An organ menu belongs to the block entity it was opened on. Breaking an organ and putting the same block back before
 * the viewer's next check leaves a block of the right type in reach, so only the shared menu can tell the old screen
 * from a new one. Clicks here go through the server's own packet handler, which asks the open menu whether it is still
 * valid before it acts, and the tests count one item across the player, the dropped organ and the replacement.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class OrganMenuLifecycleTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos BOWL = GROUND.above();
    private static final int SLOT_ZERO = 0;

    private OrganMenuLifecycleTests() {}

    // ---- helpers ----

    /** One click, as the client's packet reaches the server's handler, which checks the menu before it acts. */
    private static void click(Worker player, AbstractContainerMenu menu, int slot, ClickType type) {
        player.connection.handleContainerClick(new ServerboundContainerClickPacket(
                menu.containerId,
                menu.getStateId(),
                slot,
                0,
                type,
                menu.getCarried().copy(),
                new Int2ObjectOpenHashMap<>()));
    }

    private static void placeBowl(GameTestHelper h) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(BOWL, ProcessingModule.CULTURE_BOWL.get());
    }

    private static void load(GameTestHelper h, Item item, int count) {
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, item, count);
        for (int i = 0; i < count; i++) EcologyTests.use(h, player, BOWL, Direction.UP);
    }

    private static int count(List<ItemStack> stacks, Item item) {
        int total = 0;
        for (ItemStack stack : stacks) if (stack.is(item)) total += stack.getCount();
        return total;
    }

    private static int bowlHolds(GameTestHelper h, Item item) {
        BatchOrganContents contents =
                h.getBlockEntity(BOWL).collectComponents().get(ProcessingModule.BATCH_CONTENTS.get());
        return contents == null ? 0 : count(contents.ingredients(), item);
    }

    private static int palmHolds(GameTestHelper h, Item item) {
        return count(ShapingPalmTests.grid(h, ShapingPalmTests.AT).grid(), item);
    }

    /** What the player has: the inventory and the cursor. */
    private static int carried(Worker player, Item item) {
        return ShapingPalmTests.total(player, item);
    }

    /** What lies dropped near {@code at}: loose items and what a dropped organ carries inside. */
    private static int dropped(GameTestHelper h, BlockPos at, Item item) {
        int total = 0;
        for (ItemEntity drop : h.getEntities(EntityType.ITEM, at, 4.0)) {
            ItemStack stack = drop.getItem();
            if (stack.is(item)) total += stack.getCount();
            BatchOrganContents batch = stack.get(ProcessingModule.BATCH_CONTENTS.get());
            if (batch != null) total += count(batch.ingredients(), item);
            PalmContents palm = stack.get(ProcessingModule.PALM_CONTENTS.get());
            if (palm != null) total += count(palm.grid(), item);
        }
        return total;
    }

    /** Gives the viewer {@code inventory} of {@code item} in the first main slot and {@code cursor} on the cursor. */
    private static void give(Worker player, AbstractContainerMenu menu, Item item, int inventory, int cursor) {
        player.getInventory().items.set(9, new ItemStack(item, inventory));
        menu.setCarried(new ItemStack(item, cursor));
    }

    // ---- a batch organ ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBatchMenuOfABrokenAndReplacedBowlCannotMoveItems(GameTestHelper h) {
        placeBowl(h);
        load(h, Items.RED_MUSHROOM, 3);
        Worker viewer = new Worker(h, "old_bowl_viewer");
        AbstractContainerMenu old = viewer.open(h, BOWL);
        h.assertTrue(old.stillValid(viewer), "the screen is valid while its bowl stands");
        h.assertValueEqual(old.getSlot(SLOT_ZERO).getItem().getCount(), 3, "it shows the three mushrooms");
        give(viewer, old, Items.RED_MUSHROOM, 4, 2);
        int firstInventory = old.slots.size() - 36; // the player's slots follow the organ's

        h.getLevel().destroyBlock(h.absolutePos(BOWL), true);
        h.setBlock(BOWL, ProcessingModule.CULTURE_BOWL.get());
        h.assertBlockPresent(ProcessingModule.CULTURE_BOWL.get(), BOWL);
        h.assertFalse(old.stillValid(viewer), "a bowl of the same type is not the bowl the screen was opened on");
        h.assertValueEqual(dropped(h, BOWL, Items.RED_MUSHROOM), 3, "the broken bowl carries its mushrooms");

        click(viewer, old, SLOT_ZERO, ClickType.QUICK_MOVE);
        click(viewer, old, firstInventory, ClickType.QUICK_MOVE); // into the old handler
        click(viewer, old, SLOT_ZERO, ClickType.PICKUP); // the cursor's mushrooms onto the old slot
        click(viewer, old, SLOT_ZERO, ClickType.PICKUP);

        h.assertValueEqual(carried(viewer, Items.RED_MUSHROOM), 6, "the viewer keeps exactly what they brought");
        h.assertValueEqual(dropped(h, BOWL, Items.RED_MUSHROOM), 3, "the drop is not copied from or added to");
        h.assertValueEqual(bowlHolds(h, Items.RED_MUSHROOM), 0, "the replacement bowl stays empty");
        h.assertValueEqual(
                carried(viewer, Items.RED_MUSHROOM)
                        + dropped(h, BOWL, Items.RED_MUSHROOM)
                        + bowlHolds(h, Items.RED_MUSHROOM),
                9,
                "nine mushrooms in all: three in the bowl, six with the viewer");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aMenuOpenedOnTheReplacementBowlWorksAndTheOldOneStaysDead(GameTestHelper h) {
        placeBowl(h);
        load(h, Items.RED_MUSHROOM, 1);
        Worker viewer = new Worker(h, "old_viewer");
        AbstractContainerMenu old = viewer.open(h, BOWL);
        h.getLevel().destroyBlock(h.absolutePos(BOWL), true);
        h.setBlock(BOWL, ProcessingModule.CULTURE_BOWL.get());
        Worker next = new Worker(h, "new_viewer");
        AbstractContainerMenu fresh = next.open(h, BOWL);
        h.assertTrue(fresh.stillValid(next), "a screen opened on the replacement is valid");
        h.assertFalse(old.stillValid(viewer), "and the old one is still not");
        next.getInventory().items.set(9, new ItemStack(Items.RED_MUSHROOM, 5));
        click(next, fresh, fresh.slots.size() - 36, ClickType.QUICK_MOVE);
        h.assertValueEqual(bowlHolds(h, Items.RED_MUSHROOM), 5, "the new screen fills the replacement bowl");
        h.assertValueEqual(carried(next, Items.RED_MUSHROOM), 0, "from the viewer's hands");
        click(next, fresh, SLOT_ZERO, ClickType.QUICK_MOVE);
        h.assertValueEqual(bowlHolds(h, Items.RED_MUSHROOM), 0, "and takes them back out");
        h.assertValueEqual(carried(next, Items.RED_MUSHROOM), 5, "to the viewer");
        h.assertValueEqual(dropped(h, BOWL, Items.RED_MUSHROOM), 1, "the first bowl's mushroom was never touched");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBowlThatChangesStateKeepsItsOpenMenu(GameTestHelper h) {
        placeBowl(h);
        load(h, Items.RED_MUSHROOM, 2);
        Worker viewer = new Worker(h, "working_viewer");
        AbstractContainerMenu menu = viewer.open(h, BOWL);
        BlockEntity organ = h.getBlockEntity(BOWL);
        BlockState working = h.getBlockState(BOWL).setValue(BatchOrganBlock.WORKING, true);
        h.setBlock(BOWL, working);
        h.assertTrue(h.getBlockEntity(BOWL) == organ, "a state change keeps the same block entity");
        h.assertTrue(menu.stillValid(viewer), "so the open screen stays valid");
        click(viewer, menu, SLOT_ZERO, ClickType.QUICK_MOVE);
        h.assertValueEqual(carried(viewer, Items.RED_MUSHROOM), 2, "and still works");
        h.assertValueEqual(bowlHolds(h, Items.RED_MUSHROOM), 0, "on the organ it was opened on");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aMenuOfABowlThatWasOnlyRemovedIsInvalid(GameTestHelper h) {
        placeBowl(h);
        load(h, Items.RED_MUSHROOM, 2);
        Worker viewer = new Worker(h, "removed_viewer");
        AbstractContainerMenu old = viewer.open(h, BOWL);
        h.getLevel().destroyBlock(h.absolutePos(BOWL), true);
        h.assertFalse(old.stillValid(viewer), "a broken bowl's screen is invalid");
        click(viewer, old, SLOT_ZERO, ClickType.QUICK_MOVE);
        h.assertValueEqual(carried(viewer, Items.RED_MUSHROOM), 0, "and gives nothing");
        h.assertValueEqual(dropped(h, BOWL, Items.RED_MUSHROOM), 2, "of what the drop carries");
        h.succeed();
    }

    // ---- the Shaping Palm, a second user of the same rule ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aPalmMenuOfABrokenAndReplacedPalmCannotMoveItems(GameTestHelper h) {
        BlockPos at = ShapingPalmTests.palm(h);
        Worker viewer = new Worker(h, "old_palm_viewer");
        AbstractContainerMenu old = viewer.open(h, at);
        old.setCarried(new ItemStack(Items.OAK_PLANKS, 5));
        click(viewer, old, ShapingPalmTests.CELL, ClickType.PICKUP);
        h.assertValueEqual(palmHolds(h, Items.OAK_PLANKS), 5, "the planks are in the Palm's grid");
        give(viewer, old, Items.OAK_PLANKS, 4, 2);
        int firstInventory = old.slots.size() - 36;

        h.getLevel().destroyBlock(h.absolutePos(at), true);
        h.setBlock(at, ProcessingModule.SHAPING_PALM.get());
        h.assertFalse(old.stillValid(viewer), "a Palm of the same type is not the Palm the screen was opened on");
        h.assertValueEqual(dropped(h, at, Items.OAK_PLANKS), 5, "the broken Palm carries its planks");

        click(viewer, old, ShapingPalmTests.CELL, ClickType.QUICK_MOVE);
        click(viewer, old, firstInventory, ClickType.QUICK_MOVE);
        click(viewer, old, ShapingPalmTests.CELL + 1, ClickType.PICKUP);
        click(viewer, old, ShapingPalmTests.CELL + 1, ClickType.PICKUP);

        h.assertValueEqual(carried(viewer, Items.OAK_PLANKS), 6, "the viewer keeps exactly what they brought");
        h.assertValueEqual(dropped(h, at, Items.OAK_PLANKS), 5, "the drop is not copied from or added to");
        h.assertValueEqual(palmHolds(h, Items.OAK_PLANKS), 0, "the replacement Palm stays empty");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aMenuOpenedOnTheReplacementPalmWorks(GameTestHelper h) {
        BlockPos at = ShapingPalmTests.palm(h);
        Worker viewer = new Worker(h, "palm_old");
        AbstractContainerMenu old = viewer.open(h, at);
        h.getLevel().destroyBlock(h.absolutePos(at), true);
        h.setBlock(at, ProcessingModule.SHAPING_PALM.get());
        Worker next = new Worker(h, "palm_new");
        AbstractContainerMenu fresh = next.open(h, at);
        h.assertTrue(fresh.stillValid(next), "a screen on the replacement is valid");
        h.assertFalse(old.stillValid(viewer), "the old one is not");
        next.getInventory().items.set(9, new ItemStack(Items.COBBLESTONE, 7));
        click(next, fresh, fresh.slots.size() - 36, ClickType.QUICK_MOVE);
        h.assertValueEqual(palmHolds(h, Items.COBBLESTONE), 7, "the new screen fills the replacement");
        click(next, fresh, ShapingPalmTests.CELL, ClickType.QUICK_MOVE);
        h.assertValueEqual(palmHolds(h, Items.COBBLESTONE), 0, "and empties it");
        h.assertValueEqual(carried(next, Items.COBBLESTONE), 7, "back to the viewer");
        h.succeed();
    }

    // ---- a chunk that goes away ----

    /**
     * A bowl in a far chunk whose chunk is unloaded: the screen is invalid, and asking does not load the chunk again.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "organ_menu_unload",
            timeoutTicks = 900)
    public static void aMenuOfAnUnloadedChunkIsInvalidWithoutLoadingIt(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        BlockPos far = new BlockPos((origin.getX() & ~15) + 192000, 80, (origin.getZ() & ~15) + 224000);
        ChunkPos chunk = new ChunkPos(far);
        TestChunks.force(level, chunk);
        level.setBlockAndUpdate(
                far.below(), EcologyModule.LIVING_SUBSTRATE.get().defaultBlockState());
        level.setBlockAndUpdate(far, ProcessingModule.CULTURE_BOWL.get().defaultBlockState());
        Worker viewer = new Worker(h, "unload_viewer");
        AbstractContainerMenu menu = viewer.openAbsolute(far);
        h.assertTrue(menu.stillValid(viewer), "valid while its chunk is loaded");
        h.startSequence()
                .thenExecute(() -> level.setChunkForced(chunk.x, chunk.z, false))
                .thenWaitUntil(() -> h.assertFalse(level.hasChunkAt(far), "the bowl's chunk goes"))
                .thenExecute(() -> {
                    h.assertFalse(menu.stillValid(viewer), "an unloaded bowl's screen is invalid");
                    h.assertFalse(level.hasChunkAt(far), "and asking did not load the chunk");
                })
                .thenExecute(() -> {
                    TestChunks.force(level, chunk);
                    h.assertTrue(level.getBlockState(far).is(ProcessingModule.CULTURE_BOWL.get()), "the bowl waited");
                    level.setBlockAndUpdate(far, Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(far.below(), Blocks.AIR.defaultBlockState());
                    level.setChunkForced(chunk.x, chunk.z, false);
                })
                .thenSucceed();
    }
}
