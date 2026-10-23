package org.jd.infestusfrontier.testmod;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.control.ControlModule;
import org.jd.infestusfrontier.control.SensorPolypBlock;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.MouthBlock;
import org.jd.infestusfrontier.logistics.MouthMenu;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.logistics.api.LogisticsCapabilities;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.ItemCapsuleBlock;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.CapsuleStock;
import org.jd.infestusfrontier.storage.api.ItemStore;

/**
 * Item Capsules: touching cells form one rectangular store whose handler reaches every compartment; a kind fills its
 * own compartments before it opens an empty one; an incomplete row waits outside with its own items; a broken cell
 * carries its compartments; saves keep counts past a stack and unreadable ones keep their bytes; a body with a cell
 * out of reach has no slots until it forms again; Mouths and the Sensor Polyp read the whole body; the screen moves a
 * stack at a time. Cells stand at y=2.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class ItemCapsuleTests {
    private static final int Y = 2;
    /** Ticks for a placed cell to look for its body, and a little more. */
    private static final int FORMED = 4;

    private ItemCapsuleTests() {}

    private static BlockPos cell(int x, int layer, int z) {
        return new BlockPos(x, Y + layer, z);
    }

    /** Cells over x0..x1, z0..z1, {@code layers} high, all facing north. */
    private static void body(GameTestHelper h, int x0, int x1, int z0, int z1, int layers) {
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++)
                for (int layer = 0; layer < layers; layer++)
                    h.setBlock(cell(x, layer, z), StorageModule.ITEM_CAPSULE.get());
    }

    private static IItemHandler items(GameTestHelper h, BlockPos pos) {
        return h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), Direction.UP);
    }

    private static ItemStore store(GameTestHelper h, BlockPos pos) {
        return (ItemStore) items(h, pos);
    }

    /** The compartments the cell would carry on its item. */
    private static List<ItemStack> carried(GameTestHelper h, BlockPos pos) {
        CapsuleStock stock = h.getBlockEntity(pos).collectComponents().get(StorageModule.CAPSULE_STOCK.get());
        return stock == null ? CapsuleStock.EMPTY.held() : stock.held();
    }

    private static boolean joined(GameTestHelper h, BlockPos pos, Direction side) {
        return h.getBlockState(pos).getValue(ItemCapsuleBlock.JOINED.get(side));
    }

    private static boolean alone(GameTestHelper h, BlockPos pos) {
        for (Direction side : Direction.values()) if (joined(h, pos, side)) return false;
        return true;
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    /** A cell's item carrying {@code held}, compartment by compartment. */
    private static ItemStack capsuleWith(ItemStack... held) {
        ItemStack item = new ItemStack(StorageModule.ITEM_CAPSULE_ITEM.get());
        item.set(StorageModule.CAPSULE_STOCK.get(), new CapsuleStock(List.of(held)));
        return item;
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aFullBoxFormsOneStoreWithEveryCellsCompartments(GameTestHelper h) {
        body(h, 1, 2, 1, 2, 1);
        h.runAfterDelay(FORMED, () -> {
            h.assertTrue(joined(h, cell(1, 0, 1), Direction.EAST), "the row joined");
            h.assertTrue(joined(h, cell(1, 0, 1), Direction.SOUTH), "the rows joined");
            h.assertFalse(joined(h, cell(1, 0, 1), Direction.WEST), "the outer face stays a wall");
            h.assertValueEqual(items(h, cell(2, 0, 2)).getSlots(), 16, "four cells, four compartments each");
            Item[] kinds = {Items.COBBLESTONE, Items.DIRT, Items.SAND, Items.GRAVEL, Items.BONE};
            for (Item kind : kinds)
                h.assertTrue(
                        store(h, cell(1, 0, 1)).insert(stack(kind, 10), false).isEmpty(), "took " + kind);
            h.assertTrue(carried(h, cell(1, 0, 1)).get(3).is(Items.GRAVEL), "the first cell's four compartments");
            h.assertTrue(
                    items(h, cell(2, 0, 2)).getStackInSlot(4).is(Items.BONE), "the fifth kind opens the next cell");
            h.assertValueEqual(store(h, cell(2, 0, 2)).count(stack(Items.BONE, 1)), 10, "every cell counts the body");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aKindFillsItsOwnCompartmentBeforeItOpensAnother(GameTestHelper h) {
        body(h, 1, 2, 1, 1, 1);
        h.runAfterDelay(FORMED, () -> {
            IItemHandler handler = items(h, cell(1, 0, 1));
            h.assertTrue(
                    store(h, cell(1, 0, 1))
                            .insert(stack(Items.COBBLESTONE, 1030), false)
                            .isEmpty(),
                    "all in");
            h.assertValueEqual(handler.getStackInSlot(0).getCount(), 1024, "a compartment holds sixteen stacks");
            h.assertValueEqual(handler.getStackInSlot(1).getCount(), 6, "the rest opened the next one");
            h.assertValueEqual(
                    handler.insertItem(4, stack(Items.COBBLESTONE, 8), false).getCount(),
                    8,
                    "an empty compartment stays shut while the kind's own has room");
            h.assertValueEqual(
                    handler.insertItem(1, stack(Items.COBBLESTONE, 8), false).getCount(), 0, "the kind's own takes it");
            h.assertValueEqual(
                    handler.insertItem(5, stack(Items.DIRT, 8), false).getCount(), 8, "a new kind waits its turn");
            h.assertValueEqual(
                    handler.insertItem(2, stack(Items.DIRT, 8), false).getCount(), 0, "the first empty one opens");
            h.assertValueEqual(handler.extractItem(0, 1000, false).getCount(), 64, "a stack at a time comes out");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCompartmentHoldsSixteenOfAnItemThatDoesNotStack(GameTestHelper h) {
        body(h, 1, 1, 1, 1, 1);
        h.runAfterDelay(FORMED, () -> {
            ItemStore store = store(h, cell(1, 0, 1));
            for (int i = 0; i < 20; i++) store.insert(stack(Items.IRON_SWORD, 1), false);
            h.assertValueEqual(items(h, cell(1, 0, 1)).getStackInSlot(0).getCount(), 16, "sixteen swords");
            h.assertValueEqual(items(h, cell(1, 0, 1)).getStackInSlot(1).getCount(), 4, "and four more");
            h.assertValueEqual(store.count(stack(Items.IRON_SWORD, 1)), 20, "all counted");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCapsuleTakesNoCapsuleThatHoldsItems(GameTestHelper h) {
        body(h, 1, 1, 1, 1, 1);
        h.runAfterDelay(FORMED, () -> {
            ItemStore store = store(h, cell(1, 0, 1));
            ItemStack full = capsuleWith(stack(Items.DIRT, 5));
            h.assertValueEqual(store.insert(full, false).getCount(), 1, "a filled capsule stays out");
            h.assertTrue(
                    store.insert(new ItemStack(StorageModule.ITEM_CAPSULE_ITEM.get()), false)
                            .isEmpty(),
                    "an empty one goes in");
            h.assertFalse(StorageModule.ITEM_CAPSULE_ITEM.get().canFitInsideContainerItems(), "and fits in no bundle");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void hoppersFillAndEmptyTheBody(GameTestHelper h) {
        body(h, 1, 1, 1, 1, 2);
        h.setBlock(cell(1, 2, 1), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        ((net.minecraft.world.level.block.entity.HopperBlockEntity) h.getBlockEntity(cell(1, 2, 1)))
                .setItem(0, stack(Items.BONE, 5));
        h.runAfterDelay(80, () -> {
            h.assertValueEqual(store(h, cell(1, 0, 1)).count(stack(Items.BONE, 1)), 5, "the hopper filled the body");
            h.setBlock(cell(1, -1, 1), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        });
        h.runAfterDelay(160, () -> {
            h.assertValueEqual(store(h, cell(1, 0, 1)).count(stack(Items.BONE, 1)), 0, "a hopper under it emptied it");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anIncompleteRowWaitsOutsideWithItsOwnItems(GameTestHelper h) {
        body(h, 1, 2, 1, 1, 1);
        h.runAfterDelay(FORMED, () -> {
            store(h, cell(1, 0, 1)).insert(stack(Items.DIRT, 30), false);
            h.setBlock(cell(1, 0, 2), StorageModule.ITEM_CAPSULE.get());
            store(h, cell(1, 0, 2)).insert(stack(Items.SAND, 7), false);
        });
        h.runAfterDelay(2 * FORMED, () -> {
            h.assertTrue(alone(h, cell(1, 0, 2)), "the start of a second row waits outside");
            h.assertValueEqual(items(h, cell(1, 0, 2)).getSlots(), 4, "a store of its own");
            h.assertValueEqual(store(h, cell(1, 0, 2)).count(stack(Items.SAND, 1)), 7, "with its own items");
            h.assertValueEqual(items(h, cell(1, 0, 1)).getSlots(), 8, "the body keeps its own");
            h.setBlock(cell(2, 0, 2), StorageModule.ITEM_CAPSULE.get());
        });
        h.runAfterDelay(3 * FORMED, () -> {
            h.assertTrue(joined(h, cell(1, 0, 2), Direction.NORTH), "the full row joined the body");
            h.assertValueEqual(items(h, cell(2, 0, 2)).getSlots(), 16, "one store");
            h.assertValueEqual(store(h, cell(2, 0, 2)).count(stack(Items.DIRT, 1)), 30, "nothing moved or lost");
            h.assertValueEqual(store(h, cell(2, 0, 2)).count(stack(Items.SAND, 1)), 7, "nothing moved or lost");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBrokenCellCarriesItsCompartmentsAndTheRestFormAgain(GameTestHelper h) {
        body(h, 1, 2, 1, 2, 1);
        h.runAfterDelay(FORMED, () -> {
            for (Item kind : new Item[] {Items.DIRT, Items.SAND, Items.GRAVEL, Items.CLAY_BALL, Items.BONE})
                store(h, cell(1, 0, 1)).insert(stack(kind, 100), false);
            // the bone went into the second cell in body order, (1, 0, 2)
            h.getLevel().destroyBlock(h.absolutePos(cell(1, 0, 2)), true);
        });
        h.runAfterDelay(2 * FORMED, () -> {
            List<ItemEntity> drops = h.getEntities(EntityType.ITEM, cell(1, 0, 2), 2.0);
            h.assertValueEqual(drops.size(), 1, "one dropped cell");
            ItemStack dropped = drops.get(0).getItem();
            CapsuleStock kept = dropped.get(StorageModule.CAPSULE_STOCK.get());
            h.assertTrue(
                    kept != null
                            && kept.held().get(0).is(Items.BONE)
                            && kept.held().get(0).getCount() == 100,
                    "its bones on the item");
            h.assertValueEqual(dropped.getMaxStackSize(), 1, "a filled cell does not stack");
            h.assertValueEqual(store(h, cell(1, 0, 1)).count(stack(Items.CLAY_BALL, 1)), 100, "the rest stayed");
            int bodies = 0;
            for (BlockPos pos : List.of(cell(1, 0, 1), cell(2, 0, 1), cell(2, 0, 2))) if (alone(h, pos)) bodies++;
            h.assertValueEqual(bodies, 1, "two cells formed a row again, one waits alone");
            Player builder = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.ITEM_CAPSULE_ITEM.get(), 1);
            builder.setItemInHand(InteractionHand.MAIN_HAND, dropped.copy());
            h.placeAt(builder, builder.getMainHandItem(), cell(1, -1, 2), Direction.UP);
        });
        h.runAfterDelay(3 * FORMED, () -> {
            h.assertValueEqual(items(h, cell(1, 0, 1)).getSlots(), 16, "the placed cell joined back");
            h.assertValueEqual(store(h, cell(1, 0, 1)).count(stack(Items.BONE, 1)), 100, "with its bones");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anEmptiedCellStacksAgain(GameTestHelper h) {
        ItemStack filled = capsuleWith(stack(Items.DIRT, 20));
        filled.set(DataComponents.MAX_STACK_SIZE, 1); // as a broken filled cell drops it
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.ITEM_CAPSULE_ITEM.get(), 1);
        builder.setItemInHand(InteractionHand.MAIN_HAND, filled);
        h.placeAt(builder, builder.getMainHandItem(), cell(1, -1, 1), Direction.UP);
        h.runAfterDelay(FORMED, () -> {
            h.assertValueEqual(items(h, cell(1, 0, 1)).extractItem(0, 64, false).getCount(), 20, "emptied");
            h.getLevel().destroyBlock(h.absolutePos(cell(1, 0, 1)), true);
            List<ItemEntity> drops = h.getEntities(EntityType.ITEM, cell(1, 0, 1), 2.0);
            h.assertValueEqual(drops.size(), 1, "one dropped cell");
            h.assertValueEqual(drops.get(0).getItem().getMaxStackSize(), 64, "an empty cell stacks like any block");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aFilledCapsuleItemThatBurnsSpillsItsItemsInStacks(GameTestHelper h) {
        BlockPos at = h.absolutePos(new BlockPos(2, 2, 2));
        ItemEntity burning = new ItemEntity(
                h.getLevel(),
                at.getX() + 0.5,
                at.getY(),
                at.getZ() + 0.5,
                capsuleWith(stack(Items.DIRT, 100), stack(Items.IRON_SWORD, 3)));
        h.getLevel().addFreshEntity(burning);
        burning.hurt(h.getLevel().damageSources().lava(), 100);
        List<ItemEntity> spilled = h.getEntities(EntityType.ITEM, new BlockPos(2, 2, 2), 2.0);
        int dirt = 0;
        int swords = 0;
        for (ItemEntity entity : spilled) {
            if (entity.getItem().is(Items.DIRT)) dirt += entity.getItem().getCount();
            if (entity.getItem().is(Items.IRON_SWORD))
                swords += entity.getItem().getCount();
            h.assertTrue(
                    entity.getItem().getCount() <= entity.getItem().getMaxStackSize(), "every spilled stack is whole");
        }
        h.assertTrue(burning.isRemoved(), "the capsule burned");
        h.assertValueEqual(dirt, 100, "all the dirt spilled");
        h.assertValueEqual(swords, 3, "and every sword");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theFrontFacesThePlayerWhoPlacesIt(GameTestHelper h) {
        h.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.ITEM_CAPSULE_ITEM.get(), 1);
        builder.setYRot(0); // looking south
        h.placeAt(builder, builder.getMainHandItem(), new BlockPos(1, 1, 1), Direction.UP);
        h.assertValueEqual(
                h.getBlockState(cell(1, 0, 1)).getValue(ItemCapsuleBlock.FACING),
                Direction.NORTH,
                "front to the player");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aSavedCellLoadsBackWithMoreThanAStackAndAnUnreadableOneKeepsItsBytes(GameTestHelper h) {
        body(h, 1, 2, 1, 1, 1);
        h.runAfterDelay(FORMED, () -> {
            store(h, cell(1, 0, 1)).insert(stack(Items.COBBLESTONE, 1000), false);
            BlockEntity west = h.getBlockEntity(cell(1, 0, 1));
            CompoundTag saved = west.saveWithFullMetadata(h.getLevel().registryAccess());
            h.assertTrue(saved.contains("body"), "the body is saved");
            west.loadWithComponents(saved, h.getLevel().registryAccess());
            h.assertValueEqual(store(h, cell(2, 0, 1)).count(stack(Items.COBBLESTONE, 1)), 1000, "it loads back whole");
            // a second entry for the same compartment cannot be read
            CompoundTag hostile = saved.copy();
            ListTag stock = hostile.getList("stock", 10);
            stock.add(stock.getCompound(0).copy());
            BlockEntity east = h.getBlockEntity(cell(2, 0, 1));
            east.loadWithComponents(hostile, h.getLevel().registryAccess());
            h.assertValueEqual(items(h, cell(2, 0, 1)).getSlots(), 0, "an unreadable cell has no slots");
            h.assertValueEqual(items(h, cell(1, 0, 1)).getSlots(), 0, "nor has the body it was part of");
            CompoundTag kept = east.collectComponents().get(OrganModule.UNREADABLE_SAVE.get());
            h.assertTrue(kept != null && kept.getList("stock", 10).size() == 2, "it keeps its bytes for the item");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCarriedCompartmentPastItsLimitIsKeptUnreadable(GameTestHelper h) {
        h.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.ITEM_CAPSULE_ITEM.get(), 1);
        builder.setItemInHand(InteractionHand.MAIN_HAND, capsuleWith(stack(Items.DIRT, 5000)));
        h.placeAt(builder, builder.getMainHandItem(), new BlockPos(1, 1, 1), Direction.UP);
        h.runAfterDelay(FORMED, () -> {
            h.assertValueEqual(items(h, cell(1, 0, 1)).getSlots(), 0, "it refuses");
            h.assertTrue(
                    h.getBlockEntity(cell(1, 0, 1)).collectComponents().get(OrganModule.UNREADABLE_SAVE.get()) != null,
                    "and keeps what it was given");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aMouthReadsTheWholeBodysCount(GameTestHelper h) {
        // 24 cells, 96 slots: a full compartment of bones sits in the last one, past the 64 slots a Mouth walks in a
        // store it does not know; counted that way the body holds no bones and the Mouth would open a new compartment
        body(h, 1, 4, 1, 3, 2);
        BlockPos last = cell(4, 1, 3);
        h.getBlockEntity(last)
                .applyComponentsFromItemStack(
                        capsuleWith(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, stack(Items.BONE, 1024)));
        BlockPos intake = cell(1, -1, 1);
        BlockPos output = cell(1, -1, 5);
        h.setBlock(intake, LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, MouthMode.INTAKE));
        for (int z = 2; z <= 4; z++) h.setBlock(cell(1, -1, z), LogisticsModule.ITEM_VEIN.get());
        h.setBlock(output, LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, MouthMode.OUTPUT));
        h.setBlock(output.above(), Blocks.CHEST);
        ((ChestBlockEntity) h.getBlockEntity(output.above())).setItem(0, stack(Items.BONE, 16));
        press(h, intake, MouthMenu.AMOUNT_UP, 2); // 0, 1, 4: fill the body up to 4 bones
        h.runAfterDelay(120, () -> {
            h.assertValueEqual(
                    store(h, cell(1, 0, 1)).count(stack(Items.BONE, 1)), 1024, "the body already held more than 4");
            h.assertValueEqual(
                    ((ChestBlockEntity) h.getBlockEntity(output.above()))
                            .getItem(0)
                            .getCount(),
                    16,
                    "none sent");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 100)
    public static void aSensorPolypReadsHowFullTheBodyIs(GameTestHelper h) {
        body(h, 1, 1, 1, 1, 1);
        h.setBlock(
                cell(2, 0, 1),
                ControlModule.SENSOR_POLYP.get().defaultBlockState().setValue(SensorPolypBlock.FACING, Direction.WEST));
        h.runAfterDelay(FORMED, () -> store(h, cell(1, 0, 1)).insert(stack(Items.COBBLESTONE, 2048), false));
        h.runAfterDelay(40, () -> {
            h.assertValueEqual(
                    h.getBlockState(cell(2, 0, 1)).getValue(SensorPolypBlock.POWER), 7, "half of 4096 reads 7");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theScreenMovesAStackAtATimeAndShiftClickFillsTheBody(GameTestHelper h) {
        body(h, 1, 2, 1, 1, 1);
        h.runAfterDelay(FORMED, () -> {
            store(h, cell(1, 0, 1)).insert(stack(Items.COBBLESTONE, 200), false);
            MenuPlayer player = new MenuPlayer(h.getLevel());
            BlockPos at = h.absolutePos(cell(1, 0, 1));
            player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
            player.openMenu((MenuProvider) h.getBlockEntity(cell(1, 0, 1)), at);
            AbstractContainerMenu menu = player.containerMenu;
            menu.clicked(0, 0, ClickType.PICKUP, player);
            h.assertValueEqual(menu.getCarried().getCount(), 64, "a click takes one stack");
            h.assertValueEqual(menu.getSlot(0).getItem().getCount(), 136, "and leaves the rest");
            menu.clicked(0, 0, ClickType.PICKUP, player);
            h.assertTrue(menu.getCarried().isEmpty(), "the stack goes back");
            h.assertValueEqual(menu.getSlot(0).getItem().getCount(), 200, "whole again");
            menu.clicked(0, 0, ClickType.SWAP, player);
            h.assertValueEqual(menu.getSlot(0).getItem().getCount(), 200, "a hotbar key moves nothing");
            menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
            h.assertTrue(menu.getSlot(0).getItem().isEmpty(), "shift-click empties it into the inventory");
            h.assertValueEqual(player.getInventory().countItem(Items.COBBLESTONE), 200, "all of it");
            int inventorySlot = 4 + 27; // the first hotbar slot after the compartments and the inventory rows
            player.getInventory().setItem(0, stack(Items.DIRT, 40));
            menu.clicked(inventorySlot, 0, ClickType.QUICK_MOVE, player);
            h.assertValueEqual(store(h, cell(2, 0, 1)).count(stack(Items.DIRT, 1)), 40, "shift-click fills the body");
            h.assertTrue(player.getInventory().getItem(0).isEmpty(), "from the hand");
            player.closeContainer();
            h.succeed();
        });
    }

    /** Diamonds in a stack: loose, or inside a bundle. */
    private static int diamonds(ItemStack stack) {
        if (stack.is(Items.DIAMOND)) return stack.getCount();
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        int total = 0;
        if (contents != null)
            for (ItemStack held : contents.items()) if (held.is(Items.DIAMOND)) total += held.getCount();
        return total;
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBundleInACompartmentNeitherMakesNorLosesItems(GameTestHelper h) {
        body(h, 1, 1, 1, 1, 1);
        h.runAfterDelay(FORMED, () -> {
            ItemStack bundle = new ItemStack(Items.BUNDLE);
            bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(stack(Items.DIAMOND, 32))));
            h.assertTrue(store(h, cell(1, 0, 1)).insert(bundle, false).isEmpty(), "the bundle went in");
            MenuPlayer player = new MenuPlayer(h.getLevel());
            BlockPos at = h.absolutePos(cell(1, 0, 1));
            player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
            player.openMenu((MenuProvider) h.getBlockEntity(cell(1, 0, 1)), at);
            AbstractContainerMenu menu = player.containerMenu;
            menu.setCarried(stack(Items.DIRT, 10));
            menu.clicked(0, 1, ClickType.PICKUP, player);
            h.assertValueEqual(menu.getCarried().getCount(), 10, "no dirt vanishes into the bundle");
            h.assertValueEqual(diamonds(items(h, cell(1, 0, 1)).getStackInSlot(0)), 32, "the bundle is unchanged");
            menu.setCarried(ItemStack.EMPTY);
            menu.clicked(0, 1, ClickType.PICKUP, player);
            int total = diamonds(menu.getCarried())
                    + diamonds(items(h, cell(1, 0, 1)).getStackInSlot(0));
            h.assertValueEqual(total, 32, "a right click makes no diamonds");
            player.closeContainer();
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theRecipeMakesACapsule(GameTestHelper h) {
        List<ItemStack> grid = new ArrayList<>(List.of(
                new ItemStack(Items.CHEST),
                new ItemStack(ProcessingModule.MEMBRANE_SHEET.get()),
                new ItemStack(ProcessingModule.MEMBRANE_SHEET.get()),
                new ItemStack(EcologyModule.SPORE_CULTURE.get())));
        while (grid.size() < 9) grid.add(ItemStack.EMPTY);
        CraftingInput input = CraftingInput.of(3, 3, grid);
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
        h.assertTrue(recipe.isPresent(), "the recipe matches");
        h.assertTrue(
                recipe.get()
                        .value()
                        .assemble(input, h.getLevel().registryAccess())
                        .is(StorageModule.ITEM_CAPSULE_ITEM.get()),
                "it makes a capsule");
        h.succeed();
    }

    /**
     * A body across a chunk edge has no slots while its far half is unloaded, and its near half keeps no stale store;
     * once the far half loads the body forms again with every item. Chunks load in rings, as in the Reservoir test.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "capsule_unload",
            timeoutTicks = 900)
    public static void aBodyAcrossAnUnloadedChunkHasNoSlotsUntilItLoads(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        BlockPos anchor = new BlockPos((origin.getX() & ~15) + 192000, 80, (origin.getZ() & ~15) + 224008);
        BlockPos west = anchor.east(46);
        ChunkPos kept = new ChunkPos(anchor);
        ChunkPos near = new ChunkPos(west);
        ChunkPos far = new ChunkPos(west.east(2));
        TestChunks.force(level, kept);
        TestChunks.force(level, near);
        TestChunks.force(level, far);
        List<BlockPos> cells = List.of(west, west.east(), west.east(2), west.east(3));
        for (BlockPos pos : cells)
            level.setBlockAndUpdate(pos, StorageModule.ITEM_CAPSULE.get().defaultBlockState());
        IItemHandler[] port = new IItemHandler[1];
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(
                        level.getBlockState(west).getValue(ItemCapsuleBlock.JOINED.get(Direction.EAST))
                                && level.getBlockState(west.east(2))
                                        .getValue(ItemCapsuleBlock.JOINED.get(Direction.EAST)),
                        "the row of four formed"))
                .thenExecute(() -> {
                    port[0] = level.getCapability(Capabilities.ItemHandler.BLOCK, west, Direction.UP);
                    h.assertValueEqual(port[0].getSlots(), 16, "one store");
                    for (Item kind : new Item[] {Items.DIRT, Items.SAND, Items.GRAVEL, Items.CLAY_BALL, Items.BONE})
                        ((ItemStore) port[0]).insert(stack(kind, 50), false);
                    level.setChunkForced(near.x, near.z, false);
                    level.setChunkForced(far.x, far.z, false);
                })
                .thenWaitUntil(() -> h.assertTrue(
                        level.hasChunkAt(west) && !level.hasChunkAt(west.east(2)),
                        "the near half stays loaded, the far half unloaded"))
                .thenExecute(() -> {
                    h.assertValueEqual(port[0].getSlots(), 0, "a body with a cell out of reach has no slots");
                    h.assertValueEqual(
                            port[0].insertItem(0, stack(Items.DIRT, 1), false).getCount(), 1, "and takes nothing");
                    TestChunks.force(level, near);
                    TestChunks.force(level, far);
                })
                .thenWaitUntil(() -> h.assertValueEqual(port[0].getSlots(), 16, "the body is whole again"))
                .thenExecute(() -> {
                    h.assertValueEqual(((ItemStore) port[0]).count(stack(Items.BONE, 1)), 50, "nothing lost");
                    for (BlockPos pos : cells) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    level.setChunkForced(kept.x, kept.z, false);
                    level.setChunkForced(near.x, near.z, false);
                    level.setChunkForced(far.x, far.z, false);
                })
                .thenSucceed();
    }

    /** Opens the Mouth's screen and presses {@code button} {@code times} times. */
    private static void press(GameTestHelper h, BlockPos mouth, int button, int times) {
        MenuPlayer player = new MenuPlayer(h.getLevel());
        BlockPos at = h.absolutePos(mouth);
        player.moveTo(at.getX() + 0.5, at.getY() + 1, at.getZ() - 1.5);
        player.openMenu(h.getLevel().getCapability(LogisticsCapabilities.MOUTH_SCREEN, at, null), at);
        for (int i = 0; i < times; i++) player.containerMenu.clickMenuButton(player, button);
        player.closeContainer();
    }

    /** A server player outside the player list that opens menus without a client, as in the Mouth tests. */
    private static final class MenuPlayer extends FakePlayer {
        MenuPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.fromString("7a1d2c3e-4b5f-4a6b-8c7d-9e0f1a2b3c4d"), "capsule_tests"));
        }

        @Override
        public OptionalInt openMenu(MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extra) {
            containerMenu = provider.createMenu(1, getInventory(), this);
            return OptionalInt.of(1);
        }
    }
}
