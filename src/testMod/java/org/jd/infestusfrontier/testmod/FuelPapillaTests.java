package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.equipment.EquipmentModule;
import org.jd.infestusfrontier.equipment.FuelPapillaMenu;
import org.jd.infestusfrontier.equipment.api.Reserves;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassFluids;

/**
 * The Fuel Papilla: grown by its graft, fed by a vein, filling the chosen pieces of one standing wearer at 10 BU a
 * second for a fifth more, its settings, its unreadable bytes, a Creative break, and an organ standing on it.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class FuelPapillaTests {
    private static final BlockPos PAPILLA = new BlockPos(2, 1, 2);

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theGraftGrowsAPapillaOnlyFromExposedMatureSubstrate(GameTestHelper h) {
        h.setBlock(PAPILLA, EcologyTests.mature());
        h.setBlock(PAPILLA.east(), EcologyTests.mature());
        h.setBlock(PAPILLA.east().above(), net.minecraft.world.level.block.Blocks.STONE);
        Player grower = EcologyTests.holding(h, GameType.SURVIVAL, EquipmentModule.FUEL_PAPILLA_ITEM.get(), 2);
        EcologyTests.use(h, grower, PAPILLA.east(), Direction.NORTH);
        h.assertValueEqual(grower.getMainHandItem().getCount(), 2, "a covered cell grows nothing");
        EcologyTests.use(h, grower, PAPILLA, Direction.UP);
        h.assertBlockPresent(EquipmentModule.FUEL_PAPILLA.get(), PAPILLA);
        h.assertValueEqual(grower.getMainHandItem().getCount(), 1, "one graft spent");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aWearerGetsTenASecondAndThePadPaysAFifthMore(GameTestHelper h) {
        h.setBlock(PAPILLA, EquipmentModule.FUEL_PAPILLA.get());
        fill(h, 250);
        Player wearer = wearer(h);
        h.runAfterDelay(120, () -> {
            int hood = reserve(wearer, EquipmentSlot.HEAD);
            int thorax = reserve(wearer, EquipmentSlot.CHEST);
            int delivered = hood + thorax;
            int left = ProcessingTests.handler(h, PAPILLA).tank().stored().units();
            h.assertTrue(delivered >= 50 && delivered <= 60, "five or six seconds of 10 BU: " + delivered);
            h.assertValueEqual(delivered % 10, 0, "whole batches of 10");
            h.assertValueEqual(250 - left, delivered / 10 * 12, "each batch of 10 cost 12");
            h.assertTrue(hood == 40 && thorax == delivered - 40, "the head fills first, then the body");
            h.assertTrue(wearer.getItemBySlot(EquipmentSlot.FEET).is(Items.LEATHER_BOOTS), "leather untouched");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void theScreenChoosesThePiecesAndTheLimit(GameTestHelper h) {
        h.setBlock(PAPILLA, EquipmentModule.FUEL_PAPILLA.get());
        fill(h, 250);
        Player setter = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        AbstractContainerMenu menu =
                ((MenuProvider) h.getBlockEntity(PAPILLA)).createMenu(1, setter.getInventory(), setter);
        menu.clickMenuButton(setter, 0); // the head is skipped
        menu.clickMenuButton(setter, FuelPapillaMenu.LIMIT_BUTTON); // 100 to 25
        menu.clickMenuButton(setter, FuelPapillaMenu.LIMIT_BUTTON); // 25 to 50
        Player wearer = wearer(h);
        h.runAfterDelay(120, () -> {
            h.assertValueEqual(reserve(wearer, EquipmentSlot.HEAD), 0, "the skipped head got nothing");
            h.assertValueEqual(reserve(wearer, EquipmentSlot.CHEST), 20, "the body filled to half");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 100)
    public static void onePadServesOneLivingWearerAndNoSpectator(GameTestHelper h) {
        h.setBlock(PAPILLA, EquipmentModule.FUEL_PAPILLA.get());
        fill(h, 250);
        Player spectator = EcologyTests.holding(h, GameType.SPECTATOR, Items.AIR, 0);
        spectator.setItemSlot(EquipmentSlot.HEAD, new ItemStack(EquipmentModule.AWAKENED_HOOD.get()));
        spectator.setPos(Vec3.atBottomCenterOf(h.absolutePos(PAPILLA.above())));
        h.getLevel().addFreshEntity(spectator);
        Player first = wearer(h);
        Player second = wearer(h);
        h.runAfterDelay(50, () -> {
            h.assertValueEqual(reserve(spectator, EquipmentSlot.HEAD), 0, "a spectator gets nothing");
            int a = reserve(first, EquipmentSlot.HEAD);
            int b = reserve(second, EquipmentSlot.HEAD);
            h.assertTrue(a > 0 != b > 0, "one of the two wearers is served: " + a + " and " + b);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 60)
    public static void aBiomassVeinBesideThePadFillsIt(GameTestHelper h) {
        h.setBlock(PAPILLA, EquipmentModule.FUEL_PAPILLA.get());
        h.setBlock(PAPILLA.west(), LogisticsModule.BIOMASS_VEIN.get());
        h.getLevel()
                .getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(PAPILLA.west()), Direction.WEST)
                .fill(BiomassFluids.of(Biomass.of(250)), IFluidHandler.FluidAction.EXECUTE);
        h.runAfterDelay(20, () -> {
            h.assertValueEqual(
                    ProcessingTests.handler(h, PAPILLA).tank().stored().units(), 250, "the packet arrived");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCarriedTankTooBigIsKeptAsUnreadableBytes(GameTestHelper h) {
        h.setBlock(PAPILLA, EcologyTests.mature());
        ItemStack graft = new ItemStack(EquipmentModule.FUEL_PAPILLA_ITEM.get());
        graft.set(
                StorageModule.TANK.get(),
                ((Outcome.Done<BiomassTank>) BiomassTank.empty(Biomass.of(1000)).fill(Biomass.of(800))).value());
        Player grower = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        grower.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, graft);
        EcologyTests.use(h, grower, PAPILLA, Direction.UP);
        h.assertBlockPresent(EquipmentModule.FUEL_PAPILLA.get(), PAPILLA);
        h.assertFalse(
                ProcessingTests.handler(h, PAPILLA).insert(Biomass.of(10), true).isDone(), "it refuses biomass");
        CompoundTag saved =
                h.getBlockEntity(PAPILLA).saveWithoutMetadata(h.getLevel().registryAccess());
        h.assertValueEqual(saved.getCompound("tank").getInt("stored"), 800, "the carried 800 BU are kept, not dropped");
        h.getLevel().destroyBlock(h.absolutePos(PAPILLA), true);
        h.runAfterDelay(2, () -> {
            var drops = h.getEntities(EntityType.ITEM, PAPILLA, 2).stream()
                    .filter(drop -> drop.getItem().is(EquipmentModule.FUEL_PAPILLA_ITEM.get()))
                    .toList();
            h.assertValueEqual(drops.size(), 1, "the pad dropped");
            h.assertTrue(
                    drops.getFirst().getItem().has(OrganModule.UNREADABLE_SAVE.get()), "with its bytes on the item");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCreativeBreakKeepsTheBufferForTheNextGraft(GameTestHelper h) {
        h.setBlock(PAPILLA, EquipmentModule.FUEL_PAPILLA.get());
        fill(h, 100);
        Player creative = EcologyTests.holding(h, GameType.CREATIVE, Items.AIR, 0);
        BlockState state = h.getBlockState(PAPILLA);
        state.getBlock().playerWillDestroy(h.getLevel(), h.absolutePos(PAPILLA), state, creative);
        h.getLevel().removeBlock(h.absolutePos(PAPILLA), false);
        h.runAfterDelay(2, () -> {
            var drops = h.getEntities(EntityType.ITEM, PAPILLA, 2).stream()
                    .filter(drop -> drop.getItem().is(EquipmentModule.FUEL_PAPILLA_ITEM.get()))
                    .toList();
            h.assertValueEqual(drops.size(), 1, "the Creative player gets the pad back");
            BlockPos again = PAPILLA.east(2);
            h.setBlock(again, EcologyTests.mature());
            Player grower = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
            grower.setItemInHand(
                    net.minecraft.world.InteractionHand.MAIN_HAND,
                    drops.getFirst().getItem().copy());
            EcologyTests.use(h, grower, again, Direction.UP);
            h.assertValueEqual(ProcessingTests.handler(h, again).tank().stored().units(), 100, "with its 100 BU");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anOrganStandsOnAPapilla(GameTestHelper h) {
        h.setBlock(PAPILLA, EquipmentModule.FUEL_PAPILLA.get());
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, ProcessingModule.BONE_LOOM_ITEM.get(), 1);
        h.placeAt(builder, builder.getMainHandItem(), PAPILLA, Direction.UP);
        h.assertBlockPresent(ProcessingModule.BONE_LOOM.get(), PAPILLA.above());
        h.succeed();
    }

    private static void fill(GameTestHelper h, int units) {
        h.assertTrue(
                ProcessingTests.handler(h, PAPILLA)
                        .insert(Biomass.of(units), false)
                        .isDone(),
                "filled " + units);
    }

    /** A survival player standing on the pad in an awakened hood and thorax, with leather boots. */
    private static Player wearer(GameTestHelper h) {
        Player wearer = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        wearer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(EquipmentModule.AWAKENED_HOOD.get()));
        wearer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(EquipmentModule.AWAKENED_THORAX.get()));
        wearer.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        wearer.setPos(Vec3.atBottomCenterOf(h.absolutePos(PAPILLA.above())));
        h.getLevel().addFreshEntity(wearer); // a mock player is not in the level until it is added
        return wearer;
    }

    private static int reserve(Player player, EquipmentSlot slot) {
        return Reserves.of(player.getItemBySlot(slot)).orElseThrow().stored().units();
    }
}
