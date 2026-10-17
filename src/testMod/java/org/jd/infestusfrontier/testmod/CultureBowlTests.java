package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.processing.CultureBowlBlockEntity.Contents;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.StorageModule;

/** The Culture Bowl: ingredients, water and biomass loaded by hand, collected again, kept when broken. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class CultureBowlTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos BOWL = GROUND.above();

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void bowlPlacesOnlyOnLivingSubstrate(GameTestHelper h) {
        h.setBlock(GROUND, Blocks.DIRT);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, ProcessingModule.CULTURE_BOWL_ITEM.get(), 1);
        h.placeAt(player, player.getMainHandItem(), GROUND, Direction.UP);
        h.assertBlockPresent(Blocks.AIR, BOWL);
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.placeAt(player, player.getMainHandItem(), GROUND, Direction.UP);
        h.assertBlockPresent(ProcessingModule.CULTURE_BOWL.get(), BOWL);
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void ingredientsStackIntoSlots(GameTestHelper h) {
        placeBowl(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.RED_MUSHROOM, 8);
        for (int i = 0; i < 3; i++) EcologyTests.use(h, player, BOWL, Direction.UP);
        Contents contents = contents(h, BOWL);
        h.assertValueEqual(contents.ingredients().get(0).getCount(), 3, "slot 0 holds three");
        h.assertTrue(contents.ingredients().get(0).is(Items.RED_MUSHROOM), "slot 0 holds mushrooms");
        h.assertValueEqual(player.getMainHandItem().getCount(), 5, "three taken from the hand");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void unknownItemsPassThrough(GameTestHelper h) {
        placeBowl(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.STONE, 1);
        EcologyTests.use(h, player, BOWL, Direction.UP);
        h.assertBlockPresent(Blocks.STONE, BOWL.above());
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void waterBucketFillsTheTank(GameTestHelper h) {
        placeBowl(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.WATER_BUCKET, 1);
        EcologyTests.use(h, player, BOWL, Direction.UP);
        h.assertValueEqual(StorageTests.fluidHandler(h, BOWL).getFluidInTank(0).getAmount(), 1000, "one bucket");
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET), "hand holds the empty bucket");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        EcologyTests.use(h, player, BOWL, Direction.UP);
        h.assertValueEqual(StorageTests.fluidHandler(h, BOWL).getFluidInTank(0).getAmount(), 2000, "two buckets");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        EcologyTests.use(h, player, BOWL, Direction.UP);
        h.assertValueEqual(StorageTests.fluidHandler(h, BOWL).getFluidInTank(0).getAmount(), 2000, "third refused");
        h.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "the water bucket stays in the hand");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void biomassBucketFillsTheTank(GameTestHelper h) {
        placeBowl(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, player, BOWL, Direction.UP);
        h.assertValueEqual(ProcessingTests.handler(h, BOWL).tank().stored(), Biomass.BUCKET, "one bucket of biomass");
        h.assertValueEqual(StorageTests.fluidHandler(h, BOWL).getFluidInTank(0).getAmount(), 0, "no water");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void sneakEmptyHandReturnsAnIngredientStack(GameTestHelper h) {
        placeBowl(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.RED_MUSHROOM, 8);
        for (int i = 0; i < 3; i++) EcologyTests.use(h, player, BOWL, Direction.UP);
        Player taker = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        taker.setShiftKeyDown(true);
        EcologyTests.use(h, taker, BOWL, Direction.UP);
        h.assertTrue(taker.getInventory().countItem(Items.RED_MUSHROOM) == 3, "three mushrooms came back");
        h.assertTrue(contents(h, BOWL).ingredients().get(0).isEmpty(), "slot 0 is empty");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void brokenBowlKeepsItsContents(GameTestHelper h) {
        placeBowl(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.RED_MUSHROOM, 8);
        for (int i = 0; i < 3; i++) EcologyTests.use(h, player, BOWL, Direction.UP);
        Player water = EcologyTests.holding(h, GameType.SURVIVAL, Items.WATER_BUCKET, 1);
        EcologyTests.use(h, water, BOWL, Direction.UP);
        h.getLevel().destroyBlock(h.absolutePos(BOWL), true);
        h.runAfterDelay(2, () -> {
            java.util.List<ItemEntity> drops = h.getEntities(EntityType.ITEM, BOWL, 2.0);
            h.assertValueEqual(drops.size(), 1, "one dropped bowl");
            ItemStack dropped = drops.get(0).getItem();
            drops.get(0).discard();
            BlockPos ground2 = GROUND.east(2);
            h.setBlock(ground2, EcologyModule.LIVING_SUBSTRATE.get());
            Player placer = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
            placer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, dropped.copy());
            h.placeAt(placer, placer.getMainHandItem(), ground2, Direction.UP);
            Contents placed = contents(h, ground2.above());
            h.assertValueEqual(placed.ingredients().get(0).getCount(), 3, "three mushrooms kept");
            h.assertValueEqual(placed.waterMb(), 1000, "1000 mB kept");
            h.succeed();
        });
    }

    private static void placeBowl(GameTestHelper h) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(BOWL, ProcessingModule.CULTURE_BOWL.get());
    }

    private static Contents contents(GameTestHelper h, BlockPos pos) {
        Contents contents = h.getBlockEntity(pos).collectComponents().get(ProcessingModule.BOWL_CONTENTS.get());
        if (contents == null) h.fail("no bowl contents", pos);
        return contents;
    }
}
