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
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.storage.StorageModule;

/** The M2 workshop routes: grow the starter organs from buds, digest ten wheat and store a bucket; grow the Rack and Loom and make a first sheet and plate. */
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
        BlockPos[] grounds = {SAC_GROUND, BLADDER_GROUND, BOWL_GROUND};
        for (BlockPos ground : grounds) h.setBlock(ground, EcologyModule.LIVING_SUBSTRATE.get());
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        for (BlockPos ground : grounds) {
            ItemStack culture =
                    M1RouteTests.craft(h, List.of(rotten(), mushroom(), seeds()), EcologyModule.SPORE_CULTURE.get());
            ItemStack bud = M1RouteTests.craft(
                    h,
                    List.of(culture, rotten(), rotten(), new ItemStack(Items.BONE_MEAL)),
                    OrganModule.ORGAN_BUD_ITEM.get());
            place(h, builder, bud, ground);
            h.assertBlockPresent(OrganModule.ORGAN_BUD.get(), ground.above());
        }
        // The bowl's spore culture is the only material that is itself crafted.
        ItemStack bowlCulture =
                M1RouteTests.craft(h, List.of(rotten(), mushroom(), seeds()), EcologyModule.SPORE_CULTURE.get());
        grow(h, SAC, new ItemStack(Items.ROTTEN_FLESH, 2), new ItemStack(Items.BOWL));
        grow(h, BLADDER, new ItemStack(Items.GLASS, 2), new ItemStack(Items.SLIME_BALL));
        grow(h, BOWL, new ItemStack(Items.FLOWER_POT), bowlCulture, seeds());
        h.assertBlockPresent(ProcessingModule.DIGESTIVE_SAC.get(), SAC);
        h.assertBlockPresent(StorageModule.BIOMASS_BLADDER.get(), BLADDER);
        h.assertBlockPresent(ProcessingModule.CULTURE_BOWL.get(), BOWL);

        for (Item item : new Item[] {Items.RED_MUSHROOM, Items.WHEAT_SEEDS, Items.WATER_BUCKET}) {
            Player loader = EcologyTests.holding(h, GameType.SURVIVAL, item, 1);
            EcologyTests.use(h, loader, BOWL, Direction.UP);
        }
        h.runAfterDelay(1, () -> h.assertTrue(bowlContents(h).batch().isPresent(), "the bowl batch runs on its own"));

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
            BatchOrganContents bowlNow = bowlContents(h);
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

    /**
     * The construction-material route: two buds crafted from starter materials, a Rack and a Loom grown from them by
     * hand (neither growth needs a sheet or a plate), then a first Membrane Sheet and a first Bone Plate.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 600)
    public static void constructionMaterialsRoute(GameTestHelper h) {
        BlockPos rackGround = new BlockPos(1, 1, 1);
        BlockPos loomGround = new BlockPos(3, 1, 1);
        BlockPos rack = rackGround.above();
        BlockPos loom = loomGround.above();
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        for (BlockPos ground : List.of(rackGround, loomGround)) {
            h.setBlock(ground, EcologyModule.LIVING_SUBSTRATE.get());
            ItemStack culture =
                    M1RouteTests.craft(h, List.of(rotten(), mushroom(), seeds()), EcologyModule.SPORE_CULTURE.get());
            ItemStack bud = M1RouteTests.craft(
                    h,
                    List.of(culture, rotten(), rotten(), new ItemStack(Items.BONE_MEAL)),
                    OrganModule.ORGAN_BUD_ITEM.get());
            place(h, builder, bud, ground);
        }
        grow(h, rack, new ItemStack(Items.STRING, 4), new ItemStack(Items.STICK, 2));
        grow(h, loom, new ItemStack(Items.BONE, 2), new ItemStack(Items.STICK, 2));
        h.assertBlockPresent(ProcessingModule.MEMBRANE_RACK.get(), rack);
        h.assertBlockPresent(ProcessingModule.BONE_LOOM.get(), loom);
        for (ItemStack stack : List.of(rotten(), new ItemStack(Items.STRING), new ItemStack(Items.WATER_BUCKET))) {
            Player loader = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
            loader.setItemInHand(InteractionHand.MAIN_HAND, stack);
            EcologyTests.use(h, loader, rack, Direction.UP);
        }
        for (ItemStack stack : List.of(new ItemStack(Items.BONE), new ItemStack(StorageModule.BIOMASS_BUCKET.get()))) {
            Player loader = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
            loader.setItemInHand(InteractionHand.MAIN_HAND, stack);
            EcologyTests.use(h, loader, loom, Direction.UP);
        }
        h.runAtTickTime(450, () -> {
            h.assertTrue(output(h, rack).is(ProcessingModule.MEMBRANE_SHEET.get()), "the first Membrane Sheet");
            h.assertTrue(output(h, loom).is(ProcessingModule.BONE_PLATE.get()), "the first Bone Plate");
            h.succeed();
        });
    }

    private static ItemStack output(GameTestHelper h, BlockPos organ) {
        var registries = h.getLevel().registryAccess();
        var tag = h.getBlockEntity(organ).saveWithoutMetadata(registries);
        return BatchOrganContents.CODEC
                .parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag.get("contents"))
                .getOrThrow()
                .output();
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

    /** Hands every material to the bud at {@code bud}, one right click each; the last one grows the organ. */
    private static void grow(GameTestHelper h, BlockPos bud, ItemStack... materials) {
        for (ItemStack material : materials) {
            Player supplier = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
            supplier.setItemInHand(InteractionHand.MAIN_HAND, material);
            EcologyTests.use(h, supplier, bud, Direction.UP);
            h.assertTrue(supplier.getMainHandItem().isEmpty(), "the bud took " + material);
        }
    }

    private static void place(GameTestHelper h, Player player, ItemStack stack, BlockPos ground) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        h.placeAt(player, stack, ground, Direction.UP);
    }

    private static BatchOrganContents bowlContents(GameTestHelper h) {
        BatchOrganContents contents =
                h.getBlockEntity(BOWL).collectComponents().get(ProcessingModule.BATCH_CONTENTS.get());
        if (contents == null) h.fail("no bowl contents", BOWL);
        return contents;
    }
}
