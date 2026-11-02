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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
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
import org.jd.infestusfrontier.logistics.MouthMenu;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.logistics.api.LogisticsCapabilities;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.CapsuleStemBlock;
import org.jd.infestusfrontier.storage.ItemCapsuleBlock;
import org.jd.infestusfrontier.storage.ItemCapsuleMenu;
import org.jd.infestusfrontier.storage.ItemCapsulePodMenu;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.CapsuleStock;
import org.jd.infestusfrontier.storage.api.ItemStore;

/**
 * The Larder: an empty Mixed Craw turned by one Sealing Resin into three labelled compartments that each keep a
 * reserve. Every way stock leaves a pod from outside (a hopper, a Mouth, a handler on any side, the ledger's click, a
 * simulation followed by a competing extraction) gives a compartment's count less its reserve and nothing more;
 * insertion is never limited; only the screen's Withdraw reserve takes the reserved part. Labels, reserves and stock
 * survive break, carry, shedding, orphaning, saves and a chunk leaving. Roots stand at (8, 2, 8).
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class LarderTests {
    static final BlockPos ROOT = new BlockPos(8, 2, 8);
    /** Seed 7 forks east at tier 3, as in the stem tests. */
    private static final long FORKING = 7;

    private static final long EASTWARD = 8;
    private static final int RESERVE_UP = ItemCapsulePodMenu.RESERVE_UP * ItemCapsulePodMenu.BUTTON_STEP;
    private static final int RESERVE_DOWN = ItemCapsulePodMenu.RESERVE_DOWN * ItemCapsulePodMenu.BUTTON_STEP;
    private static final int WITHDRAW = ItemCapsulePodMenu.WITHDRAW * ItemCapsulePodMenu.BUTTON_STEP;
    /** The first label well of a Larder's screen: treatment well, three compartments, then three labels. */
    private static final int LABELS = ItemCapsulePodMenu.COMPARTMENT + PodKind.LARDER_COMPARTMENTS;

    private LarderTests() {}

    // ---- helpers ----

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

    private static boolean isPod(GameTestHelper h, BlockPos pos) {
        return h.getBlockState(pos).is(StorageModule.ITEM_CAPSULE.get())
                || h.getBlockState(pos).is(StorageModule.MIXED_CRAW.get())
                || h.getBlockState(pos).is(StorageModule.LARDER.get());
    }

    private static boolean isLarder(GameTestHelper h, BlockPos pos) {
        return h.getBlockState(pos).is(StorageModule.LARDER.get());
    }

    /** Every pod of the tree, in the order the template is walked. */
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

    static IItemHandler items(GameTestHelper h, BlockPos pos) {
        return h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), Direction.UP);
    }

    private static IItemHandler items(GameTestHelper h, BlockPos pos, Direction side) {
        return h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), side);
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

    private static ItemStack resin(int count) {
        return new ItemStack(LogisticsModule.SEALING_RESIN.get(), count);
    }

    private static ItemStack graft(int count) {
        return new ItemStack(EcologyModule.SKELETAL_GRAFT.get(), count);
    }

    private static MenuPlayer open(GameTestHelper h, BlockPos pod) {
        MenuPlayer player = new MenuPlayer(h.getLevel());
        BlockPos at = h.absolutePos(pod);
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        player.openMenu((MenuProvider) h.getBlockEntity(pod), at);
        return player;
    }

    private static MenuPlayer treat(GameTestHelper h, BlockPos pod, ItemStack treatment) {
        MenuPlayer player = open(h, pod);
        player.containerMenu.setCarried(treatment);
        player.containerMenu.clicked(ItemCapsulePodMenu.TREATMENT, 0, ClickType.PICKUP, player);
        return player;
    }

    /** A pod of {@code block} standing at {@code pod}, facing north, with no support: nothing here needs any. */
    private static BlockPos pod(
            GameTestHelper h, BlockPos pod, net.minecraft.world.level.block.Block block, int grade) {
        h.setBlock(
                pod,
                block.defaultBlockState()
                        .setValue(ItemCapsuleBlock.GRADE, grade)
                        .setValue(ItemCapsuleBlock.FACING, Direction.NORTH));
        return pod;
    }

    private static BlockPos larder(GameTestHelper h, BlockPos pod, int grade) {
        return pod(h, pod, StorageModule.LARDER.get(), grade);
    }

    static BlockPos aloneLarder(GameTestHelper h, int grade) {
        h.setBlock(ROOT.below(), Blocks.STONE);
        return larder(h, ROOT, grade);
    }

    private static CapsuleStock stockOf(GameTestHelper h, BlockPos pod) {
        CapsuleStock stock = h.getBlockEntity(pod).collectComponents().get(StorageModule.CAPSULE_STOCK.get());
        return stock == null ? CapsuleStock.EMPTY : stock;
    }

    private static CompoundTag saved(GameTestHelper h, BlockEntity entity) {
        return entity.saveWithFullMetadata(h.getLevel().registryAccess());
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
            h.fail("could not save and read the pod: " + e.getMessage());
        }
    }

    /** Steps compartment {@code compartment}'s reserve at the pod's screen to {@code wanted}, up through the settings. */
    static void reserve(GameTestHelper h, BlockPos pod, int compartment, int wanted) {
        MenuPlayer player = open(h, pod);
        for (int i = 0; i < 16 && stockOf(h, pod).mark(compartment).reserve() != wanted; i++)
            player.containerMenu.clickMenuButton(player, RESERVE_UP + compartment);
        player.closeContainer();
        h.assertValueEqual(
                stockOf(h, pod).mark(compartment).reserve(), wanted, "reserve of compartment " + compartment);
    }

    /** Labels compartment {@code compartment} with {@code item} by clicking it on the label well. */
    static void label(GameTestHelper h, BlockPos pod, int compartment, Item item) {
        MenuPlayer player = open(h, pod);
        player.containerMenu.setCarried(stack(item, 1));
        player.containerMenu.clicked(LABELS + compartment, 0, ClickType.PICKUP, player);
        player.closeContainer();
        h.assertTrue(stockOf(h, pod).mark(compartment).label().is(item), "labelled " + item);
    }

    static int reserveOf(GameTestHelper h, BlockPos pod, int compartment) {
        return stockOf(h, pod).mark(compartment).reserve();
    }

    private static List<Item> kinds(int count) {
        List<Item> kinds = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (kinds.size() == count) break;
            ItemStack probe = new ItemStack(item);
            if (probe.getMaxStackSize() == 64
                    && !probe.isEmpty()
                    && !probe.is(StorageModule.ITEM_CAPSULE_ITEM.get())
                    && !probe.is(StorageModule.MIXED_CRAW_ITEM.get())
                    && !probe.is(StorageModule.LARDER_ITEM.get())) kinds.add(item);
        }
        if (kinds.size() != count) throw new IllegalStateException("the registry has too few 64-stack items");
        return kinds;
    }

    /** What every handler slot of {@code pod} gives to an outside caller that asks for a lot, for real. */
    private static int drain(GameTestHelper h, BlockPos pod, Direction side, ItemStack like) {
        IItemHandler handler = items(h, pod, side);
        int got = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++)
            for (int i = 0; i < 64; i++) {
                if (!ItemStack.isSameItemSameComponents(handler.getStackInSlot(slot), like)) break;
                ItemStack taken = handler.extractItem(slot, 64, false);
                if (taken.isEmpty()) break;
                got += taken.getCount();
            }
        return got;
    }

    // ---- the mutation ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void oneSealingResinTurnsAnEmptyCrawIntoALarderKeepingGradeFacingAndTree(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        List<BlockPos> pods = pods(h);
        int before = items(h, ROOT).getSlots();
        BlockPos pod = pods.get(pods.size() / 2);
        Direction facing = h.getBlockState(pod).getValue(ItemCapsuleBlock.FACING);
        treat(h, pod, graft(1)).closeContainer();
        treat(h, pod, gel(1)).closeContainer();
        h.assertTrue(h.getBlockState(pod).is(StorageModule.MIXED_CRAW.get()), "a Craw first");
        h.assertValueEqual(items(h, ROOT).getSlots(), before + 3, "four compartments");
        MenuPlayer resined = treat(h, pod, resin(3));
        h.assertTrue(isLarder(h, pod), "the Craw is a Larder");
        h.assertValueEqual(h.getBlockState(pod).getValue(ItemCapsuleBlock.GRADE), 1, "it keeps its grade");
        h.assertValueEqual(h.getBlockState(pod).getValue(ItemCapsuleBlock.FACING), facing, "and its facing");
        h.assertValueEqual(items(h, ROOT).getSlots(), before + 2, "the tree reads its three compartments");
        h.assertValueEqual(items(h, pod).getSlots(), before + 2, "from the pod too: one tree");
        resined.closeContainer();
        h.assertValueEqual(resined.getInventory().countItem(LogisticsModule.SEALING_RESIN.get()), 2, "one resin spent");
        h.assertValueEqual(pods(h).size(), pods.size(), "no pod was added or lost");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aPodThatIsNotAnEmptyCrawRefusesTheResinUnchanged(GameTestHelper h) {
        BlockPos bulk = pod(h, new BlockPos(2, 2, 2), StorageModule.ITEM_CAPSULE.get(), 0);
        BlockPos craw = pod(h, new BlockPos(6, 2, 2), StorageModule.MIXED_CRAW.get(), 0);
        BlockPos larder = larder(h, new BlockPos(10, 2, 2), 0);
        store(h, craw).insert(stack(Items.DIRT, 5), false);
        CompoundTag bulkBefore = saved(h, h.getBlockEntity(bulk));
        CompoundTag crawBefore = saved(h, h.getBlockEntity(craw));
        CompoundTag larderBefore = saved(h, h.getBlockEntity(larder));
        MenuPlayer onBulk = treat(h, bulk, resin(1));
        h.assertValueEqual(onBulk.containerMenu.getCarried().getCount(), 1, "a bulk pod's well never takes resin");
        MenuPlayer onLarder = treat(h, larder, resin(1));
        h.assertValueEqual(onLarder.containerMenu.getCarried().getCount(), 1, "nor does a Larder's");
        MenuPlayer onCraw = treat(h, craw, resin(1));
        h.assertTrue(craw.equals(craw) && !isLarder(h, craw), "a Craw that holds items is not turned");
        h.assertValueEqual(
                onCraw.containerMenu
                        .getSlot(ItemCapsulePodMenu.TREATMENT)
                        .getItem()
                        .getCount(),
                1,
                "the resin stays in its well");
        h.assertValueEqual(saved(h, h.getBlockEntity(bulk)), bulkBefore, "the bulk pod saves as before");
        h.assertValueEqual(saved(h, h.getBlockEntity(craw)), crawBefore, "the Craw saves as before");
        h.assertValueEqual(saved(h, h.getBlockEntity(larder)), larderBefore, "the Larder saves as before");
        onCraw.closeContainer();
        h.assertValueEqual(
                onCraw.getInventory().countItem(LogisticsModule.SEALING_RESIN.get()), 1, "and comes back on closing");
        h.assertValueEqual(store(h, craw).count(stack(Items.DIRT, 1)), 5, "the Craw keeps its dirt");
        items(h, craw).extractItem(0, 5, false);
        treat(h, craw, resin(1)).closeContainer();
        h.assertTrue(isLarder(h, craw), "emptied, the same Craw takes the resin");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anUnreadableCrawRefusesTheResinAndKeepsItsBytes(GameTestHelper h) {
        BlockPos craw = pod(h, ROOT, StorageModule.MIXED_CRAW.get(), 0);
        BlockEntity entity = h.getBlockEntity(craw);
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
        MenuPlayer player = open(h, craw);
        player.containerMenu.setCarried(resin(1));
        player.containerMenu.clicked(ItemCapsulePodMenu.TREATMENT, 0, ClickType.PICKUP, player);
        h.assertFalse(isLarder(h, craw), "not mutated");
        h.assertTrue(
                h.getBlockEntity(craw).collectComponents().get(OrganModule.UNREADABLE_SAVE.get()) != null,
                "and its bytes are kept");
        player.closeContainer();
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aLarderHoldsThreeKindsAtTheCrawsDepthAtEveryGrade(GameTestHelper h) {
        for (int grade = 0; grade <= 2; grade++) {
            BlockPos larder = larder(h, new BlockPos(2 + 4 * grade, 2, 2), grade);
            IItemHandler handler = items(h, larder);
            h.assertValueEqual(handler.getSlots(), 3, "three compartments at grade " + grade);
            List<Item> kinds = kinds(4);
            for (int i = 0; i < 3; i++) {
                h.assertValueEqual(
                        store(h, larder)
                                .insert(stack(kinds.get(i), 64 << grade), false)
                                .getCount(),
                        0,
                        "a kind fills one compartment's depth at grade " + grade);
                h.assertValueEqual(handler.getSlotLimit(i), 64 << grade, "a Craw's depth at grade " + grade);
            }
            h.assertValueEqual(
                    store(h, larder).insert(stack(kinds.get(3), 1), false).getCount(), 1, "a fourth kind is refused");
            h.assertValueEqual(store(h, larder).stored(), 3L * (64 << grade), "three full compartments");
        }
        h.succeed();
    }

    // ---- labels ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theFirstItemLabelsACompartmentAndTheLabelStaysWhenItEmpties(GameTestHelper h) {
        BlockPos larder = aloneLarder(h, 1);
        h.assertTrue(stockOf(h, larder).mark(0).isNone(), "a new Larder has no label");
        store(h, larder).insert(stack(Items.IRON_INGOT, 10), false);
        h.assertTrue(stockOf(h, larder).mark(0).label().is(Items.IRON_INGOT), "the first item labels it");
        h.assertValueEqual(drain(h, larder, Direction.UP, stack(Items.IRON_INGOT, 1)), 10, "emptied by a handler");
        h.assertValueEqual(store(h, larder).stored(), 0L, "the compartment is empty");
        h.assertTrue(stockOf(h, larder).mark(0).label().is(Items.IRON_INGOT), "its label stays");
        h.assertFalse(stockOf(h, larder).isBare(), "an empty labelled Larder is worth carrying");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aLabelledCompartmentAcceptsItsKindAloneFromEveryPath(GameTestHelper h) {
        BlockPos larder = aloneLarder(h, 1);
        List<Item> others = kinds(3);
        // three compartments, all labelled, then emptied: only their own kinds may come back in
        store(h, larder).insert(stack(Items.IRON_INGOT, 1), false);
        store(h, larder).insert(stack(Items.GOLD_INGOT, 1), false);
        store(h, larder).insert(stack(Items.COPPER_INGOT, 1), false);
        drain(h, larder, Direction.UP, stack(Items.IRON_INGOT, 1));
        drain(h, larder, Direction.UP, stack(Items.GOLD_INGOT, 1));
        drain(h, larder, Direction.UP, stack(Items.COPPER_INGOT, 1));
        IItemHandler handler = items(h, larder);
        for (Item other : others) {
            ItemStack offered = stack(other, 5);
            h.assertValueEqual(store(h, larder).insert(offered, true).getCount(), 5, "refused, simulated: " + other);
            h.assertValueEqual(store(h, larder).insert(offered, false).getCount(), 5, "refused: " + other);
            for (int slot = 0; slot < 3; slot++) {
                h.assertValueEqual(handler.insertItem(slot, offered, false).getCount(), 5, "slot refuses " + other);
                h.assertFalse(handler.isItemValid(slot, offered), "and is not valid for " + other);
            }
        }
        h.assertValueEqual(store(h, larder).stored(), 0L, "nothing slipped in");
        h.assertValueEqual(
                store(h, larder).insert(stack(Items.GOLD_INGOT, 5), false).getCount(), 0, "its own kind goes in");
        h.assertTrue(handler.getStackInSlot(1).is(Items.GOLD_INGOT), "into its own compartment, not the first empty");
        // a hopper above sees the same rule
        h.setBlock(larder.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = (HopperBlockEntity) h.getBlockEntity(larder.above());
        hopper.setItem(0, stack(others.get(0), 5));
        hopper.setItem(1, stack(Items.COPPER_INGOT, 3));
        h.runAfterDelay(80, () -> {
            h.assertValueEqual(store(h, larder).count(stack(Items.COPPER_INGOT, 1)), 3, "the hopper's copper went in");
            h.assertValueEqual(store(h, larder).count(stack(others.get(0), 1)), 0, "and its other kind stayed out");
            h.assertValueEqual(hopper.getItem(0).getCount(), 5, "still in the hopper");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aLabelIsSetByClickingAnItemOnItAndClearedOnlyWhileTheCompartmentIsEmpty(GameTestHelper h) {
        BlockPos larder = aloneLarder(h, 0);
        MenuPlayer player = open(h, larder);
        AbstractContainerMenu menu = player.containerMenu;
        h.assertValueEqual(menu.slots.size(), 1 + 3 + 3 + 36, "treatment well, three compartments, three labels, 36");
        menu.setCarried(stack(Items.EMERALD, 7));
        menu.clicked(LABELS + 1, 0, ClickType.PICKUP, player);
        h.assertTrue(stockOf(h, larder).mark(1).label().is(Items.EMERALD), "an item clicked on a label sets it");
        h.assertValueEqual(menu.getCarried().getCount(), 7, "and is not spent");
        h.assertTrue(menu.getSlot(LABELS + 1).getItem().is(Items.EMERALD), "the label well shows it");
        h.assertValueEqual(store(h, larder).insert(stack(Items.DIRT, 4), false).getCount(), 0, "dirt opens another");
        h.assertTrue(stockOf(h, larder).mark(0).label().is(Items.DIRT), "the first empty, unlabelled compartment");
        h.assertValueEqual(
                store(h, larder).insert(stack(Items.EMERALD, 4), false).getCount(),
                0,
                "emerald goes where it is labelled");
        h.assertValueEqual(items(h, larder).getStackInSlot(1).getCount(), 4, "into the second compartment");
        menu.setCarried(stack(Items.DIAMOND, 1));
        menu.clicked(LABELS + 1, 0, ClickType.PICKUP, player);
        h.assertTrue(
                stockOf(h, larder).mark(1).label().is(Items.EMERALD),
                "a compartment that holds emeralds keeps its label");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(LABELS + 1, 0, ClickType.PICKUP, player);
        h.assertTrue(stockOf(h, larder).mark(1).label().is(Items.EMERALD), "and cannot be cleared while it holds");
        drain(h, larder, Direction.UP, stack(Items.EMERALD, 1));
        menu.clicked(LABELS + 1, 0, ClickType.PICKUP, player);
        h.assertTrue(stockOf(h, larder).mark(1).label().isEmpty(), "an empty compartment gives up its label");
        menu.setCarried(stack(Items.DIAMOND, 1));
        menu.clicked(LABELS + 1, 0, ClickType.PICKUP, player);
        h.assertTrue(stockOf(h, larder).mark(1).label().is(Items.DIAMOND), "and takes another");
        // the label of a compartment that holds dirt can be set to dirt, not to anything else
        menu.setCarried(stack(Items.STONE, 1));
        menu.clicked(LABELS, 0, ClickType.PICKUP, player);
        h.assertTrue(stockOf(h, larder).mark(0).label().is(Items.DIRT), "dirt stays");
        player.closeContainer();
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anEmptyLabelledCompartmentTakesItsKindBeforeAnUnlabelledPodInTheTree(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        List<BlockPos> pods = pods(h);
        BlockPos bulk = pods.get(0);
        BlockPos target = pods.get(1);
        treat(h, target, gel(1)).closeContainer();
        treat(h, target, resin(1)).closeContainer();
        label(h, target, 2, Items.IRON_INGOT);
        ItemStore store = store(h, ROOT);
        h.assertValueEqual(store.insert(stack(Items.IRON_INGOT, 9), false).getCount(), 0, "the tree takes the iron");
        h.assertValueEqual(stockOf(h, target).held().get(2).getCount(), 9, "into the compartment labelled for it");
        h.assertTrue(stockOf(h, bulk).isEmpty(), "and no bulk pod took any");
        h.assertValueEqual(drain(h, ROOT, Direction.UP, stack(Items.IRON_INGOT, 1)), 9, "emptied");
        h.assertValueEqual(store.insert(stack(Items.IRON_INGOT, 4), false).getCount(), 0, "refilled");
        h.assertValueEqual(stockOf(h, target).held().get(2).getCount(), 4, "the label held through the empty spell");
        h.assertTrue(stockOf(h, bulk).isEmpty(), "again no bulk pod took any");
        h.assertValueEqual(store.count(stack(Items.IRON_INGOT, 1)), 4, "counted once");
        h.succeed();
    }

    // ---- the reserve on every external path ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void everySideOfAPodGivesOnlyTheCountAboveTheReserve(GameTestHelper h) {
        BlockPos larder = aloneLarder(h, 1);
        ItemStore store = store(h, larder);
        store.insert(stack(Items.DIRT, 100), false);
        reserve(h, larder, 0, 64);
        h.assertValueEqual(store.count(stack(Items.DIRT, 1)), 100, "held");
        h.assertValueEqual(store.automatable(stack(Items.DIRT, 1)), 36, "automatable");
        List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
        sides.add(null);
        for (Direction side : sides) {
            IItemHandler handler = items(h, larder, side);
            h.assertValueEqual(handler.getStackInSlot(0).getCount(), 36, "an outside reader sees 36 from " + side);
            h.assertValueEqual(handler.extractItem(0, 64, true).getCount(), 36, "a simulation offers 36 from " + side);
        }
        h.assertValueEqual(
                items(h, larder, Direction.DOWN).extractItem(0, 20, false).getCount(), 20, "20 for real");
        h.assertValueEqual(
                items(h, larder, Direction.NORTH).extractItem(0, 64, false).getCount(), 16, "then 16");
        for (Direction side : sides) {
            h.assertTrue(items(h, larder, side).extractItem(0, 64, false).isEmpty(), "then nothing from " + side);
            h.assertTrue(items(h, larder, side).getStackInSlot(0).isEmpty(), "and nothing to see from " + side);
        }
        h.assertValueEqual(store.count(stack(Items.DIRT, 1)), 64, "the reserve is untouched");
        h.assertValueEqual(store.automatable(stack(Items.DIRT, 1)), 0, "nothing is automatable");
        h.assertValueEqual(store.stored(), 64L, "the tree agrees");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aSimulationFollowedByACompetingExtractionNeverTakesTheReserve(GameTestHelper h) {
        BlockPos larder = aloneLarder(h, 0);
        store(h, larder).insert(stack(Items.DIRT, 40), false);
        reserve(h, larder, 0, 32);
        IItemHandler hopperSide = items(h, larder, Direction.DOWN);
        IItemHandler pipeSide = items(h, larder, Direction.EAST);
        ItemStack promised = hopperSide.extractItem(0, 64, true);
        h.assertValueEqual(promised.getCount(), 8, "the hopper is promised the free eight");
        ItemStack rival = pipeSide.extractItem(0, 5, false);
        h.assertValueEqual(rival.getCount(), 5, "a competing pipe takes five of them");
        ItemStack kept = hopperSide.extractItem(0, promised.getCount(), false);
        h.assertValueEqual(kept.getCount(), 3, "the hopper's real extraction gets only what is left above the reserve");
        h.assertTrue(pipeSide.extractItem(0, 64, false).isEmpty(), "and then nothing");
        h.assertValueEqual(store(h, larder).count(stack(Items.DIRT, 1)), 32, "the reserve is whole");
        // the same with the simulations of both consumers first, then both for real
        store(h, larder).insert(stack(Items.DIRT, 10), false);
        ItemStack first = hopperSide.extractItem(0, 64, true);
        ItemStack second = pipeSide.extractItem(0, 64, true);
        h.assertValueEqual(first.getCount() + second.getCount(), 20, "both were offered the same ten");
        int real = hopperSide.extractItem(0, first.getCount(), false).getCount()
                + pipeSide.extractItem(0, second.getCount(), false).getCount();
        h.assertValueEqual(real, 10, "together they take the ten once");
        h.assertValueEqual(store(h, larder).count(stack(Items.DIRT, 1)), 32, "and the reserve holds");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aReserveEditBetweenASimulationAndTheExtractionIsHonoured(GameTestHelper h) {
        BlockPos larder = aloneLarder(h, 0);
        store(h, larder).insert(stack(Items.DIRT, 40), false);
        reserve(h, larder, 0, 32);
        IItemHandler handler = items(h, larder);
        h.assertValueEqual(handler.extractItem(0, 64, true).getCount(), 8, "eight are free");
        reserve(h, larder, 0, 64);
        h.assertTrue(
                handler.extractItem(0, 8, false).isEmpty(), "raised past the stock, the real extraction gets none");
        h.assertValueEqual(store(h, larder).automatable(stack(Items.DIRT, 1)), 0, "none are free");
        h.assertValueEqual(store(h, larder).count(stack(Items.DIRT, 1)), 40, "all are held");
        MenuPlayer player = open(h, larder);
        player.containerMenu.clickMenuButton(player, RESERVE_DOWN); // 64 -> 32
        player.containerMenu.clickMenuButton(player, RESERVE_DOWN); // 32 -> 16
        player.closeContainer();
        h.assertValueEqual(reserveOf(h, larder, 0), 16, "stepped down to sixteen");
        h.assertValueEqual(handler.extractItem(0, 64, false).getCount(), 24, "lowered, the extra comes free");
        h.assertValueEqual(store(h, larder).count(stack(Items.DIRT, 1)), 16, "down to the new reserve");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void stockBelowTheReserveLendsNothingAndInsertionIsNeverLimited(GameTestHelper h) {
        BlockPos larder = aloneLarder(h, 0);
        store(h, larder).insert(stack(Items.DIRT, 10), false);
        reserve(h, larder, 0, 64);
        IItemHandler handler = items(h, larder);
        h.assertTrue(handler.getStackInSlot(0).isEmpty(), "ten held under a reserve of 64 show as nothing");
        h.assertTrue(handler.extractItem(0, 64, true).isEmpty(), "and offer nothing");
        h.assertValueEqual(store(h, larder).automatable(stack(Items.DIRT, 1)), 0, "not even one");
        h.assertValueEqual(store(h, larder).count(stack(Items.DIRT, 1)), 10, "the ten are still held");
        h.assertValueEqual(store(h, larder).insert(stack(Items.DIRT, 30), false).getCount(), 0, "insertion is free");
        h.assertValueEqual(
                store(h, larder).insert(stack(Items.DIRT, 30), false).getCount(), 0, "as far as there is room");
        h.assertValueEqual(store(h, larder).count(stack(Items.DIRT, 1)), 70, "seventy held");
        h.assertTrue(handler.getStackInSlot(0).isEmpty(), "the full compartment under its reserve shows nothing");
        h.assertValueEqual(
                store(h, larder).automatable(stack(Items.DIRT, 1)),
                6,
                "only the six in the compartment the dirt overflowed into, which has no reserve");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theSumOfTheReservesOfTwoLardersProtectsOneKind(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        List<BlockPos> pods = pods(h);
        BlockPos first = pods.get(0);
        BlockPos second = pods.get(1);
        BlockPos bulk = pods.get(2);
        for (BlockPos pod : List.of(first, second)) {
            treat(h, pod, gel(1)).closeContainer();
            treat(h, pod, resin(1)).closeContainer();
        }
        // fill each Larder by hand so the iron lands in both, then a bulk pod holds iron too
        for (BlockPos pod : List.of(first, second)) {
            MenuPlayer screen = open(h, pod);
            screen.containerMenu
                    .getSlot(ItemCapsulePodMenu.COMPARTMENT)
                    .set(stack(Items.IRON_INGOT, pod.equals(first) ? 50 : 20));
            screen.closeContainer();
        }
        reserve(h, first, 0, 32);
        reserve(h, second, 0, 16);
        MenuPlayer screen = open(h, bulk);
        screen.containerMenu.getSlot(ItemCapsulePodMenu.COMPARTMENT).set(stack(Items.IRON_INGOT, 100));
        screen.closeContainer();
        ItemStore tree = store(h, ROOT);
        ItemStack iron = stack(Items.IRON_INGOT, 1);
        h.assertValueEqual(tree.count(iron), 170, "held in all three");
        h.assertValueEqual(tree.automatable(iron), 100 + 18 + 4, "the bulk pod's 100 and what the two reserves leave");
        h.assertValueEqual(drain(h, ROOT, Direction.UP, iron), 122, "a pipe at the root drains exactly that");
        h.assertValueEqual(tree.count(iron), 48, "the two reserves, summed, remain");
        h.assertValueEqual(stockOf(h, first).held().getFirst().getCount(), 32, "32 in the first");
        h.assertValueEqual(stockOf(h, second).held().getFirst().getCount(), 16, "16 in the second");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void ordinaryPodsAndCrawsGiveEverythingAndSaveNoMarks(GameTestHelper h) {
        BlockPos bulk = pod(h, new BlockPos(2, 2, 2), StorageModule.ITEM_CAPSULE.get(), 0);
        BlockPos craw = pod(h, new BlockPos(6, 2, 2), StorageModule.MIXED_CRAW.get(), 1);
        for (BlockPos pod : List.of(bulk, craw)) {
            store(h, pod).insert(stack(Items.DIRT, 50), false);
            h.assertValueEqual(items(h, pod).getStackInSlot(0).getCount(), 50, "the whole stack is seen");
            h.assertValueEqual(store(h, pod).automatable(stack(Items.DIRT, 1)), 50, "all automatable");
            h.assertFalse(saved(h, h.getBlockEntity(pod)).getCompound("stock").contains("marks"), "no marks saved");
            h.assertValueEqual(items(h, pod).extractItem(0, 64, true).getCount(), 50, "offered whole");
            h.assertValueEqual(drain(h, pod, Direction.UP, stack(Items.DIRT, 1)), 50, "and given whole");
            h.assertValueEqual(store(h, pod).stored(), 0L, "nothing is held back");
        }
        h.succeed();
    }

    // ---- consumers: a hopper, Mouths, a pipe ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void aHopperDrainsALarderToItsReserveStopsAndResumesWhenItIsRestocked(GameTestHelper h) {
        BlockPos chest = new BlockPos(8, 2, 8);
        BlockPos hopperPos = chest.above();
        BlockPos larder = larder(h, hopperPos.above(), 0);
        h.setBlock(chest, Blocks.CHEST);
        h.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        ChestBlockEntity box = (ChestBlockEntity) h.getBlockEntity(chest);
        store(h, larder).insert(stack(Items.DIRT, 20), false);
        reserve(h, larder, 0, 16);
        h.runAfterDelay(100, () -> {
            h.assertValueEqual(count(box, Items.DIRT), 4, "the hopper took the four above the reserve");
            h.assertValueEqual(store(h, larder).count(stack(Items.DIRT, 1)), 16, "and stopped at the reserve");
            store(h, larder).insert(stack(Items.DIRT, 3), false);
        });
        h.runAfterDelay(200, () -> {
            h.assertValueEqual(count(box, Items.DIRT), 7, "restocked, it resumed and took the three");
            h.assertValueEqual(store(h, larder).count(stack(Items.DIRT, 1)), 16, "and stopped at the reserve again");
            h.assertValueEqual(
                    count(box, Items.DIRT) + store(h, larder).count(stack(Items.DIRT, 1)),
                    23,
                    "every item is somewhere once");
            h.succeed();
        });
    }

    private static int count(ChestBlockEntity box, Item item) {
        int total = 0;
        for (int slot = 0; slot < box.getContainerSize(); slot++)
            if (box.getItem(slot).is(item)) total += box.getItem(slot).getCount();
        return total;
    }

    /** An output Mouth under a Larder joined by veins to an intake Mouth under a chest, in the column at x. */
    private static ChestBlockEntity line(GameTestHelper h, int x, int amount) {
        BlockPos source = new BlockPos(x, 2, 8);
        BlockPos sink = new BlockPos(x, 2, 4);
        h.setBlock(source, LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, MouthMode.OUTPUT));
        larder(h, source.above(), 2);
        for (int z = 5; z <= 7; z++) h.setBlock(new BlockPos(x, 2, z), LogisticsModule.ITEM_VEIN.get());
        h.setBlock(sink, LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, MouthMode.INTAKE));
        h.setBlock(sink.above(), Blocks.CHEST);
        if (amount > 0) press(h, source, MouthMenu.AMOUNT_UP, amount);
        return (ChestBlockEntity) h.getBlockEntity(sink.above());
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 600)
    public static void aMouthDrainsToTheReserveAndItsOwnAmountCombinesWithItAsTheLarger(GameTestHelper h) {
        // Mouth amounts: 1 click is 1, 2 is 4, 3 is 8, 4 is 16; the reserve is 8 in every column
        ChestBlockEntity none = line(h, 2, 0);
        ChestBlockEntity small = line(h, 6, 2); // keeps 4: the reserve of 8 is larger
        ChestBlockEntity large = line(h, 10, 4); // keeps 16: the Mouth's amount is larger
        for (int x : List.of(2, 6, 10)) {
            BlockPos larder = new BlockPos(x, 3, 8);
            store(h, larder).insert(stack(Items.DIRT, 40), false);
            reserve(h, larder, 0, 8);
        }
        h.runAfterDelay(500, () -> {
            h.assertValueEqual(count(none, Items.DIRT), 32, "no amount: the reserve of 8 stopped the Mouth");
            h.assertValueEqual(count(small, Items.DIRT), 32, "an amount of 4 under a reserve of 8: the reserve");
            h.assertValueEqual(count(large, Items.DIRT), 24, "an amount of 16 over a reserve of 8: the amount");
            h.assertValueEqual(store(h, new BlockPos(2, 3, 8)).count(stack(Items.DIRT, 1)), 8, "8 held");
            h.assertValueEqual(store(h, new BlockPos(10, 3, 8)).count(stack(Items.DIRT, 1)), 16, "16 held");
            // the Mouth's amount stops only that Mouth: another consumer takes the large column down to the reserve
            h.assertValueEqual(
                    drain(h, new BlockPos(10, 3, 8), Direction.UP, stack(Items.DIRT, 1)), 8, "a pipe takes 8 more");
            h.assertValueEqual(store(h, new BlockPos(10, 3, 8)).count(stack(Items.DIRT, 1)), 8, "never below 8");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 600)
    public static void aMouthStopsAtTheReserveAndResumesWhenTheLarderIsRestocked(GameTestHelper h) {
        ChestBlockEntity sink = line(h, 4, 0);
        BlockPos larder = new BlockPos(4, 3, 8);
        store(h, larder).insert(stack(Items.DIRT, 30), false);
        reserve(h, larder, 0, 16);
        h.runAfterDelay(200, () -> {
            h.assertValueEqual(count(sink, Items.DIRT), 14, "the Mouth moved everything above the reserve");
            h.assertValueEqual(store(h, larder).count(stack(Items.DIRT, 1)), 16, "and stopped there");
        });
        h.runAfterDelay(300, () -> store(h, larder).insert(stack(Items.DIRT, 12), false));
        h.runAfterDelay(500, () -> {
            h.assertValueEqual(count(sink, Items.DIRT), 26, "restocked, it took the twelve");
            h.assertValueEqual(
                    count(sink, Items.DIRT) + store(h, larder).count(stack(Items.DIRT, 1)), 42, "conserved exactly");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 500)
    public static void aHopperAndAPipeCompetingForALarderTakeTheFreePartOnce(GameTestHelper h) {
        BlockPos chest = new BlockPos(8, 2, 8);
        BlockPos hopperPos = chest.above();
        BlockPos larder = larder(h, hopperPos.above(), 1);
        h.setBlock(chest, Blocks.CHEST);
        h.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        ChestBlockEntity box = (ChestBlockEntity) h.getBlockEntity(chest);
        store(h, larder).insert(stack(Items.DIRT, 60), false);
        reserve(h, larder, 0, 32);
        int[] piped = {0};
        for (int tick = 3; tick < 150; tick += 3)
            h.runAfterDelay(
                    tick,
                    () -> piped[0] += items(h, larder, Direction.EAST)
                            .extractItem(0, 2, false)
                            .getCount());
        h.runAfterDelay(300, () -> {
            h.assertValueEqual(
                    count(box, Items.DIRT) + piped[0], 28, "between them the two consumers took exactly the free 28");
            h.assertValueEqual(store(h, larder).count(stack(Items.DIRT, 1)), 32, "the reserve is whole");
            h.succeed();
        });
    }

    // ---- manual withdrawal ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aClickTakesOnlyTheFreePartAndWithdrawReserveTakesAStackOfTheReservedPart(GameTestHelper h) {
        BlockPos larder = aloneLarder(h, 2);
        store(h, larder).insert(stack(Items.DIRT, 200), false);
        reserve(h, larder, 0, 128);
        MenuPlayer player = open(h, larder);
        AbstractContainerMenu menu = player.containerMenu;
        menu.clicked(ItemCapsulePodMenu.COMPARTMENT, 0, ClickType.PICKUP, player);
        h.assertValueEqual(menu.getCarried().getCount(), 64, "a click takes a stack of the free 72");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(ItemCapsulePodMenu.COMPARTMENT, 0, ClickType.PICKUP, player);
        h.assertValueEqual(menu.getCarried().getCount(), 8, "then the eight that are left free");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(ItemCapsulePodMenu.COMPARTMENT, 0, ClickType.PICKUP, player);
        h.assertTrue(menu.getCarried().isEmpty(), "and never the reserve");
        menu.clicked(ItemCapsulePodMenu.COMPARTMENT, 0, ClickType.QUICK_MOVE, player);
        menu.clicked(ItemCapsulePodMenu.COMPARTMENT, 0, ClickType.THROW, player);
        menu.clicked(ItemCapsulePodMenu.COMPARTMENT, 0, ClickType.PICKUP_ALL, player);
        h.assertValueEqual(
                store(h, larder).count(stack(Items.DIRT, 1)), 128, "not by shift-click, drop or double-click");
        h.assertValueEqual(player.containerMenu.getCarried().getCount(), 0, "nothing in the cursor");
        h.assertValueEqual(menu.clickMenuButton(player, WITHDRAW), true, "Withdraw reserve works");
        h.assertValueEqual(menu.getCarried().getCount(), 64, "up to one stack of the reserved part");
        h.assertValueEqual(store(h, larder).count(stack(Items.DIRT, 1)), 64, "out of the store");
        h.assertValueEqual(reserveOf(h, larder, 0), 128, "the setting is unchanged");
        h.assertValueEqual(menu.clickMenuButton(player, WITHDRAW), false, "the cursor is full of dirt");
        menu.setCarried(stack(Items.STONE, 3));
        h.assertValueEqual(menu.clickMenuButton(player, WITHDRAW), false, "another kind in the cursor blocks it");
        h.assertValueEqual(store(h, larder).count(stack(Items.DIRT, 1)), 64, "and takes nothing");
        menu.setCarried(stack(Items.DIRT, 60));
        h.assertTrue(menu.clickMenuButton(player, WITHDRAW), "a cursor with room takes what fits");
        h.assertValueEqual(menu.getCarried().getCount(), 64, "topped up to a stack");
        h.assertValueEqual(store(h, larder).count(stack(Items.DIRT, 1)), 60, "four came out");
        menu.setCarried(ItemStack.EMPTY);
        h.assertValueEqual(reserveOf(h, larder, 0), 128, "still unchanged");
        player.closeContainer();
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void withdrawReserveTakesNothingFromAnEmptyOrUnreservedCompartment(GameTestHelper h) {
        BlockPos larder = aloneLarder(h, 0);
        store(h, larder).insert(stack(Items.DIRT, 10), false);
        MenuPlayer player = open(h, larder);
        AbstractContainerMenu menu = player.containerMenu;
        h.assertFalse(menu.clickMenuButton(player, WITHDRAW), "no reserve, nothing reserved");
        h.assertFalse(menu.clickMenuButton(player, WITHDRAW + 2), "an empty compartment gives nothing");
        h.assertFalse(menu.clickMenuButton(player, WITHDRAW + 3), "there is no fourth compartment");
        h.assertFalse(menu.clickMenuButton(player, -1), "and no negative button");
        h.assertValueEqual(store(h, larder).count(stack(Items.DIRT, 1)), 10, "nothing moved");
        player.closeContainer();
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theScreenReadsHeldKeptAndFreeForEveryCompartment(GameTestHelper h) {
        BlockPos larder = aloneLarder(h, 1);
        store(h, larder).insert(stack(Items.DIRT, 100), false);
        store(h, larder).insert(stack(Items.STONE, 5), false);
        reserve(h, larder, 0, 64);
        reserve(h, larder, 1, 8);
        MenuPlayer player = open(h, larder);
        ItemCapsulePodMenu menu = (ItemCapsulePodMenu) player.containerMenu;
        menu.broadcastChanges();
        h.assertValueEqual(menu.count(0), 100, "held");
        h.assertValueEqual(menu.reserve(0), 64, "reserve");
        h.assertValueEqual(menu.kept(0), 64, "kept");
        h.assertValueEqual(menu.free(0), 36, "free");
        h.assertValueEqual(menu.kept(1), 5, "a stock under its reserve is all kept");
        h.assertValueEqual(menu.free(1), 0, "and none is free");
        h.assertValueEqual(menu.kept(2) + menu.free(2) + menu.reserve(2), 0, "an unused compartment shows nothing");
        player.closeContainer();
        h.succeed();
    }

    // ---- the tree's ledger ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theLedgerShowsHeldAndKeptAndItsClickTakesOnlyTheFreePart(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        List<BlockPos> pods = pods(h);
        BlockPos larder = pods.get(1);
        treat(h, larder, gel(1)).closeContainer();
        treat(h, larder, resin(1)).closeContainer();
        label(h, larder, 0, Items.DIRT);
        store(h, ROOT).insert(stack(Items.DIRT, 50), false);
        reserve(h, larder, 0, 32);
        h.assertTrue(stockOf(h, larder).held().get(0).is(Items.DIRT), "the dirt went to the Larder");
        MenuPlayer player = new MenuPlayer(h.getLevel());
        BlockPos at = h.absolutePos(ROOT);
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        player.openMenu((MenuProvider) h.getBlockEntity(ROOT), at);
        ItemCapsuleMenu menu = (ItemCapsuleMenu) player.containerMenu;
        menu.broadcastChanges();
        h.assertTrue(menu.getSlot(0).getItem().is(Items.DIRT), "the kind is in the first cell");
        h.assertValueEqual(menu.getSlot(0).getItem().getCount(), 50, "with its whole count");
        h.assertValueEqual(menu.reserved(0), 32, "of which 32 are kept");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        h.assertValueEqual(menu.getCarried().getCount(), 18, "a click takes the free 18, not a stack");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 0, ClickType.PICKUP, player);
        h.assertTrue(menu.getCarried().isEmpty(), "and then nothing");
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        h.assertValueEqual(store(h, ROOT).count(stack(Items.DIRT, 1)), 32, "shift-click takes none of the reserve");
        player.closeContainer();
        h.succeed();
    }

    // ---- carrying, breaking, orphaning, shedding and saving ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aLarderSavedToBytesLoadsBackWithLabelsReservesAndStockExactly(GameTestHelper h) {
        BlockPos larder = aloneLarder(h, 1);
        store(h, larder).insert(stack(Items.BONE, 100), false);
        store(h, larder).insert(stack(Items.COAL, 9), false);
        store(h, larder).insert(stack(Items.IRON_INGOT, 3), false);
        drain(h, larder, Direction.UP, stack(Items.IRON_INGOT, 1));
        reserve(h, larder, 0, 64);
        reserve(h, larder, 2, 4);
        CapsuleStock before = stockOf(h, larder);
        CompoundTag bytes = saved(h, h.getBlockEntity(larder));
        h.assertValueEqual(bytes.getCompound("stock").getString("kind"), "larder", "the kind is saved");
        h.assertTrue(bytes.getCompound("stock").contains("marks"), "and its labels and reserves");
        reload(h, larder);
        h.assertTrue(isLarder(h, larder), "still a Larder");
        h.assertTrue(stockOf(h, larder).equals(before), "exactly the stock, labels and reserves it had");
        h.assertValueEqual(saved(h, h.getBlockEntity(larder)), bytes, "and saves the same bytes");
        h.assertTrue(stockOf(h, larder).mark(2).label().is(Items.IRON_INGOT), "an empty compartment keeps its label");
        h.assertValueEqual(reserveOf(h, larder, 2), 4, "and its reserve");
        h.assertValueEqual(items(h, larder).getStackInSlot(0).getCount(), 36, "the reserve still holds after loading");
        h.assertValueEqual(drain(h, larder, Direction.UP, stack(Items.BONE, 1)), 36, "only the free part leaves");
        reload(h, larder);
        h.assertValueEqual(store(h, larder).count(stack(Items.BONE, 1)), 64, "the reserve survives a second reload");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aBrokenLarderCarriesStockLabelsAndReservesAndAPressedOneRejoinsTheTree(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        List<BlockPos> pods = pods(h);
        BlockPos pod = pods.get(1);
        Direction facing = h.getBlockState(pod).getValue(ItemCapsuleBlock.FACING);
        treat(h, pod, graft(1)).closeContainer();
        treat(h, pod, gel(1)).closeContainer();
        treat(h, pod, resin(1)).closeContainer();
        h.assertTrue(isLarder(h, pod), "a Larder");
        MenuPlayer filler = open(h, pod);
        filler.containerMenu.getSlot(ItemCapsulePodMenu.COMPARTMENT).set(stack(Items.BONE, 20));
        filler.containerMenu.getSlot(ItemCapsulePodMenu.COMPARTMENT + 1).set(stack(Items.COAL, 3));
        filler.closeContainer();
        int slots = items(h, ROOT).getSlots();
        reserve(h, pod, 0, 4);
        reserve(h, pod, 1, 1);
        CapsuleStock before = stockOf(h, pod);
        h.assertTrue(!before.isBare(), "the Larder has something to carry");
        ItemStore tree = store(h, ROOT);
        long held = tree.stored();
        h.getLevel().destroyBlock(h.absolutePos(pod), true);
        List<ItemEntity> drops = h.getEntities(EntityType.ITEM, pod, 2.0);
        h.assertValueEqual(drops.size(), 1, "one dropped pod");
        ItemStack dropped = drops.getFirst().getItem();
        h.assertTrue(dropped.is(StorageModule.LARDER_ITEM.get()), "a Larder, not a Craw");
        CapsuleStock kept = dropped.get(StorageModule.CAPSULE_STOCK.get());
        h.assertTrue(kept != null && kept.kind() == PodKind.LARDER, "carrying its kind");
        h.assertTrue(kept.equals(before), "and exactly its stock, labels and reserves");
        h.assertValueEqual(dropped.getMaxStackSize(), 1, "a Larder with stock does not stack");
        h.assertValueEqual(items(h, ROOT).getSlots(), slots - 3, "the tree goes on without its compartments");
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.LARDER_ITEM.get(), 1);
        builder.setItemInHand(InteractionHand.MAIN_HAND, dropped.copy());
        h.placeAt(builder, builder.getMainHandItem(), pod.relative(facing.getOpposite()), facing);
        h.assertTrue(
                isLarder(h, pod) && h.getBlockState(pod).getValue(ItemCapsuleBlock.FACING) == facing, "pressed back");
        h.assertValueEqual(h.getBlockState(pod).getValue(ItemCapsuleBlock.GRADE), 1, "at its grade");
        h.assertTrue(stockOf(h, pod).equals(before), "with exactly its stock, labels and reserves");
        h.assertValueEqual(items(h, ROOT).getSlots(), slots, "and the tree reads it again");
        h.assertValueEqual(tree.stored(), held, "its stock counted once");
        h.assertTrue(tree.automatable(stack(Items.BONE, 1)) < tree.count(stack(Items.BONE, 1)), "its reserve holds");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aCreativeBreakAndABrokenRootKeepEveryLabelAndReserve(GameTestHelper h) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        plant(h, FORKING);
        growAll(h);
        List<BlockPos> pods = pods(h);
        for (int i = 0; i < 2; i++) {
            treat(h, pods.get(i), gel(1)).closeContainer();
            treat(h, pods.get(i), resin(1)).closeContainer();
        }
        List<CapsuleStock> before = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            MenuPlayer filler = open(h, pods.get(i));
            filler.containerMenu.getSlot(ItemCapsulePodMenu.COMPARTMENT).set(stack(Items.BONE, 9));
            filler.closeContainer();
            reserve(h, pods.get(i), 0, 1);
            before.add(stockOf(h, pods.get(i)));
        }
        int cells = 0;
        for (int y = 1; y < 12; y++)
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++)
                    if (h.getBlockState(new BlockPos(x, y, z)).is(StorageModule.CAPSULE_STEM.get())) cells++;
        // a Creative player breaking one gets it back whole
        Player creative = EcologyTests.holding(h, GameType.CREATIVE, Items.AIR, 1);
        BlockPos first = pods.get(0);
        h.getBlockState(first)
                .getBlock()
                .playerWillDestroy(h.getLevel(), h.absolutePos(first), h.getBlockState(first), creative);
        List<ItemEntity> creativeDrops = h.getEntities(EntityType.ITEM, first, 2.0);
        h.assertValueEqual(creativeDrops.size(), 1, "the Creative break dropped the pod");
        h.assertTrue(
                before.get(0).equals(creativeDrops.getFirst().getItem().get(StorageModule.CAPSULE_STOCK.get())),
                "with its stock, labels and reserves");
        creativeDrops.getFirst().discard();
        h.getLevel().destroyBlock(h.absolutePos(ROOT), true);
        h.runAfterDelay(4 * cells + 10, () -> {
            int larders = 0;
            int reserves = 0;
            for (ItemEntity entity : h.getEntities(EntityType.ITEM)) {
                ItemStack item = entity.getItem();
                CapsuleStock carried = item.get(StorageModule.CAPSULE_STOCK.get());
                if (!item.is(StorageModule.LARDER_ITEM.get()) || carried == null) continue;
                larders++;
                for (int compartment = 0; compartment < 3; compartment++)
                    reserves += carried.mark(compartment).reserve();
            }
            h.assertValueEqual(larders, 2, "the two Larders are shed as Larders");
            h.assertValueEqual(reserves, 2, "each carrying its reserve");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anOrphanedLarderLendsNothingToTheTreeAndKeepsItsReserveUntilItRejoins(GameTestHelper h) {
        plant(h, FORKING);
        growAll(h);
        List<BlockPos> pods = pods(h);
        BlockPos orphan = null;
        for (BlockPos pod : pods) if (pod.getY() > ROOT.getY() + 2) orphan = pod;
        h.assertTrue(orphan != null, "a pod above the cut");
        treat(h, orphan, gel(1)).closeContainer();
        treat(h, orphan, resin(1)).closeContainer();
        ItemStack iron = stack(Items.IRON_INGOT, 1);
        MenuPlayer screen = open(h, orphan);
        screen.containerMenu.getSlot(ItemCapsulePodMenu.COMPARTMENT).set(stack(Items.IRON_INGOT, 40));
        screen.closeContainer();
        reserve(h, orphan, 0, 32);
        ItemStore tree = store(h, ROOT);
        h.assertValueEqual(tree.count(iron), 40, "the tree holds it");
        h.assertValueEqual(tree.automatable(iron), 8, "and lends the free eight");
        int slots = items(h, ROOT).getSlots();
        h.getLevel().destroyBlock(h.absolutePos(ROOT.above(2)), true);
        h.assertTrue(items(h, ROOT).getSlots() < slots, "the root no longer reaches it");
        h.assertValueEqual(tree.count(iron), 0, "the tree does not count it");
        h.assertValueEqual(tree.automatable(iron), 0, "and cannot lend it");
        h.assertValueEqual(drain(h, ROOT, Direction.UP, iron), 0, "a pipe at the root gets none of it");
        h.assertValueEqual(store(h, orphan).count(iron), 40, "the orphan holds it as a store of its own");
        h.assertValueEqual(store(h, orphan).automatable(iron), 8, "and its reserve still holds");
        h.assertValueEqual(drain(h, orphan, Direction.UP, iron), 8, "a pipe at the orphan gets the free eight");
        h.assertValueEqual(store(h, orphan).count(iron), 32, "never the reserve");
        h.assertTrue(grow(h) && grow(h), "two bone meal regrow the cell in place");
        h.assertValueEqual(items(h, ROOT).getSlots(), slots, "every compartment is reached again");
        h.assertValueEqual(store(h, ROOT).count(iron), 32, "counted once");
        h.assertValueEqual(store(h, ROOT).automatable(iron), 0, "with its reserve intact");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aLarderWithABadMarkIsKeptUnreadableAndOldPodsLoadUnchanged(GameTestHelper h) {
        BlockPos larder = aloneLarder(h, 0);
        store(h, larder).insert(stack(Items.BONE, 30), false);
        CompoundTag good = saved(h, h.getBlockEntity(larder));
        // a reserve past the compartment's limit
        CompoundTag deep = good.copy();
        deep.getCompound("stock").getList("marks", 10).getCompound(0).putInt("reserve", 65);
        h.getBlockEntity(larder).loadWithComponents(deep, h.getLevel().registryAccess());
        h.assertValueEqual(items(h, larder).getSlots(), 0, "a reserve past the depth is unreadable");
        h.assertTrue(
                h.getBlockEntity(larder).collectComponents().get(OrganModule.UNREADABLE_SAVE.get()) != null,
                "and its bytes are kept");
        // stock in a compartment that has no label
        CompoundTag unlabelled = good.copy();
        unlabelled.getCompound("stock").remove("marks");
        h.getBlockEntity(larder).loadWithComponents(unlabelled, h.getLevel().registryAccess());
        h.assertValueEqual(items(h, larder).getSlots(), 0, "stock with no label is unreadable");
        // a label over another kind
        CompoundTag wrong = good.copy();
        wrong.getCompound("stock")
                .getList("marks", 10)
                .getCompound(0)
                .getCompound("label")
                .putString("id", "minecraft:stick");
        h.getBlockEntity(larder).loadWithComponents(wrong, h.getLevel().registryAccess());
        h.assertValueEqual(items(h, larder).getSlots(), 0, "a label over another kind is unreadable");
        // marks in a Craw, and a Larder's bytes in a Craw
        BlockPos craw = pod(h, new BlockPos(2, 1, 2), StorageModule.MIXED_CRAW.get(), 0);
        h.getBlockEntity(craw).loadWithComponents(good.copy(), h.getLevel().registryAccess());
        h.assertValueEqual(items(h, craw).getSlots(), 0, "a Craw refuses a Larder's stock");
        BlockPos second = pod(h, new BlockPos(2, 1, 8), StorageModule.MIXED_CRAW.get(), 0);
        CompoundTag marked = saved(h, h.getBlockEntity(second));
        ListTag marks = new ListTag();
        CompoundTag mark = new CompoundTag();
        mark.putInt("slot", 0);
        mark.putInt("reserve", 1);
        marks.add(mark);
        marked.getCompound("stock").put("marks", marks);
        h.getBlockEntity(second).loadWithComponents(marked, h.getLevel().registryAccess());
        h.assertValueEqual(items(h, second).getSlots(), 0, "marks in a Craw are unreadable");
        // a Larder with the Craw's four compartments
        CompoundTag crowded = good.copy();
        ListTag held = crowded.getCompound("stock").getList("held", 10);
        CompoundTag fourth = new CompoundTag();
        fourth.putInt("slot", 3);
        fourth.put("item", held.getCompound(0).getCompound("item").copy());
        fourth.putInt("count", 1);
        held.add(fourth);
        h.getBlockEntity(larder).loadWithComponents(crowded, h.getLevel().registryAccess());
        h.assertValueEqual(items(h, larder).getSlots(), 0, "a fourth compartment is unreadable");
        // a good Larder loads, and an old Craw's stock is a Craw's still
        h.getBlockEntity(larder).loadWithComponents(good, h.getLevel().registryAccess());
        h.assertValueEqual(store(h, larder).count(stack(Items.BONE, 1)), 30, "a well-formed Larder loads");
        BlockPos oldCraw = pod(h, new BlockPos(6, 1, 2), StorageModule.MIXED_CRAW.get(), 1);
        store(h, oldCraw).insert(stack(Items.BONE, 70), false);
        CompoundTag craw70 = saved(h, h.getBlockEntity(oldCraw));
        h.assertFalse(craw70.getCompound("stock").contains("marks"), "a Craw saves no marks");
        h.getBlockEntity(oldCraw).loadWithComponents(craw70, h.getLevel().registryAccess());
        h.assertValueEqual(drain(h, oldCraw, Direction.UP, stack(Items.BONE, 1)), 70, "and gives all of its bones");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aPodItemOrAStockedLarderIsRefusedByEveryPodAndAnEmptyOneIsNot(GameTestHelper h) {
        BlockPos larder = aloneLarder(h, 0);
        store(h, larder).insert(stack(Items.BONE, 3), false);
        reserve(h, larder, 0, 1);
        ItemStack carried = new ItemStack(StorageModule.LARDER_ITEM.get());
        carried.set(StorageModule.CAPSULE_STOCK.get(), stockOf(h, larder));
        BlockPos other = pod(h, new BlockPos(6, 2, 2), StorageModule.ITEM_CAPSULE.get(), 2);
        h.assertValueEqual(store(h, other).insert(carried, false).getCount(), 1, "a stocked Larder item never nests");
        h.assertValueEqual(store(h, larder).insert(carried, false).getCount(), 1, "not even in a Larder");
        h.succeed();
    }

    // ---- discovery ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theLarderAndItsResinAreDiscoverableByTagAndRecipe(GameTestHelper h) {
        h.assertTrue(resin(1).is(StorageModule.LARDER_RESIN), "Sealing Resin is the resin");
        h.assertFalse(gel(1).is(StorageModule.LARDER_RESIN), "Elastic Gel is not");
        h.assertFalse(resin(1).is(StorageModule.CRAW_GEL), "and the resin is not a gel");
        h.assertTrue(
                new ItemStack(StorageModule.LARDER_ITEM.get()).getItem() != StorageModule.MIXED_CRAW_ITEM.get(),
                "the Larder is an item of its own");
        h.succeed();
    }

    // ---- bounded work ----

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void manyReadsAndSimulationsAgainstAReserveChangeNothing(GameTestHelper h) {
        BlockPos larder = aloneLarder(h, 2);
        store(h, larder).insert(stack(Items.DIRT, 200), false);
        reserve(h, larder, 0, 128);
        CompoundTag before = saved(h, h.getBlockEntity(larder));
        IItemHandler handler = items(h, larder);
        ItemStore store = store(h, larder);
        for (int i = 0; i < 5000; i++) {
            h.assertValueEqual(handler.extractItem(0, 64, true).getCount(), 64, "offered");
            handler.getStackInSlot(0);
            store.count(stack(Items.DIRT, 1));
            store.automatable(stack(Items.DIRT, 1));
            store.insert(stack(Items.DIRT, 64), true);
            handler.insertItem(0, stack(Items.DIRT, 64), true);
        }
        h.assertValueEqual(saved(h, h.getBlockEntity(larder)), before, "the pod saves exactly as before");
        h.assertValueEqual(store.stored(), 200L, "nothing moved");
        h.succeed();
    }

    // ---- real chunk save and load ----

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "capsule_unload",
            timeoutTicks = 1200)
    public static void aLarderInATreeKeepsItsReserveThroughItsChunkLeavingAndLendsNothingWhileItIsGone(
            GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        BlockPos anchor = new BlockPos((origin.getX() & ~15) + 192000, 80, (origin.getZ() & ~15) + 226200);
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
                    FakePlayer player = new MenuPlayer(level);
                    player.moveTo(east.getX() + 0.5, east.getY(), east.getZ() - 1.5);
                    for (ItemStack treatment : List.of(gel(1), resin(1))) {
                        player.openMenu((MenuProvider) level.getBlockEntity(east), east);
                        player.containerMenu.setCarried(treatment);
                        player.containerMenu.clicked(ItemCapsulePodMenu.TREATMENT, 0, ClickType.PICKUP, player);
                    }
                    h.assertTrue(level.getBlockState(east).is(StorageModule.LARDER.get()), "mutated");
                    player.openMenu((MenuProvider) level.getBlockEntity(east), east);
                    port[0] = level.getCapability(Capabilities.ItemHandler.BLOCK, root, Direction.UP);
                    ItemStore store = (ItemStore) port[0];
                    player.containerMenu.setCarried(stack(Items.BONE, 1));
                    player.containerMenu.clicked(LABELS, 0, ClickType.PICKUP, player);
                    store.insert(stack(Items.BONE, 40), false);
                    for (int i = 0; i < 3; i++) player.containerMenu.clickMenuButton(player, RESERVE_UP);
                    player.closeContainer();
                    before[0] = level.getBlockEntity(east).collectComponents().get(StorageModule.CAPSULE_STOCK.get());
                    h.assertTrue(before[0] != null && before[0].kind() == PodKind.LARDER, "the Larder holds stock");
                    h.assertValueEqual(before[0].mark(0).reserve(), 8, "and a reserve");
                    h.assertValueEqual(before[0].held().get(0).getCount(), 40, "of forty bones");
                    level.setChunkForced(far.x, far.z, false);
                    level.setChunkForced(near.x, near.z, false);
                })
                .thenWaitUntil(() -> h.assertFalse(level.hasChunkAt(east), "the Larder's chunk is gone"))
                .thenExecute(() -> {
                    IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, root, Direction.UP);
                    int lent = 0;
                    if (handler != null)
                        for (int slot = 0; slot < handler.getSlots(); slot++)
                            lent += handler.extractItem(slot, 64, true).getCount();
                    h.assertValueEqual(lent, 0, "while the Larder is gone the root lends none of its bones");
                    TestChunks.force(level, near);
                    TestChunks.force(level, far);
                })
                .thenWaitUntil(() -> h.assertTrue(level.hasChunkAt(east), "and back"))
                .thenWaitUntil(() -> h.assertTrue(
                        level.getBlockEntity(east) != null
                                && level.getBlockState(east).is(StorageModule.LARDER.get()),
                        "the Larder is back"))
                .thenExecute(() -> {
                    CapsuleStock after =
                            level.getBlockEntity(east).collectComponents().get(StorageModule.CAPSULE_STOCK.get());
                    h.assertTrue(before[0].equals(after), "exactly the stock, label and reserve it had");
                    ItemStore store =
                            (ItemStore) level.getCapability(Capabilities.ItemHandler.BLOCK, root, Direction.UP);
                    h.assertValueEqual(store.count(stack(Items.BONE, 1)), 40, "counted once by the tree");
                    h.assertValueEqual(
                            store.automatable(stack(Items.BONE, 1)),
                            Math.max(0, 40 - after.mark(0).reserve()),
                            "and lent only above its reserve");
                    for (BlockPos pos : BlockPos.betweenClosed(root.offset(-2, -1, -2), root.offset(2, 7, 2)))
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    level.setChunkForced(kept.x, kept.z, false);
                    level.setChunkForced(near.x, near.z, false);
                    level.setChunkForced(far.x, far.z, false);
                })
                .thenSucceed();
    }

    // ---- helpers that open screens ----

    /** Presses a Mouth's button {@code times} times at a server player's screen. */
    private static void press(GameTestHelper h, BlockPos mouth, int button, int times) {
        MenuPlayer player = new MenuPlayer(h.getLevel());
        BlockPos at = h.absolutePos(mouth);
        player.moveTo(at.getX() + 0.5, at.getY() + 1, at.getZ() - 1.5);
        player.openMenu(h.getLevel().getCapability(LogisticsCapabilities.MOUTH_SCREEN, at, null), at);
        for (int i = 0; i < times; i++) player.containerMenu.clickMenuButton(player, button);
        player.closeContainer();
    }

    /** A server player outside the player list that opens menus without a client, as in the stem tests. */
    private static final class MenuPlayer extends FakePlayer {
        MenuPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.fromString("6c1d2c3e-4b5f-4a6b-8c7d-9e0f1a2b3c4d"), "larder_tests"));
        }

        @Override
        public OptionalInt openMenu(MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extra) {
            containerMenu = provider.createMenu(1, getInventory(), this);
            return OptionalInt.of(1);
        }
    }
}
