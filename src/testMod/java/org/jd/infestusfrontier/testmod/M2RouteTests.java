package org.jd.infestusfrontier.testmod;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.equipment.EquipmentModule;
import org.jd.infestusfrontier.equipment.api.Reserves;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.storage.BiomassBladderMenu;
import org.jd.infestusfrontier.storage.StorageModule;

/**
 * The M2 workshop routes: grow the starter organs from buds, digest ten wheat and store a bucket; grow the Rack and
 * Loom and make a first sheet and plate; grow the Furnace and the Cradle from the sheets and plates, smelt, awaken a
 * piece and refuel it.
 */
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

    /**
     * The route from construction materials to equipment: a Rack and a Loom grown from starter materials make four
     * sheets and two plates; those grow a Bio-Furnace (a furnace and two sheets) and an Awakening Cradle (two plates and
     * two sheets); the Furnace smelts raw iron, the Cradle wakes a crafted Dormant Thorax, and a grown Bladder refuels
     * the awakened piece. Biomass arrives in buckets, as the Digestive Sac's route above delivers it.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 2800)
    public static void growthRouteFromMaterialsToAnAwakenedRefueledPiece(GameTestHelper h) {
        BlockPos rack = grownBud(h, 1, List.of(new ItemStack(Items.STRING, 4), new ItemStack(Items.STICK, 2)));
        BlockPos loom = grownBud(h, 3, List.of(new ItemStack(Items.BONE, 2), new ItemStack(Items.STICK, 2)));
        BlockPos bladder = grownBud(h, 5, List.of(new ItemStack(Items.GLASS, 2), new ItemStack(Items.SLIME_BALL)));
        BlockPos furnaceBud = budAt(h, 7);
        BlockPos cradleBud = budAt(h, 9);
        h.assertBlockPresent(ProcessingModule.MEMBRANE_RACK.get(), rack);
        h.assertBlockPresent(ProcessingModule.BONE_LOOM.get(), loom);
        h.assertBlockPresent(StorageModule.BIOMASS_BLADDER.get(), bladder);

        load(h, rack, Items.ROTTEN_FLESH, 4);
        load(h, rack, Items.STRING, 4);
        load(h, rack, Items.WATER_BUCKET, 1);
        load(h, loom, Items.BONE, 2);
        load(h, loom, StorageModule.BIOMASS_BUCKET.get(), 1);
        h.runAtTickTime(1750, () -> {
            // A taker's hand holds what it takes, so each organ gets its own taker.
            Player sheetTaker = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
            sheetTaker.setShiftKeyDown(true);
            EcologyTests.use(h, sheetTaker, rack, Direction.UP);
            Player plateTaker = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
            plateTaker.setShiftKeyDown(true);
            EcologyTests.use(h, plateTaker, loom, Direction.UP);
            h.assertValueEqual(
                    sheetTaker.getInventory().countItem(ProcessingModule.MEMBRANE_SHEET.get()), 4, "four sheets");
            h.assertValueEqual(plateTaker.getInventory().countItem(ProcessingModule.BONE_PLATE.get()), 2, "two plates");

            // A furnace and two sheets grow a Bio-Furnace; two plates and two sheets grow a Cradle.
            grow(h, furnaceBud, new ItemStack(Items.FURNACE), new ItemStack(ProcessingModule.MEMBRANE_SHEET.get(), 2));
            grow(
                    h,
                    cradleBud,
                    new ItemStack(ProcessingModule.BONE_PLATE.get(), 2),
                    new ItemStack(ProcessingModule.MEMBRANE_SHEET.get(), 2));
            h.assertBlockPresent(ProcessingModule.BIO_FURNACE.get(), furnaceBud);
            h.assertBlockPresent(ProcessingModule.AWAKENING_CRADLE.get(), cradleBud);

            // Smelting spends biomass on a normal recipe; the Cradle wakes a crafted piece with a crafted culture.
            load(h, furnaceBud, Items.RAW_IRON, 1);
            load(h, furnaceBud, StorageModule.BIOMASS_BUCKET.get(), 1);
            ItemStack culture =
                    M1RouteTests.craft(h, List.of(rotten(), mushroom(), seeds()), EcologyModule.SPORE_CULTURE.get());
            ItemStack thorax = dormantThorax(h);
            Player loader = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
            for (ItemStack stack : List.of(thorax, culture)) {
                loader.setItemInHand(InteractionHand.MAIN_HAND, stack);
                EcologyTests.use(h, loader, cradleBud, Direction.UP);
            }
            load(h, cradleBud, StorageModule.BIOMASS_BUCKET.get(), 1);
            load(h, bladder, StorageModule.BIOMASS_BUCKET.get(), 1);
        });
        h.runAtTickTime(2450, () -> {
            h.assertTrue(output(h, furnaceBud).is(Items.IRON_INGOT), "the grown furnace smelted an iron ingot");
            ItemStack woken = output(h, cradleBud);
            h.assertTrue(woken.is(EquipmentModule.AWAKENED_THORAX.get()), "the grown cradle woke the thorax");
            Player taker = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
            taker.setShiftKeyDown(true);
            EcologyTests.use(h, taker, cradleBud, Direction.UP);
            h.assertValueEqual(
                    taker.getInventory().countItem(EquipmentModule.AWAKENED_THORAX.get()), 1, "the piece is taken");
            ItemStack piece = taker.getInventory().items.stream()
                    .filter(stack -> stack.is(EquipmentModule.AWAKENED_THORAX.get()))
                    .findFirst()
                    .orElseThrow();
            var wokenReserve = Reserves.of(piece).orElseThrow();
            h.assertValueEqual(wokenReserve.stored(), Biomass.ZERO, "the cradle's piece wakes empty");
            h.assertValueEqual(wokenReserve.capacity(), EquipmentModule.RESERVE_CAPACITY, "with a 40 BU reserve");
            Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
            BiomassBladderMenu menu = (BiomassBladderMenu)
                    ((MenuProvider) h.getBlockEntity(bladder)).createMenu(1, player.getInventory(), player);
            menu.slots.get(0).set(piece);
            h.assertTrue(menu.clickMenuButton(player, BiomassBladderMenu.FILL_BUTTON), "fill handled");
            h.assertValueEqual(
                    Reserves.of(menu.slots.get(0).getItem()).orElseThrow().stored(),
                    Biomass.of(40),
                    "the awakened piece holds its 40 BU");
            h.succeed();
        });
    }

    /** A ground of Living Substrate at {@code x} with a bud crafted from starter materials on it; the bud's position. */
    private static BlockPos budAt(GameTestHelper h, int x) {
        BlockPos ground = new BlockPos(x, 1, 1);
        h.setBlock(ground, EcologyModule.LIVING_SUBSTRATE.get());
        ItemStack culture =
                M1RouteTests.craft(h, List.of(rotten(), mushroom(), seeds()), EcologyModule.SPORE_CULTURE.get());
        ItemStack bud = M1RouteTests.craft(
                h,
                List.of(culture, rotten(), rotten(), new ItemStack(Items.BONE_MEAL)),
                OrganModule.ORGAN_BUD_ITEM.get());
        place(h, EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0), bud, ground);
        h.assertBlockPresent(OrganModule.ORGAN_BUD.get(), ground.above());
        return ground.above();
    }

    /** A bud at {@code x} grown into its organ by supplying {@code materials}; the organ's position. */
    private static BlockPos grownBud(GameTestHelper h, int x, List<ItemStack> materials) {
        BlockPos bud = budAt(h, x);
        grow(h, bud, materials.toArray(new ItemStack[0]));
        return bud;
    }

    /** One item per use, as a player loads a batch organ by hand. */
    private static void load(GameTestHelper h, BlockPos organ, Item item, int count) {
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, item, count);
        for (int i = 0; i < count; i++) EcologyTests.use(h, player, organ, Direction.UP);
    }

    /** The Dormant Thorax of the crafting table's chest pattern: eight rotten flesh around a gap. */
    private static ItemStack dormantThorax(GameTestHelper h) {
        List<ItemStack> grid = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(i == 1 ? ItemStack.EMPTY : rotten());
        CraftingInput input = CraftingInput.of(3, 3, grid);
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
        h.assertTrue(recipe.isPresent(), "the chest pattern crafts a piece");
        ItemStack piece = recipe.get().value().assemble(input, h.getLevel().registryAccess());
        h.assertTrue(piece.is(EquipmentModule.DORMANT_THORAX.get()), "a Dormant Thorax");
        return piece;
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
