package org.jd.infestusfrontier.testmod;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.CultureBowlContents;
import org.jd.infestusfrontier.storage.StorageModule;

/** The whole M2 workshop route in one test: craft the organs, grow a culture, digest ten wheat, store a bucket. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class M2RouteTests {
    private static final BlockPos SAC_GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos SAC = SAC_GROUND.above();
    private static final BlockPos BLADDER_GROUND = new BlockPos(3, 1, 1);
    private static final BlockPos BLADDER = BLADDER_GROUND.above();
    private static final BlockPos BOWL_GROUND = new BlockPos(5, 1, 1);
    private static final BlockPos BOWL = BOWL_GROUND.above();
    private static final int WHEAT_GAP = 42;

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 1500)
    public static void m2Route(GameTestHelper h) {
        ItemStack culture1 =
                M1RouteTests.craft(h, List.of(rotten(), mushroom(), seeds()), EcologyModule.SPORE_CULTURE.get());
        ItemStack culture2 =
                M1RouteTests.craft(h, List.of(rotten(), mushroom(), seeds()), EcologyModule.SPORE_CULTURE.get());
        ItemStack bud1 = M1RouteTests.craft(
                h,
                List.of(culture1, rotten(), rotten(), new ItemStack(Items.BONE_MEAL)),
                OrganModule.ORGAN_BUD_ITEM.get());
        ItemStack bud2 = M1RouteTests.craft(
                h,
                List.of(culture2, rotten(), rotten(), new ItemStack(Items.BONE_MEAL)),
                OrganModule.ORGAN_BUD_ITEM.get());
        ItemStack sac = M1RouteTests.craft(
                h,
                List.of(bud1, rotten(), rotten(), new ItemStack(Items.BOWL)),
                ProcessingModule.DIGESTIVE_SAC_ITEM.get());
        ItemStack bladder = M1RouteTests.craft(
                h,
                List.of(bud2, new ItemStack(Items.GLASS), new ItemStack(Items.GLASS), new ItemStack(Items.SLIME_BALL)),
                StorageModule.BIOMASS_BLADDER_ITEM.get());
        ItemStack culture3 =
                M1RouteTests.craft(h, List.of(rotten(), mushroom(), seeds()), EcologyModule.SPORE_CULTURE.get());
        ItemStack bowl = M1RouteTests.craft(
                h,
                List.of(new ItemStack(Items.FLOWER_POT), culture3, seeds()),
                ProcessingModule.CULTURE_BOWL_ITEM.get());

        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        h.setBlock(SAC_GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(BLADDER_GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(BOWL_GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        place(h, builder, sac, SAC_GROUND);
        place(h, builder, bladder, BLADDER_GROUND);
        place(h, builder, bowl, BOWL_GROUND);
        h.assertBlockPresent(ProcessingModule.DIGESTIVE_SAC.get(), SAC);
        h.assertBlockPresent(StorageModule.BIOMASS_BLADDER.get(), BLADDER);
        h.assertBlockPresent(ProcessingModule.CULTURE_BOWL.get(), BOWL);

        for (Item item : new Item[] {Items.RED_MUSHROOM, Items.WHEAT_SEEDS, Items.WATER_BUCKET}) {
            Player loader = EcologyTests.holding(h, GameType.SURVIVAL, item, 1);
            EcologyTests.use(h, loader, BOWL, Direction.UP);
        }
        EcologyTests.use(h, EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0), BOWL, Direction.UP);
        h.assertTrue(bowlContents(h).batch().isPresent(), "the bowl batch runs at tick " + h.getTick());

        Player feeder = EcologyTests.holding(h, GameType.SURVIVAL, Items.WHEAT, 10);
        ProcessingTests.feedRepeatedly(h, feeder, 10, WHEAT_GAP, () -> {
            h.assertTrue(feeder.getMainHandItem().isEmpty(), "ten wheat consumed");
            h.assertValueEqual(ProcessingTests.tank(h).stored(), Biomass.of(1000), "ten wheat fill the sac");
            Player carrier = EcologyTests.holding(h, GameType.SURVIVAL, Items.BUCKET, 1);
            EcologyTests.use(h, carrier, SAC, Direction.UP);
            h.assertTrue(carrier.getMainHandItem().is(StorageModule.BIOMASS_BUCKET.get()), "bucket taken from sac");
            EcologyTests.use(h, carrier, BLADDER, Direction.UP);
            h.assertTrue(carrier.getMainHandItem().is(Items.BUCKET), "bucket emptied into the bladder");
        });
        h.runAtTickTime(1210, () -> {
            CultureBowlContents bowlNow = bowlContents(h);
            h.assertTrue(bowlNow.output().is(EcologyModule.SPORE_CULTURE.get()), "spore culture in the output");
            h.assertValueEqual(bowlNow.output().getCount(), 1, "one spore culture");
            h.assertValueEqual(ProcessingTests.handler(h, BLADDER).tank().stored(), Biomass.of(1000), "bladder 1000");
            h.assertValueEqual(ProcessingTests.tank(h).stored(), Biomass.ZERO, "sac empty");
            h.getLevel().destroyBlock(h.absolutePos(SAC), true);
            h.runAfterDelay(2, () -> {
                List<ItemEntity> drops = h.getEntities(EntityType.ITEM, SAC, 2.0);
                h.assertValueEqual(drops.size(), 1, "one dropped sac");
                OrganHistory history = drops.get(0).getItem().get(OrganModule.HISTORY.get());
                h.assertTrue(history != null, "the dropped sac carries a history");
                h.assertValueEqual(history.completedBatches(), 10, "ten completed batches");
                h.succeed();
            });
        });
    }

    private static ItemStack rotten() {
        return new ItemStack(Items.ROTTEN_FLESH);
    }

    private static ItemStack mushroom() {
        return new ItemStack(Items.RED_MUSHROOM);
    }

    private static ItemStack seeds() {
        return new ItemStack(Items.WHEAT_SEEDS);
    }

    private static void place(GameTestHelper h, Player player, ItemStack stack, BlockPos ground) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        h.placeAt(player, stack, ground, Direction.UP);
    }

    private static CultureBowlContents bowlContents(GameTestHelper h) {
        CultureBowlContents contents =
                h.getBlockEntity(BOWL).collectComponents().get(ProcessingModule.BOWL_CONTENTS.get());
        if (contents == null) h.fail("no bowl contents", BOWL);
        return contents;
    }
}
