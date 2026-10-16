package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.storage.BiomassBladderBlock;
import org.jd.infestusfrontier.storage.BiomassBladderMenu;
import org.jd.infestusfrontier.storage.StorageModule;

/** The Biomass Bladder: buckets in and out, the fill level, the transfer limit and the screen. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class StorageTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos BLADDER = GROUND.above();

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void bucketsFillTheBladderAndShowTheLevel(GameTestHelper h) {
        placeBladder(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, player, BLADDER, Direction.UP);
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET), "empty bucket returned");
        h.assertValueEqual(tank(h).stored(), Biomass.BUCKET, "one bucket stored");
        assertFill(h, 1);
        Player carrier = EcologyTests.holding(h, GameType.SURVIVAL, Items.BUCKET, 1);
        EcologyTests.use(h, carrier, BLADDER, Direction.UP);
        h.assertTrue(carrier.getMainHandItem().is(StorageModule.BIOMASS_BUCKET.get()), "bucket filled back");
        h.assertValueEqual(tank(h).stored(), Biomass.ZERO, "bladder empty again");
        assertFill(h, 0);
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void fullBladderRefusesAFifthBucketUnchanged(GameTestHelper h) {
        placeBladder(h);
        Player player = EcologyTests.holding(h, GameType.CREATIVE, StorageModule.BIOMASS_BUCKET.get(), 1);
        for (int i = 0; i < 4; i++) {
            final int round = i;
            h.runAfterDelay(round * 25L, () -> EcologyTests.use(h, player, BLADDER, Direction.UP));
        }
        h.runAfterDelay(100, () -> {
            h.assertValueEqual(tank(h).stored(), Biomass.of(4000), "four buckets fill it");
            assertFill(h, 4);
            EcologyTests.use(h, player, BLADDER, Direction.UP);
            h.assertValueEqual(tank(h).stored(), Biomass.of(4000), "fifth bucket refused");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void onePlayerGetsFourTransfersPerSecond(GameTestHelper h) {
        placeBladder(h);
        Player player = EcologyTests.holding(h, GameType.CREATIVE, StorageModule.BIOMASS_BUCKET.get(), 1);
        for (int i = 0; i < 5; i++) EcologyTests.use(h, player, BLADDER, Direction.UP);
        h.assertValueEqual(tank(h).stored(), Biomass.of(4000), "four of five admitted");
        Player other = EcologyTests.holding(h, GameType.CREATIVE, Items.BUCKET, 1);
        EcologyTests.use(h, other, BLADDER, Direction.UP);
        h.assertValueEqual(tank(h).stored(), Biomass.of(3000), "another player is not limited by the first");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void menuReadsTheLiveBalanceAndABlockInHandBuilds(GameTestHelper h) {
        placeBladder(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, player, BLADDER, Direction.UP);
        // No GameTest player can receive a screen yet, so the menu is built the way the block entity builds it.
        MenuProvider provider = h.getBlockEntity(BLADDER);
        AbstractContainerMenu menu = provider.createMenu(1, player.getInventory(), player);
        h.assertTrue(menu instanceof BiomassBladderMenu, "the bladder offers its menu");
        BiomassBladderMenu bladder = (BiomassBladderMenu) menu;
        h.assertValueEqual(bladder.stored(), 1000, "menu reads the stored amount");
        h.assertValueEqual(bladder.capacity(), 4000, "menu reads the capacity");
        h.assertFalse(bladder.clickMenuButton(player, BiomassBladderMenu.FILL_BUTTON), "fill has nothing to do yet");
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, Items.STONE, 1);
        EcologyTests.use(h, builder, BLADDER, Direction.UP);
        h.assertBlockPresent(Blocks.STONE, BLADDER.above());
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void bladderPlacesOnlyOnLivingSubstrate(GameTestHelper h) {
        h.setBlock(GROUND, Blocks.DIRT);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BLADDER_ITEM.get(), 1);
        h.placeAt(player, player.getMainHandItem(), GROUND, Direction.UP);
        h.assertBlockPresent(Blocks.AIR, BLADDER);
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "bladder kept");
        h.succeed();
    }

    static void placeBladder(GameTestHelper h) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(BLADDER, StorageModule.BIOMASS_BLADDER.get());
    }

    static org.jd.infestusfrontier.core.storage.BiomassTank tank(GameTestHelper h) {
        return ProcessingTests.handler(h, BLADDER).tank();
    }

    static void assertFill(GameTestHelper h, int level) {
        h.assertBlockState(
                BLADDER,
                (BlockState s) -> s.getValue(BiomassBladderBlock.FILL) == level,
                () -> "expected fill " + level);
    }
}
