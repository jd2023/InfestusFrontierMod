package org.jd.infestusfrontier.testmod;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.item.BoneMealItem;
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
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
import org.jd.infestusfrontier.storage.CapsuleStemBlock;
import org.jd.infestusfrontier.storage.ItemCapsuleBlock;
import org.jd.infestusfrontier.storage.ItemCapsuleMenu;
import org.jd.infestusfrontier.storage.ItemCapsulePodMenu;
import org.jd.infestusfrontier.storage.StemPart;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.CapsuleStock;
import org.jd.infestusfrontier.storage.api.ItemStore;

/**
 * Capsule Stems: a planted root grows trunk cells and pods from bone meal, dearer each level, never where
 * nothing fits; a tree is one store reached from any cell or pod, filling a kind's own pod before an empty one; a
 * broken cell orphans the pods above it until bone meal regrows it in place, dearer each level; a pod opens only
 * itself, with a treatment well; a broken root kills the stem, each
 * dying cell shedding its pods as items; a root plants only on a foundation and dies with it; a broken pod carries
 * its stock and a replanted one rejoins; grafts grow a pod's grade; saves
 * keep plans and stock past a stack and unreadable ones keep their bytes; Mouths and the Sensor Polyp read the whole
 * tree; the screen moves a stack at a time. Roots stand at (8, 2, 8).
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class CapsuleStemTests {
    private static final BlockPos ROOT = new BlockPos(8, 2, 8);
    /** Seeds whose plans the tests rely on: 7 forks east at tier 3, 8 grows its first pod east at tier 1. */
    private static final long FORKING = 7;

    private static final long EASTWARD = 8;

    private CapsuleStemTests() {}

    /** A root on Living Substrate, as a planted one must be. */
    private static void plant(GameTestHelper h, long seed) {
        h.setBlock(ROOT.below(), EcologyModule.LIVING_SUBSTRATE.get());
        CapsuleStemBlock.plant(h.getLevel(), h.absolutePos(ROOT), seed);
    }

    /** One bone meal on the root; true when the tree grew. */
    private static boolean grow(GameTestHelper h) {
        return BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), h.getLevel(), h.absolutePos(ROOT));
    }

    /** Bone meal until nothing more fits; the number of bone meal fed. */
    private static int growAll(GameTestHelper h) {
        int fed = 0;
        while (fed < 1024 && grow(h)) fed++;
        return fed;
    }

    private static boolean isStem(GameTestHelper h, BlockPos pos) {
        return h.getBlockState(pos).is(StorageModule.CAPSULE_STEM.get());
    }

    private static boolean isPod(GameTestHelper h, BlockPos pos) {
        return h.getBlockState(pos).is(StorageModule.ITEM_CAPSULE.get());
    }

    /** Every pod of the tree, in no particular order, by walking the template. */
    private static List<BlockPos> pods(GameTestHelper h) {
        List<BlockPos> pods = new ArrayList<>();
        for (int x = 0; x < 16; x++)
            for (int y = 1; y < 12; y++)
                for (int z = 0; z < 16; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isPod(h, pos)) pods.add(pos);
                }
        return pods;
    }

    private static List<BlockPos> stems(GameTestHelper h) {
        List<BlockPos> stems = new ArrayList<>();
        for (int x = 0; x < 16; x++)
            for (int y = 1; y < 12; y++)
                for (int z = 0; z < 16; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isStem(h, pos)) stems.add(pos);
                }
        return stems;
    }

    private static IItemHandler items(GameTestHelper h, BlockPos pos) {
        return h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), Direction.UP);
    }

    private static ItemStore store(GameTestHelper h, BlockPos pos) {
        return (ItemStore) items(h, pos);
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    /** A pod's item carrying {@code held}. */
    private static ItemStack podWith(ItemStack held) {
        ItemStack item = new ItemStack(StorageModule.ITEM_CAPSULE_ITEM.get());
        item.set(StorageModule.CAPSULE_STOCK.get(), new CapsuleStock(List.of(held), 0));
        return item;
    }

    private static Direction facing(GameTestHelper h, BlockPos pod) {
        return h.getBlockState(pod).getValue(ItemCapsuleBlock.FACING);
    }

    /** The stem cell a pod hangs on. */
    private static BlockPos cellOf(GameTestHelper h, BlockPos pod) {
        return pod.relative(facing(h, pod).getOpposite());
    }

    private static Player sneaking(GameTestHelper h) {
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        player.setShiftKeyDown(true);
        return player;
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aPlantedRootRisesThenGrowsStaggeredPodsOnFreeSidesAndForksOnce(GameTestHelper h) {
        plant(h, FORKING);
        h.assertTrue(h.getBlockState(ROOT).getValue(CapsuleStemBlock.PART) == StemPart.ROOT, "a root");
        h.assertTrue(grow(h), "the first bone meal grows");
        h.assertTrue(
                h.getBlockState(ROOT.above()).getValue(CapsuleStemBlock.PART) == StemPart.BUD,
                "one trunk cell up, the bud");
        h.assertTrue(grow(h), "the second bone meal grows");
        List<BlockPos> first = pods(h);
        h.assertValueEqual(first.size(), 1, "a pod on tier one");
        h.assertTrue(cellOf(h, first.getFirst()).equals(ROOT.above()), "hanging on the trunk, facing out");
        h.assertTrue(h.getBlockState(ROOT.above()).getValue(CapsuleStemBlock.COLLAR), "the cell wears a collar");
        int fed = growAll(h);
        h.assertTrue(fed >= 100 && fed <= 600, "the tree took " + fed + " more bone meal");
        h.assertFalse(grow(h), "and then no more");
        List<BlockPos> pods = pods(h);
        h.assertTrue(pods.size() >= 12 && pods.size() <= 16, pods.size() + " pods");
        for (BlockPos pod : pods) {
            BlockPos cell = cellOf(h, pod);
            h.assertTrue(isStem(h, cell), "every pod hangs on a stem cell");
            StemPart part = h.getBlockState(cell).getValue(CapsuleStemBlock.PART);
            h.assertTrue(part == StemPart.TRUNK || part == StemPart.BUD, "on a trunk cell, never the root or a branch");
            BlockPos below = pod.below();
            h.assertFalse(
                    isPod(h, below) && facing(h, below) == facing(h, pod) && isStem(h, cell.below()),
                    "no pod sits straight over the one below on the same side");
        }
        Map<StemPart, Integer> parts = new HashMap<>();
        for (BlockPos stem : stems(h))
            parts.merge(h.getBlockState(stem).getValue(CapsuleStemBlock.PART), 1, Integer::sum);
        h.assertValueEqual(parts.getOrDefault(StemPart.ROOT, 0), 1, "one root");
        h.assertValueEqual(parts.getOrDefault(StemPart.BRANCH, 0), 1, "one fork");
        h.assertValueEqual(parts.getOrDefault(StemPart.BUD, 0), 2, "a bud on each trunk");
        h.assertTrue(
                h.getBlockState(ROOT.east().above(3)).getValue(CapsuleStemBlock.PART) == StemPart.BRANCH
                        && h.getBlockState(ROOT.east().above(3)).getValue(CapsuleStemBlock.FACING) == Direction.EAST,
                "seed 7 forks east at tier three");
        h.assertValueEqual(items(h, ROOT).getSlots(), pods.size(), "the root reaches every pod");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void boneMealIsRefusedAndKeptWhereNothingFits(GameTestHelper h) {
        plant(h, FORKING);
        h.setBlock(ROOT.above(), Blocks.STONE);
        ItemStack meal = stack(Items.BONE_MEAL, 3);
        h.assertFalse(BoneMealItem.growCrop(meal, h.getLevel(), h.absolutePos(ROOT)), "nothing fits above");
        h.assertValueEqual(meal.getCount(), 3, "the bone meal is kept");
        h.setBlock(ROOT.above(), Blocks.AIR);
        h.assertTrue(BoneMealItem.growCrop(meal, h.getLevel(), h.absolutePos(ROOT)), "room again");
        h.assertValueEqual(meal.getCount(), 2, "one spent");
        for (Direction side : Direction.Plane.HORIZONTAL)
            h.setBlock(ROOT.above().relative(side), Blocks.STONE);
        h.setBlock(ROOT.above(2), Blocks.STONE);
        h.assertFalse(grow(h), "walled in, the tree stops");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aKindFillsItsOwnPodBeforeItOpensAnotherAndEveryCellReachesTheStore(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        IItemHandler handler = items(h, ROOT);
        int pods = handler.getSlots();
        h.assertTrue(store(h, ROOT).insert(stack(Items.COBBLESTONE, 520), false).isEmpty(), "all in");
        h.assertValueEqual(handler.getStackInSlot(0).getCount(), 512, "a young pod holds eight stacks");
        h.assertValueEqual(handler.getStackInSlot(1).getCount(), 8, "the rest opened the next pod");
        h.assertValueEqual(
                handler.insertItem(3, stack(Items.COBBLESTONE, 8), false).getCount(),
                8,
                "an empty pod stays shut while the kind's own has room");
        h.assertValueEqual(
                handler.insertItem(1, stack(Items.COBBLESTONE, 8), false).getCount(), 0, "its own takes it");
        h.assertValueEqual(
                handler.insertItem(3, stack(Items.DIRT, 8), false).getCount(), 8, "a new kind waits its turn");
        h.assertValueEqual(
                handler.insertItem(2, stack(Items.DIRT, 8), false).getCount(), 0, "the first empty one opens");
        h.assertValueEqual(handler.extractItem(0, 1000, false).getCount(), 64, "a stack at a time comes out");
        h.assertValueEqual(store(h, ROOT).count(stack(Items.COBBLESTONE, 1)), 464, "counted across pods");
        for (BlockPos pod : pods(h)) {
            h.assertValueEqual(items(h, pod).getSlots(), pods, "every pod opens the whole tree");
            h.assertValueEqual(store(h, pod).count(stack(Items.DIRT, 1)), 8, "with the same stock");
        }
        h.assertValueEqual(items(h, ROOT.above()).getSlots(), pods, "so does every trunk cell");
        for (int i = 0; i < 20; i++) store(h, ROOT).insert(stack(Items.IRON_SWORD, 1), false);
        h.assertValueEqual(handler.getStackInSlot(3).getCount(), 8, "eight swords fill a young pod");
        h.assertValueEqual(handler.getStackInSlot(4).getCount(), 8, "eight more the next");
        h.assertValueEqual(store(h, ROOT).count(stack(Items.IRON_SWORD, 1)), 20, "all counted");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aPodOpensOnlyItselfWhereAHandMovesStacksAndGraftsGrowIt(GameTestHelper h) {
        plant(h, FORKING);
        grow(h);
        grow(h);
        BlockPos pod = pods(h).getFirst();
        Player hand = EcologyTests.holding(h, GameType.SURVIVAL, Items.DIRT, 40);
        EcologyTests.useHeld(h, hand, pod, facing(h, pod));
        h.assertValueEqual(hand.getMainHandItem().getCount(), 40, "a hand with items opens the pod, nothing goes in");
        MenuPlayer player = new MenuPlayer(h.getLevel());
        BlockPos at = h.absolutePos(pod);
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        player.openMenu((MenuProvider) h.getBlockEntity(pod), at);
        AbstractContainerMenu menu = player.containerMenu;
        h.assertTrue(menu instanceof ItemCapsulePodMenu, "a pod opens its own screen");
        int inventorySlot = 2 + 27;
        player.getInventory().setItem(0, stack(Items.DIRT, 100));
        menu.clicked(inventorySlot, 0, ClickType.QUICK_MOVE, player);
        h.assertValueEqual(
                menu.getSlot(ItemCapsulePodMenu.COMPARTMENT).getItem().getCount(), 100, "shift-click fills it");
        h.assertValueEqual(store(h, ROOT).count(stack(Items.DIRT, 1)), 100, "and the tree counts it");
        menu.clicked(ItemCapsulePodMenu.COMPARTMENT, 0, ClickType.PICKUP, player);
        h.assertValueEqual(menu.getCarried().getCount(), 64, "a click takes one stack");
        h.assertValueEqual(
                menu.getSlot(ItemCapsulePodMenu.COMPARTMENT).getItem().getCount(), 36, "and leaves the rest");
        menu.clicked(ItemCapsulePodMenu.COMPARTMENT, 0, ClickType.PICKUP, player);
        menu.setCarried(stack(EcologyModule.SKELETAL_GRAFT.get(), 3));
        menu.clicked(ItemCapsulePodMenu.TREATMENT, 0, ClickType.PICKUP, player);
        h.assertValueEqual(h.getBlockState(pod).getValue(ItemCapsuleBlock.GRADE), 2, "two grafts grow it twice");
        h.assertValueEqual(menu.getSlot(ItemCapsulePodMenu.TREATMENT).getItem().getCount(), 1, "the third waits");
        h.assertValueEqual(items(h, pod).getSlotLimit(0), 2048, "thirty-two stacks now");
        player.getInventory().setItem(0, stack(EcologyModule.SKELETAL_GRAFT.get(), 1));
        menu.clicked(inventorySlot, 0, ClickType.QUICK_MOVE, player);
        h.assertValueEqual(
                menu.getSlot(ItemCapsulePodMenu.COMPARTMENT).getItem().getCount(), 100, "a graft is never stock");
        player.closeContainer();
        h.assertValueEqual(
                player.getInventory().countItem(EcologyModule.SKELETAL_GRAFT.get()),
                2,
                "closing hands the spare grafts back");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void boneMealCostDoublesWithEachLevelAndIsKeptBetweenUses(GameTestHelper h) {
        plant(h, FORKING);
        Map<Integer, Integer> costs = new HashMap<>();
        int meals = 0;
        for (int step = 0; step < 64; step++) {
            List<BlockPos> before = new ArrayList<>(stems(h));
            before.addAll(pods(h));
            int fed = 0;
            boolean grew = false;
            while (fed < 64 && !grew) {
                if (!grow(h)) break;
                fed++;
                List<BlockPos> after = new ArrayList<>(stems(h));
                after.addAll(pods(h));
                grew = after.size() > before.size();
                CompoundTag saved =
                        h.getBlockEntity(ROOT).saveWithoutMetadata(h.getLevel().registryAccess());
                h.assertValueEqual(saved.getInt("fed"), grew ? 0 : fed, "the root keeps what it was fed");
                if (grew) {
                    after.removeAll(before);
                    int level = after.getFirst().getY() - ROOT.getY();
                    h.assertValueEqual(fed, 1 << (level - 1), "a growth at level " + level + " costs its bone meal");
                    costs.merge(level, 1, Integer::sum);
                }
            }
            if (!grew) break;
            meals += fed;
        }
        h.assertTrue(costs.size() >= 6, "the tree reached six levels");
        h.assertTrue(meals > 100, "a whole tree is well over a hundred bone meal");
        h.assertFalse(grow(h), "nothing more to feed");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aSkeletalGraftGrowsAPodAGradeAndDoublesWhatItHolds(GameTestHelper h) {
        plant(h, FORKING);
        grow(h);
        grow(h);
        BlockPos pod = pods(h).getFirst();
        Player grafter = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SKELETAL_GRAFT.get(), 3);
        EcologyTests.useHeld(h, grafter, pod, facing(h, pod));
        h.assertValueEqual(h.getBlockState(pod).getValue(ItemCapsuleBlock.GRADE), 1, "grade one");
        h.assertValueEqual(grafter.getMainHandItem().getCount(), 2, "one graft spent");
        h.assertValueEqual(
                store(h, pod).insert(stack(Items.COBBLESTONE, 1030), false).getCount(), 6, "sixteen stacks fit");
        EcologyTests.useHeld(h, grafter, pod, facing(h, pod));
        h.assertValueEqual(h.getBlockState(pod).getValue(ItemCapsuleBlock.GRADE), 2, "grade two");
        h.assertValueEqual(items(h, pod).getSlotLimit(0), 2048, "thirty-two stacks");
        EcologyTests.useHeld(h, grafter, pod, facing(h, pod));
        h.assertValueEqual(grafter.getMainHandItem().getCount(), 1, "the last grade takes no graft");
        h.getLevel().destroyBlock(h.absolutePos(pod), true);
        List<ItemEntity> drops = h.getEntities(EntityType.ITEM, pod, 2.0);
        h.assertValueEqual(drops.size(), 1, "one dropped pod");
        CapsuleStock kept = drops.getFirst().getItem().get(StorageModule.CAPSULE_STOCK.get());
        h.assertTrue(
                kept != null && kept.grade() == 2 && kept.held().getFirst().getCount() == 1024,
                "grade and stock travel");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBrokenTrunkCellOrphansThePodsAboveUntilBoneMealRegrowsItInPlace(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        List<BlockPos> pods = pods(h);
        Map<BlockPos, BlockState> stems = new HashMap<>();
        for (BlockPos stem : stems(h)) stems.put(stem, h.getBlockState(stem));
        store(h, ROOT).insert(stack(Items.BONE, 300), false);
        BlockPos cut = ROOT.above(2);
        int under = 0;
        for (BlockPos pod : pods) if (cellOf(h, pod).equals(ROOT.above())) under++;
        h.getLevel().destroyBlock(h.absolutePos(cut), true);
        List<ItemEntity> drops = h.getEntities(EntityType.ITEM, cut, 2.0);
        h.assertTrue(
                drops.size() == 1 && drops.getFirst().getItem().is(EcologyModule.DEAD_TISSUE.get()), "tissue dies");
        h.assertValueEqual(items(h, ROOT).getSlots(), under, "the root reaches only the pods under the cut");
        h.assertValueEqual(pods(h).size(), pods.size(), "every pod still stands");
        BlockPos orphan = null;
        for (BlockPos pod : pods) if (pod.getY() > cut.getY()) orphan = pod;
        h.assertValueEqual(items(h, orphan).getSlots(), 1, "an orphan is a store of its own");
        h.assertValueEqual(store(h, ROOT).count(stack(Items.BONE, 1)), 300, "the bones sit under the cut");
        h.assertTrue(grow(h), "bone meal feeds the root");
        h.assertFalse(isStem(h, cut), "a cell at the second level costs two");
        h.assertTrue(grow(h), "the second regrows");
        h.assertTrue(h.getBlockState(cut).equals(stems.get(cut)), "the cell as it was, in its place");
        for (BlockPos stem : stems.keySet())
            h.assertTrue(h.getBlockState(stem).equals(stems.get(stem)), "the stem as it was");
        h.assertValueEqual(items(h, ROOT).getSlots(), pods.size(), "every pod is reached again");
        h.assertValueEqual(items(h, orphan).getSlots(), pods.size(), "the orphan is back in the tree");
        h.assertFalse(grow(h), "a whole tree has nothing more to grow");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aBrokenRootKillsTheStemFromTheBottomAndShedsItsPodsAsItems(GameTestHelper h) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        plant(h, FORKING);
        growAll(h);
        List<BlockPos> pods = pods(h);
        int cells = stems(h).size();
        store(h, ROOT).insert(stack(Items.BONE, 100), false);
        h.getLevel().destroyBlock(h.absolutePos(ROOT), true);
        h.assertValueEqual(stems(h).size(), cells - 1, "the rest stand for a moment");
        h.assertValueEqual(pods(h).size(), pods.size(), "with their pods");
        h.runAfterDelay(4 * cells + 10, () -> {
            h.assertValueEqual(stems(h).size(), 0, "the stem is dead");
            h.assertValueEqual(pods(h).size(), 0, "no pod stands");
            int capsules = 0, bones = 0;
            for (ItemEntity drop : h.getEntities(EntityType.ITEM)) {
                ItemStack item = drop.getItem();
                if (!item.is(StorageModule.ITEM_CAPSULE_ITEM.get())) continue;
                capsules += item.getCount();
                CapsuleStock carried = item.get(StorageModule.CAPSULE_STOCK.get());
                if (carried != null)
                    for (ItemStack held : carried.held()) if (held.is(Items.BONE)) bones += held.getCount();
            }
            h.assertValueEqual(capsules, pods.size(), "every pod dropped as an item");
            h.assertValueEqual(bones, 100, "with their items");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 100)
    public static void aRootPlantsOnlyOnAFoundationAndDiesWithIt(GameTestHelper h) {
        h.setBlock(ROOT.below(), Blocks.STONE);
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.CAPSULE_STEM_ITEM.get(), 1);
        h.placeAt(builder, builder.getMainHandItem(), ROOT.below(), Direction.UP);
        h.assertFalse(isStem(h, ROOT), "no root on stone");
        h.setBlock(ROOT.below(), EcologyModule.LIVING_SUBSTRATE.get());
        h.placeAt(builder, builder.getMainHandItem(), ROOT.below(), Direction.UP);
        h.assertTrue(isStem(h, ROOT), "a root on Living Substrate");
        h.assertTrue(grow(h) && grow(h), "which grows");
        int cells = stems(h).size();
        h.setBlock(ROOT.below(), Blocks.AIR);
        h.runAfterDelay(4 * cells + 10, () -> {
            h.assertValueEqual(stems(h).size(), 0, "the tree dies with its foundation");
            h.assertValueEqual(pods(h).size(), 0, "and sheds its pod");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aStemReplantedOnADeadRootsSpotKeepsNoneOfTheOldCells(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        int cells = stems(h).size();
        h.getLevel().destroyBlock(h.absolutePos(ROOT), true);
        plant(h, EASTWARD); // within the death window: the old cells' root is a root again, with another plan
        h.assertValueEqual(stems(h).size(), cells, "the old cells still stand for a moment");
        h.runAfterDelay(4 * cells + 10, () -> {
            h.assertValueEqual(stems(h).size(), 1, "only the new root stands");
            h.assertTrue(grow(h), "and it grows its own plan");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aPlanPastItsBoundsDoesNotDecode(GameTestHelper h) {
        plant(h, FORKING);
        BlockEntity root = h.getBlockEntity(ROOT);
        CompoundTag saved = root.saveWithFullMetadata(h.getLevel().registryAccess());
        ListTag cells = saved.getCompound("plan").getList("cells", 10);
        for (int i = 1; i < 15; i++) {
            CompoundTag cell = cells.getCompound(0).copy();
            cell.putInt("y", i);
            cell.putString("part", "trunk");
            cell.putInt("parent", i - 1);
            cell.putInt("tier", i);
            cells.add(cell);
        }
        root.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertFalse(grow(h), "fifteen cells is no plan");
        h.assertValueEqual(items(h, ROOT).getSlots(), 0, "and no store");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBrokenPodCarriesItsStockAndAReplantedOneRejoinsTheTree(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        List<BlockPos> pods = pods(h);
        store(h, ROOT).insert(stack(Items.BONE, 100), false);
        BlockPos pod = null;
        for (BlockPos candidate : pods) {
            CapsuleStock carried =
                    h.getBlockEntity(candidate).collectComponents().get(StorageModule.CAPSULE_STOCK.get());
            if (carried != null && !carried.isEmpty()) pod = candidate;
        }
        Direction facing = facing(h, pod);
        h.getLevel().destroyBlock(h.absolutePos(pod), true);
        List<ItemEntity> drops = h.getEntities(EntityType.ITEM, pod, 2.0);
        h.assertValueEqual(drops.size(), 1, "one dropped pod");
        ItemStack dropped = drops.getFirst().getItem();
        CapsuleStock kept = dropped.get(StorageModule.CAPSULE_STOCK.get());
        h.assertTrue(
                kept != null
                        && kept.held().getFirst().is(Items.BONE)
                        && kept.held().getFirst().getCount() == 100,
                "its bones on the item");
        h.assertValueEqual(dropped.getMaxStackSize(), 1, "a filled pod does not stack");
        h.assertValueEqual(items(h, ROOT).getSlots(), pods.size() - 1, "the tree goes on without it");
        h.assertValueEqual(store(h, ROOT).count(stack(Items.BONE, 1)), 0, "and without its bones");
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.ITEM_CAPSULE_ITEM.get(), 1);
        builder.setItemInHand(InteractionHand.MAIN_HAND, dropped.copy());
        h.placeAt(builder, builder.getMainHandItem(), pod.relative(facing.getOpposite()), facing);
        h.assertTrue(isPod(h, pod) && facing(h, pod) == facing, "pressed on the cell, it faces out again");
        h.assertValueEqual(items(h, ROOT).getSlots(), pods.size(), "the pod rejoined");
        h.assertValueEqual(store(h, ROOT).count(stack(Items.BONE, 1)), 100, "with its bones");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aLostPodGrowsAgainBeforeTheTreeGrowsOn(GameTestHelper h) {
        plant(h, FORKING);
        for (int i = 0; i < 6 && pods(h).size() < 2; i++) grow(h);
        List<BlockPos> pods = pods(h);
        h.assertValueEqual(pods.size(), 2, "two pods");
        BlockPos lost = pods.getFirst();
        Direction facing = facing(h, lost);
        h.getLevel().destroyBlock(h.absolutePos(lost), false);
        h.assertTrue(grow(h), "bone meal");
        h.assertTrue(isPod(h, lost) && facing(h, lost) == facing, "regrows the lost pod first, where it was");
        h.assertValueEqual(h.getBlockState(lost).getValue(ItemCapsuleBlock.GRADE), 0, "young");
        h.assertValueEqual(items(h, ROOT).getSlots(), 2, "in the tree");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aPodOnItsOwnIsItsOwnStoreAndTakesNoFilledCapsule(GameTestHelper h) {
        h.setBlock(ROOT, Blocks.STONE);
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.ITEM_CAPSULE_ITEM.get(), 1);
        builder.setYRot(0); // looking south
        h.placeAt(builder, builder.getMainHandItem(), ROOT, Direction.UP);
        BlockPos pod = ROOT.above();
        h.assertTrue(isPod(h, pod) && facing(h, pod) == Direction.NORTH, "its face to the player");
        h.assertValueEqual(items(h, pod).getSlots(), 1, "a store of one compartment");
        ItemStore store = store(h, pod);
        h.assertValueEqual(store.insert(podWith(stack(Items.DIRT, 5)), false).getCount(), 1, "a filled pod stays out");
        h.assertTrue(
                store.insert(new ItemStack(StorageModule.ITEM_CAPSULE_ITEM.get()), false)
                        .isEmpty(),
                "an empty one goes in");
        h.assertFalse(StorageModule.ITEM_CAPSULE_ITEM.get().canFitInsideContainerItems(), "and fits in no bundle");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void hoppersFillAndEmptyTheTree(GameTestHelper h) {
        plant(h, FORKING);
        grow(h);
        grow(h);
        BlockPos pod = pods(h).getFirst();
        h.setBlock(pod.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        ((HopperBlockEntity) h.getBlockEntity(pod.above())).setItem(0, stack(Items.BONE, 5));
        h.runAfterDelay(80, () -> {
            h.assertValueEqual(store(h, ROOT).count(stack(Items.BONE, 1)), 5, "the hopper filled the tree");
            h.setBlock(pod.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        });
        h.runAfterDelay(160, () -> {
            h.assertValueEqual(store(h, ROOT).count(stack(Items.BONE, 1)), 0, "a hopper under a pod emptied it");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aSavedTreeLoadsBackAndAnUnreadablePodKeepsItsBytesOutsideTheTree(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        int pods = items(h, ROOT).getSlots();
        store(h, ROOT).insert(stack(Items.COBBLESTONE, 400), false);
        BlockEntity root = h.getBlockEntity(ROOT);
        CompoundTag saved = root.saveWithFullMetadata(h.getLevel().registryAccess());
        h.assertTrue(saved.contains("plan"), "the plan is saved");
        root.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertValueEqual(items(h, ROOT).getSlots(), pods, "it loads back with its tree");
        h.assertValueEqual(store(h, ROOT).count(stack(Items.COBBLESTONE, 1)), 400, "and its stock");
        BlockPos first = null;
        for (BlockPos candidate : pods(h)) {
            CapsuleStock carried =
                    h.getBlockEntity(candidate).collectComponents().get(StorageModule.CAPSULE_STOCK.get());
            if (carried != null && !carried.isEmpty()) first = candidate;
        }
        BlockEntity pod = h.getBlockEntity(first);
        CompoundTag hostile = pod.saveWithFullMetadata(h.getLevel().registryAccess());
        ListTag held = hostile.getCompound("stock").getList("held", 10);
        h.assertValueEqual(held.size(), 1, "the cobblestone pod saves its compartment");
        held.add(held.getCompound(0).copy()); // the same compartment twice cannot be read
        pod.loadWithComponents(hostile, h.getLevel().registryAccess());
        h.assertValueEqual(items(h, first).getSlots(), 0, "an unreadable pod has no slots");
        h.assertValueEqual(items(h, ROOT).getSlots(), pods - 1, "and the tree goes on without it");
        CompoundTag kept = pod.collectComponents().get(OrganModule.UNREADABLE_SAVE.get());
        h.assertTrue(
                kept != null && kept.getCompound("stock").getList("held", 10).size() == 2, "it keeps its bytes");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anUnreadablePlanKeepsItsBytesAndGrowsNothing(GameTestHelper h) {
        plant(h, FORKING);
        grow(h);
        BlockEntity root = h.getBlockEntity(ROOT);
        CompoundTag saved = root.saveWithFullMetadata(h.getLevel().registryAccess());
        ListTag cells = saved.getCompound("plan").getList("cells", 10);
        CompoundTag bad = cells.getCompound(1).copy();
        bad.putInt("parent", 5); // grown from a cell that comes later: no plan
        cells.set(1, bad);
        root.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertFalse(grow(h), "bone meal is refused");
        h.assertValueEqual(items(h, ROOT).getSlots(), 0, "no store");
        CompoundTag again = root.saveWithFullMetadata(h.getLevel().registryAccess());
        h.assertValueEqual(
                again.getCompound("plan").getList("cells", 10).getCompound(1).getInt("parent"),
                5,
                "the bytes come back as they were");
        h.succeed();
    }

    /** The check a grown cell makes of its root when it is back in reach, fired by hand on every cell. */
    private static void recheck(GameTestHelper h) {
        for (BlockPos stem : stems(h))
            h.getLevel().scheduleTick(h.absolutePos(stem), StorageModule.CAPSULE_STEM.get(), 1);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 100)
    public static void anUnreadableRootsTreeAndAPlanlessRootStandInertThroughTheCellsCheck(GameTestHelper h) {
        plant(h, FORKING);
        for (int i = 0; i < 12; i++) grow(h);
        int cells = stems(h).size();
        int pods = pods(h).size();
        h.assertTrue(cells >= 3 && pods >= 2, "a tree with cells and pods");
        BlockEntity root = h.getBlockEntity(ROOT);
        CompoundTag saved = root.saveWithFullMetadata(h.getLevel().registryAccess());
        ListTag planned = saved.getCompound("plan").getList("cells", 10);
        CompoundTag bad = planned.getCompound(1).copy();
        bad.putInt("parent", 5);
        planned.set(1, bad);
        root.loadWithComponents(saved, h.getLevel().registryAccess());
        BlockPos loose = ROOT.west(5);
        h.setBlock(loose.below(), EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(loose, StorageModule.CAPSULE_STEM.get()); // a root without data, as /setblock makes it
        recheck(h);
        h.runAfterDelay(5, () -> {
            h.assertValueEqual(stems(h).size(), cells + 1, "every cell stands, the plan-less root with them");
            h.assertValueEqual(pods(h).size(), pods, "every pod hangs on");
            h.assertTrue(isStem(h, loose), "a root without a plan is inert, not dead");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCarriedCompartmentPastItsLimitIsKeptUnreadable(GameTestHelper h) {
        h.setBlock(ROOT, Blocks.STONE);
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.ITEM_CAPSULE_ITEM.get(), 1);
        builder.setItemInHand(InteractionHand.MAIN_HAND, podWith(stack(Items.DIRT, 5000)));
        h.placeAt(builder, builder.getMainHandItem(), ROOT, Direction.UP);
        h.assertValueEqual(items(h, ROOT.above()).getSlots(), 0, "it refuses");
        h.assertTrue(
                h.getBlockEntity(ROOT.above()).collectComponents().get(OrganModule.UNREADABLE_SAVE.get()) != null,
                "and keeps what it was given");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aMouthUnderTheRootReadsTheWholeTreesCount(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        store(h, ROOT).insert(stack(Items.BONE, 10), false);
        BlockPos intake = ROOT.below();
        BlockPos output = ROOT.below().south(4);
        h.setBlock(intake, LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, MouthMode.INTAKE));
        for (int z = 1; z <= 3; z++) h.setBlock(ROOT.below().south(z), LogisticsModule.ITEM_VEIN.get());
        h.setBlock(output, LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, MouthMode.OUTPUT));
        h.setBlock(output.above(), Blocks.CHEST);
        ((ChestBlockEntity) h.getBlockEntity(output.above())).setItem(0, stack(Items.BONE, 16));
        press(h, intake, MouthMenu.AMOUNT_UP, 2); // 0, 1, 4: fill the tree up to 4 bones
        h.runAfterDelay(120, () -> {
            h.assertValueEqual(store(h, ROOT).count(stack(Items.BONE, 1)), 10, "the tree already held more than 4");
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
    public static void aSensorPolypReadsHowFullAPodIs(GameTestHelper h) {
        h.setBlock(ROOT, Blocks.STONE);
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.ITEM_CAPSULE_ITEM.get(), 1);
        h.placeAt(builder, builder.getMainHandItem(), ROOT, Direction.UP);
        BlockPos pod = ROOT.above();
        h.setBlock(
                pod.east(),
                ControlModule.SENSOR_POLYP.get().defaultBlockState().setValue(SensorPolypBlock.FACING, Direction.WEST));
        store(h, pod).insert(stack(Items.COBBLESTONE, 256), false);
        h.runAfterDelay(40, () -> {
            h.assertValueEqual(h.getBlockState(pod.east()).getValue(SensorPolypBlock.POWER), 7, "half of 512 reads 7");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theScreenMovesAStackAtATimeAndShiftClickFillsTheTree(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        store(h, ROOT).insert(stack(Items.COBBLESTONE, 200), false);
        MenuPlayer player = new MenuPlayer(h.getLevel());
        BlockPos at = h.absolutePos(ROOT);
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        player.openMenu((MenuProvider) h.getBlockEntity(ROOT), at);
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
        int inventorySlot = ItemCapsuleMenu.PAGE_SIZE + 27;
        player.getInventory().setItem(0, stack(Items.DIRT, 40));
        menu.clicked(inventorySlot, 0, ClickType.QUICK_MOVE, player);
        h.assertValueEqual(store(h, ROOT).count(stack(Items.DIRT, 1)), 40, "shift-click fills the tree");
        h.assertTrue(player.getInventory().getItem(0).isEmpty(), "from the hand");
        h.assertFalse(menu.clickMenuButton(player, ItemCapsuleMenu.NEXT), "one page holds every pod");
        player.closeContainer();
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void everyCellOpensTheSameStoreUntilTheTreeChanges(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        store(h, ROOT).insert(stack(Items.BONE, 200), false);
        BlockPos top = ROOT.above(2);
        MenuPlayer player = new MenuPlayer(h.getLevel());
        BlockPos at = h.absolutePos(ROOT.above());
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        player.openMenu((MenuProvider) h.getBlockEntity(ROOT.above()), at);
        MenuPlayer other = new MenuPlayer(h.getLevel());
        BlockPos front = h.absolutePos(top);
        other.moveTo(front.getX() + 0.5, front.getY(), front.getZ() - 1.5);
        other.openMenu((MenuProvider) h.getBlockEntity(top), front);
        AbstractContainerMenu first = player.containerMenu;
        AbstractContainerMenu second = other.containerMenu;
        h.assertTrue(first.getSlot(0).getItem().is(Items.BONE), "a trunk cell opens the tree");
        h.assertTrue(second.getSlot(0).getItem().is(Items.BONE), "so does the cell above it");
        first.clicked(0, 0, ClickType.PICKUP, player);
        h.assertValueEqual(first.getCarried().getCount(), 64, "one viewer takes a stack");
        h.assertValueEqual(second.getSlot(0).getItem().getCount(), 136, "the other sees it go");
        second.clicked(0, 0, ClickType.PICKUP, other);
        h.assertValueEqual(second.getCarried().getCount(), 64, "the second viewer withdraws once");
        h.assertValueEqual(store(h, ROOT).count(stack(Items.BONE, 1)), 72, "one owner of the stock");
        second.clicked(0, 0, ClickType.PICKUP, other);
        other.closeContainer();
        first.clicked(0, 0, ClickType.PICKUP, player);
        h.destroyBlock(ROOT.above());
        h.assertFalse(first.stillValid(player), "a tree change invalidates the open page");
        first.clicked(0, 0, ClickType.PICKUP, player);
        h.assertTrue(first.getCarried().isEmpty(), "a changed tree cannot be read through stale slots");
        player.closeContainer();
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theClientRefusesToSwapAnotherKindIntoAnOccupiedCompartment(GameTestHelper h) {
        MenuPlayer player = new MenuPlayer(h.getLevel());
        RegistryFriendlyByteBuf extra =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        ItemCapsuleMenu menu;
        try {
            extra.writeBlockPos(h.absolutePos(ROOT));
            menu = new ItemCapsuleMenu(99, player.getInventory(), extra);
        } finally {
            extra.release();
        }
        menu.setData(0, 2);
        menu.getSlot(0).set(stack(Items.COBBLESTONE, 200));
        menu.setCarried(stack(Items.DIRT, 3));
        h.assertFalse(menu.getSlot(0).mayPlace(menu.getCarried()), "client refuses a different occupied kind");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        h.assertTrue(menu.getCarried().is(Items.DIRT), "prediction keeps the cursor kind");
        h.assertValueEqual(menu.getCarried().getCount(), 3, "prediction keeps the cursor count");
        h.assertValueEqual(menu.getSlot(0).getItem().getCount(), 200, "prediction keeps the compartment count");
        h.assertTrue(menu.getSlot(0).mayPlace(stack(Items.COBBLESTONE, 1)), "matching kinds still fit");
        h.assertTrue(menu.getSlot(1).mayPlace(stack(Items.DIRT, 1)), "empty compartments still fit");
        h.succeed();
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
        h.setBlock(ROOT, Blocks.STONE);
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.ITEM_CAPSULE_ITEM.get(), 1);
        h.placeAt(builder, builder.getMainHandItem(), ROOT, Direction.UP);
        BlockPos pod = ROOT.above();
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(stack(Items.DIAMOND, 32))));
        h.assertTrue(store(h, pod).insert(bundle, false).isEmpty(), "the bundle went in");
        MenuPlayer player = new MenuPlayer(h.getLevel());
        BlockPos at = h.absolutePos(pod);
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        player.openMenu((MenuProvider) h.getBlockEntity(pod), at);
        AbstractContainerMenu menu = player.containerMenu;
        menu.setCarried(stack(Items.DIRT, 10));
        menu.clicked(0, 1, ClickType.PICKUP, player);
        h.assertValueEqual(menu.getCarried().getCount(), 10, "no dirt vanishes into the bundle");
        h.assertValueEqual(diamonds(items(h, pod).getStackInSlot(0)), 32, "the bundle is unchanged");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 1, ClickType.PICKUP, player);
        int total = diamonds(menu.getCarried()) + diamonds(items(h, pod).getStackInSlot(0));
        h.assertValueEqual(total, 32, "a right click makes no diamonds");
        player.closeContainer();
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aFilledCapsuleItemThatBurnsSpillsItsItemsInStacks(GameTestHelper h) {
        BlockPos at = h.absolutePos(new BlockPos(2, 2, 2));
        ItemEntity burning = new ItemEntity(
                h.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, podWith(stack(Items.DIRT, 100)));
        h.getLevel().addFreshEntity(burning);
        burning.hurt(h.getLevel().damageSources().lava(), 100);
        List<ItemEntity> spilled = h.getEntities(EntityType.ITEM, new BlockPos(2, 2, 2), 2.0);
        int dirt = 0;
        for (ItemEntity entity : spilled) {
            if (entity.getItem().is(Items.DIRT)) dirt += entity.getItem().getCount();
            h.assertTrue(
                    entity.getItem().getCount() <= entity.getItem().getMaxStackSize(), "every spilled stack is whole");
        }
        h.assertTrue(burning.isRemoved(), "the capsule burned");
        h.assertValueEqual(dirt, 100, "all the dirt spilled");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theRecipeMakesAStem(GameTestHelper h) {
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
                        .is(StorageModule.CAPSULE_STEM_ITEM.get()),
                "it makes a stem");
        h.succeed();
    }

    /**
     * A tree across a chunk edge reaches only the pods in loaded chunks and keeps no stale store; once the far chunk
     * loads the tree is whole again with every item. Chunks load in rings, as in the Reservoir test.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "capsule_unload",
            timeoutTicks = 900)
    public static void aTreeAcrossAnUnloadedChunkReachesOnlyItsLoadedPods(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        BlockPos anchor = new BlockPos((origin.getX() & ~15) + 192000, 80, (origin.getZ() & ~15) + 224008);
        BlockPos root = anchor.east(47); // the last column of its chunk: the first pod grows east into the next
        ChunkPos kept = new ChunkPos(anchor);
        ChunkPos near = new ChunkPos(root);
        ChunkPos far = new ChunkPos(root.east());
        TestChunks.force(level, kept);
        TestChunks.force(level, near);
        TestChunks.force(level, far);
        CapsuleStemBlock.plant(level, root, EASTWARD);
        for (int i = 0; i < 3; i++) BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, root);
        BlockPos east = root.above().east();
        h.assertTrue(level.getBlockState(east).is(StorageModule.ITEM_CAPSULE.get()), "seed 8 grows its first pod east");
        IItemHandler[] port = new IItemHandler[1];
        h.startSequence()
                .thenExecute(() -> {
                    port[0] = level.getCapability(Capabilities.ItemHandler.BLOCK, root, Direction.UP);
                    h.assertValueEqual(port[0].getSlots(), 2, "two pods");
                    ((ItemStore) port[0]).insert(stack(Items.BONE, 50), false);
                    h.assertTrue(port[0].getStackInSlot(0).is(Items.BONE), "the bones went to the east pod");
                    // the root's chunk stays loaded as the kept chunk's border; the far chunk may go
                    level.setChunkForced(near.x, near.z, false);
                    level.setChunkForced(far.x, far.z, false);
                })
                .thenWaitUntil(() -> h.assertTrue(
                        level.hasChunkAt(root) && !level.hasChunkAt(east), "the root stays loaded, the east pod not"))
                .thenExecute(() -> {
                    h.assertValueEqual(port[0].getSlots(), 1, "a tree reaches only the pods in reach");
                    h.assertValueEqual(((ItemStore) port[0]).count(stack(Items.BONE, 1)), 0, "and counts only them");
                    TestChunks.force(level, near);
                    TestChunks.force(level, far);
                })
                .thenWaitUntil(() -> h.assertValueEqual(port[0].getSlots(), 2, "the tree is whole again"))
                .thenExecute(() -> {
                    h.assertValueEqual(((ItemStore) port[0]).count(stack(Items.BONE, 1)), 50, "nothing lost");
                    for (BlockPos pos : BlockPos.betweenClosed(root.offset(-2, 0, -2), root.offset(2, 7, 2)))
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    level.setChunkForced(kept.x, kept.z, false);
                    level.setChunkForced(near.x, near.z, false);
                    level.setChunkForced(far.x, far.z, false);
                })
                .thenSucceed();
    }

    /**
     * A pod whose root's chunk goes out of reach stands in alone with what it holds and is read again on every use,
     * so it is the tree's again the moment the root is back; nothing caches it as standalone.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "capsule_unload",
            timeoutTicks = 900)
    public static void aPodWhoseRootIsOutOfReachStandsInAloneUntilTheRootIsBack(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        BlockPos anchor = new BlockPos((origin.getX() & ~15) + 192000, 80, (origin.getZ() & ~15) + 224072);
        BlockPos root = anchor.east(47);
        BlockPos east = root.above().east();
        ChunkPos near = new ChunkPos(root);
        ChunkPos far = new ChunkPos(east);
        ChunkPos kept = new ChunkPos(east.east(32)); // two chunks on: the pod's chunk stays loaded, the root's goes
        TestChunks.force(level, kept);
        TestChunks.force(level, near);
        TestChunks.force(level, far);
        level.setBlockAndUpdate(
                root.below(), EcologyModule.LIVING_SUBSTRATE.get().defaultBlockState());
        CapsuleStemBlock.plant(level, root, EASTWARD);
        for (int i = 0; i < 3; i++) BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, root);
        h.assertTrue(level.getBlockState(east).is(StorageModule.ITEM_CAPSULE.get()), "seed 8 grows its first pod east");
        IItemHandler[] port = new IItemHandler[1];
        h.startSequence()
                .thenExecute(() -> {
                    port[0] = level.getCapability(Capabilities.ItemHandler.BLOCK, east, Direction.UP);
                    h.assertValueEqual(port[0].getSlots(), 2, "the pod opens the tree");
                    ((ItemStore) port[0]).insert(stack(Items.BONE, 50), false);
                    level.setChunkForced(near.x, near.z, false);
                    level.setChunkForced(far.x, far.z, false);
                })
                .thenWaitUntil(() ->
                        h.assertTrue(!level.hasChunkAt(root) && level.hasChunkAt(east), "the root goes, the pod stays"))
                .thenExecute(() -> {
                    h.assertValueEqual(port[0].getSlots(), 1, "the pod stands in alone");
                    h.assertValueEqual(((ItemStore) port[0]).count(stack(Items.BONE, 1)), 50, "with its bones");
                    h.assertValueEqual(port[0].getSlots(), 1, "and again");
                    TestChunks.force(level, near);
                })
                .thenWaitUntil(() -> h.assertTrue(level.hasChunkAt(root), "the root is back"))
                .thenWaitUntil(() -> h.assertValueEqual(port[0].getSlots(), 2, "the pod is the tree's again"))
                .thenExecute(() -> {
                    h.assertValueEqual(
                            level.getCapability(Capabilities.ItemHandler.BLOCK, root, Direction.UP)
                                    .getSlots(),
                            2,
                            "and the root reaches it");
                    for (BlockPos pos : BlockPos.betweenClosed(root.offset(-2, -1, -2), root.offset(2, 7, 2)))
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    level.setChunkForced(kept.x, kept.z, false);
                    level.setChunkForced(near.x, near.z, false);
                    level.setChunkForced(far.x, far.z, false);
                })
                .thenSucceed();
    }

    /**
     * A root broken while part of its tree is out of reach cannot tell those cells; they find out when they are back,
     * die and shed their pods. A root merely out of reach is never taken for a dead one.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "capsule_unload",
            timeoutTicks = 1200)
    public static void aCellOutOfReachWhenItsRootBreaksDiesWhenItIsBack(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        BlockPos anchor = new BlockPos((origin.getX() & ~15) + 192000, 80, (origin.getZ() & ~15) + 224136);
        BlockPos root = anchor.east(47);
        BlockPos branch = root.east().above(3); // seed 7 forks east at tier three, into the next chunk
        ChunkPos kept = new ChunkPos(anchor);
        ChunkPos near = new ChunkPos(root);
        ChunkPos far = new ChunkPos(branch);
        TestChunks.force(level, kept);
        TestChunks.force(level, near);
        TestChunks.force(level, far);
        level.setBlockAndUpdate(
                root.below(), EcologyModule.LIVING_SUBSTRATE.get().defaultBlockState());
        CapsuleStemBlock.plant(level, root, FORKING);
        for (int i = 0; i < 1024 && BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, root); i++) {}
        h.assertTrue(level.getBlockState(branch).is(StorageModule.CAPSULE_STEM.get()), "the fork stands east");
        List<BlockPos> farCells = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(branch.offset(0, -3, -2), branch.offset(2, 4, 2)))
            if (level.getBlockState(pos).is(StorageModule.CAPSULE_STEM.get())) farCells.add(pos.immutable());
        h.startSequence()
                .thenExecute(() -> {
                    level.setChunkForced(near.x, near.z, false);
                    level.setChunkForced(far.x, far.z, false);
                })
                .thenWaitUntil(() -> h.assertTrue(
                        level.hasChunkAt(root) && !level.hasChunkAt(branch), "the fork's chunk goes, the root stays"))
                .thenExecute(() -> level.destroyBlock(root, true))
                .thenExecute(() -> TestChunks.force(level, far))
                .thenWaitUntil(() -> h.assertTrue(level.hasChunkAt(branch), "the fork's chunk is back"))
                .thenWaitUntil(() -> {
                    for (BlockPos pos : farCells)
                        h.assertFalse(level.getBlockState(pos).is(StorageModule.CAPSULE_STEM.get()), "a far cell dies");
                })
                .thenExecute(() -> TestChunks.force(level, near)) // ticking again, the near cells pay their death
                .thenWaitUntil(() -> {
                    h.assertFalse(
                            level.getBlockState(root.above()).is(StorageModule.CAPSULE_STEM.get()),
                            "the near cells die");
                    // the pods east of the main trunk stand in the far chunk but hang on near cells: shed last
                    for (BlockPos pos : BlockPos.betweenClosed(root.offset(-2, 0, -2), root.offset(3, 8, 2)))
                        h.assertFalse(
                                level.getBlockState(pos).is(StorageModule.ITEM_CAPSULE.get()), "every pod is shed");
                })
                .thenExecute(() -> {
                    for (BlockPos pos : BlockPos.betweenClosed(root.offset(-2, -1, -2), root.offset(3, 8, 2)))
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
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
