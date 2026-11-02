package org.jd.infestusfrontier.testmod;

import com.mojang.authlib.GameProfile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.storage.PodKind;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.MouthBlock;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.CapsuleStemBlock;
import org.jd.infestusfrontier.storage.ItemCapsuleBlock;
import org.jd.infestusfrontier.storage.ItemCapsulePodMenu;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.CapsuleStock;
import org.jd.infestusfrontier.storage.api.ItemStore;

/**
 * The Mixed Craw: an empty bulk pod turned by one Elastic Gel into four compartments of one kind each at a quarter of
 * the depth, half a bulk pod's total. The mutation keeps grade, facing and tree and refuses a pod that holds anything,
 * a Craw or a second kind of treatment unchanged. Bulk pods and Craws share one tree ledger without counting an item
 * twice; kinds are told apart by their components; a full tree refuses unchanged; a Craw keeps its stock through break,
 * carry, pressing back, orphaning, shedding and saves. Roots stand at (8, 2, 8).
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class MixedCrawTests {
    private static final BlockPos ROOT = new BlockPos(8, 2, 8);
    /** Seed 7 forks east at tier 3, as in the stem tests. */
    private static final long FORKING = 7;

    private static final long EASTWARD = 8;

    private MixedCrawTests() {}

    private static void plant(GameTestHelper h, long seed) {
        h.setBlock(ROOT.below(), EcologyModule.LIVING_SUBSTRATE.get());
        CapsuleStemBlock.plant(h.getLevel(), h.absolutePos(ROOT), seed);
    }

    private static boolean grow(GameTestHelper h) {
        return BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), h.getLevel(), h.absolutePos(ROOT));
    }

    private static void growAll(GameTestHelper h) {
        for (int fed = 0; fed < 1024 && grow(h); fed++) {}
    }

    private static boolean isBulk(GameTestHelper h, BlockPos pos) {
        return h.getBlockState(pos).is(StorageModule.ITEM_CAPSULE.get());
    }

    private static boolean isCraw(GameTestHelper h, BlockPos pos) {
        return h.getBlockState(pos).is(StorageModule.MIXED_CRAW.get());
    }

    /** Every pod of the tree, bulk or Craw, in the order the template is walked. */
    private static List<BlockPos> pods(GameTestHelper h) {
        List<BlockPos> pods = new ArrayList<>();
        for (int x = 0; x < 16; x++)
            for (int y = 1; y < 12; y++)
                for (int z = 0; z < 16; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isBulk(h, pos) || isCraw(h, pos)) pods.add(pos);
                }
        return pods;
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

    private static ItemStack gel(int count) {
        return new ItemStack(ProcessingModule.ELASTIC_GEL.get(), count);
    }

    /** A diamond pickaxe worn by {@code damage}: a kind of its own, told apart from the rest only by its components. */
    private static ItemStack worn(int damage) {
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        pick.setDamageValue(damage);
        return pick;
    }

    private static ItemStack named(Item item, int count, String name) {
        ItemStack named = new ItemStack(item, count);
        named.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return named;
    }

    /** {@code count} different items that stack to 64 and fit a pod. */
    private static List<Item> kinds(int count) {
        List<Item> kinds = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (kinds.size() == count) break;
            ItemStack probe = new ItemStack(item);
            if (probe.getMaxStackSize() == 64 && !probe.isEmpty() && !probe.is(StorageModule.ITEM_CAPSULE_ITEM.get()))
                kinds.add(item);
        }
        if (kinds.size() != count) throw new IllegalStateException("the registry has too few 64-stack items");
        return kinds;
    }

    private static int total(ItemStore store, List<Item> kinds) {
        int total = 0;
        for (Item kind : kinds) total += store.count(stack(kind, 1));
        return total;
    }

    /** Opens a pod's own screen for a server player. */
    private static MenuPlayer open(GameTestHelper h, BlockPos pod) {
        MenuPlayer player = new MenuPlayer(h.getLevel());
        BlockPos at = h.absolutePos(pod);
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        player.openMenu((MenuProvider) h.getBlockEntity(pod), at);
        return player;
    }

    /** Puts {@code treatment} in the well of the pod at {@code pod}; what the well keeps is returned on closing. */
    private static MenuPlayer treat(GameTestHelper h, BlockPos pod, ItemStack treatment) {
        MenuPlayer player = open(h, pod);
        player.containerMenu.setCarried(treatment);
        player.containerMenu.clicked(ItemCapsulePodMenu.TREATMENT, 0, ClickType.PICKUP, player);
        return player;
    }

    /** A bulk pod standing alone on stone at {@code at}'s floor; the pod's position is above it. */
    private static BlockPos alone(GameTestHelper h, BlockPos floor, int grade) {
        h.setBlock(floor, Blocks.STONE);
        BlockPos pod = floor.above();
        h.setBlock(
                pod,
                StorageModule.ITEM_CAPSULE
                        .get()
                        .defaultBlockState()
                        .setValue(ItemCapsuleBlock.GRADE, grade)
                        .setValue(ItemCapsuleBlock.FACING, Direction.NORTH));
        return pod;
    }

    private static BlockPos aloneCraw(GameTestHelper h, BlockPos floor, int grade) {
        h.setBlock(floor, Blocks.STONE);
        BlockPos pod = floor.above();
        h.setBlock(
                pod,
                StorageModule.MIXED_CRAW
                        .get()
                        .defaultBlockState()
                        .setValue(ItemCapsuleBlock.GRADE, grade)
                        .setValue(ItemCapsuleBlock.FACING, Direction.NORTH));
        return pod;
    }

    private static CapsuleStock stockOf(GameTestHelper h, BlockPos pod) {
        return h.getBlockEntity(pod).collectComponents().get(StorageModule.CAPSULE_STOCK.get());
    }

    private static CompoundTag saved(GameTestHelper h, BlockEntity entity) {
        return entity.saveWithFullMetadata(h.getLevel().registryAccess());
    }

    /** Saves the block entity to bytes and loads them into a fresh one of the same block, put in its place. */
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
            h.fail("could not save and read the pod: " + e.getMessage());
        }
    }

    // ---- the mutation ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void oneElasticGelTurnsAnEmptyPodIntoACrawKeepingGradeFacingAndTree(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        List<BlockPos> pods = pods(h);
        int before = items(h, ROOT).getSlots();
        h.assertValueEqual(before, pods.size(), "a bulk pod is one compartment");
        BlockPos pod = pods.get(pods.size() / 2);
        Direction facing = h.getBlockState(pod).getValue(ItemCapsuleBlock.FACING);
        Player player = treat(h, pod, stack(EcologyModule.SKELETAL_GRAFT.get(), 1));
        player.closeContainer();
        h.assertValueEqual(h.getBlockState(pod).getValue(ItemCapsuleBlock.GRADE), 1, "grown a grade first");
        MenuPlayer gelled = treat(h, pod, gel(3));
        h.assertTrue(isCraw(h, pod), "the pod is a Mixed Craw");
        h.assertValueEqual(h.getBlockState(pod).getValue(ItemCapsuleBlock.GRADE), 1, "it keeps its grade");
        h.assertValueEqual(h.getBlockState(pod).getValue(ItemCapsuleBlock.FACING), facing, "and its facing");
        h.assertValueEqual(
                items(h, ROOT).getSlots(), before + PodKind.CRAW_COMPARTMENTS - 1, "the tree reads four compartments");
        h.assertValueEqual(items(h, pod).getSlots(), before + 3, "from the pod too: one tree");
        gelled.closeContainer();
        h.assertValueEqual(
                gelled.getInventory().countItem(ProcessingModule.ELASTIC_GEL.get()),
                2,
                "one gel spent, the spare two come back");
        h.assertValueEqual(pods(h).size(), pods.size(), "no pod was added or lost");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aPodThatHoldsAnythingRefusesTheGelUnchanged(GameTestHelper h) {
        BlockPos pod = alone(h, ROOT.below(), 1);
        store(h, pod).insert(stack(Items.DIRT, 5), false);
        CompoundTag before = saved(h, h.getBlockEntity(pod));
        MenuPlayer player = treat(h, pod, gel(1));
        h.assertTrue(isBulk(h, pod), "still a bulk pod");
        h.assertValueEqual(store(h, pod).count(stack(Items.DIRT, 1)), 5, "with its dirt");
        h.assertValueEqual(
                player.containerMenu
                        .getSlot(ItemCapsulePodMenu.TREATMENT)
                        .getItem()
                        .getCount(),
                1,
                "the gel stays in the well");
        h.assertValueEqual(saved(h, h.getBlockEntity(pod)), before, "the pod saves exactly as before");
        player.closeContainer();
        h.assertValueEqual(
                player.getInventory().countItem(ProcessingModule.ELASTIC_GEL.get()), 1, "and comes back on closing");
        store(h, pod).insert(stack(Items.DIRT, 0), false);
        items(h, pod).extractItem(0, 5, false);
        MenuPlayer emptied = treat(h, pod, gel(1));
        h.assertTrue(isCraw(h, pod), "emptied, the same pod takes the gel");
        emptied.closeContainer();
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aShiftClickedGelIsStoredNotRefusedByAPodThatHoldsItems(GameTestHelper h) {
        BlockPos pod = alone(h, ROOT.below(), 0);
        store(h, pod).insert(stack(Items.DIRT, 5), false);
        MenuPlayer player = open(h, pod);
        AbstractContainerMenu menu = player.containerMenu;
        player.getInventory().setItem(0, gel(3));
        int inventorySlot = 2 + 27;
        menu.clicked(inventorySlot, 0, ClickType.QUICK_MOVE, player);
        h.assertTrue(isBulk(h, pod), "the pod is untouched");
        h.assertTrue(
                menu.getSlot(ItemCapsulePodMenu.TREATMENT).getItem().isEmpty(), "the well never took it to refuse it");
        h.assertValueEqual(store(h, pod).count(stack(Items.DIRT, 1)), 5, "the dirt stays");
        player.closeContainer();
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCrawTakesNoSecondGelAndNoOtherItemTurnsItBack(GameTestHelper h) {
        BlockPos craw = aloneCraw(h, ROOT.below(), 0);
        MenuPlayer player = open(h, craw);
        AbstractContainerMenu menu = player.containerMenu;
        menu.setCarried(gel(1));
        menu.clicked(ItemCapsulePodMenu.TREATMENT, 0, ClickType.PICKUP, player);
        h.assertTrue(isCraw(h, craw), "still a Craw");
        h.assertValueEqual(menu.getCarried().getCount(), 1, "the well does not take a gel");
        for (Item other : List.of(
                ProcessingModule.FUSION_BINDER.get(),
                StorageModule.BIOMASS_BUCKET.get(),
                EcologyModule.CALCIFYING_GRAFT.get(),
                Items.BONE_MEAL,
                Items.SLIME_BALL)) {
            menu.setCarried(stack(other, 1));
            menu.clicked(ItemCapsulePodMenu.TREATMENT, 0, ClickType.PICKUP, player);
            h.assertTrue(isCraw(h, craw), "still a Craw after " + other);
            h.assertValueEqual(menu.getCarried().getCount(), 1, "the well takes only grafts");
        }
        player.closeContainer();
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anUnreadablePodRefusesTheGelAndKeepsItsBytes(GameTestHelper h) {
        BlockPos pod = alone(h, ROOT.below(), 0);
        BlockEntity entity = h.getBlockEntity(pod);
        CompoundTag hostile = saved(h, entity);
        ListTag held = hostile.getCompound("stock").getList("held", 10);
        CompoundTag extra = new CompoundTag();
        extra.putInt("slot", 0);
        extra.put("item", new CompoundTag());
        extra.putInt("count", 1);
        held.add(extra.copy());
        held.add(extra.copy());
        hostile.getCompound("stock").put("held", held);
        entity.loadWithComponents(hostile, h.getLevel().registryAccess());
        h.assertValueEqual(items(h, pod).getSlots(), 0, "a pod with a broken save has no slots");
        CompoundTag kept = entity.collectComponents().get(OrganModule.UNREADABLE_SAVE.get());
        h.assertTrue(kept != null, "its bytes are kept");
        MenuPlayer player = open(h, pod);
        player.containerMenu.setCarried(gel(1));
        player.containerMenu.clicked(ItemCapsulePodMenu.TREATMENT, 0, ClickType.PICKUP, player);
        h.assertTrue(isBulk(h, pod), "it is not mutated");
        h.assertTrue(
                h.getBlockEntity(pod).collectComponents().get(OrganModule.UNREADABLE_SAVE.get()) != null,
                "and its bytes are kept");
        player.closeContainer();
        h.succeed();
    }

    // ---- capacity ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCrawHoldsHalfOfABulkPodAtEveryGradeAndOneKindAQuarterOfThatDepth(GameTestHelper h) {
        for (int grade = 0; grade <= CapsuleStock.LAST_GRADE; grade++) {
            BlockPos bulk = alone(h, new BlockPos(2 + 4 * grade, 1, 2), grade);
            BlockPos craw = aloneCraw(h, new BlockPos(2 + 4 * grade, 1, 8), grade);
            h.assertValueEqual(items(h, bulk).getSlots(), 1, "a bulk pod is one compartment");
            h.assertValueEqual(items(h, craw).getSlots(), 4, "a Craw is four");
            long full = store(h, bulk).capacity();
            long half = store(h, craw).capacity();
            h.assertValueEqual(full, 8L * 64 << grade, "a bulk pod holds 8, 16 or 32 stacks");
            h.assertValueEqual(half * 2, full, "a Craw holds half of it at grade " + grade);
            store(h, bulk).insert(stack(Items.DIRT, 1), false);
            store(h, craw).insert(stack(Items.DIRT, 1), false);
            h.assertValueEqual(items(h, craw).getSlotLimit(0), 64 << grade, "one kind: 1, 2 or 4 stacks");
            h.assertValueEqual(items(h, bulk).getSlotLimit(0) / items(h, craw).getSlotLimit(0), 8, "a quarter of half");
        }
        BlockPos tools = aloneCraw(h, new BlockPos(14, 1, 2), 2);
        store(h, tools).insert(worn(1), false);
        h.assertValueEqual(items(h, tools).getSlotLimit(0), 4, "an unstackable kind is 4 items at grade 2");
        BlockPos young = aloneCraw(h, new BlockPos(14, 1, 8), 0);
        store(h, young).insert(worn(1), false);
        h.assertValueEqual(items(h, young).getSlotLimit(0), 1, "and 1 when young");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCrawTakesFourKindsAndRefusesAFifthUnchanged(GameTestHelper h) {
        BlockPos craw = aloneCraw(h, ROOT.below(), 0);
        ItemStore store = store(h, craw);
        List<Item> kinds = kinds(5);
        for (int i = 0; i < 4; i++)
            h.assertTrue(store.insert(stack(kinds.get(i), 10), false).isEmpty(), "kind " + i + " fits");
        CompoundTag before = saved(h, h.getBlockEntity(craw));
        ItemStack fifth = stack(kinds.get(4), 10);
        h.assertValueEqual(store.insert(fifth, true).getCount(), 10, "simulated, the fifth kind is refused");
        h.assertValueEqual(store.insert(fifth, false).getCount(), 10, "and really refused");
        h.assertValueEqual(items(h, craw).insertItem(0, fifth, false).getCount(), 10, "by every slot");
        h.assertFalse(items(h, craw).isItemValid(0, fifth), "no slot says it is valid");
        h.assertValueEqual(fifth.getCount(), 10, "the offered stack is not touched");
        h.assertValueEqual(saved(h, h.getBlockEntity(craw)), before, "the Craw saves exactly as before");
        // a kind it holds still goes in, up to the depth of one compartment: one stack, 64, of the first kind
        ItemStack more = stack(kinds.getFirst(), 100);
        ItemStack rest = store.insert(more, false);
        h.assertValueEqual(rest.getCount(), 100 - 54, "a held kind fills its compartment to 64 and no further");
        h.assertValueEqual(store.count(stack(kinds.getFirst(), 1)), 64, "one stack deep");
        h.assertValueEqual(store.stored(), 64 + 30L, "the Craw holds 64 + 3 * 10");
        h.succeed();
    }

    // ---- shared capacity ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void kindsContendForTheCompartmentsOfBulkPodsAndCrawsWithoutCountingTwice(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        List<BlockPos> pods = pods(h);
        int crawed = 3;
        for (int i = 0; i < crawed; i++) treat(h, pods.get(2 * i), gel(1)).closeContainer();
        int slots = pods.size() - crawed + crawed * PodKind.CRAW_COMPARTMENTS;
        IItemHandler tree = items(h, ROOT);
        ItemStore store = store(h, ROOT);
        h.assertValueEqual(tree.getSlots(), slots, "bulk pods one slot, Craws four");
        List<Item> kinds = kinds(slots + 1);
        // every kind arrives once, so each opens one compartment of its own
        for (int i = 0; i < slots; i++)
            h.assertTrue(store.insert(stack(kinds.get(i), 1), false).isEmpty(), "kind " + i + " opened a compartment");
        ItemStack stranger = stack(kinds.get(slots), 1);
        h.assertValueEqual(
                store.insert(stranger, true).getCount(), 1, "the tree is full of kinds: no room for one more");
        h.assertValueEqual(store.insert(stranger, false).getCount(), 1, "really refused");
        h.assertValueEqual(store.stored(), (long) slots, "one of each is held, once");
        // now each kind fills the depth of the compartment it opened, and the ledger agrees with the handler
        long limits = 0;
        for (int slot = 0; slot < slots; slot++) limits += tree.getSlotLimit(slot);
        h.assertValueEqual(store.capacity(), limits, "the capacity is the sum of every compartment's own limit");
        long bulkLimit = 8L * 64;
        long crawLimit = 64;
        h.assertValueEqual(
                limits,
                (pods.size() - crawed) * bulkLimit + crawed * PodKind.CRAW_COMPARTMENTS * crawLimit,
                "bulk pods hold 512 of a 64 kind, Craw compartments 64");
        for (int i = 0; i < slots; i++) {
            ItemStack kind = stack(kinds.get(i), 1000);
            ItemStack rest = store.insert(kind, false);
            h.assertTrue(rest.getCount() < 1000, "kind " + i + " filled its own compartment");
        }
        h.assertValueEqual(store.stored(), limits, "every compartment is full");
        h.assertValueEqual(total(store, kinds), (int) limits, "the kinds add up to what the tree holds, none twice");
        for (int i = 0; i < slots; i++)
            h.assertValueEqual(
                    store.insert(stack(kinds.get(i), 1), false).getCount(), 1, "kind " + i + " is refused when full");
        h.assertValueEqual(store.stored(), limits, "a full tree took nothing");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aKindFillsItsOwnCompartmentsInABulkPodAndACrawBeforeItOpensAnEmptyOne(GameTestHelper h) {
        plant(h, FORKING);
        for (int i = 0; i < 3; i++) grow(h);
        List<BlockPos> pods = pods(h);
        h.assertTrue(pods.size() >= 2, "two pods at least");
        treat(h, pods.getFirst(), gel(1)).closeContainer();
        IItemHandler tree = items(h, ROOT);
        ItemStore store = store(h, ROOT);
        int craw = PodKind.CRAW_COMPARTMENTS;
        // the Craw's compartments come first, one stack deep each: 64 + 64 + 30 of a kind spreads over three
        h.assertTrue(store.insert(stack(Items.DIRT, 158), false).isEmpty(), "dirt fits the tree");
        int[] held = new int[craw + (pods.size() - 1)];
        for (int slot = 0; slot < held.length; slot++)
            held[slot] = tree.getStackInSlot(slot).getCount();
        h.assertValueEqual(held[0], 64, "the first compartment is one stack");
        h.assertValueEqual(held[1], 64, "the second");
        h.assertValueEqual(held[2], 30, "the third holds the rest");
        h.assertValueEqual(held[3], 0, "the fourth is empty");
        h.assertTrue(store.insert(stack(Items.STONE, 5), false).isEmpty(), "another kind opens the empty one");
        h.assertValueEqual(tree.getStackInSlot(3).getCount(), 5, "in the Craw's last compartment");
        h.assertTrue(store.insert(stack(Items.DIRT, 2), false).isEmpty(), "dirt tops up its own third compartment");
        h.assertValueEqual(tree.getStackInSlot(2).getCount(), 32, "before it opens another");
        h.assertValueEqual(store.count(stack(Items.DIRT, 1)), 160, "the ledger sums over compartments");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void kindsToldApartOnlyByComponentsAreKindsOfTheirOwn(GameTestHelper h) {
        BlockPos craw = aloneCraw(h, ROOT.below(), 2);
        IItemHandler handler = items(h, craw);
        ItemStore store = store(h, craw);
        for (int damage = 0; damage < 4; damage++)
            h.assertTrue(store.insert(worn(damage), false).isEmpty(), "pickaxe worn by " + damage);
        for (int damage = 0; damage < 4; damage++) {
            h.assertValueEqual(handler.getStackInSlot(damage).getCount(), 1, "one to a compartment");
            h.assertTrue(
                    ItemStack.matches(handler.getStackInSlot(damage), worn(damage)),
                    "slot " + damage + " keeps its own wear");
            h.assertValueEqual(store.count(worn(damage)), 1, "and counts it apart");
        }
        h.assertValueEqual(store.insert(worn(2), false).getCount(), 0, "an equal pickaxe joins its kind");
        h.assertValueEqual(handler.getStackInSlot(2).getCount(), 2, "so the third compartment holds two");
        h.assertValueEqual(store.insert(worn(9), false).getCount(), 1, "a fifth wear has no compartment");
        ItemStack taken = handler.extractItem(2, 1, false);
        h.assertTrue(ItemStack.matches(taken, worn(2)), "extraction gives the stack as it was stored, wear and all");
        h.assertTrue(
                ItemStack.isSameItemSameComponents(handler.extractItem(1, 1, true), worn(1)),
                "a simulated take shows the same");
        // names are components too: the same item named A and B are two kinds, an unnamed one a third
        BlockPos names = aloneCraw(h, new BlockPos(2, 1, 2), 0);
        ItemStore named = store(h, names);
        named.insert(named(Items.DIRT, 3, "A"), false);
        named.insert(named(Items.DIRT, 3, "B"), false);
        named.insert(stack(Items.DIRT, 3), false);
        h.assertValueEqual(items(h, names).getStackInSlot(0).getCount(), 3, "A");
        h.assertValueEqual(items(h, names).getStackInSlot(1).getCount(), 3, "B");
        h.assertValueEqual(items(h, names).getStackInSlot(2).getCount(), 3, "and plain");
        h.assertValueEqual(named.count(named(Items.DIRT, 1, "B")), 3, "a name counts only its own");
        h.assertValueEqual(named.count(stack(Items.DIRT, 1)), 3, "plain dirt counts only plain dirt");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBulkPodBesideACrawKeepsComponentKindsSeparateInTheLedger(GameTestHelper h) {
        plant(h, FORKING);
        for (int i = 0; i < 3; i++) grow(h);
        List<BlockPos> pods = pods(h);
        treat(h, pods.getFirst(), gel(1)).closeContainer();
        ItemStore store = store(h, ROOT);
        store.insert(named(Items.BONE, 20, "Fresh"), false);
        store.insert(named(Items.BONE, 20, "Old"), false);
        store.insert(stack(Items.BONE, 20), false);
        h.assertValueEqual(store.count(named(Items.BONE, 1, "Fresh")), 20, "fresh bones");
        h.assertValueEqual(store.count(named(Items.BONE, 1, "Old")), 20, "old bones");
        h.assertValueEqual(store.count(stack(Items.BONE, 1)), 20, "plain bones");
        h.assertValueEqual(store.stored(), 60L, "every item once");
        h.succeed();
    }

    // ---- hoppers and Mouths ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 300)
    public static void aHopperFillsACrawToItsFourKindsAndKeepsTheFifth(GameTestHelper h) {
        BlockPos craw = aloneCraw(h, ROOT.below(), 0);
        h.setBlock(craw.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = (HopperBlockEntity) h.getBlockEntity(craw.above());
        for (int wear = 0; wear < 5; wear++) hopper.setItem(wear, worn(wear));
        h.runAfterDelay(120, () -> {
            h.assertValueEqual(store(h, craw).stored(), 4L, "four pickaxes went in, one to a compartment");
            int left = 0;
            for (int slot = 0; slot < hopper.getContainerSize(); slot++)
                left += hopper.getItem(slot).getCount();
            h.assertValueEqual(left, 1, "the fifth kind stays in the hopper");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void anIntakeMouthFillsACrawToItsRoomAndLeavesTheRestInTheChest(GameTestHelper h) {
        BlockPos intake = new BlockPos(4, 2, 4);
        BlockPos craw = intake.above();
        h.setBlock(intake, LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, MouthMode.INTAKE));
        h.setBlock(
                craw,
                StorageModule.MIXED_CRAW.get().defaultBlockState().setValue(ItemCapsuleBlock.FACING, Direction.NORTH));
        BlockPos source = new BlockPos(4, 2, 8);
        h.setBlock(source, LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, MouthMode.OUTPUT));
        for (int z = 5; z <= 7; z++) h.setBlock(new BlockPos(4, 2, z), LogisticsModule.ITEM_VEIN.get());
        h.setBlock(source.above(), Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) h.getBlockEntity(source.above());
        List<Item> kinds = kinds(6);
        for (int i = 0; i < 6; i++) chest.setItem(i, stack(kinds.get(i), 5));
        h.runAfterDelay(300, () -> {
            h.assertValueEqual(store(h, craw).stored(), 20L, "four kinds of five went in");
            int left = 0;
            for (int slot = 0; slot < chest.getContainerSize(); slot++)
                left += chest.getItem(slot).getCount();
            h.assertValueEqual(left, 10, "the other two kinds waited, whole, in the chest");
            h.assertValueEqual(total(store(h, craw), kinds) + left, 30, "every item is somewhere once");
            h.succeed();
        });
    }

    // ---- full trees ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void aTreeOfSixteenCrawsHoldsSixtyFourKindsAndRefusesTheNextUnchanged(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        List<BlockPos> pods = pods(h);
        for (BlockPos pod : pods) treat(h, pod, gel(1)).closeContainer();
        int slots = pods.size() * PodKind.CRAW_COMPARTMENTS;
        IItemHandler tree = items(h, ROOT);
        h.assertValueEqual(tree.getSlots(), slots, "four compartments a pod, at most sixty-four in all");
        h.assertTrue(slots <= 168, "inside the declared bound of compartments");
        ItemStore store = store(h, ROOT);
        List<Item> kinds = kinds(slots + 1);
        for (int i = 0; i < slots; i++) store.insert(stack(kinds.get(i), 64), false);
        h.assertValueEqual(store.stored(), 64L * slots, "every compartment holds a stack");
        h.assertValueEqual(store.capacity(), 64L * slots, "and the tree is exactly full");
        CompoundTag before = saved(h, h.getBlockEntity(ROOT));
        List<CompoundTag> podsBefore = new ArrayList<>();
        for (BlockPos pod : pods) podsBefore.add(saved(h, h.getBlockEntity(pod)));
        for (int i = 0; i <= slots; i++) {
            ItemStack offered = stack(kinds.get(i), 64);
            h.assertValueEqual(store.insert(offered, true).getCount(), 64, "simulated, kind " + i + " is refused");
            h.assertValueEqual(store.insert(offered, false).getCount(), 64, "really refused");
        }
        for (int slot = 0; slot < slots; slot++)
            h.assertValueEqual(
                    tree.insertItem(slot, stack(tree.getStackInSlot(slot).getItem(), 1), false)
                            .getCount(),
                    1,
                    "slot " + slot + " is full");
        h.assertValueEqual(store.stored(), 64L * slots, "nothing was taken");
        h.assertValueEqual(saved(h, h.getBlockEntity(ROOT)), before, "the root is unchanged");
        for (int i = 0; i < pods.size(); i++)
            h.assertValueEqual(saved(h, h.getBlockEntity(pods.get(i))), podsBefore.get(i), "and so is every pod");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 300)
    public static void aFullCrawStopsAHopperAndAMouthWithoutLoss(GameTestHelper h) {
        BlockPos craw = aloneCraw(h, ROOT.below(), 0);
        ItemStore store = store(h, craw);
        List<Item> kinds = kinds(5);
        for (int i = 0; i < 4; i++) store.insert(stack(kinds.get(i), 64), false);
        h.setBlock(craw.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = (HopperBlockEntity) h.getBlockEntity(craw.above());
        for (int i = 0; i < 5; i++) hopper.setItem(i, stack(kinds.get(i), 10));
        h.runAfterDelay(100, () -> {
            h.assertValueEqual(store.stored(), 256L, "a full Craw took nothing");
            int left = 0;
            for (int slot = 0; slot < hopper.getContainerSize(); slot++)
                left += hopper.getItem(slot).getCount();
            h.assertValueEqual(left, 50, "the hopper still holds every item");
            h.succeed();
        });
    }

    // ---- carrying, breaking, orphaning and saving ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aBrokenCrawCarriesItsStockGradeAndKindAndAPressedOneRejoinsTheTree(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        List<BlockPos> pods = pods(h);
        BlockPos pod = pods.get(1);
        Direction facing = h.getBlockState(pod).getValue(ItemCapsuleBlock.FACING);
        treat(h, pod, stack(EcologyModule.SKELETAL_GRAFT.get(), 1)).closeContainer();
        treat(h, pod, gel(1)).closeContainer();
        h.assertTrue(isCraw(h, pod), "a Craw");
        MenuPlayer screen = open(h, pod);
        List<ItemStack> held =
                List.of(stack(Items.BONE, 2), stack(Items.COAL, 3), worn(4), named(Items.STICK, 2, "Rod"));
        for (int i = 0; i < held.size(); i++)
            screen.containerMenu
                    .getSlot(ItemCapsulePodMenu.COMPARTMENT + i)
                    .set(held.get(i).copy());
        screen.closeContainer();
        ItemStore store = store(h, ROOT);
        long stored = store.stored();
        h.assertValueEqual(stored, 2L + 3 + 1 + 2, "the tree counts the Craw's stock");
        CapsuleStock before = stockOf(h, pod);
        h.assertValueEqual(before.held().size(), 4, "four compartments held");
        h.getLevel().destroyBlock(h.absolutePos(pod), true);
        List<ItemEntity> drops = h.getEntities(EntityType.ITEM, pod, 2.0);
        h.assertValueEqual(drops.size(), 1, "one dropped pod");
        ItemStack dropped = drops.getFirst().getItem();
        h.assertTrue(dropped.is(StorageModule.MIXED_CRAW_ITEM.get()), "a Mixed Craw, not a bulk pod");
        CapsuleStock kept = dropped.get(StorageModule.CAPSULE_STOCK.get());
        h.assertTrue(kept != null && kept.kind() == PodKind.CRAW, "carrying its kind");
        h.assertValueEqual(kept.grade(), 1, "and its grade");
        h.assertTrue(kept.equals(before), "and exactly its stock");
        h.assertValueEqual(dropped.getMaxStackSize(), 1, "a stocked Craw does not stack");
        h.assertValueEqual(store.stored(), 0L, "the tree goes on without it");
        h.assertValueEqual(items(h, ROOT).getSlots(), pods.size() - 1, "and without its compartments");
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.MIXED_CRAW_ITEM.get(), 1);
        builder.setItemInHand(InteractionHand.MAIN_HAND, dropped.copy());
        h.placeAt(builder, builder.getMainHandItem(), pod.relative(facing.getOpposite()), facing);
        h.assertTrue(
                isCraw(h, pod) && h.getBlockState(pod).getValue(ItemCapsuleBlock.FACING) == facing, "pressed back");
        h.assertValueEqual(h.getBlockState(pod).getValue(ItemCapsuleBlock.GRADE), 1, "at its grade");
        h.assertTrue(stockOf(h, pod).equals(before), "with exactly the stock it had");
        h.assertValueEqual(store.stored(), stored, "and the tree counts it again, once");
        h.assertValueEqual(items(h, ROOT).getSlots(), pods.size() + 3, "one bulk pod fewer than the slots of a Craw");
        h.assertValueEqual(store.count(named(Items.STICK, 1, "Rod")), 2, "a name keeps its kind");
        h.assertValueEqual(store.count(worn(4)), 1, "and so does wear");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aCrawOrphanedByABrokenTrunkCellKeepsItsStockAndRejoinsWhenTheCellGrowsBack(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        List<BlockPos> pods = pods(h);
        BlockPos orphan = null;
        for (BlockPos pod : pods) if (pod.getY() > ROOT.getY() + 2) orphan = pod;
        h.assertTrue(orphan != null, "a pod above the cut");
        treat(h, orphan, gel(1)).closeContainer();
        h.assertTrue(isCraw(h, orphan), "a Craw");
        MenuPlayer screen = open(h, orphan);
        screen.containerMenu.getSlot(ItemCapsulePodMenu.COMPARTMENT).set(stack(Items.BONE, 7));
        screen.containerMenu.getSlot(ItemCapsulePodMenu.COMPARTMENT + 1).set(worn(3));
        screen.closeContainer();
        int slots = items(h, ROOT).getSlots();
        BlockPos cut = ROOT.above(2);
        h.getLevel().destroyBlock(h.absolutePos(cut), true);
        h.assertValueEqual(items(h, orphan).getSlots(), 4, "an orphaned Craw is a store of its own, four compartments");
        h.assertValueEqual(store(h, orphan).count(stack(Items.BONE, 1)), 7, "with its bones");
        h.assertValueEqual(store(h, orphan).count(worn(3)), 1, "and its pickaxe");
        h.assertTrue(items(h, ROOT).getSlots() < slots, "the root no longer reaches it");
        h.assertValueEqual(store(h, ROOT).count(stack(Items.BONE, 1)), 0, "so the tree does not count it");
        h.assertTrue(grow(h) && grow(h), "two bone meal regrow the cell in place");
        h.assertValueEqual(items(h, ROOT).getSlots(), slots, "every compartment is reached again");
        h.assertValueEqual(store(h, ROOT).count(stack(Items.BONE, 1)), 7, "the bones once, not twice");
        h.assertValueEqual(store(h, ROOT).count(worn(3)), 1, "and the pickaxe");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aBrokenRootShedsCrawsWithTheirStockAsItems(GameTestHelper h) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        plant(h, FORKING);
        growAll(h);
        List<BlockPos> pods = pods(h);
        int cells = 0;
        for (int y = 1; y < 12; y++)
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++)
                    if (h.getBlockState(new BlockPos(x, y, z)).is(StorageModule.CAPSULE_STEM.get())) cells++;
        for (int i = 0; i < 3; i++) treat(h, pods.get(i), gel(1)).closeContainer();
        List<Item> kinds = kinds(12);
        ItemStore store = store(h, ROOT);
        for (int i = 0; i < 12; i++) store.insert(stack(kinds.get(i), 5 + i), false);
        store.insert(worn(5), false);
        h.getLevel().destroyBlock(h.absolutePos(ROOT), true);
        h.runAfterDelay(4 * cells + 10, () -> {
            int craws = 0;
            int pickaxes = 0;
            int[] counts = new int[12];
            for (ItemEntity entity : h.getEntities(EntityType.ITEM)) {
                ItemStack item = entity.getItem();
                if (item.is(StorageModule.MIXED_CRAW_ITEM.get())) craws += item.getCount();
                CapsuleStock carried = item.get(StorageModule.CAPSULE_STOCK.get());
                if (carried == null) continue;
                for (ItemStack held : carried.held()) {
                    if (held.is(Items.DIAMOND_PICKAXE)) pickaxes += held.getCount();
                    for (int i = 0; i < 12; i++) if (held.is(kinds.get(i))) counts[i] += held.getCount();
                }
            }
            h.assertValueEqual(craws, 3, "the three Craws are shed as Craws");
            h.assertValueEqual(pods(h).size(), 0, "no pod stands");
            for (int i = 0; i < 12; i++)
                h.assertValueEqual(counts[i], 5 + i, "every item of kind " + i + " is carried by a shed pod");
            h.assertValueEqual(pickaxes, 1, "and the pickaxe");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCrawSavedToBytesLoadsBackWithItsKindStockAndGradeExactly(GameTestHelper h) {
        BlockPos craw = aloneCraw(h, ROOT.below(), 1);
        ItemStore store = store(h, craw);
        store.insert(stack(Items.BONE, 100), false);
        store.insert(worn(7), false);
        store.insert(named(Items.STICK, 5, "Rod"), false);
        CapsuleStock before = stockOf(h, craw);
        CompoundTag bytes = saved(h, h.getBlockEntity(craw));
        h.assertValueEqual(bytes.getCompound("stock").getString("kind"), "craw", "the kind is saved");
        reload(h, craw);
        h.assertTrue(isCraw(h, craw), "still a Craw");
        h.assertTrue(stockOf(h, craw).equals(before), "exactly the stock it had");
        h.assertValueEqual(saved(h, h.getBlockEntity(craw)), bytes, "and saves the same bytes");
        h.assertValueEqual(items(h, craw).getSlots(), 4, "four compartments");
        h.assertValueEqual(store(h, craw).count(stack(Items.BONE, 1)), 100, "a hundred bones over two compartments");
        reload(h, craw);
        h.assertValueEqual(saved(h, h.getBlockEntity(craw)), bytes, "a second reload changes nothing");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aSavedTreeWithCrawsLoadsBackWhole(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        List<BlockPos> pods = pods(h);
        for (int i = 0; i < 4; i++) treat(h, pods.get(i), gel(1)).closeContainer();
        ItemStore store = store(h, ROOT);
        List<Item> kinds = kinds(8);
        for (int i = 0; i < 8; i++) store.insert(stack(kinds.get(i), 17 + i), false);
        int slots = items(h, ROOT).getSlots();
        long stored = store.stored();
        for (BlockPos pod : pods) reload(h, pod);
        reload(h, ROOT);
        h.assertValueEqual(items(h, ROOT).getSlots(), slots, "the same compartments");
        h.assertValueEqual(store(h, ROOT).stored(), stored, "the same items");
        for (int i = 0; i < 8; i++)
            h.assertValueEqual(store(h, ROOT).count(stack(kinds.get(i), 1)), 17 + i, "kind " + i + " is whole");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anOldBulkPodLoadsAsBulkAndAMismatchedKindIsKeptUnreadable(GameTestHelper h) {
        BlockPos bulk = alone(h, ROOT.below(), 1);
        store(h, bulk).insert(stack(Items.BONE, 30), false);
        CompoundTag old = saved(h, h.getBlockEntity(bulk));
        old.getCompound("stock").remove("kind"); // a save from before pods had kinds
        h.getBlockEntity(bulk).loadWithComponents(old, h.getLevel().registryAccess());
        h.assertTrue(isBulk(h, bulk), "a bulk pod");
        h.assertValueEqual(items(h, bulk).getSlots(), 1, "one compartment");
        h.assertValueEqual(store(h, bulk).count(stack(Items.BONE, 1)), 30, "with its bones");
        // the same bytes claiming to be a Craw's are refused by a bulk pod, whole
        CompoundTag claim = saved(h, h.getBlockEntity(bulk));
        claim.getCompound("stock").putString("kind", "craw");
        h.getBlockEntity(bulk).loadWithComponents(claim, h.getLevel().registryAccess());
        h.assertValueEqual(items(h, bulk).getSlots(), 0, "a bulk pod refuses a Craw's stock");
        h.assertTrue(
                h.getBlockEntity(bulk).collectComponents().get(OrganModule.UNREADABLE_SAVE.get()) != null,
                "and keeps the bytes");
        BlockPos craw = aloneCraw(h, new BlockPos(2, 1, 2), 0);
        CompoundTag crawBytes = saved(h, h.getBlockEntity(craw));
        crawBytes.getCompound("stock").putString("kind", "bulk");
        h.getBlockEntity(craw).loadWithComponents(crawBytes, h.getLevel().registryAccess());
        h.assertValueEqual(items(h, craw).getSlots(), 0, "a Craw refuses a bulk pod's stock");
        CompoundTag future = saved(h, h.getBlockEntity(craw));
        future.getCompound("stock").putString("kind", "larder");
        h.getBlockEntity(craw).loadWithComponents(future, h.getLevel().registryAccess());
        h.assertValueEqual(items(h, craw).getSlots(), 0, "a kind it does not know is unreadable");
        BlockPos second = aloneCraw(h, new BlockPos(2, 1, 8), 0);
        store(h, second).insert(stack(Items.BONE, 3), false);
        CompoundTag good = saved(h, h.getBlockEntity(second));
        h.getBlockEntity(second).loadWithComponents(good, h.getLevel().registryAccess());
        h.assertValueEqual(store(h, second).count(stack(Items.BONE, 1)), 3, "a well-formed Craw loads");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCrawPastItsDepthOrWithAFifthCompartmentIsKeptUnreadable(GameTestHelper h) {
        BlockPos craw = aloneCraw(h, ROOT.below(), 0);
        store(h, craw).insert(stack(Items.BONE, 64), false);
        CompoundTag tag = saved(h, h.getBlockEntity(craw));
        ListTag held = tag.getCompound("stock").getList("held", 10);
        held.getCompound(0).putInt("count", 65); // a compartment of a young Craw is one stack
        h.getBlockEntity(craw).loadWithComponents(tag, h.getLevel().registryAccess());
        h.assertValueEqual(items(h, craw).getSlots(), 0, "past its depth: unreadable");
        BlockPos bulk = alone(h, new BlockPos(2, 1, 2), 0);
        store(h, bulk).insert(stack(Items.BONE, 64), false);
        CompoundTag two = saved(h, h.getBlockEntity(bulk));
        ListTag crowd = two.getCompound("stock").getList("held", 10);
        CompoundTag other = crowd.getCompound(0).copy();
        other.putInt("slot", 1);
        crowd.add(other);
        h.getBlockEntity(bulk).loadWithComponents(two, h.getLevel().registryAccess());
        h.assertValueEqual(items(h, bulk).getSlots(), 0, "a second compartment in a bulk pod: unreadable");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aStockedCrawOrPodIsRefusedByEveryPodAndAnEmptyOneIsNot(GameTestHelper h) {
        BlockPos craw = aloneCraw(h, ROOT.below(), 0);
        BlockPos bulk = alone(h, new BlockPos(2, 1, 2), 0);
        ItemStack filled = new ItemStack(StorageModule.MIXED_CRAW_ITEM.get());
        filled.set(StorageModule.CAPSULE_STOCK.get(), new CapsuleStock(List.of(stack(Items.BONE, 3)), 0, PodKind.CRAW));
        for (BlockPos target : List.of(craw, bulk)) {
            ItemStore store = store(h, target);
            h.assertValueEqual(store.insert(filled, false).getCount(), 1, "a filled Craw never nests");
            h.assertValueEqual(store.insert(filled, true).getCount(), 1, "simulated too");
            h.assertValueEqual(store.stored(), 0L, "nothing went in");
        }
        h.assertFalse(
                StorageModule.MIXED_CRAW_ITEM.get().canFitInsideContainerItems(), "and it fits in no bundle or box");
        h.assertTrue(
                store(h, craw)
                        .insert(new ItemStack(StorageModule.MIXED_CRAW_ITEM.get()), false)
                        .isEmpty(),
                "a bare one is only an item");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCrawScreenHasFourCompartmentsAndShiftClickFillsThemInOrderOneKindEach(GameTestHelper h) {
        BlockPos craw = aloneCraw(h, ROOT.below(), 0);
        MenuPlayer player = open(h, craw);
        AbstractContainerMenu menu = player.containerMenu;
        h.assertTrue(menu instanceof ItemCapsulePodMenu, "a Craw opens a pod screen");
        h.assertValueEqual(menu.slots.size(), 1 + 4 + 36, "treatment well, four compartments, the player's 36");
        int first = 1 + 4;
        player.getInventory().setItem(0, stack(Items.DIRT, 100));
        player.getInventory().setItem(1, stack(Items.STONE, 10));
        player.getInventory().setItem(2, gel(2));
        menu.clicked(first + 27, 0, ClickType.QUICK_MOVE, player);
        menu.clicked(first + 27 + 1, 0, ClickType.QUICK_MOVE, player);
        menu.clicked(first + 27 + 2, 0, ClickType.QUICK_MOVE, player);
        h.assertValueEqual(
                menu.getSlot(ItemCapsulePodMenu.COMPARTMENT).getItem().getCount(),
                64,
                "the first compartment: a stack");
        h.assertValueEqual(
                menu.getSlot(ItemCapsulePodMenu.COMPARTMENT + 1).getItem().getCount(), 36, "dirt goes on to the next");
        h.assertTrue(
                menu.getSlot(ItemCapsulePodMenu.COMPARTMENT + 2).getItem().is(Items.STONE),
                "another kind takes the third");
        h.assertTrue(
                menu.getSlot(ItemCapsulePodMenu.COMPARTMENT + 3).getItem().is(ProcessingModule.ELASTIC_GEL.get()),
                "a gel in a Craw is stock, in the fourth");
        h.assertValueEqual(store(h, craw).stored(), 64L + 36 + 10 + 2, "and the store agrees");
        menu.clicked(ItemCapsulePodMenu.COMPARTMENT, 0, ClickType.PICKUP, player);
        h.assertValueEqual(menu.getCarried().getCount(), 64, "a click takes one stack");
        player.closeContainer();
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theCrawAndItsGelAreDiscoverableByTagAndRecipe(GameTestHelper h) {
        h.assertTrue(stack(ProcessingModule.ELASTIC_GEL.get(), 1).is(StorageModule.CRAW_GEL), "Elastic Gel is the gel");
        h.assertFalse(
                stack(ProcessingModule.FUSION_BINDER.get(), 1).is(StorageModule.CRAW_GEL), "Fusion Binder is not");
        h.assertTrue(
                new ItemStack(StorageModule.MIXED_CRAW_ITEM.get()).getItem() != StorageModule.ITEM_CAPSULE_ITEM.get(),
                "the Craw is an item of its own");
        h.succeed();
    }

    // ---- real chunk save and load ----

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "capsule_unload",
            timeoutTicks = 1200)
    public static void aCrawInATreeSurvivesItsChunkLeavingAndComingBack(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        BlockPos anchor = new BlockPos((origin.getX() & ~15) + 192000, 80, (origin.getZ() & ~15) + 224200);
        BlockPos root = anchor.east(47);
        BlockPos east = root.above().east();
        ChunkPos kept = new ChunkPos(anchor);
        ChunkPos near = new ChunkPos(root);
        ChunkPos far = new ChunkPos(east);
        TestChunks.force(level, kept);
        TestChunks.force(level, near);
        TestChunks.force(level, far);
        level.setBlockAndUpdate(
                root.below(), EcologyModule.LIVING_SUBSTRATE.get().defaultBlockState());
        CapsuleStemBlock.plant(level, root, EASTWARD);
        for (int i = 0; i < 3; i++) BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, root);
        h.assertTrue(level.getBlockState(east).is(StorageModule.ITEM_CAPSULE.get()), "seed 8 grows its first pod east");
        IItemHandler[] port = new IItemHandler[1];
        CapsuleStock[] before = new CapsuleStock[1];
        h.startSequence()
                .thenExecute(() -> {
                    port[0] = level.getCapability(Capabilities.ItemHandler.BLOCK, root, Direction.UP);
                    h.assertValueEqual(port[0].getSlots(), 2, "two pods");
                    FakePlayer player = new MenuPlayer(level);
                    player.moveTo(east.getX() + 0.5, east.getY(), east.getZ() - 1.5);
                    player.openMenu((MenuProvider) level.getBlockEntity(east), east);
                    player.containerMenu.setCarried(gel(1));
                    player.containerMenu.clicked(ItemCapsulePodMenu.TREATMENT, 0, ClickType.PICKUP, player);
                    h.assertTrue(level.getBlockState(east).is(StorageModule.MIXED_CRAW.get()), "mutated");
                    port[0] = level.getCapability(Capabilities.ItemHandler.BLOCK, root, Direction.UP);
                    h.assertValueEqual(port[0].getSlots(), 5, "one compartment and four");
                    ItemStore store = (ItemStore) port[0];
                    store.insert(stack(Items.BONE, 40), false);
                    store.insert(stack(Items.COAL, 20), false);
                    store.insert(named(Items.STICK, 9, "Rod"), false);
                    store.insert(worn(2), false);
                    before[0] = ((CapsuleStock)
                            level.getBlockEntity(east).collectComponents().get(StorageModule.CAPSULE_STOCK.get()));
                    h.assertTrue(before[0] != null && before[0].kind() == PodKind.CRAW, "the Craw holds stock");
                    level.setChunkForced(far.x, far.z, false);
                    level.setChunkForced(near.x, near.z, false); // the kept chunk's ring holds the root's chunk
                })
                .thenWaitUntil(() -> h.assertFalse(level.hasChunkAt(east), "the Craw's chunk is gone"))
                .thenExecute(() -> {
                    TestChunks.force(level, near);
                    TestChunks.force(level, far);
                })
                .thenWaitUntil(() -> h.assertTrue(level.hasChunkAt(east), "and back"))
                .thenWaitUntil(() -> h.assertValueEqual(
                        level.getCapability(Capabilities.ItemHandler.BLOCK, root, Direction.UP)
                                .getSlots(),
                        5,
                        "the tree reads the Craw's four compartments again"))
                .thenExecute(() -> {
                    h.assertTrue(level.getBlockState(east).is(StorageModule.MIXED_CRAW.get()), "still a Craw");
                    CapsuleStock after =
                            level.getBlockEntity(east).collectComponents().get(StorageModule.CAPSULE_STOCK.get());
                    h.assertTrue(before[0].equals(after), "exactly the stock it had");
                    h.assertValueEqual(
                            ((ItemStore) level.getCapability(Capabilities.ItemHandler.BLOCK, root, Direction.UP))
                                    .count(stack(Items.BONE, 1)),
                            40,
                            "counted once by the tree");
                    for (BlockPos pos : BlockPos.betweenClosed(root.offset(-2, -1, -2), root.offset(2, 7, 2)))
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    level.setChunkForced(kept.x, kept.z, false);
                    level.setChunkForced(near.x, near.z, false);
                    level.setChunkForced(far.x, far.z, false);
                })
                .thenSucceed();
    }

    /** A server player outside the player list that opens menus without a client, as in the stem tests. */
    private static final class MenuPlayer extends FakePlayer {
        MenuPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.fromString("5b1d2c3e-4b5f-4a6b-8c7d-9e0f1a2b3c4d"), "craw_tests"));
        }

        @Override
        public OptionalInt openMenu(MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extra) {
            containerMenu = provider.createMenu(1, getInventory(), this);
            return OptionalInt.of(1);
        }
    }
}
