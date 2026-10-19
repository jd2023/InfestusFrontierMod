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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.processing.BatchOrganMenu;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.storage.StorageModule;

/** The Bone Loom: the third batch organ, with one slot, biomass and no water. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class BoneLoomTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos LOOM = GROUND.above();
    private static final int PLATE_TICKS = 400;

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 700)
    public static void aBoneAndFiftyBiomassGrowAPlate(GameTestHelper h) {
        placeLoom(h);
        load(h, Items.BONE, 1);
        load(h, StorageModule.BIOMASS_BUCKET.get(), 1);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        BatchOrganMenu menu = menu(h, player);
        h.assertValueEqual(menu.ingredientSlots(), 1, "one well");
        h.assertValueEqual(menu.stored(), 1000, "a bucket of biomass went in");
        h.assertTrue(menu.clickMenuButton(player, BatchOrganMenu.START_BUTTON), "start handled");
        BatchOrganContents started = contents(h);
        h.assertTrue(started.batch().isPresent(), "a batch runs");
        h.assertValueEqual(started.biomass().stored().units(), 950, "50 BU bound the bone");
        h.runAtTickTime(PLATE_TICKS - 2, () -> h.assertTrue(contents(h).output().isEmpty(), "not done early"));
        h.runAtTickTime(PLATE_TICKS + 2, () -> {
            h.assertTrue(contents(h).output().is(ProcessingModule.BONE_PLATE.get()), "one Bone Plate");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void loomTakesNoWater(GameTestHelper h) {
        placeLoom(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.WATER_BUCKET, 1);
        EcologyTests.use(h, player, LOOM, Direction.UP);
        h.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "the water stays in the bucket");
        h.assertValueEqual(contents(h).waterMb(), 0, "no water tank");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void startRefusesWithoutBiomass(GameTestHelper h) {
        placeLoom(h);
        load(h, Items.BONE, 1);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        menu(h, player).clickMenuButton(player, BatchOrganMenu.START_BUTTON);
        h.assertTrue(contents(h).batch().isEmpty(), "no batch without the 50 BU");
        h.assertTrue(contents(h).ingredients().get(0).is(Items.BONE), "the bone stays");
        h.succeed();
    }

    private static void placeLoom(GameTestHelper h) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(LOOM, ProcessingModule.BONE_LOOM.get());
    }

    private static void load(GameTestHelper h, Item item, int count) {
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, item, count);
        for (int i = 0; i < count; i++) EcologyTests.use(h, player, LOOM, Direction.UP);
    }

    private static BatchOrganMenu menu(GameTestHelper h, Player player) {
        MenuProvider provider = (MenuProvider) h.getBlockEntity(LOOM);
        return (BatchOrganMenu) provider.createMenu(1, player.getInventory(), player);
    }

    private static BatchOrganContents contents(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        var tag = h.getBlockEntity(LOOM).saveWithoutMetadata(registries);
        return BatchOrganContents.CODEC
                .parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag.get("contents"))
                .getOrThrow();
    }
}
