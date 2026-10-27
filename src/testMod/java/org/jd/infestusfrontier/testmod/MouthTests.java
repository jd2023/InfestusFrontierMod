package org.jd.infestusfrontier.testmod;

import com.mojang.authlib.GameProfile;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.interaction.InteractionModule;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.MouthBlock;
import org.jd.infestusfrontier.logistics.MouthMenu;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.logistics.Port;
import org.jd.infestusfrontier.logistics.VascularJunctionBlock;
import org.jd.infestusfrontier.logistics.api.LogisticsCapabilities;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;

/**
 * Mouths under chests and organs, joined by Item Veins: routing by filter and priority, amounts, retained remainders through reload and break, a destination that disappears, an organ fed and emptied by one Mouth, the
 * probe on a store, and the shared pulse quota. Cells sit at y=1, their stores at y=2.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = "infestusfrontier_tests", bus = EventBusSubscriber.Bus.MOD)
public final class MouthTests {
    private static final int Y = 1;
    /** When a freshly placed output Mouth sends its first transfer. */
    private static final int FIRST_SEND = 8;

    private MouthTests() {}

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void anOutputMouthSendsAChestsItemsThroughVeinsToAnIntakeMouthsChest(GameTestHelper h) {
        BlockPos from = cell(1);
        BlockPos to = cell(5);
        store(h, from, new ItemStack(Items.BONE, 16));
        store(h, to);
        mouth(h, from, MouthMode.OUTPUT);
        veins(h, 2, 4);
        mouth(h, to, MouthMode.INTAKE);
        h.runAfterDelay(100, () -> {
            h.assertValueEqual(count(h, to, Items.BONE), 16, "every bone arrived");
            h.assertValueEqual(count(h, from, Items.BONE), 0, "the source is empty");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aVeinAboveAMouthDoesNotJoinIt(GameTestHelper h) {
        mouth(h, cell(1), MouthMode.INTAKE);
        h.setBlock(cell(1).above(), LogisticsModule.ITEM_VEIN.get());
        h.setBlock(cell(2), LogisticsModule.ITEM_VEIN.get());
        h.assertFalse(h.getBlockState(cell(1)).getValue(PipeBlock.UP), "the top face holds the store");
        h.assertFalse(h.getBlockState(cell(1).above()).getValue(PipeBlock.DOWN), "the vein above stays apart");
        h.assertTrue(h.getBlockState(cell(1)).getValue(PipeBlock.EAST), "the vein beside joins");
        h.assertTrue(h.getBlockState(cell(2)).getValue(PipeBlock.WEST), "both ways");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aMouthSwitchedToOutputOnItsScreenStartsSending(GameTestHelper h) {
        store(h, cell(1), new ItemStack(Items.BONE, 8));
        store(h, cell(3));
        mouth(h, cell(1), MouthMode.INTAKE);
        veins(h, 2, 2);
        mouth(h, cell(3), MouthMode.INTAKE);
        h.runAfterDelay(20, () -> {
            configure(h, cell(1), control -> control.press(MouthMenu.MODE_BUTTON, 1));
            h.assertTrue(h.getBlockState(cell(1)).getValue(MouthBlock.MODE) == MouthMode.OUTPUT, "now output");
            h.runAfterDelay(60, () -> {
                h.assertValueEqual(count(h, cell(3), Items.BONE), 8, "the switched Mouth sent its store");
                h.succeed();
            });
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 300)
    public static void theOneMouthThatAllowsAnItemGetsItHoweverManyComeFirst(GameTestHelper h) {
        // nineteen takers in a 5 by 4 block; only the farthest allows bones, the rest list sticks
        BlockPos source = new BlockPos(1, Y, 1);
        BlockPos farthest = new BlockPos(5, Y, 4);
        grid(h, source);
        store(h, source, new ItemStack(Items.BONE, 8));
        for (int x = 1; x <= 5; x++)
            for (int z = 1; z <= 4; z++) {
                BlockPos at = new BlockPos(x, Y, z);
                if (!at.equals(source) && !at.equals(farthest)) configure(h, at, control -> control.list(Items.STICK));
            }
        h.runAfterDelay(200, () -> {
            h.assertValueEqual(count(h, farthest, Items.BONE), 8, "the bones reached the last Mouth in order");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void fullStoresFirstInOrderDoNotHideAnEmptyOneBehindThem(GameTestHelper h) {
        BlockPos source = new BlockPos(1, Y, 1);
        BlockPos farthest = new BlockPos(5, Y, 4);
        grid(h, source);
        store(h, source, new ItemStack(Items.BONE, 8));
        for (int x = 1; x <= 5; x++)
            for (int z = 1; z <= 4; z++) {
                BlockPos at = new BlockPos(x, Y, z);
                if (at.equals(source) || at.equals(farthest)) continue;
                ChestBlockEntity full = (ChestBlockEntity) h.getBlockEntity(at.above());
                for (int slot = 0; slot < full.getContainerSize(); slot++)
                    full.setItem(slot, new ItemStack(Items.STONE, 64));
            }
        h.runAfterDelay(300, () -> {
            h.assertValueEqual(count(h, farthest, Items.BONE), 8, "the bones reached the one store with room");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void withNoMouthThatTakesNothingLeavesTheStore(GameTestHelper h) {
        store(h, cell(1), new ItemStack(Items.BONE, 16));
        store(h, cell(3));
        mouth(h, cell(1), MouthMode.OUTPUT);
        veins(h, 2, 2);
        mouth(h, cell(3), MouthMode.CLOSED);
        h.runAfterDelay(60, () -> {
            h.assertValueEqual(count(h, cell(1), Items.BONE), 16, "nothing extracted");
            h.assertValueEqual(count(h, cell(3), Items.BONE), 0, "nothing delivered");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void filtersSendEachItemToTheMouthThatAllowsIt(GameTestHelper h) {
        store(h, cell(1), new ItemStack(Items.BONE, 8), new ItemStack(Items.STICK, 8));
        store(h, cell(3));
        store(h, cell(5));
        mouth(h, cell(1), MouthMode.OUTPUT);
        veins(h, 2, 2);
        mouth(h, cell(3), MouthMode.INTAKE);
        veins(h, 4, 4);
        mouth(h, cell(5), MouthMode.INTAKE);
        configure(h, cell(3), control -> control.list(Items.BONE));
        configure(h, cell(5), control -> control.list(Items.BONE).press(MouthMenu.LIST_BUTTON, 1));
        h.runAfterDelay(120, () -> {
            h.assertValueEqual(count(h, cell(3), Items.BONE), 8, "bones to the Mouth that allows bones");
            h.assertValueEqual(count(h, cell(3), Items.STICK), 0, "no sticks there");
            h.assertValueEqual(count(h, cell(5), Items.STICK), 8, "sticks to the Mouth that denies bones");
            h.assertValueEqual(count(h, cell(5), Items.BONE), 0, "no bones there");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void priorityBeatsDistance(GameTestHelper h) {
        store(h, cell(1), new ItemStack(Items.BONE, 32));
        store(h, cell(3));
        store(h, cell(6));
        mouth(h, cell(1), MouthMode.OUTPUT);
        veins(h, 2, 2);
        mouth(h, cell(3), MouthMode.INTAKE);
        veins(h, 4, 5);
        mouth(h, cell(6), MouthMode.INTAKE);
        configure(h, cell(6), control -> control.press(MouthMenu.PRIORITY_UP, 1));
        h.runAfterDelay(150, () -> {
            h.assertValueEqual(count(h, cell(6), Items.BONE), 32, "the far Mouth of higher priority took all");
            h.assertValueEqual(count(h, cell(3), Items.BONE), 0, "the near one none");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void equalDestinationsTakeTurns(GameTestHelper h) {
        store(h, cell(3), new ItemStack(Items.BONE, 32));
        store(h, cell(1));
        store(h, cell(5));
        mouth(h, cell(3), MouthMode.OUTPUT);
        veins(h, 2, 2);
        veins(h, 4, 4);
        mouth(h, cell(1), MouthMode.INTAKE);
        mouth(h, cell(5), MouthMode.INTAKE);
        h.runAfterDelay(150, () -> {
            h.assertValueEqual(count(h, cell(1), Items.BONE), 16, "west half");
            h.assertValueEqual(count(h, cell(5), Items.BONE), 16, "east half");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void anOutputKeepsItsAmountAndAnIntakeFillsUpToItsOwn(GameTestHelper h) {
        store(h, cell(1), new ItemStack(Items.BONE, 20));
        store(h, cell(3));
        store(h, cell(5), new ItemStack(Items.STICK, 20));
        store(h, cell(7));
        mouth(h, cell(1), MouthMode.OUTPUT);
        veins(h, 2, 2);
        mouth(h, cell(3), MouthMode.INTAKE);
        mouth(h, cell(5), MouthMode.OUTPUT);
        veins(h, 6, 6);
        mouth(h, cell(7), MouthMode.INTAKE);
        h.setBlock(cell(4), Blocks.STONE); // two networks side by side
        configure(h, cell(1), control -> control.press(MouthMenu.AMOUNT_UP, 3)); // 0, 1, 4, 8: keep 8
        configure(h, cell(7), control -> control.press(MouthMenu.AMOUNT_UP, 2)); // fill up to 4
        h.runAfterDelay(120, () -> {
            h.assertValueEqual(count(h, cell(1), Items.BONE), 8, "the output kept eight");
            h.assertValueEqual(count(h, cell(3), Items.BONE), 12, "the rest arrived");
            h.assertValueEqual(count(h, cell(7), Items.STICK), 4, "the intake filled to four");
            h.assertValueEqual(count(h, cell(5), Items.STICK), 16, "the other sticks stayed home");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aFullDestinationLeavesTheItemsInTheSource(GameTestHelper h) {
        store(h, cell(1), new ItemStack(Items.BONE, 16));
        ChestBlockEntity full = store(h, cell(3));
        for (int slot = 0; slot < full.getContainerSize(); slot++) full.setItem(slot, new ItemStack(Items.STONE, 64));
        mouth(h, cell(1), MouthMode.OUTPUT);
        veins(h, 2, 2);
        mouth(h, cell(3), MouthMode.INTAKE);
        h.runAfterDelay(60, () -> {
            h.assertValueEqual(count(h, cell(1), Items.BONE), 16, "nothing left the source");
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "mouth_remainder",
            timeoutTicks = 200)
    public static void aRetainedRemainderSurvivesReloadAndIsDeliveredOnce(GameTestHelper h) {
        store(h, cell(1), new ItemStack(Items.BONE, 8));
        store(h, cell(8));
        mouth(h, cell(1), MouthMode.OUTPUT);
        veins(h, 2, 7);
        mouth(h, cell(8), MouthMode.INTAKE);
        var registries = h.getLevel().registryAccess();
        CompoundTag saved = h.getBlockEntity(cell(1)).saveWithFullMetadata(registries);
        saved.put("remainder", new ItemStack(Items.BONE, 8).save(registries));
        BlockEntity restored =
                BlockEntity.loadStatic(h.absolutePos(cell(1)), h.getBlockState(cell(1)), saved, registries);
        h.getLevel().removeBlockEntity(h.absolutePos(cell(1)));
        h.getLevel().setBlockEntity(restored);
        BlockPos sender = h.absolutePos(cell(1));
        h.getLevel().getBlockState(sender).tick(h.getLevel(), sender, h.getLevel().random);
        h.assertValueEqual(count(h, cell(8), Items.BONE), 8, "one pulse delivers only the remainder");
        h.assertValueEqual(count(h, cell(1), Items.BONE), 8, "fresh extraction waits for another pulse");
        h.runAfterDelay(100, () -> {
            h.assertValueEqual(count(h, cell(8), Items.BONE), 16, "remainder and source delivered exactly once");
            h.assertValueEqual(count(h, cell(1), Items.BONE), 0, "source empty");
            h.assertFalse(
                    h.getBlockEntity(cell(1)).saveWithoutMetadata(registries).contains("remainder"),
                    "no held remainder");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void breakingAMouthDropsItsRemainderOnce(GameTestHelper h) {
        store(h, cell(1), new ItemStack(Items.BONE, 16));
        mouth(h, cell(1), MouthMode.OUTPUT);
        var registries = h.getLevel().registryAccess();
        CompoundTag saved = h.getBlockEntity(cell(1)).saveWithoutMetadata(registries);
        saved.put("remainder", new ItemStack(Items.BONE, 8).save(registries));
        h.getBlockEntity(cell(1)).loadWithComponents(saved, registries);
        h.getLevel().destroyBlock(h.absolutePos(cell(1)), true);
        h.runAfterDelay(2, () -> {
            h.assertItemEntityCountIs(Items.BONE, cell(1), 2, 8);
            h.assertValueEqual(count(h, cell(1), Items.BONE), 16, "partner inventory remains intact");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aRemovedDestinationIsSkippedBeforeExtraction(GameTestHelper h) {
        store(h, cell(4), new ItemStack(Items.BONE, 8));
        store(h, cell(1));
        store(h, cell(9));
        mouth(h, cell(4), MouthMode.OUTPUT);
        veins(h, 2, 3);
        mouth(h, cell(1), MouthMode.INTAKE);
        veins(h, 5, 8);
        mouth(h, cell(9), MouthMode.INTAKE);
        configure(h, cell(9), control -> control.press(MouthMenu.PRIORITY_UP, 1));
        h.setBlock(cell(9).above(), Blocks.STONE);
        h.runAfterDelay(80, () -> {
            h.assertValueEqual(count(h, cell(1), Items.BONE), 8, "live accepting endpoint receives the transfer");
            h.assertValueEqual(count(h, cell(4), Items.BONE), 0, "source debited once");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 600)
    public static void aBothMouthFeedsALoomAndTakesItsPlates(GameTestHelper h) {
        store(h, cell(1), new ItemStack(Items.BONE, 1));
        BlockPos loom = cell(3).above();
        store(h, cell(5));
        mouth(h, cell(1), MouthMode.OUTPUT);
        veins(h, 2, 2);
        mouth(h, cell(3), MouthMode.BOTH);
        veins(h, 4, 4);
        mouth(h, cell(5), MouthMode.INTAKE);
        h.setBlock(loom, ProcessingModule.BONE_LOOM.get());
        h.getLevel()
                .getCapability(BiomassCapabilities.BLOCK, h.absolutePos(loom), null)
                .insert(Biomass.of(200), false);
        configure(h, cell(5), control -> control.list(ProcessingModule.BONE_PLATE.get()));
        h.runAfterDelay(500, () -> {
            h.assertValueEqual(count(h, cell(5), ProcessingModule.BONE_PLATE.get()), 1, "the plate was delivered");
            h.assertValueEqual(count(h, cell(5), Items.BONE), 0, "no bone reached the plate chest");
            h.assertValueEqual(count(h, cell(1), Items.BONE), 0, "the source fed the loom");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anOutputMouthNeverTakesAnOrgansIngredients(GameTestHelper h) {
        BlockPos loom = cell(1).above();
        mouth(h, cell(1), MouthMode.OUTPUT);
        veins(h, 2, 2);
        store(h, cell(3));
        mouth(h, cell(3), MouthMode.INTAKE);
        h.setBlock(loom, ProcessingModule.BONE_LOOM.get());
        var input = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(loom), Direction.DOWN);
        h.assertTrue(input.insertItem(0, new ItemStack(Items.BONE), false).isEmpty(), "bone admitted");
        h.runAfterDelay(60, () -> {
            h.assertValueEqual(count(h, cell(3), Items.BONE), 0, "the Mouth took no ingredient");
            h.assertValueEqual(input.getStackInSlot(0).getCount(), 1, "the bone stays in the unfed loom");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theProbeOnAStoreOpensItsMouthAndSneakingDoesNot(GameTestHelper h) {
        store(h, cell(1));
        mouth(h, cell(1), MouthMode.INTAKE);
        ServerPlayer player = fakePlayer(h);
        BlockPos chest = h.absolutePos(cell(1).above());
        player.moveTo(chest.getX() + 0.5, chest.getY() + 1, chest.getZ() - 1.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(InteractionModule.SYNAPTIC_PROBE.get()));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(chest), Direction.NORTH, chest, false);
        player.setShiftKeyDown(true);
        player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        h.assertFalse(player.containerMenu instanceof MouthMenu, "sneaking reads instead");
        player.setShiftKeyDown(false);
        player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        boolean opened = player.containerMenu instanceof MouthMenu;
        player.closeContainer();
        h.assertTrue(opened, "the probe on the chest opened the Mouth under it");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "mouth_boundary")
    public static void aDoubleChestAcrossAnUnloadedChunkIsNoStore(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        // x ends in 15: the chest's other half would stand in the next chunk, which nothing has loaded
        BlockPos edge = new BlockPos((origin.getX() & ~15) + 160015, 80, (origin.getZ() & ~15) + 160008);
        level.getChunkAt(edge);
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (MouthMode atTheEdge : new MouthMode[] {MouthMode.OUTPUT, MouthMode.INTAKE}) {
            BlockPos edgeMouth = edge.south(atTheEdge == MouthMode.OUTPUT ? 0 : 4);
            BlockPos other = edgeMouth.west(2);
            h.assertFalse(level.hasChunkAt(edgeMouth.east()), "the next chunk starts unloaded");
            level.setBlock(
                    edgeMouth.above(),
                    Blocks.CHEST
                            .defaultBlockState()
                            .setValue(ChestBlock.FACING, Direction.NORTH)
                            .setValue(ChestBlock.TYPE, ChestType.LEFT),
                    quiet);
            level.setBlock(other.above(), Blocks.CHEST.defaultBlockState(), quiet);
            level.setBlock(edgeMouth, mouthState(atTheEdge), quiet);
            level.setBlock(edgeMouth.west(), LogisticsModule.ITEM_VEIN.get().defaultBlockState(), quiet);
            MouthMode across = atTheEdge == MouthMode.OUTPUT ? MouthMode.INTAKE : MouthMode.OUTPUT;
            level.setBlock(other, mouthState(across), quiet);
            BlockPos giver = atTheEdge == MouthMode.OUTPUT ? edgeMouth : other;
            BlockPos taker = atTheEdge == MouthMode.OUTPUT ? other : edgeMouth;
            ChestBlockEntity source = (ChestBlockEntity) level.getBlockEntity(giver.above());
            ChestBlockEntity target = (ChestBlockEntity) level.getBlockEntity(taker.above());
            source.setItem(0, new ItemStack(Items.BONE, 8));
            level.getBlockState(giver).tick(level, giver, level.random);
            h.assertFalse(level.hasChunkAt(edgeMouth.east()), "no lookup loaded the other half");
            h.assertValueEqual(source.getItem(0).getCount(), 8, "the source keeps its items");
            h.assertTrue(target.isEmpty(), "the target got nothing");
        }
        h.succeed();
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "mouth_unload",
            timeoutTicks = 900)
    public static void aRouteCutByAnUnloadedChunkIsFoundWhenTheChunkLoads(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        BlockPos start = new BlockPos((origin.getX() & ~15) + 192002, 80, (origin.getZ() & ~15) + 192008);
        // the destination stands three chunks east: once only the first chunk is forced, it is not loaded
        BlockPos end = start.east(48);
        ChunkPos first = new ChunkPos(start);
        ChunkPos last = new ChunkPos(end);
        for (int x = first.x; x <= last.x; x++) TestChunks.force(level, new ChunkPos(x, first.z));
        level.setBlockAndUpdate(start.above(), Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) level.getBlockEntity(start.above())).setItem(0, new ItemStack(Items.BONE, 8));
        level.setBlockAndUpdate(start, mouthState(MouthMode.CLOSED));
        for (int x = 1; x < 48; x++)
            level.setBlockAndUpdate(
                    start.east(x), LogisticsModule.ITEM_VEIN.get().defaultBlockState());
        level.setBlockAndUpdate(end, mouthState(MouthMode.INTAKE));
        level.setBlockAndUpdate(end.above(), Blocks.CHEST.defaultBlockState());
        for (int x = first.x + 1; x <= last.x; x++) level.setChunkForced(x, first.z, false);
        // the game time at each step, so a failure names the step that was slow
        long[] at = new long[2];
        h.startSequence()
                .thenWaitUntil(() -> h.assertFalse(level.hasChunkAt(end), "the far chunk unloaded"))
                // scheduled block ticks run only where the chunk's entities have loaded, which a busy server may delay
                .thenWaitUntil(() -> h.assertTrue(
                        level.areEntitiesLoaded(ChunkPos.asLong(start)), "the source chunk ticks its scheduled ticks"))
                .thenExecute(() -> at[0] = level.getGameTime())
                .thenExecute(() -> level.setBlockAndUpdate(start, mouthState(MouthMode.OUTPUT)))
                .thenIdle(60)
                .thenExecute(() -> h.assertValueEqual(
                        ((ChestBlockEntity) level.getBlockEntity(start.above()))
                                .getItem(0)
                                .getCount(),
                        8,
                        "no destination while its chunk is unloaded"))
                .thenExecute(() -> {
                    TestChunks.force(level, last);
                    at[1] = level.getGameTime();
                })
                .thenWaitUntil(() -> h.assertTrue(
                        level.hasChunkAt(end)
                                && level.getBlockEntity(end.above()) instanceof ChestBlockEntity chest
                                && chest.getItem(0).getCount() == 8,
                        "found and served once the chunk loaded, with no network edited; ready at " + at[0]
                                + ", forced at " + at[1] + ", now " + level.getGameTime() + ", far loaded "
                                + level.hasChunkAt(end) + ", source holds "
                                + ((ChestBlockEntity) level.getBlockEntity(start.above()))
                                        .getItem(0)
                                        .getCount()
                                + ", pulse scheduled "
                                + level.getBlockTicks().hasScheduledTick(start, LogisticsModule.MOUTH.get())
                                + ", source chunk entity-ticking " + level.isPositionEntityTicking(start)
                                + ", graph "
                                + level.getCapability(
                                        org.jd.infestusfrontier.logistics.api.LogisticsCapabilities.NETWORK_STATUS,
                                        start,
                                        null)
                                + ", path " + path(level, start, end)))
                .thenExecute(() -> {
                    level.setChunkForced(first.x, first.z, false);
                    level.setChunkForced(last.x, last.z, false);
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aMouthWithUnreadableDataKeepsItAndRefusesEverything(GameTestHelper h) {
        store(h, cell(1), new ItemStack(Items.BONE, 16));
        store(h, cell(3));
        mouth(h, cell(1), MouthMode.OUTPUT);
        veins(h, 2, 2);
        mouth(h, cell(3), MouthMode.INTAKE);
        CompoundTag data = new CompoundTag();
        CompoundTag items = new CompoundTag();
        items.putString("id", "minecraft:bone");
        items.putInt("count", 9);
        data.put("remainder", items);
        var registries = h.getLevel().registryAccess();
        h.getBlockEntity(cell(1)).loadWithComponents(data, registries);
        configure(h, cell(1), control -> {
            control.press(MouthMenu.MODE_BUTTON, 1)
                    .press(MouthMenu.AMOUNT_UP, 1)
                    .list(Items.STICK);
            h.assertTrue(control.menu().getSlot(0).getItem().isEmpty(), "the filter refuses a click");
        });
        h.assertTrue(h.getBlockState(cell(1)).getValue(MouthBlock.MODE) == MouthMode.OUTPUT, "the mode stays");
        h.runAfterDelay(40, () -> {
            h.assertValueEqual(count(h, cell(1), Items.BONE), 16, "nothing extracted");
            h.assertValueEqual(count(h, cell(3), Items.BONE), 0, "nothing delivered");
            CompoundTag saved = h.getBlockEntity(cell(1)).saveWithoutMetadata(registries);
            h.assertValueEqual(saved.getCompound("remainder"), items, "the bytes save back unchanged");
            h.getLevel().destroyBlock(h.absolutePos(cell(1)), true);
            h.runAfterDelay(2, () -> {
                h.assertItemEntityCountIs(Items.BONE, cell(1), 2, 0);
                h.assertItemEntityCountIs(EcologyModule.DEAD_TISSUE.get(), cell(1), 2, 1);
                h.assertItemEntityNotPresent(LogisticsModule.MOUTH_ITEM.get(), cell(1), 2);
                h.succeed();
            });
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "mouth_offers",
            timeoutTicks = 300)
    public static void aPulseBoundsOffersAndRetainsARefusedExecution(GameTestHelper h) {
        // one giving Mouth and nineteen taking Mouths in a 5 by 4 block, each under a store that says yes when asked
        // and then refuses the items; once a remainder is held, the stores refuse even when asked
        Counter.reset();
        for (int x = 1; x <= 5; x++)
            for (int z = 1; z <= 4; z++) {
                BlockPos at = new BlockPos(x, Y, z);
                mouth(h, at, x == 1 && z == 1 ? MouthMode.OUTPUT : MouthMode.INTAKE);
                if (x != 1 || z != 1) h.setBlock(at.above(), Blocks.LODESTONE);
            }
        store(h, new BlockPos(1, Y, 1), new ItemStack(Items.BONE, 64));
        h.startSequence()
                .thenIdle(50)
                .thenExecute(() -> {
                    Counter.refuseAll = true;
                    Counter.max = 0;
                })
                .thenIdle(120)
                .thenExecute(() -> {
                    h.assertTrue(
                            Counter.max <= 18,
                            "at most 16 offers and one simulated/executed delivery a pulse, saw " + Counter.max
                                    + " inserts in a tick");
                    int[] carried = new int[1];
                    configure(
                            h,
                            new BlockPos(1, Y, 1),
                            control -> carried[0] = control.menu().carried());
                    h.assertValueEqual(
                            count(h, new BlockPos(1, Y, 1), Items.BONE) + carried[0], 64, "every bone is somewhere");
                    Counter.reset();
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aMouthNeverTakesTheSacsFeed(GameTestHelper h) {
        BlockPos sac = cell(1).above();
        mouth(h, cell(1), MouthMode.OUTPUT);
        veins(h, 2, 2);
        store(h, cell(3));
        mouth(h, cell(3), MouthMode.INTAKE);
        h.setBlock(sac, ProcessingModule.DIGESTIVE_SAC.get());
        var port = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(sac), Direction.DOWN);
        h.assertTrue(
                port.insertItem(0, new ItemStack(Items.ROTTEN_FLESH, 4), false).isEmpty(), "feed goes in");
        h.assertTrue(port.extractItem(0, 64, true).isEmpty(), "and never comes out");
        h.runAfterDelay(40, () -> {
            h.assertValueEqual(count(h, cell(3), Items.ROTTEN_FLESH), 0, "the Mouth took no feed");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theProbeOnAJunctionStandingOnAMouthCyclesItsPort(GameTestHelper h) {
        mouth(h, cell(1), MouthMode.INTAKE);
        BlockPos junction = cell(1).above();
        h.setBlock(junction, LogisticsModule.VASCULAR_JUNCTION.get());
        ServerPlayer player = fakePlayer(h);
        BlockPos at = h.absolutePos(junction);
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(InteractionModule.SYNAPTIC_PROBE.get()));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.NORTH, at, false);
        player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        h.assertFalse(player.containerMenu instanceof MouthMenu, "the Mouth below stays shut");
        h.assertTrue(
                h.getBlockState(junction).getValue(VascularJunctionBlock.PORT.get(Direction.NORTH)) != Port.CLOSED,
                "the clicked port moved on");
        h.succeed();
    }

    /** The first cell from {@code start} to {@code end} along x that is not what the route needs, or "whole". */
    private static String path(ServerLevel level, BlockPos start, BlockPos end) {
        for (BlockPos at = start.east(); !at.equals(end); at = at.east()) {
            if (!level.hasChunkAt(at)) return "unloaded at " + at.getX();
            if (!level.getBlockState(at).is(LogisticsModule.ITEM_VEIN.get()))
                return level.getBlockState(at) + " at " + at.getX();
        }
        return "whole; end " + level.getBlockState(end) + " with " + level.getBlockEntity(end) + ", store "
                + level.getBlockEntity(end.above());
    }

    // --- fixtures ---

    private static BlockPos cell(int x) {
        return new BlockPos(x, Y, 1);
    }

    private static void mouth(GameTestHelper h, BlockPos at, MouthMode mode) {
        h.setBlock(at, mouthState(mode));
    }

    /** A giving Mouth at {@code source} and taking Mouths on the rest of a 5 by 4 block, each under a chest. */
    private static void grid(GameTestHelper h, BlockPos source) {
        for (int x = 1; x <= 5; x++)
            for (int z = 1; z <= 4; z++) {
                BlockPos at = new BlockPos(x, Y, z);
                mouth(h, at, at.equals(source) ? MouthMode.OUTPUT : MouthMode.INTAKE);
                store(h, at);
            }
    }

    private static BlockState mouthState(MouthMode mode) {
        return LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, mode);
    }

    /**
     * A store on lodestone, for the offer budget test only (batch mouth_offers runs alone): it counts every insert
     * per tick, says yes to a simulated insert until {@link #refuseAll}, and never takes the items.
     */
    @SubscribeEvent
    public static void countingStore(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.ItemHandler.BLOCK,
                (level, pos, state, entity, side) -> new IItemHandler() {
                    @Override
                    public int getSlots() {
                        return 1;
                    }

                    @Override
                    public ItemStack getStackInSlot(int slot) {
                        return ItemStack.EMPTY;
                    }

                    @Override
                    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                        Counter.hit(level.getGameTime());
                        return simulate && !Counter.refuseAll ? ItemStack.EMPTY : stack;
                    }

                    @Override
                    public ItemStack extractItem(int slot, int amount, boolean simulate) {
                        return ItemStack.EMPTY;
                    }

                    @Override
                    public int getSlotLimit(int slot) {
                        return 64;
                    }

                    @Override
                    public boolean isItemValid(int slot, ItemStack stack) {
                        return true;
                    }
                },
                Blocks.LODESTONE);
    }

    private static final class Counter {
        static boolean refuseAll;
        static long tick = -1;
        static int now;
        static int max;

        static void hit(long time) {
            if (time != tick) {
                tick = time;
                now = 0;
            }
            max = Math.max(max, ++now);
        }

        static void reset() {
            refuseAll = false;
            tick = -1;
            now = 0;
            max = 0;
        }
    }

    private static void veins(GameTestHelper h, int fromX, int toX) {
        for (int x = fromX; x <= toX; x++) h.setBlock(cell(x), LogisticsModule.ITEM_VEIN.get());
    }

    /** A chest on the cell, holding {@code contents} from its first slot. */
    private static ChestBlockEntity store(GameTestHelper h, BlockPos at, ItemStack... contents) {
        h.setBlock(at.above(), Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) h.getBlockEntity(at.above());
        for (int slot = 0; slot < contents.length; slot++) chest.setItem(slot, contents[slot]);
        return chest;
    }

    private static int count(GameTestHelper h, BlockPos at, Item item) {
        var handler = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(at.above()), null);
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++)
            if (handler.getStackInSlot(slot).is(item))
                total += handler.getStackInSlot(slot).getCount();
        return total;
    }

    /** The Mouth's screen as a player opens it; settings change only through its buttons and filter slots. */
    private record Control(ServerPlayer player, MouthMenu menu) {
        Control press(int button, int times) {
            for (int i = 0; i < times; i++) menu.clickMenuButton(player, button);
            return this;
        }

        Control list(Item... items) {
            for (int slot = 0; slot < items.length; slot++) {
                menu.setCarried(new ItemStack(items[slot], 5));
                menu.clicked(slot, 0, ClickType.PICKUP, player);
            }
            menu.setCarried(ItemStack.EMPTY);
            return this;
        }
    }

    private static void configure(GameTestHelper h, BlockPos mouth, Consumer<Control> settings) {
        ServerPlayer player = fakePlayer(h);
        BlockPos at = h.absolutePos(mouth);
        player.moveTo(at.getX() + 0.5, at.getY() + 1, at.getZ() - 1.5);
        MenuProvider provider = h.getLevel().getCapability(LogisticsCapabilities.MOUTH_SCREEN, at, null);
        player.openMenu(provider, at);
        settings.accept(new Control(player, (MouthMenu) player.containerMenu));
        player.closeContainer();
    }

    private static ServerPlayer fakePlayer(GameTestHelper h) {
        return new MenuPlayer(h.getLevel());
    }

    /**
     * A server player that is never in the player list, so no login event or other test sees it. It opens a menu the
     * way the server does, without the transfer to a client it does not have.
     */
    private static final class MenuPlayer extends FakePlayer {
        MenuPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.fromString("3c6e8f1a-6d1e-4b8e-9a64-2f5c0e7d4b11"), "mouth_tests"));
        }

        @Override
        public OptionalInt openMenu(MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extra) {
            containerMenu = provider.createMenu(1, getInventory(), this);
            return OptionalInt.of(1);
        }
    }
}
