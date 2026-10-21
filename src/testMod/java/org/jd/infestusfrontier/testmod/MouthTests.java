package org.jd.infestusfrontier.testmod;

import com.mojang.authlib.GameProfile;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.TickQuota;
import org.jd.infestusfrontier.interaction.InteractionModule;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.MouthBlock;
import org.jd.infestusfrontier.logistics.MouthMenu;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.logistics.api.LogisticsCapabilities;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;

/**
 * Mouths under chests and organs, joined by Item Veins: routing by filter, priority and distance, amounts, packets on
 * the way through a reload and a break, a destination that disappears, an organ fed and emptied by one Mouth, the
 * probe on a store, and the shared pulse quota. Cells sit at y=1, their stores at y=2.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class MouthTests {
    private static final int Y = 1;
    /** When a freshly placed output Mouth sends its first packet. */
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

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aPacketOnTheWaySurvivesAReloadAndArrivesOnce(GameTestHelper h) {
        store(h, cell(1), new ItemStack(Items.BONE, 16));
        store(h, cell(8));
        mouth(h, cell(1), MouthMode.OUTPUT);
        veins(h, 2, 7);
        mouth(h, cell(8), MouthMode.INTAKE);
        h.runAfterDelay(FIRST_SEND + 2, () -> {
            h.assertValueEqual(count(h, cell(1), Items.BONE), 8, "one packet left");
            h.assertValueEqual(count(h, cell(8), Items.BONE), 0, "it is still on the way");
            var registries = h.getLevel().registryAccess();
            CompoundTag saved = h.getBlockEntity(cell(1)).saveWithFullMetadata(registries);
            h.assertValueEqual(saved.getList("packets", 10).size(), 1, "the packet is saved");
            BlockEntity restored =
                    BlockEntity.loadStatic(h.absolutePos(cell(1)), h.getBlockState(cell(1)), saved, registries);
            h.getLevel().removeBlockEntity(h.absolutePos(cell(1)));
            h.getLevel().setBlockEntity(restored);
            h.runAfterDelay(100, () -> {
                h.assertValueEqual(count(h, cell(8), Items.BONE), 16, "every bone arrived, once");
                h.assertValueEqual(count(h, cell(1), Items.BONE), 0, "the source is empty");
                h.succeed();
            });
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void breakingAMouthDropsItsPacketsOnce(GameTestHelper h) {
        store(h, cell(1), new ItemStack(Items.BONE, 16));
        store(h, cell(8));
        mouth(h, cell(1), MouthMode.OUTPUT);
        veins(h, 2, 7);
        mouth(h, cell(8), MouthMode.INTAKE);
        h.runAfterDelay(FIRST_SEND + 2, () -> {
            h.assertValueEqual(count(h, cell(1), Items.BONE), 8, "one packet on the way");
            h.getLevel().destroyBlock(h.absolutePos(cell(1)), true);
            h.runAfterDelay(2, () -> {
                h.assertItemEntityCountIs(Items.BONE, cell(1), 2, 8);
                h.assertItemEntityCountIs(LogisticsModule.MOUTH_ITEM.get(), cell(1), 2, 1);
                for (var drop : h.getEntities(EntityType.ITEM, cell(1), 2))
                    if (drop.getItem().is(LogisticsModule.MOUTH_ITEM.get()))
                        h.assertFalse(
                                drop.getItem().has(DataComponents.BLOCK_ENTITY_DATA),
                                "the Mouth item carries no second copy");
                h.runAfterDelay(60, () -> {
                    h.assertValueEqual(count(h, cell(8), Items.BONE), 0, "the dropped packet never arrives");
                    h.succeed();
                });
            });
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aPacketWhoseDestinationLostItsStoreGoesToTheNext(GameTestHelper h) {
        store(h, cell(4), new ItemStack(Items.BONE, 8));
        store(h, cell(1));
        store(h, cell(9));
        mouth(h, cell(4), MouthMode.OUTPUT);
        veins(h, 2, 3);
        mouth(h, cell(1), MouthMode.INTAKE);
        veins(h, 5, 8);
        mouth(h, cell(9), MouthMode.INTAKE);
        configure(h, cell(9), control -> control.press(MouthMenu.PRIORITY_UP, 1));
        h.runAfterDelay(FIRST_SEND + 2, () -> {
            h.assertValueEqual(count(h, cell(4), Items.BONE), 0, "the packet left for the far Mouth");
            h.setBlock(cell(9).above(), Blocks.STONE);
            h.runAfterDelay(80, () -> {
                h.assertValueEqual(count(h, cell(1), Items.BONE), 8, "it went to the other Mouth instead");
                h.succeed();
            });
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

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "mouth_quota")
    public static void mouthsShareTheDimensionQuotaAndRetryAfterDenial(GameTestHelper h) {
        store(h, cell(1), new ItemStack(Items.BONE, 8));
        store(h, cell(3));
        mouth(h, cell(1), MouthMode.OUTPUT);
        veins(h, 2, 2);
        mouth(h, cell(3), MouthMode.INTAKE);
        var type = NeoForgeRegistries.ATTACHMENT_TYPES.get(
                ResourceLocation.fromNamespaceAndPath("infestusfrontier", "mouth_quota"));
        TickQuota quota = (TickQuota) h.getLevel().getData(type);
        long tick = h.getLevel().getGameTime();
        for (int i = 0; i < 64; i++) h.assertTrue(quota.take(tick), "quota slot " + i);
        h.assertFalse(quota.take(tick), "the sixty-fifth pulse is refused");
        h.getBlockState(cell(1)).tick(h.getLevel(), h.absolutePos(cell(1)), h.getLevel().random);
        h.assertValueEqual(count(h, cell(1), Items.BONE), 8, "a refused pulse leaves the source alone");
        h.runAfterDelay(40, () -> {
            h.assertValueEqual(count(h, cell(3), Items.BONE), 8, "a later pulse moves the packet");
            h.succeed();
        });
    }

    // --- fixtures ---

    private static BlockPos cell(int x) {
        return new BlockPos(x, Y, 1);
    }

    private static void mouth(GameTestHelper h, BlockPos at, MouthMode mode) {
        h.setBlock(at, LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, mode));
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
     * way the server does, without the packet to a client it does not have.
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
