package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.TickQuota;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = "infestusfrontier_tests", bus = EventBusSubscriber.Bus.MOD)
public final class MouthTests {
    private static final BlockPos HOST = new BlockPos(2, 2, 2);
    private static final BlockPos MOUTH = HOST.east();
    private static final BlockPos PARTNER = MOUTH.east();

    private MouthTests() {}

    @SubscribeEvent
    public static void rejectingTarget(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.ItemHandler.BLOCK,
                (level, pos, state, entity, side) -> new ItemStackHandler(1) {
                    @Override
                    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                        return simulate ? super.insertItem(slot, stack, true) : stack;
                    }
                },
                Blocks.GOLD_BLOCK);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void refusedInsertionSurvivesReloadAndBreakingDropsItsPacketOnce(GameTestHelper h) {
        chest(h, HOST).setItem(0, new ItemStack(Items.BONE, 8));
        h.setBlock(PARTNER, Blocks.GOLD_BLOCK);
        mouth(h, false);
        h.runAfterDelay(12, () -> {
            h.assertValueEqual(count(h, HOST), 0, "packet was extracted");
            var registries = h.getLevel().registryAccess();
            CompoundTag saved = h.getBlockEntity(MOUTH).saveWithFullMetadata(registries);
            h.assertValueEqual(saved.getCompound("pending").getInt("count"), 8, "refused packet is saved");
            BlockEntity restored =
                    BlockEntity.loadStatic(h.absolutePos(MOUTH), h.getBlockState(MOUTH), saved, registries);
            h.getLevel().removeBlockEntity(h.absolutePos(MOUTH));
            h.getLevel().setBlockEntity(restored);
            h.getLevel().destroyBlock(h.absolutePos(MOUTH), true);
            h.runAfterDelay(2, () -> {
                h.assertItemEntityCountIs(Items.BONE, MOUTH, 2, 8);
                h.assertItemEntityCountIs(LogisticsModule.OUTPUT_MOUTH_ITEM.get(), MOUTH, 2, 1);
                for (var drop : h.getEntities(EntityType.ITEM, MOUTH, 2))
                    if (drop.getItem().is(LogisticsModule.OUTPUT_MOUTH_ITEM.get()))
                        h.assertFalse(
                                drop.getItem().has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA),
                                "the mouth item holds no second copy");
                h.succeed();
            });
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void refusedPacketResumesWhenThePartnerChanges(GameTestHelper h) {
        chest(h, HOST).setItem(0, new ItemStack(Items.BONE, 8));
        h.setBlock(PARTNER, Blocks.GOLD_BLOCK);
        mouth(h, false);
        h.runAfterDelay(12, () -> {
            chest(h, PARTNER);
            h.runAfterDelay(24, () -> {
                h.assertValueEqual(count(h, PARTNER), 8, "retained packet delivered");
                h.assertValueEqual(count(h, HOST), 0, "no second extraction");
                h.assertTrue(
                        h.getBlockEntity(MOUTH)
                                .saveWithoutMetadata(h.getLevel().registryAccess())
                                .getCompound("pending")
                                .isEmpty(),
                        "buffer cleared");
                h.succeed();
            });
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void corruptPacketIsPreservedAndTheMouthRefusesWork(GameTestHelper h) {
        chest(h, HOST).setItem(0, new ItemStack(Items.BONE, 8));
        chest(h, PARTNER);
        mouth(h, false);
        CompoundTag data = new CompoundTag();
        CompoundTag packet = new CompoundTag();
        packet.putString("id", "minecraft:bone");
        packet.putInt("count", 9);
        data.put("pending", packet);
        h.getBlockEntity(MOUTH).loadWithComponents(data, h.getLevel().registryAccess());
        h.runAfterDelay(25, () -> {
            h.assertValueEqual(count(h, HOST), 8, "nothing extracted");
            h.assertValueEqual(count(h, PARTNER), 0, "nothing delivered");
            h.assertValueEqual(
                    h.getBlockEntity(MOUTH)
                            .saveWithoutMetadata(h.getLevel().registryAccess())
                            .getCompound("pending"),
                    packet,
                    "unreadable data retained");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "mouth_quota")
    public static void mouthsShareTheDimensionQuotaAndRetryAfterDenial(GameTestHelper h) {
        chest(h, HOST).setItem(0, new ItemStack(Items.BONE, 8));
        chest(h, PARTNER);
        mouth(h, false);
        var type = NeoForgeRegistries.ATTACHMENT_TYPES.get(
                ResourceLocation.fromNamespaceAndPath("infestusfrontier", "mouth_quota"));
        TickQuota quota = (TickQuota) h.getLevel().getData(type);
        long tick = h.getLevel().getGameTime();
        for (int i = 0; i < 64; i++) h.assertTrue(quota.take(tick), "quota slot " + i);
        h.assertFalse(quota.take(tick), "sixty fifth pulse refused");
        h.getBlockState(MOUTH).tick(h.getLevel(), h.absolutePos(MOUTH), h.getLevel().random);
        h.assertValueEqual(count(h, HOST), 8, "quota denial leaves the source alone");
        h.runAfterDelay(25, () -> {
            h.assertValueEqual(count(h, PARTNER), 8, "later admission moves the packet");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 460)
    public static void twoMouthsRunALoomFromAChestIntoAChest(GameTestHelper h) {
        BlockPos loom = new BlockPos(3, 2, 3);
        h.setBlock(loom.below(), EcologyTests.mature());
        h.setBlock(loom, ProcessingModule.BONE_LOOM.get());
        h.getLevel()
                .getCapability(BiomassCapabilities.BLOCK, h.absolutePos(loom), null)
                .insert(Biomass.of(50), false);
        chest(h, loom.west(2)).setItem(0, new ItemStack(Items.BONE));
        ChestBlockEntity target = chest(h, loom.east(2));
        h.setBlock(
                loom.west(),
                LogisticsModule.INTAKE_MOUTH
                        .get()
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.WEST));
        h.setBlock(
                loom.east(),
                LogisticsModule.OUTPUT_MOUTH
                        .get()
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.EAST));
        h.runAfterDelay(440, () -> {
            h.assertTrue(target.getItem(0).is(ProcessingModule.BONE_PLATE.get()), "finished plate delivered");
            h.assertValueEqual(target.getItem(0).getCount(), 1, "one bone becomes one plate");
            h.assertValueEqual(count(h, loom.west(2)), 0, "source consumed once");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void intakePullsFromPartnerAndOutputPushesFromHost(GameTestHelper h) {
        chest(h, HOST).setItem(0, new ItemStack(Items.BONE, 16));
        chest(h, PARTNER);
        mouth(h, false);
        h.runAfterDelay(9, () -> {
            h.assertValueEqual(count(h, HOST), 8, "one packet leaves the host");
            h.assertValueEqual(count(h, PARTNER), 8, "one packet reaches the partner");
            h.setBlock(MOUTH, Blocks.AIR);
            mouth(h, true);
            h.runAfterDelay(9, () -> {
                h.assertValueEqual(count(h, HOST), 16, "intake returns the partner packet");
                h.assertValueEqual(count(h, PARTNER), 0, "intake leaves the partner empty");
                h.succeed();
            });
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void fullTargetKeepsItemsUpstreamAndPartialRoomMovesOnlyWhatFits(GameTestHelper h) {
        ChestBlockEntity source = chest(h, HOST);
        source.setItem(0, new ItemStack(Items.BONE, 16));
        ChestBlockEntity target = chest(h, PARTNER);
        for (int i = 0; i < target.getContainerSize(); i++) target.setItem(i, new ItemStack(Items.BONE, 64));
        mouth(h, false);
        h.runAfterDelay(25, () -> {
            h.assertValueEqual(count(h, HOST), 16, "full destination never extracts");
            target.setItem(26, new ItemStack(Items.BONE, 61));
            h.runAfterDelay(45, () -> {
                h.assertValueEqual(count(h, HOST), 13, "only three items leave");
                h.assertValueEqual(target.getItem(26).getCount(), 64, "three items fill the gap in the last slot");
                h.succeed();
            });
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void outputCannotStealAnOrgansIngredients(GameTestHelper h) {
        h.setBlock(HOST.below(), EcologyTests.mature());
        h.setBlock(HOST, ProcessingModule.BONE_LOOM.get());
        var input = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(HOST), Direction.EAST);
        h.assertTrue(input != null, "loom exposes its inventory");
        h.assertTrue(input.insertItem(0, new ItemStack(Items.BONE), false).isEmpty(), "bone admitted");
        chest(h, PARTNER);
        mouth(h, false);
        h.runAfterDelay(30, () -> {
            h.assertValueEqual(count(h, PARTNER), 0, "the mouth cannot extract ingredients");
            h.assertValueEqual(input.getStackInSlot(0).getCount(), 1, "ingredient remains in the unfueled loom");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void outputFeedsAnItemVeinAndRotatedMouthsUseTheirFacing(GameTestHelper h) {
        chest(h, HOST).setItem(0, new ItemStack(Items.BONE, 8));
        h.setBlock(PARTNER, LogisticsModule.ITEM_VEIN.get());
        ChestBlockEntity target = chest(h, PARTNER.above());
        mouth(h, false);
        h.runAfterDelay(22, () -> {
            h.assertValueEqual(target.getItem(0).getCount(), 8, "packet passes through the native vein");
            h.setBlock(MOUTH, Blocks.AIR);
            h.setBlock(
                    HOST.above(),
                    LogisticsModule.OUTPUT_MOUTH
                            .get()
                            .defaultBlockState()
                            .setValue(BlockStateProperties.FACING, Direction.UP));
            ChestBlockEntity upper = chest(h, HOST.above(2));
            ((ChestBlockEntity) h.getBlockEntity(HOST)).setItem(0, new ItemStack(Items.BONE, 8));
            h.runAfterDelay(12, () -> {
                h.assertValueEqual(upper.getItem(0).getCount(), 8, "upward mouth uses the upper partner");
                h.succeed();
            });
        });
    }

    private static void mouth(GameTestHelper h, boolean intake) {
        h.setBlock(
                MOUTH,
                (intake ? LogisticsModule.INTAKE_MOUTH.get() : LogisticsModule.OUTPUT_MOUTH.get())
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.EAST));
    }

    private static ChestBlockEntity chest(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, Blocks.CHEST);
        return (ChestBlockEntity) h.getBlockEntity(pos);
    }

    private static int count(GameTestHelper h, BlockPos pos) {
        ChestBlockEntity chest = (ChestBlockEntity) h.getBlockEntity(pos);
        int total = 0;
        for (int i = 0; i < chest.getContainerSize(); i++)
            total += chest.getItem(i).getCount();
        return total;
    }
}
