package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.equipment.EquipmentModule;
import org.jd.infestusfrontier.equipment.api.Reserves;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.StorageModule;

/** The Fuel Papilla: a bucket fills it, a standing player's worn pieces fill from it, 10 BU a second each, and it roots organs. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class FuelPapillaTests {
    private static final BlockPos PAPILLA = new BlockPos(2, 1, 2);

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aStandingPlayersPiecesFillTenASecondUntilFull(GameTestHelper h) {
        h.setBlock(PAPILLA, EquipmentModule.FUEL_PAPILLA.get());
        Player pourer = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, pourer, PAPILLA, Direction.UP);
        h.assertValueEqual(
                ProcessingTests.handler(h, PAPILLA).tank().stored().units(), 1000, "a bucket fills the cell");
        Player wearer = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        wearer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(EquipmentModule.AWAKENED_HOOD.get()));
        wearer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(EquipmentModule.AWAKENED_THORAX.get()));
        wearer.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        wearer.setPos(Vec3.atBottomCenterOf(h.absolutePos(PAPILLA.above())));
        h.getLevel().addFreshEntity(wearer); // a mock player is not in the level until it is added
        h.runAfterDelay(25, () -> {
            int hood = Reserves.of(wearer.getItemBySlot(EquipmentSlot.HEAD))
                    .orElseThrow()
                    .stored()
                    .units();
            h.assertTrue(hood == 10 || hood == 20, "one or two feeds into the hood: " + hood);
        });
        h.runAfterDelay(120, () -> {
            h.assertValueEqual(
                    Reserves.of(wearer.getItemBySlot(EquipmentSlot.HEAD))
                            .orElseThrow()
                            .stored()
                            .units(),
                    40,
                    "the hood is full");
            h.assertValueEqual(
                    Reserves.of(wearer.getItemBySlot(EquipmentSlot.CHEST))
                            .orElseThrow()
                            .stored()
                            .units(),
                    40,
                    "the thorax is full");
            h.assertValueEqual(
                    ProcessingTests.handler(h, PAPILLA).tank().stored().units(), 920, "80 BU left the cell, no more");
            h.assertTrue(
                    wearer.getItemBySlot(EquipmentSlot.FEET).is(Items.LEATHER_BOOTS), "leather boots are untouched");
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
}
