package org.jd.infestusfrontier.testmod;

import com.mojang.authlib.GameProfile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.mineral.Metal;
import org.jd.infestusfrontier.core.mineral.Parcel;
import org.jd.infestusfrontier.mineral.MineralModule;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.ShapingPalmMenu;
import org.jd.infestusfrontier.processing.api.PalmContents;

/**
 * The Shaping Palm, stage 1: a crafting grid that stays in the organ. Every craft here goes through the menu a player
 * would use, so the engine's own result slot, recipe lookup and remainder rule run. The tests count items: a craft
 * takes one of each filled cell and gives the recipe's result and remainders once; a full inventory, another viewer
 * who got there first, a client's claim, a save, a chunk leaving and a break never make or lose an item. The Palm
 * stands at (2, 2, 2) on substrate.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class ShapingPalmTests {
    private static final int Y = 1;
    private static final int RESULT = ShapingPalmMenu.RESULT;
    private static final int CELL = ShapingPalmMenu.FIRST_CELL;
    private static final BlockPos AT = new BlockPos(2, Y + 1, 2);

    private ShapingPalmTests() {}

    // ---- helpers ----

    private static BlockPos palm(GameTestHelper h) {
        h.setBlock(AT.below(), EcologyTests.mature());
        h.setBlock(AT, ProcessingModule.SHAPING_PALM.get());
        return AT;
    }

    private static MenuPlayer open(GameTestHelper h, BlockPos at, String name) {
        MenuPlayer player = new MenuPlayer(h.getLevel(), name);
        BlockPos absolute = h.absolutePos(at);
        player.moveTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() - 1.5);
        player.openMenu((MenuProvider) h.getBlockEntity(at), absolute);
        player.containerMenu.broadcastChanges(); // the first tick of an open screen looks the result up
        return player;
    }

    private static MenuPlayer open(GameTestHelper h, BlockPos at) {
        return open(h, at, "palm_tests");
    }

    /** Puts {@code stack} in grid cell {@code cell} (0 to 8) with the cursor, as a click on the well does. */
    private static void put(MenuPlayer player, int cell, ItemStack stack) {
        player.containerMenu.setCarried(stack);
        player.containerMenu.clicked(CELL + cell, 0, ClickType.PICKUP, player);
    }

    private static ItemStack in(MenuPlayer player, int cell) {
        return player.containerMenu.getSlot(CELL + cell).getItem();
    }

    private static ItemStack result(MenuPlayer player) {
        return player.containerMenu.getSlot(RESULT).getItem();
    }

    /** What the organ itself holds, read from its block entity, not from any menu. */
    private static PalmContents grid(GameTestHelper h, BlockPos at) {
        return h.getBlockEntity(at)
                .collectComponents()
                .getOrDefault(ProcessingModule.PALM_CONTENTS.get(), PalmContents.EMPTY);
    }

    private static ItemStack cell(GameTestHelper h, BlockPos at, int cell) {
        return grid(h, at).cell(cell);
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    private static int held(Player player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) if (stack.is(item)) count += stack.getCount();
        return count;
    }

    private static int total(Player player, Item item) {
        return held(player, item)
                + (player.containerMenu.getCarried().is(item)
                        ? player.containerMenu.getCarried().getCount()
                        : 0);
    }

    private static void fillInventory(Player player) {
        for (int i = 0; i < player.getInventory().items.size(); i++)
            player.getInventory().items.set(i, stack(Items.DIRT, 64));
    }

    private static void close(GameTestHelper h, MenuPlayer player) {
        player.closeContainer();
        h.assertTrue(player.containerMenu == player.inventoryMenu, "the screen closed");
    }

    private static void reload(GameTestHelper h, BlockPos at) {
        var registries = h.getLevel().registryAccess();
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            NbtIo.writeCompressed(h.getBlockEntity(at).saveWithFullMetadata(registries), bytes);
            CompoundTag read =
                    NbtIo.readCompressed(new ByteArrayInputStream(bytes.toByteArray()), NbtAccounter.unlimitedHeap());
            BlockPos absolute = h.absolutePos(at);
            BlockEntity fresh =
                    ((EntityBlock) h.getBlockState(at).getBlock()).newBlockEntity(absolute, h.getBlockState(at));
            fresh.loadWithComponents(read, registries);
            h.getLevel().removeBlockEntity(absolute);
            h.getLevel().setBlockEntity(fresh);
        } catch (IOException e) {
            h.fail("could not save and read the Palm: " + e.getMessage());
        }
    }

    private static List<ItemEntity> loose(GameTestHelper h, BlockPos at) {
        return h.getEntities(EntityType.ITEM, at, 4.0);
    }

    // ---- the grid is kept ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void closingTheScreenKeepsTheGridAndReopeningShowsIt(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer first = open(h, at);
        put(first, 0, stack(Items.OAK_PLANKS, 5));
        put(first, 4, stack(Items.STICK, 3));
        ItemStack named = stack(Items.WOODEN_PICKAXE, 1);
        named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("kept"));
        put(first, 8, named.copy());
        close(h, first);
        h.assertValueEqual(held(first, Items.OAK_PLANKS), 0, "nothing came back to the player's inventory");
        h.assertValueEqual(cell(h, at, 0).getCount(), 5, "the planks are in the organ");
        h.assertTrue(ItemStack.matches(cell(h, at, 8), named), "a stack with its name is kept whole");
        MenuPlayer second = open(h, at, "palm_second");
        h.assertValueEqual(in(second, 0).getCount(), 5, "reopened, the planks are in their well");
        h.assertTrue(in(second, 4).is(Items.STICK) && in(second, 4).getCount() == 3, "and the sticks");
        h.assertTrue(ItemStack.matches(in(second, 8), named), "and the named pickaxe");
        h.assertTrue(in(second, 1).isEmpty(), "an empty cell stays empty");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void shiftClickMovesStacksBetweenTheGridAndTheInventory(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = open(h, at);
        player.getInventory().items.set(0, stack(Items.COBBLESTONE, 20));
        player.containerMenu.clicked(player.containerMenu.slots.size() - 9, 0, ClickType.QUICK_MOVE, player);
        h.assertValueEqual(cell(h, at, 0).getCount(), 20, "a shift-click on the hotbar fills the first cell");
        player.containerMenu.clicked(CELL, 0, ClickType.QUICK_MOVE, player);
        h.assertTrue(grid(h, at).isEmpty(), "and a shift-click on the cell empties it");
        h.assertValueEqual(held(player, Items.COBBLESTONE), 20, "into the inventory, nothing lost");
        h.succeed();
    }

    // ---- crafting follows the table ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aShapedRecipeTakesOneOfEachCellAndGivesItsResultOnce(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = open(h, at);
        for (int cell = 0; cell < 3; cell++) put(player, cell, stack(Items.OAK_PLANKS, 4));
        put(player, 4, stack(Items.STICK, 3));
        put(player, 7, stack(Items.STICK, 3));
        h.assertTrue(result(player).is(Items.WOODEN_PICKAXE), "the pickaxe shows in the result well");
        player.containerMenu.clicked(RESULT, 0, ClickType.PICKUP, player);
        h.assertTrue(player.containerMenu.getCarried().is(Items.WOODEN_PICKAXE), "a click takes it");
        h.assertValueEqual(player.containerMenu.getCarried().getCount(), 1, "one pickaxe");
        for (int cell : List.of(0, 1, 2)) h.assertValueEqual(cell(h, at, cell).getCount(), 3, "one plank taken");
        for (int cell : List.of(4, 7)) h.assertValueEqual(cell(h, at, cell).getCount(), 2, "one stick taken");
        h.assertValueEqual(player.crafted(Items.WOODEN_PICKAXE), 1, "it counts as the player's craft");
        h.assertTrue(result(player).is(Items.WOODEN_PICKAXE), "the grid still makes one");
        h.assertTrue(loose(h, at).isEmpty(), "nothing fell on the ground");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aShapelessRecipeCraftsFromAnyArrangement(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = open(h, at);
        put(player, 8, stack(Items.WHEAT_SEEDS, 2));
        put(player, 0, stack(Items.ROTTEN_FLESH, 2));
        put(player, 5, stack(Items.RED_MUSHROOM, 2));
        h.assertTrue(
                result(player).is(org.jd.infestusfrontier.ecology.EcologyModule.SPORE_CULTURE.get()),
                "a Spore Culture");
        player.containerMenu.clicked(RESULT, 0, ClickType.PICKUP, player);
        h.assertValueEqual(player.containerMenu.getCarried().getCount(), 1, "one culture");
        h.assertValueEqual(cell(h, at, 8).getCount(), 1, "one seed taken");
        h.assertValueEqual(cell(h, at, 0).getCount(), 1, "one flesh taken");
        h.assertValueEqual(cell(h, at, 5).getCount(), 1, "one mushroom taken");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aReturnedContainerGoesBackToItsEmptiedCell(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = open(h, at);
        for (int cell = 0; cell < 3; cell++) put(player, cell, stack(Items.MILK_BUCKET, 1));
        put(player, 3, stack(Items.SUGAR, 2));
        put(player, 4, stack(Items.EGG, 2));
        put(player, 5, stack(Items.SUGAR, 2));
        for (int cell = 6; cell < 9; cell++) put(player, cell, stack(Items.WHEAT, 2));
        h.assertTrue(result(player).is(Items.CAKE), "a cake");
        player.containerMenu.clicked(RESULT, 0, ClickType.PICKUP, player);
        h.assertTrue(player.containerMenu.getCarried().is(Items.CAKE), "one cake in the cursor");
        for (int cell = 0; cell < 3; cell++) {
            h.assertTrue(
                    cell(h, at, cell).is(Items.BUCKET), "the milk bucket's remainder, a bucket, is in cell " + cell);
            h.assertValueEqual(cell(h, at, cell).getCount(), 1, "one bucket");
        }
        h.assertValueEqual(held(player, Items.BUCKET), 0, "no bucket was also handed to the player");
        h.assertValueEqual(cell(h, at, 3).getCount(), 1, "one sugar taken");
        h.assertValueEqual(cell(h, at, 4).getCount(), 1, "one egg taken");
        h.assertTrue(result(player).isEmpty(), "no milk is left, so there is no second cake");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aRemainderThatCannotJoinItsCellGoesToTheInventory(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.HONEY_BOTTLE, 5));
        h.assertTrue(result(player).is(Items.SUGAR), "sugar from a honey bottle");
        player.containerMenu.clicked(RESULT, 0, ClickType.PICKUP, player);
        h.assertValueEqual(player.containerMenu.getCarried().getCount(), 3, "three sugar");
        h.assertValueEqual(cell(h, at, 0).getCount(), 4, "one honey bottle taken");
        h.assertTrue(cell(h, at, 0).is(Items.HONEY_BOTTLE), "the cell still holds honey");
        h.assertValueEqual(held(player, Items.GLASS_BOTTLE), 1, "the glass bottle went to the inventory");
        h.assertTrue(loose(h, at).isEmpty(), "and none was dropped");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aComponentBearingInputIsKeptWholeAndCraftsNormally(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = open(h, at);
        ItemStack tailings = MineralModule.tailings(new Parcel(Metal.IRON, 4));
        tailings.setCount(3);
        put(player, 2, tailings.copy());
        reload(h, at);
        h.assertTrue(ItemStack.matches(cell(h, at, 2), tailings), "the parcel component survives a save");
        MenuPlayer again = open(h, at, "palm_again");
        h.assertTrue(result(again).is(MineralModule.ROCK_RESIDUE.get()), "Tailings craft into Rock Residue");
        again.containerMenu.clicked(RESULT, 0, ClickType.PICKUP, again);
        h.assertValueEqual(again.containerMenu.getCarried().getCount(), 1, "one residue");
        h.assertValueEqual(cell(h, at, 2).getCount(), 2, "one Tailings taken");
        h.assertTrue(
                cell(h, at, 2).get(MineralModule.PARCEL.get()).equals(new Parcel(Metal.IRON, 4)),
                "the rest keeps its parcel");

        // a special recipe reads its inputs' components: a shield takes a banner's colour
        BlockPos second = new BlockPos(5, Y + 1, 2);
        h.setBlock(second.below(), EcologyTests.mature());
        h.setBlock(second, ProcessingModule.SHAPING_PALM.get());
        MenuPlayer shield = open(h, second, "palm_shield");
        put(shield, 0, stack(Items.SHIELD, 1));
        put(shield, 1, stack(Items.RED_BANNER, 1));
        h.assertTrue(result(shield).is(Items.SHIELD), "a decorated shield");
        h.assertTrue(DyeColor.RED == result(shield).get(DataComponents.BASE_COLOR), "in the banner's colour");
        shield.containerMenu.clicked(RESULT, 0, ClickType.PICKUP, shield);
        h.assertTrue(
                DyeColor.RED == shield.containerMenu.getCarried().get(DataComponents.BASE_COLOR),
                "taken with its component");
        h.assertTrue(cell(h, second, 0).isEmpty() && cell(h, second, 1).isEmpty(), "both inputs were used up");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void thePalmItselfCraftsFromItsPartsAtAnyTable(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager();
        CraftingInput palmParts = CraftingInput.of(
                3,
                2,
                List.of(
                        stack(OrganModule.ORGAN_BUD_ITEM.get(), 1),
                        stack(Items.CRAFTING_TABLE, 1),
                        stack(ProcessingModule.MEMBRANE_SHEET.get(), 1),
                        stack(ProcessingModule.MEMBRANE_SHEET.get(), 1),
                        stack(ProcessingModule.BONE_PLATE.get(), 1),
                        ItemStack.EMPTY));
        var made = recipes.getRecipeFor(RecipeType.CRAFTING, palmParts, h.getLevel());
        h.assertTrue(made.isPresent(), "an Organ Bud, a crafting table, two Membrane Sheets and a Bone Plate craft");
        h.assertTrue(
                made.get()
                        .value()
                        .assemble(palmParts, h.getLevel().registryAccess())
                        .is(ProcessingModule.SHAPING_PALM_ITEM.get()),
                "a Shaping Palm");
        h.succeed();
    }

    // ---- full inventories and refusals ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aFullInventoryRefusesAShiftCraftAndChangesNothing(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = open(h, at);
        for (int cell = 0; cell < 3; cell++) put(player, cell, stack(Items.OAK_PLANKS, 4));
        put(player, 4, stack(Items.STICK, 3));
        put(player, 7, stack(Items.STICK, 3));
        fillInventory(player);
        PalmContents before = grid(h, at);
        for (int i = 0; i < 3000; i++) player.containerMenu.clicked(RESULT, 0, ClickType.QUICK_MOVE, player);
        h.assertValueEqual(grid(h, at), before, "3000 refused shift-clicks changed nothing in the grid");
        h.assertValueEqual(held(player, Items.WOODEN_PICKAXE), 0, "and gave no pickaxe");
        h.assertValueEqual(held(player, Items.DIRT), 36 * 64, "the inventory is as it was");
        h.assertTrue(loose(h, at).isEmpty(), "and nothing was dropped");
        player.containerMenu.clicked(RESULT, 0, ClickType.PICKUP, player);
        h.assertTrue(
                player.containerMenu.getCarried().is(Items.WOODEN_PICKAXE),
                "a click that takes it into the cursor still works, the result needs no slot");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aRemainderWithNowhereToGoRefusesTheCraftUnchanged(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.HONEY_BOTTLE, 5));
        fillInventory(player);
        PalmContents before = grid(h, at);
        player.containerMenu.clicked(RESULT, 0, ClickType.PICKUP, player);
        h.assertTrue(player.containerMenu.getCarried().isEmpty(), "nothing was taken");
        h.assertValueEqual(grid(h, at), before, "the honey is still all there");
        h.assertValueEqual(held(player, Items.GLASS_BOTTLE), 0, "no bottle appeared");
        h.assertTrue(loose(h, at).isEmpty(), "and none was dropped, as a crafting table would");
        player.getInventory().items.set(0, ItemStack.EMPTY);
        player.containerMenu.clicked(RESULT, 0, ClickType.PICKUP, player);
        h.assertValueEqual(
                player.containerMenu.getCarried().getCount(), 3, "with one free slot the craft goes through");
        h.assertValueEqual(held(player, Items.GLASS_BOTTLE), 1, "and the bottle has its slot");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aShiftCraftMakesAsManyAsTheGridHoldsAndStopsWhereTheInventoryEnds(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.OAK_PLANKS, 20));
        put(player, 3, stack(Items.OAK_PLANKS, 20));
        fillInventory(player);
        player.getInventory().items.set(5, ItemStack.EMPTY);
        player.containerMenu.clicked(RESULT, 0, ClickType.QUICK_MOVE, player);
        h.assertValueEqual(held(player, Items.STICK), 64, "one free slot holds 16 crafts of four sticks");
        h.assertValueEqual(cell(h, at, 0).getCount(), 4, "20 planks less 16 crafts");
        h.assertValueEqual(cell(h, at, 3).getCount(), 4, "in both cells");
        h.assertTrue(loose(h, at).isEmpty(), "nothing was dropped");
        player.getInventory().items.set(6, ItemStack.EMPTY);
        player.containerMenu.clicked(RESULT, 0, ClickType.QUICK_MOVE, player);
        h.assertValueEqual(held(player, Items.STICK), 80, "with room, the last four crafts follow");
        h.assertTrue(grid(h, at).isEmpty(), "the grid is used up exactly");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aPalmWithoutSubstrateKeepsItsGridAndCraftsNothing(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.OAK_PLANKS, 2));
        put(player, 3, stack(Items.OAK_PLANKS, 2));
        h.setBlock(at.below(), Blocks.STONE);
        PalmContents before = grid(h, at);
        player.containerMenu.clicked(RESULT, 0, ClickType.PICKUP, player);
        h.assertTrue(player.containerMenu.getCarried().isEmpty(), "nothing was crafted");
        h.assertValueEqual(grid(h, at), before, "and the grid is unchanged");
        put(player, 1, stack(Items.OAK_PLANKS, 1));
        h.assertValueEqual(cell(h, at, 1).getCount(), 1, "the grid can still be filled");
        player.containerMenu.clicked(CELL + 1, 0, ClickType.PICKUP, player);
        h.assertTrue(cell(h, at, 1).isEmpty(), "and emptied");
        player.containerMenu.setCarried(ItemStack.EMPTY);
        h.setBlock(at.below(), EcologyTests.mature());
        player.containerMenu.clicked(RESULT, 0, ClickType.PICKUP, player);
        h.assertTrue(player.containerMenu.getCarried().is(Items.STICK), "with its foundation back it crafts");
        h.succeed();
    }

    // ---- several viewers and the client ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aSecondViewerCannotTakeACraftTheFirstAlreadyTook(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer first = open(h, at, "palm_first");
        put(first, 0, stack(Items.OAK_PLANKS, 1));
        put(first, 3, stack(Items.OAK_PLANKS, 1));
        MenuPlayer second = open(h, at, "palm_second");
        second.containerMenu.broadcastChanges();
        h.assertTrue(result(second).is(Items.STICK), "the second viewer sees the same craft");
        first.containerMenu.clicked(RESULT, 0, ClickType.PICKUP, first);
        h.assertValueEqual(first.containerMenu.getCarried().getCount(), 4, "the first takes four sticks");
        h.assertTrue(grid(h, at).isEmpty(), "and uses the grid up");
        h.assertTrue(result(second).is(Items.STICK), "the second viewer's well has not been refreshed yet");
        second.containerMenu.clicked(RESULT, 0, ClickType.PICKUP, second);
        h.assertTrue(second.containerMenu.getCarried().isEmpty(), "its click gets nothing");
        h.assertValueEqual(total(first, Items.STICK) + total(second, Items.STICK), 4, "four sticks in all");
        second.containerMenu.broadcastChanges();
        h.assertTrue(result(second).isEmpty(), "and its well is empty now");
        h.assertTrue(in(second, 0).isEmpty(), "its cells follow the organ");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void twoViewersCraftTurnAboutAndEveryItemIsCountedOnce(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer first = open(h, at, "palm_first");
        MenuPlayer second = open(h, at, "palm_second");
        put(first, 0, stack(Items.OAK_PLANKS, 4));
        put(first, 3, stack(Items.OAK_PLANKS, 4));
        for (int i = 0; i < 5; i++) {
            MenuPlayer taker = i % 2 == 0 ? first : second;
            taker.containerMenu.clicked(RESULT, 0, ClickType.PICKUP, taker);
            taker.getInventory().add(taker.containerMenu.getCarried().copy());
            taker.containerMenu.setCarried(ItemStack.EMPTY);
        }
        h.assertValueEqual(held(first, Items.STICK), 8, "the first viewer made two crafts of four sticks");
        h.assertValueEqual(held(second, Items.STICK), 8, "so did the second, from a well it never refreshed");
        h.assertTrue(grid(h, at).isEmpty(), "four planks from each cell, none left over and none minted");
        h.assertValueEqual(total(first, Items.OAK_PLANKS) + total(second, Items.OAK_PLANKS), 0, "no plank came out");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aViewerSeesTheOtherViewersChangesToTheGrid(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer first = open(h, at, "palm_first");
        MenuPlayer second = open(h, at, "palm_second");
        put(first, 0, stack(Items.OAK_PLANKS, 2));
        put(first, 3, stack(Items.OAK_PLANKS, 2));
        second.containerMenu.broadcastChanges();
        h.assertTrue(in(second, 0).is(Items.OAK_PLANKS), "the second viewer's well shows the planks");
        h.assertTrue(result(second).is(Items.STICK), "and its result well the sticks they make");
        second.containerMenu.clicked(CELL + 3, 0, ClickType.QUICK_MOVE, second);
        h.assertValueEqual(held(second, Items.OAK_PLANKS), 2, "the second viewer took the planks of one cell");
        first.containerMenu.broadcastChanges();
        h.assertTrue(in(first, 3).isEmpty(), "the first viewer's well follows");
        h.assertTrue(result(first).is(Items.OAK_BUTTON), "and so does its result: one plank makes a button");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aClientCannotMintAResultOrTakeInputsTwice(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = open(h, at);
        player.containerMenu.clicked(RESULT, 0, ClickType.PICKUP, player);
        h.assertTrue(player.containerMenu.getCarried().isEmpty(), "a click on an empty result well gives nothing");
        player.containerMenu.setCarried(stack(Items.DIAMOND, 3));
        player.containerMenu.clicked(RESULT, 0, ClickType.PICKUP, player);
        h.assertValueEqual(player.containerMenu.getCarried().getCount(), 3, "nothing can be put into the result well");
        h.assertTrue(grid(h, at).isEmpty(), "and the grid took nothing");
        player.containerMenu.setCarried(ItemStack.EMPTY);
        player.containerMenu.clicked(RESULT, 0, ClickType.QUICK_MOVE, player);
        player.containerMenu.clicked(RESULT, 1, ClickType.PICKUP, player);
        player.containerMenu.clicked(RESULT, 0, ClickType.THROW, player);
        player.containerMenu.clicked(RESULT, 0, ClickType.SWAP, player);
        h.assertTrue(
                player.getInventory().isEmpty()
                        && player.containerMenu.getCarried().isEmpty(),
                "no way takes a result that is not there");

        put(player, 0, stack(Items.OAK_PLANKS, 1));
        put(player, 3, stack(Items.OAK_PLANKS, 1));
        player.containerMenu.clicked(RESULT, 0, ClickType.PICKUP, player);
        ItemStack sticks = player.containerMenu.getCarried();
        h.assertValueEqual(sticks.getCount(), 4, "one craft, four sticks");
        player.containerMenu.setCarried(ItemStack.EMPTY);
        player.getInventory().add(sticks.copy());
        for (int i = 0; i < 20; i++)
            for (ClickType type : List.of(ClickType.PICKUP, ClickType.QUICK_MOVE, ClickType.SWAP, ClickType.THROW))
                player.containerMenu.clicked(RESULT, 0, type, player);
        h.assertValueEqual(total(player, Items.STICK), 4, "a used-up grid makes no more, however it is clicked");
        h.assertTrue(grid(h, at).isEmpty(), "and the grid took nothing twice");
        h.assertTrue(loose(h, at).isEmpty(), "nor does a throw");
        h.succeed();
    }

    // ---- saves, chunks and breaking ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aSaveAndReadKeepsEveryStackOnce(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = open(h, at);
        ItemStack damaged = stack(Items.IRON_PICKAXE, 1);
        damaged.set(DataComponents.DAMAGE, 7);
        put(player, 1, damaged.copy());
        put(player, 5, stack(Items.OAK_PLANKS, 64));
        put(player, 7, stack(Items.HONEY_BOTTLE, 16));
        player.closeContainer();
        PalmContents before = grid(h, at);
        reload(h, at);
        h.assertValueEqual(grid(h, at), before, "saved and read back, every stack and component is the same");
        reload(h, at);
        h.assertValueEqual(grid(h, at), before, "a second round trip changes nothing");
        h.assertValueEqual(cell(h, at, 5).getCount(), 64, "a full stack is one stack");
        h.assertTrue(ItemStack.matches(cell(h, at, 1), damaged), "with its damage");
        h.succeed();
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "palm_unload",
            timeoutTicks = 1200)
    public static void aFilledGridInAChunkThatUnloadsComesBackFromTheDiskOnce(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        BlockPos at = new BlockPos((origin.getX() & ~15) + 248002, 80, (origin.getZ() & ~15) + 248008);
        ChunkPos chunk = new ChunkPos(at);
        TestChunks.force(level, chunk);
        level.setBlockAndUpdate(at.below(), EcologyTests.mature());
        level.setBlockAndUpdate(at, ProcessingModule.SHAPING_PALM.get().defaultBlockState());
        ItemStack tailings = MineralModule.tailings(new Parcel(Metal.GOLD, 4));
        tailings.setCount(5);
        MenuPlayer player = new MenuPlayer(level, "palm_chunk");
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        player.openMenu((MenuProvider) level.getBlockEntity(at), at);
        put(player, 0, stack(Items.MILK_BUCKET, 1));
        put(player, 4, tailings.copy());
        put(player, 8, stack(Items.HONEY_BOTTLE, 9));
        player.closeContainer();
        PalmContents[] before = new PalmContents[1];
        h.startSequence()
                .thenExecute(() -> {
                    before[0] = level.getBlockEntity(at).collectComponents().get(ProcessingModule.PALM_CONTENTS.get());
                    h.assertTrue(before[0] != null && !before[0].isEmpty(), "the Palm holds its grid");
                    level.setChunkForced(chunk.x, chunk.z, false);
                })
                .thenWaitUntil(() -> h.assertFalse(level.hasChunkAt(at), "the chunk unloaded"))
                .thenExecute(() -> TestChunks.force(level, chunk))
                .thenWaitUntil(() -> h.assertTrue(level.hasChunkAt(at), "the chunk is back"))
                .thenExecute(() -> {
                    PalmContents back =
                            level.getBlockEntity(at).collectComponents().get(ProcessingModule.PALM_CONTENTS.get());
                    h.assertTrue(back != null, "the grid came back from the disk");
                    h.assertValueEqual(back, before[0], "every stack is the one that was saved, and only one");
                    h.assertTrue(
                            level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(at).inflate(6))
                                    .isEmpty(),
                            "nothing was spilled by the unload");
                    level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                    level.setChunkForced(chunk.x, chunk.z, false);
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void breakingAPalmGivesItsGridOnceAndPlacingItGivesItBack(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.MILK_BUCKET, 1));
        put(player, 2, stack(Items.SUGAR, 12));
        ItemStack enchanted = stack(Items.DIAMOND_PICKAXE, 1);
        enchanted.enchant(h.getLevel().registryAccess().holderOrThrow(Enchantments.EFFICIENCY), 3);
        put(player, 6, enchanted.copy());
        player.closeContainer();
        PalmContents before = grid(h, at);
        h.getLevel().destroyBlock(h.absolutePos(at), true);
        List<ItemEntity> drops = loose(h, at);
        h.assertValueEqual(drops.size(), 1, "one drop, the organ itself, and no loose ingredient");
        ItemStack dropped = drops.get(0).getItem();
        h.assertTrue(dropped.is(ProcessingModule.SHAPING_PALM_ITEM.get()), "it is the Palm");
        h.assertValueEqual(dropped.get(ProcessingModule.PALM_CONTENTS.get()), before, "carrying its grid, as it was");
        drops.get(0).discard();
        Player placer = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        placer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, dropped.copy());
        h.placeAt(placer, placer.getMainHandItem(), at.below(), Direction.UP);
        h.assertBlockPresent(ProcessingModule.SHAPING_PALM.get(), at);
        h.assertValueEqual(grid(h, at), before, "placed back, every stack is in its cell");
        h.assertTrue(loose(h, at).isEmpty(), "and nothing is left lying");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCreativePlayerWhoBreaksAFilledPalmStillGetsItBackWhole(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = open(h, at);
        put(player, 4, stack(Items.COAL, 33));
        player.closeContainer();
        PalmContents before = grid(h, at);
        Player creative = EcologyTests.holding(h, GameType.CREATIVE, Items.AIR, 0);
        var state = h.getBlockState(at);
        state.getBlock().playerWillDestroy(h.getLevel(), h.absolutePos(at), state, creative);
        h.getLevel().removeBlock(h.absolutePos(at), false);
        List<ItemEntity> drops = loose(h, at);
        h.assertValueEqual(drops.size(), 1, "one drop");
        h.assertValueEqual(
                drops.get(0).getItem().get(ProcessingModule.PALM_CONTENTS.get()), before, "with the grid in it");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anUnreadableSaveIsKeptAsBytesAndTakesNothing(GameTestHelper h) {
        BlockPos at = palm(h);
        var registries = h.getLevel().registryAccess();
        BlockEntity entity = h.getBlockEntity(at);
        CompoundTag tag = entity.saveWithFullMetadata(registries);
        ListTag grid = new ListTag();
        CompoundTag stone = new CompoundTag();
        stone.putString("id", "minecraft:stone");
        stone.putInt("count", 99);
        grid.add(stone);
        CompoundTag contents = new CompoundTag();
        contents.put("grid", grid);
        tag.put("contents", contents);
        entity.loadWithComponents(tag, registries);
        h.assertTrue(
                h.getBlockEntity(at).collectComponents().get(OrganModule.UNREADABLE_SAVE.get()) != null,
                "99 stone in one cell cannot be a grid");
        h.assertTrue(
                h.getBlockEntity(at)
                        .saveWithFullMetadata(registries)
                        .getCompound("contents")
                        .equals(contents),
                "the bytes are saved back as they were");
        MenuPlayer player = open(h, at);
        player.containerMenu.setCarried(stack(Items.STONE, 3));
        player.containerMenu.clicked(CELL, 0, ClickType.PICKUP, player);
        h.assertValueEqual(player.containerMenu.getCarried().getCount(), 3, "nothing goes into an unreadable Palm");
        h.assertTrue(grid(h, at).isEmpty(), "and it shows no grid");
        CompoundTag broken = h.getBlockEntity(at).saveWithFullMetadata(registries);
        broken.getCompound("contents")
                .getList("grid", 10)
                .getCompound(0)
                .put("id", StringTag.valueOf("minecraft:no_such_item"));
        h.getBlockEntity(at).loadWithComponents(broken, registries);
        h.assertTrue(
                h.getBlockEntity(at).collectComponents().get(OrganModule.UNREADABLE_SAVE.get()) != null,
                "an item that does not exist is unreadable too");
        h.succeed();
    }

    // ---- no storage and no automation yet ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 80)
    public static void aPalmHasNoItemHandlerAndNoHopperReachesIt(GameTestHelper h) {
        BlockPos at = palm(h);
        MenuPlayer player = open(h, at);
        put(player, 0, stack(Items.OAK_PLANKS, 4));
        player.closeContainer();
        for (Direction side : Direction.values())
            h.assertTrue(
                    h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(at), side) == null,
                    "no item handler on " + side);
        h.assertTrue(
                h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(at), null) == null,
                "and none for a sideless caller");
        h.assertFalse(containsPalm(Organs.FEEDING_HOSTS), "no Feeding Lobe serves it");
        h.assertFalse(containsPalm(Organs.YIELD_HOSTS), "and no Yield Lobe");
        h.assertTrue(containsPalm(Organs.ORGANS), "it is an organ");
        h.setBlock(at.above(), Blocks.HOPPER);
        h.setBlock(
                at.east(),
                Blocks.HOPPER
                        .defaultBlockState()
                        .setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.WEST));
        ((net.minecraft.world.level.block.entity.HopperBlockEntity) h.getBlockEntity(at.above()))
                .setItem(0, stack(Items.STONE, 8));
        ((net.minecraft.world.level.block.entity.HopperBlockEntity) h.getBlockEntity(at.east()))
                .setItem(0, stack(Items.GRAVEL, 8));
        PalmContents before = grid(h, at);
        h.runAfterDelay(40, () -> {
            h.assertValueEqual(grid(h, at), before, "no hopper put anything in or took anything out");
            h.assertValueEqual(
                    ((net.minecraft.world.level.block.entity.HopperBlockEntity) h.getBlockEntity(at.above()))
                            .getItem(0)
                            .getCount(),
                    8,
                    "the hopper above kept its stone");
            h.assertValueEqual(
                    ((net.minecraft.world.level.block.entity.HopperBlockEntity) h.getBlockEntity(at.east()))
                            .getItem(0)
                            .getCount(),
                    8,
                    "and the hopper beside it its gravel");
            h.succeed();
        });
    }

    private static boolean containsPalm(net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tag) {
        for (var holder : BuiltInRegistries.BLOCK.getTagOrEmpty(tag))
            if (holder.value() == ProcessingModule.SHAPING_PALM.get()) return true;
        return false;
    }

    /** A server player outside the player list that opens menus without a client, as in the stem tests. */
    private static final class MenuPlayer extends FakePlayer {
        private final java.util.Map<Stat<?>, Integer> awarded = new java.util.HashMap<>();

        MenuPlayer(ServerLevel level, String name) {
            super(level, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
        }

        /** A fake player drops statistics; this one keeps what the crafting slot awards. */
        @Override
        public void awardStat(Stat<?> stat, int amount) {
            awarded.merge(stat, amount, Integer::sum);
        }

        int crafted(Item item) {
            return awarded.getOrDefault(Stats.ITEM_CRAFTED.get(item), 0);
        }

        @Override
        public OptionalInt openMenu(MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extra) {
            containerMenu = provider.createMenu(1, getInventory(), this);
            return OptionalInt.of(1);
        }
    }
}
