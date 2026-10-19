package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.processing.BatchOrganMenu;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.storage.StorageModule;

/** The Membrane Rack: the second batch organ, with two slots, water and no biomass. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class MembraneRackTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos RACK = GROUND.above();
    private static final int FLESH_TICKS = 600;
    private static final int LEATHER_TICKS = 1200;

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void rackPlacesOnlyOnLivingSubstrate(GameTestHelper h) {
        h.setBlock(GROUND, Blocks.STONE);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, ProcessingModule.MEMBRANE_RACK_ITEM.get(), 1);
        h.placeAt(player, player.getMainHandItem(), GROUND, Direction.UP);
        h.assertBlockPresent(Blocks.AIR, RACK);
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.placeAt(player, player.getMainHandItem(), GROUND, Direction.UP);
        h.assertBlockPresent(ProcessingModule.MEMBRANE_RACK.get(), RACK);
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 700)
    public static void fleshStringAndWaterMakeASheet(GameTestHelper h) {
        placeRack(h);
        load(h, Items.ROTTEN_FLESH, 1);
        load(h, Items.STRING, 1);
        load(h, Items.WATER_BUCKET, 1);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        BatchOrganMenu menu = menu(h, player);
        h.assertValueEqual(menu.ingredientSlots(), 2, "two ingredient wells");
        h.assertValueEqual(menu.waterCapacity(), 1000, "a small water tank");
        h.assertTrue(menu.clickMenuButton(player, BatchOrganMenu.START_BUTTON), "start handled");
        BatchOrganContents started = contents(h);
        h.assertTrue(started.batch().isPresent(), "a batch runs");
        h.assertValueEqual(started.waterMb(), 900, "100 mB of water used");
        h.runAtTickTime(FLESH_TICKS - 2, () -> h.assertTrue(contents(h).output().isEmpty(), "not done early"));
        h.runAtTickTime(FLESH_TICKS + 2, () -> {
            BatchOrganContents done = contents(h);
            h.assertTrue(done.output().is(ProcessingModule.MEMBRANE_SHEET.get()), "one Membrane Sheet");
            h.assertValueEqual(done.history().completedBatches(), 1, "counted");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 1300)
    public static void leatherTakesTwiceAsLong(GameTestHelper h) {
        placeRack(h);
        load(h, Items.LEATHER, 1);
        load(h, Items.STRING, 1);
        load(h, Items.WATER_BUCKET, 1);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        menu(h, player).clickMenuButton(player, BatchOrganMenu.START_BUTTON);
        h.assertTrue(contents(h).batch().isPresent(), "a batch runs");
        h.runAtTickTime(FLESH_TICKS + 2, () -> h.assertTrue(contents(h).output().isEmpty(), "leather is slower"));
        h.runAtTickTime(LEATHER_TICKS + 2, () -> {
            h.assertTrue(contents(h).output().is(ProcessingModule.MEMBRANE_SHEET.get()), "one Membrane Sheet");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void rackTakesNoBiomass(GameTestHelper h) {
        placeRack(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, player, RACK, Direction.UP);
        h.assertTrue(player.getMainHandItem().is(StorageModule.BIOMASS_BUCKET.get()), "the bucket stays full");
        h.assertTrue(contents(h).biomass().capacity().isZero(), "no biomass tank");
        h.succeed();
    }

    private static void placeRack(GameTestHelper h) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(RACK, ProcessingModule.MEMBRANE_RACK.get());
    }

    private static void load(GameTestHelper h, Item item, int count) {
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, item, count);
        for (int i = 0; i < count; i++) EcologyTests.use(h, player, RACK, Direction.UP);
    }

    private static BatchOrganMenu menu(GameTestHelper h, Player player) {
        MenuProvider provider = (MenuProvider) h.getBlockEntity(RACK);
        return (BatchOrganMenu) provider.createMenu(1, player.getInventory(), player);
    }

    private static BatchOrganContents contents(GameTestHelper h) {
        var tag = h.getBlockEntity(RACK).saveWithoutMetadata(h.getLevel().registryAccess());
        return BatchOrganContents.CODEC
                .parse(
                        h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                        tag.get("contents"))
                .getOrThrow();
    }
}
