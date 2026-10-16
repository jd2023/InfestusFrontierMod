package org.jd.infestusfrontier.testmod;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassHandler;

/** The Digestive Sac: feeding, reservation, pause, buckets and the state carried on its item. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class ProcessingTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos SAC = GROUND.above();
    private static final int FLESH_TICKS = 160;
    private static final int WHEAT_TICKS = 40;

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void sacPlacesOnlyOnLivingSubstrate(GameTestHelper h) {
        h.setBlock(GROUND, Blocks.STONE);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, ProcessingModule.DIGESTIVE_SAC_ITEM.get(), 1);
        h.placeAt(player, player.getMainHandItem(), GROUND, Direction.UP);
        h.assertBlockPresent(Blocks.AIR, SAC);
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "sac still in hand");
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.placeAt(player, player.getMainHandItem(), GROUND, Direction.UP);
        h.assertBlockPresent(ProcessingModule.DIGESTIVE_SAC.get(), SAC);
        h.assertValueEqual(player.getMainHandItem().getCount(), 0, "sac placed");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 300)
    public static void fleshBecomesFiftyBiomassAfterEightSeconds(GameTestHelper h) {
        placeSac(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.ROTTEN_FLESH, 2);
        EcologyTests.use(h, player, SAC, Direction.UP);
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "one flesh taken");
        h.assertValueEqual(tank(h).reserved(), Biomass.of(50), "yield reserved up front");
        h.assertValueEqual(tank(h).stored(), Biomass.ZERO, "nothing stored yet");
        h.runAfterDelay(FLESH_TICKS - 2, () -> h.assertValueEqual(tank(h).stored(), Biomass.ZERO, "not done early"));
        h.runAfterDelay(FLESH_TICKS + 2, () -> {
            h.assertValueEqual(tank(h).stored(), Biomass.of(50), "fifty BU stored");
            h.assertValueEqual(tank(h).reserved(), Biomass.ZERO, "reservation committed");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void sacRefusesASecondItemWhileWorking(GameTestHelper h) {
        placeSac(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.WHEAT, 2);
        EcologyTests.use(h, player, SAC, Direction.UP);
        EcologyTests.use(h, player, SAC, Direction.UP);
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "second wheat kept");
        h.assertValueEqual(tank(h).reserved(), Biomass.of(100), "one yield reserved");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void sacPassesUnrelatedItemsThrough(GameTestHelper h) {
        placeSac(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.STONE, 2);
        EcologyTests.use(h, player, SAC, Direction.UP);
        h.assertBlockPresent(Blocks.STONE, SAC.above());
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "stone placed against the sac");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 600)
    public static void fullSacRefusesFeedAndAnEmptyBucketTakesAThousand(GameTestHelper h) {
        placeSac(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.WHEAT, 12);
        feedRepeatedly(h, player, 10, WHEAT_TICKS + 2, () -> {
            h.assertValueEqual(tank(h).stored(), Biomass.of(1000), "ten wheat fill the sac");
            EcologyTests.use(h, player, SAC, Direction.UP);
            h.assertValueEqual(player.getMainHandItem().getCount(), 2, "eleventh wheat refused");
            Player carrier = EcologyTests.holding(h, GameType.SURVIVAL, Items.BUCKET, 1);
            EcologyTests.use(h, carrier, SAC, Direction.UP);
            h.assertTrue(carrier.getMainHandItem().is(StorageModule.BIOMASS_BUCKET.get()), "bucket filled");
            h.assertValueEqual(tank(h).stored(), Biomass.ZERO, "sac emptied");
            EcologyTests.use(h, player, SAC, Direction.UP);
            h.assertValueEqual(player.getMainHandItem().getCount(), 1, "wheat accepted again");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void bucketRefusesWhenLessThanAThousandStored(GameTestHelper h) {
        placeSac(h);
        Player carrier = EcologyTests.holding(h, GameType.SURVIVAL, Items.BUCKET, 1);
        EcologyTests.use(h, carrier, SAC, Direction.UP);
        h.assertTrue(carrier.getMainHandItem().is(Items.BUCKET), "bucket unchanged");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void biomassBucketIsRefusedBySac(GameTestHelper h) {
        placeSac(h);
        Player carrier = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, carrier, SAC, Direction.UP);
        h.assertTrue(carrier.getMainHandItem().is(StorageModule.BIOMASS_BUCKET.get()), "biomass kept");
        h.assertValueEqual(tank(h).stored(), Biomass.ZERO, "sac takes nothing in");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 300)
    public static void sacPausesWithoutSubstrateAndResumes(GameTestHelper h) {
        placeSac(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.WHEAT, 1);
        EcologyTests.use(h, player, SAC, Direction.UP);
        h.runAfterDelay(10, () -> h.setBlock(GROUND, Blocks.STONE));
        h.runAfterDelay(WHEAT_TICKS + 20, () -> {
            h.assertValueEqual(tank(h).stored(), Biomass.ZERO, "paused batch did not finish");
            h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        });
        h.runAfterDelay(WHEAT_TICKS + 20 + WHEAT_TICKS, () -> {
            h.assertValueEqual(tank(h).stored(), Biomass.of(100), "resumed batch finished");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 300)
    public static void brokenSacCarriesItsBiomassAndBatchToTheNewPlace(GameTestHelper h) {
        placeSac(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.WHEAT, 3);
        feedRepeatedly(h, player, 2, WHEAT_TICKS + 2, () -> {
            EcologyTests.use(h, player, SAC, Direction.UP);
            h.assertValueEqual(tank(h).stored(), Biomass.of(200), "two batches stored");
            h.getLevel().destroyBlock(h.absolutePos(SAC), true);
            h.runAfterDelay(2, () -> {
                List<ItemEntity> drops = h.getEntities(EntityType.ITEM, SAC, 2.0);
                h.assertValueEqual(drops.size(), 1, "one dropped sac");
                ItemStack dropped = drops.get(0).getItem();
                BiomassTank carried = dropped.get(StorageModule.TANK.get());
                h.assertTrue(carried != null && carried.stored().equals(Biomass.of(200)), "item carries 200 BU");
                h.assertTrue(
                        carried.reserved().equals(Biomass.of(100)), "item carries the running batch's reservation");
                drops.get(0).discard();
                BlockPos ground2 = GROUND.east(2);
                h.setBlock(ground2, EcologyModule.LIVING_SUBSTRATE.get());
                Player placer = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
                placer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, dropped.copy());
                h.placeAt(placer, placer.getMainHandItem(), ground2, Direction.UP);
                BiomassTank placed = handler(h, ground2.above()).tank();
                h.assertValueEqual(placed.stored(), Biomass.of(200), "placed sac keeps 200 BU");
                h.runAfterDelay(WHEAT_TICKS + 2, () -> {
                    h.assertValueEqual(
                            handler(h, ground2.above()).tank().stored(), Biomass.of(300), "carried batch finished");
                    h.succeed();
                });
            });
        });
    }

    static void placeSac(GameTestHelper h) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(SAC, ProcessingModule.DIGESTIVE_SAC.get());
    }

    static BiomassHandler handler(GameTestHelper h, BlockPos pos) {
        BiomassHandler handler = h.getLevel().getCapability(BiomassCapabilities.BLOCK, h.absolutePos(pos), null);
        if (handler == null) h.fail("no biomass capability", pos);
        return handler;
    }

    static BiomassTank tank(GameTestHelper h) {
        return handler(h, SAC).tank();
    }

    /** Feeds the held item {@code times} times, waiting {@code gap} ticks after each, then runs {@code then}. */
    static void feedRepeatedly(GameTestHelper h, Player player, int times, int gap, Runnable then) {
        for (int i = 0; i < times; i++) {
            final int round = i;
            h.runAfterDelay((long) round * gap, () -> EcologyTests.use(h, player, SAC, Direction.UP));
        }
        h.runAfterDelay((long) times * gap, then);
    }

    static ItemStack stackOf(Item item, int count) {
        return new ItemStack(item, count);
    }
}
