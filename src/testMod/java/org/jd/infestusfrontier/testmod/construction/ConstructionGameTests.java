package org.jd.infestusfrontier.testmod.construction;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.testmod.integration.ContentAssertion;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class ConstructionGameTests {
    private static final ResourceLocation CULTURE = id("construction/spore_culture");
    private static final ResourceLocation BUD = id("construction/organ_bud");
    private static final ResourceLocation SEED_POUCH = id("construction/seed_pouch");
    private static final ResourceLocation LIVING_SKIN = id("construction/living_skin");
    private static final ResourceLocation LIVING_SKIN_SLAB = id("construction/living_skin_slab");
    private static final ResourceLocation LIVING_SKIN_STAIRS = id("construction/living_skin_stairs");
    private static final ResourceLocation LIVING_SKIN_COVERING = id("construction/living_skin_covering");
    private static final ResourceLocation RIB_FRAME = id("construction/rib_frame");
    private static final ResourceLocation MEMBRANE_WINDOW = id("construction/membrane_window");
    private static final ResourceLocation SUBSTRATE = id("ecology/living_substrate");

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void sporeCultureCraftsExactlyOneFromExactInputs(GameTestHelper helper) {
        ItemStack result = craft(helper, CULTURE, List.of(
                new ItemStack(Items.ROTTEN_FLESH),
                new ItemStack(Items.RED_MUSHROOM),
                new ItemStack(Items.WHEAT_SEEDS)));
        helper.assertTrue(result.is(item(CULTURE)) && result.getCount() == 1,
                "I000 must craft exactly one Spore Culture");

        var wrongInput = input(List.of(
                new ItemStack(Items.ROTTEN_FLESH),
                new ItemStack(Items.BROWN_MUSHROOM),
                new ItemStack(Items.WHEAT_SEEDS)));
        CraftingRecipe recipe = recipe(helper, CULTURE);
        helper.assertTrue(!recipe.matches(wrongInput, helper.getLevel()),
                "I000 must require the exact red-mushroom recipe");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:construction.spore_culture.obtain");
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void sporeCultureIsPaidIntoTheBudRecipe(GameTestHelper helper) {
        ItemStack result = craft(helper, BUD, List.of(
                new ItemStack(item(CULTURE)),
                new ItemStack(Items.ROTTEN_FLESH),
                new ItemStack(Items.ROTTEN_FLESH),
                new ItemStack(Items.BONE_MEAL)));
        helper.assertTrue(result.is(item(BUD)) && result.getCount() == 1,
                "I001 must consume one Culture and craft exactly one Organ Bud");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:construction.spore_culture.use");
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void craftedBudPlacesAsItsAliasedBlockItem(GameTestHelper helper) {
        Item organBudItem = item(BUD);
        Block organBudBlock = block(BUD);
        helper.assertTrue(organBudItem instanceof BlockItem blockItem && blockItem.getBlock() == organBudBlock,
                "I001 and T0-16 must be one block-item representation");

        ItemStack culture = craft(helper, CULTURE, List.of(
                new ItemStack(Items.ROTTEN_FLESH),
                new ItemStack(Items.RED_MUSHROOM),
                new ItemStack(Items.WHEAT_SEEDS)));
        ItemStack craftedBud = craft(helper, BUD, List.of(
                culture,
                new ItemStack(Items.ROTTEN_FLESH),
                new ItemStack(Items.ROTTEN_FLESH),
                new ItemStack(Items.BONE_MEAL)));

        BlockPos support = helper.absolutePos(BlockPos.ZERO);
        helper.getLevel().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, craftedBud);
        var hit = new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false);
        var result = organBudItem.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));

        helper.assertTrue(result.consumesAction(), "Organ Bud block item must place normally");
        helper.assertTrue(helper.getLevel().getBlockState(support.above()).is(organBudBlock),
                "Placed I001 must create T0-16");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "Survival placement must spend exactly one bud");

        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:construction.organ_bud.obtain");
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void unavailableConstructionLeavesBudAndInputUnchanged(GameTestHelper helper) {
        BlockPos budPos = helper.absolutePos(BlockPos.ZERO);
        Block organBud = block(BUD);
        helper.getLevel().setBlockAndUpdate(budPos, organBud.defaultBlockState());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBBLESTONE, 2));
        var hit = new BlockHitResult(Vec3.atCenterOf(budPos), Direction.UP, budPos, false);
        var result = helper.getLevel().getBlockState(budPos).useItemOn(
                player.getItemInHand(InteractionHand.MAIN_HAND),
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                hit);

        helper.assertTrue(result == ItemInteractionResult.FAIL,
                "An unavailable construction recipe must refuse without falling through to item use");
        helper.assertTrue(helper.getLevel().getBlockState(budPos).is(organBud),
                "Refused construction must leave the bud");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 2,
                "Refused construction must not spend inputs");
        helper.assertTrue(helper.getLevel().getBlockEntity(budPos) == null && !(organBud instanceof EntityBlock),
                "An idle bud must hold no block entity state");
        helper.assertTrue(!helper.getLevel().getBlockState(budPos).isRandomlyTicking(),
                "An idle bud must have no random ticker");

        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:construction.organ_bud.use");
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void shellPartsCraftByTheirCatalogRoutesAndRemainPassive(GameTestHelper helper) {
        var sheet = item(id("processing/membrane_sheet"));
        var plate = item(id("processing/bone_plate"));
        var culture = item(CULTURE);
        var expected = java.util.Map.of(
                SEED_POUCH, craft(helper, SEED_POUCH, List.of(new ItemStack(Items.LEATHER), new ItemStack(sheet), new ItemStack(culture))),
                LIVING_SKIN, craft(helper, LIVING_SKIN, List.of(new ItemStack(sheet), new ItemStack(Items.DIRT), new ItemStack(culture))),
                RIB_FRAME, craft(helper, RIB_FRAME, List.of(new ItemStack(plate), new ItemStack(plate), new ItemStack(sheet))),
                MEMBRANE_WINDOW, craft(helper, MEMBRANE_WINDOW, List.of(new ItemStack(Items.GLASS), new ItemStack(sheet), new ItemStack(Items.BONE))));
        helper.assertTrue(expected.get(SEED_POUCH).getCount() == 1 && expected.get(LIVING_SKIN).getCount() == 4
                        && expected.get(RIB_FRAME).getCount() == 2 && expected.get(MEMBRANE_WINDOW).getCount() == 2,
                "T0 shell recipes must preserve their catalog quantities");
        Item skin = item(LIVING_SKIN);
        helper.assertTrue(craft(helper, LIVING_SKIN_SLAB, List.of(
                        new ItemStack(skin), new ItemStack(skin), new ItemStack(skin))).getCount() == 6,
                "Three skin blocks cut into six volume-conserving slabs");
        helper.assertTrue(craft(helper, LIVING_SKIN_STAIRS, List.of(
                        new ItemStack(skin), ItemStack.EMPTY, ItemStack.EMPTY,
                        new ItemStack(skin), new ItemStack(skin), ItemStack.EMPTY,
                        new ItemStack(skin), new ItemStack(skin), new ItemStack(skin))).getCount() == 4,
                "Six skin blocks cut into four corner-capable stairs");
        helper.assertTrue(craft(helper, LIVING_SKIN_COVERING, List.of(new ItemStack(skin))).getCount() == 4,
                "One skin block cuts into four thin coverings");

        int x = 0;
        for (var id : List.of(LIVING_SKIN, LIVING_SKIN_SLAB, LIVING_SKIN_STAIRS,
                LIVING_SKIN_COVERING, RIB_FRAME, MEMBRANE_WINDOW)) {
            BlockPos pos = helper.absolutePos(new BlockPos(x++, 1, 0));
            helper.getLevel().setBlockAndUpdate(pos, block(id).defaultBlockState());
            helper.assertTrue(helper.getLevel().getBlockEntity(pos) == null && !(block(id) instanceof EntityBlock),
                    "Passive shell parts must have no block entity: " + id);
        }
        BlockPos firstWindow = helper.absolutePos(new BlockPos(5, 1, 0));
        BlockPos joinedWindow = firstWindow.east();
        helper.getLevel().setBlockAndUpdate(joinedWindow, block(MEMBRANE_WINDOW).defaultBlockState());
        BlockPos middleWindow = joinedWindow;
        BlockPos endWindow = joinedWindow.east();
        helper.getLevel().setBlockAndUpdate(endWindow, block(MEMBRANE_WINDOW).defaultBlockState());
        helper.assertTrue(helper.getLevel().getBlockState(firstWindow).toString().contains("east=true")
                        && helper.getLevel().getBlockState(middleWindow).toString().contains("post=false")
                        && helper.getLevel().getBlockState(endWindow).toString().contains("post=true"),
                "Adjacent membrane windows must form reciprocal joins");
        for (String name : List.of("seed_pouch", "living_skin", "rib_frame", "membrane_window")) {
            ContentAssertion.passGameTest(helper, "infestusfrontier_tests:construction." + name + ".obtain");
        }
        for (String name : List.of("living_skin", "rib_frame", "membrane_window")) {
            ContentAssertion.passGameTest(helper, "infestusfrontier_tests:construction." + name + ".use");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void seedPouchRetainsPlantingStockAndLumenMutatesOnlyMatureSubstrate(GameTestHelper helper) {
        BlockPos pouchPos = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.getLevel().setBlockAndUpdate(pouchPos.below(), block(SUBSTRATE).defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pouchPos, block(SEED_POUCH).defaultBlockState());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        useBlock(helper, pouchPos, player, new ItemStack(Items.WHEAT_SEEDS, 4));
        helper.assertTrue(player.getMainHandItem().isEmpty(), "Seed Pouch accepts bounded planting stock");
        useBlock(helper, pouchPos, player, ItemStack.EMPTY);
        var saved = helper.getLevel().getBlockEntity(pouchPos).saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(saved.getList("stocks", 10).getCompound(0).getInt("count") == 1,
                "Ordinary withdrawal must retain one planting stock");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:construction.seed_pouch.use");

        BlockPos substratePos = helper.absolutePos(new BlockPos(2, 1, 0));
        helper.getLevel().setBlockAndUpdate(substratePos, block(SUBSTRATE).defaultBlockState());
        var lumen = item(id("processing/lumen_secretion"));
        useBlock(helper, substratePos, player, new ItemStack(lumen));
        helper.assertTrue(player.getMainHandItem().is(lumen)
                        && !helper.getLevel().getBlockState(substratePos).toString().contains("function=lumen"),
                "Lumen Secretion must refuse an immature cell unchanged");
        useBlock(helper, substratePos, player, new ItemStack(Items.BONE_MEAL));
        useBlock(helper, substratePos, player, new ItemStack(Items.BONE_MEAL));
        useBlock(helper, substratePos, player, new ItemStack(lumen));
        helper.assertTrue(helper.getLevel().getBlockState(substratePos).toString().contains("function=lumen")
                        && helper.getLevel().getBlockState(substratePos).getLightEmission() == 12,
                "T0-13 must mutate a mature actual block and emit local light");
        var graft = item(id("processing/skeletal_graft"));
        useBlock(helper, substratePos, player, new ItemStack(graft));
        helper.assertTrue(helper.getLevel().getBlockState(substratePos).toString().contains("framework=bone_ribbed")
                        && helper.getLevel().getBlockState(substratePos).toString().contains("function=lumen"),
                "Skeletal reinforcement must preserve the cell function rather than upgrade a machine");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:construction.lumen_tissue.obtain");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:construction.lumen_tissue.use");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void budMutationRefusesIncompatibleSubstrateWithoutPayment(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, block(BUD).defaultBlockState());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(1, new ItemStack(Items.ROTTEN_FLESH, 2));
        useBlock(helper, pos, player, new ItemStack(Items.BOWL));
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(block(BUD))
                        && player.getMainHandItem().is(Items.BOWL)
                        && player.getInventory().getItem(1).getCount() == 2,
                "A paid Bud recipe must refuse incompatible support before replacement or payment");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void loomBudRoutePaysExactlyOnceAndRefusesIncompletePayment(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos.below(), block(SUBSTRATE).defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, block(BUD).defaultBlockState());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(1, new ItemStack(Items.STICK));
        useBlock(helper, pos, player, new ItemStack(Items.BONE, 3));
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(block(BUD))
                && player.getMainHandItem().getCount() == 3 && player.getInventory().getItem(1).getCount() == 1,
                "Incomplete Loom graft must preserve Bud and all inputs");
        player.getInventory().setItem(1, new ItemStack(Items.STICK, 3));
        useBlock(helper, pos, player, new ItemStack(Items.BONE, 3));
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(block(id("processing/bone_loom")))
                && player.getMainHandItem().getCount() == 1 && player.getInventory().getItem(1).getCount() == 1,
                "Loom Bud route must consume exactly two bones and two sticks");
        var entity = helper.getLevel().getBlockEntity(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE, 3));
        player.getInventory().setItem(1, new ItemStack(Items.STICK, 3));
        var staleResult = block(BUD).defaultBlockState().useItemOn(player.getMainHandItem(), helper.getLevel(),
                player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        helper.assertTrue(staleResult == ItemInteractionResult.FAIL && helper.getLevel().getBlockEntity(pos) == entity
                && player.getMainHandItem().getCount() == 3 && player.getInventory().getItem(1).getCount() == 3,
                "A repeated stale Bud trigger with sufficient stock must not replace the core or spend twice");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void pouchRejectsCorruptSavesWithoutDestroyingRecoveryData(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos.below(), block(SUBSTRATE).defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, block(SEED_POUCH).defaultBlockState());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        useBlock(helper, pos, player, new ItemStack(Items.WHEAT_SEEDS, 12));
        var entity = helper.getLevel().getBlockEntity(pos);
        var registries = helper.getLevel().registryAccess();
        var valid = entity.saveWithoutMetadata(registries);
        for (int corruption = 0; corruption < 8; corruption++) {
            var bad = valid.copy();
            var stocks = bad.getList("stocks", 10);
            if (corruption == 0) bad.putInt("schema", 99);
            if (corruption == 1) for (int n = 0; n < 4; n++) stocks.add(stocks.getCompound(0).copy());
            if (corruption == 2) stocks.getCompound(0).putInt("count", 65);
            if (corruption == 3) stocks.getCompound(0).putString("resource", "missing:seed");
            if (corruption == 4) stocks.add(stocks.getCompound(0).copy());
            if (corruption == 5) bad.putString("stocks", "wrong type");
            if (corruption == 6) stocks.getCompound(0).putString("resource", "wheat_seeds");
            if (corruption == 7) bad.putInt("reserve", 65);
            entity.loadWithComponents(bad, registries);
            useBlock(helper, pos, player, new ItemStack(Items.CARROT));
            helper.assertTrue(player.getMainHandItem().getCount() == 1 && bad.equals(entity.saveWithoutMetadata(registries)),
                    "Rejected pouch save must refuse insertion and preserve exact data: " + corruption);
            useBlock(helper, pos, player, ItemStack.EMPTY);
            helper.assertTrue(bad.equals(entity.saveWithoutMetadata(registries)), "Rejected data must survive withdrawal");
            var drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos, entity);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(item(SEED_POUCH)), "One recoverable pouch");
            var data = drops.getFirst().get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
            helper.assertTrue(data != null, "Rejected pouch must carry original data when dismantled");
            helper.getLevel().removeBlock(pos, false);
            player.setItemInHand(InteractionHand.MAIN_HAND, drops.getFirst());
            item(SEED_POUCH).useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false)));
            entity = helper.getLevel().getBlockEntity(pos);
            helper.assertTrue(entity != null && bad.equals(entity.saveWithoutMetadata(registries)),
                    "Actual block-item replacement must preserve rejected data");
        }
        entity.loadWithComponents(valid, registries);
        helper.getLevel().removeBlock(pos, false);
        helper.getLevel().setBlockAndUpdate(pos, block(SEED_POUCH).defaultBlockState());
        entity = helper.getLevel().getBlockEntity(pos);
        entity.loadWithComponents(valid, registries);
        helper.assertTrue(valid.equals(entity.saveWithoutMetadata(registries)), "Valid stock survives actual entity reload");
        useBlock(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(entity.saveWithoutMetadata(registries).getList("stocks", 10).getCompound(0).getInt("count") == 1,
                "Recovered valid stock is available without duplication");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void pouchReserveSelectionAndAllOrdinaryPlantingStock(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos.below(), block(SUBSTRATE).defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, block(SEED_POUCH).defaultBlockState());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (var seed : List.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SEEDS,
                Items.PUMPKIN_SEEDS, Items.TORCHFLOWER_SEEDS, Items.PITCHER_POD, Items.CARROT, Items.POTATO,
                Items.OAK_SAPLING, Items.MANGROVE_PROPAGULE)) {
            player.setShiftKeyDown(false);
            useBlock(helper, pos, player, new ItemStack(seed, 20));
            helper.assertTrue(player.getMainHandItem().isEmpty(), "Pouch admits planting stock: " + seed);
            player.setShiftKeyDown(true);
            useBlock(helper, pos, player, ItemStack.EMPTY);
        }
        player.setShiftKeyDown(false);
        useBlock(helper, pos, player, new ItemStack(Items.WHEAT_SEEDS, 20));
        var controller = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "PouchReserve"));
        controller.setGameMode(GameType.SURVIVAL);
        controller.setPos(Vec3.atCenterOf(pos).add(0, 0, -2));
        controller.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        for (boolean sneak : List.of(false, true, false)) {
            controller.setShiftKeyDown(sneak);
            controller.gameMode.useItemOn(controller, helper.getLevel(), controller.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        }
        var entity = helper.getLevel().getBlockEntity(pos);
        var saved = entity.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(saved.getInt("reserve") == 2 && controller.getMainHandItem().is(Items.STICK),
                "Reserve selection is reusable and player controlled");
        entity.loadWithComponents(saved, helper.getLevel().registryAccess());
        useBlock(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(entity.saveWithoutMetadata(helper.getLevel().registryAccess()).getList("stocks", 10)
                .getCompound(0).getInt("count") == 2, "Selected reserve persists and governs surplus withdrawal");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void pouchPassesOffhandBuildingThroughPlayerDispatch(GameTestHelper helper) {
        var player = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "PouchBuilder"));
        player.setGameMode(GameType.SURVIVAL);
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos.below(), block(SUBSTRATE).defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, block(SEED_POUCH).defaultBlockState());
        useBlock(helper, pos, player, new ItemStack(Items.WHEAT_SEEDS, 8));
        player.setPos(Vec3.atCenterOf(pos).add(0, 0, -2));
        var before = helper.getLevel().getBlockEntity(pos).saveWithoutMetadata(helper.getLevel().registryAccess());
        for (var hand : InteractionHand.values()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            player.setItemInHand(hand, new ItemStack(Items.COBBLESTONE));
            var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false);
            var result = player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            if (hand == InteractionHand.OFF_HAND) {
                helper.assertTrue(!result.consumesAction(), "Pouch must pass empty-main-hand dispatch through");
                result = player.gameMode.useItemOn(player, helper.getLevel(), player.getOffhandItem(), hand, hit);
            }
            helper.assertTrue(result.consumesAction() && helper.getLevel().getBlockState(pos.above()).is(Blocks.COBBLESTONE)
                    && before.equals(helper.getLevel().getBlockEntity(pos).saveWithoutMetadata(helper.getLevel().registryAccess())),
                    "Pouch must permit placement without withdrawing stock");
            helper.getLevel().removeBlock(pos.above(), false);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 100)
    public static void windowsJoinVerticallyAndLumenPropagatesIntoRealAir(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (int x = 0; x < 3; x++) for (int y = 0; y < 2; y++) {
            helper.getLevel().setBlockAndUpdate(pos.offset(x, y, 0), block(MEMBRANE_WINDOW).defaultBlockState());
        }
        helper.assertTrue(helper.getLevel().getBlockState(pos.east()).toString().contains("up=true")
                && helper.getLevel().getBlockState(pos.east().above()).toString().contains("down=true"),
                "Stacked windows must omit the shared horizontal rib on both blocks");
        var light = helper.absolutePos(new BlockPos(4, 1, 4));
        helper.getLevel().setBlockAndUpdate(light, block(SUBSTRATE).defaultBlockState());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        useBlock(helper, light, player, new ItemStack(Items.BONE_MEAL));
        useBlock(helper, light, player, new ItemStack(Items.BONE_MEAL));
        useBlock(helper, light, player, new ItemStack(item(id("processing/lumen_secretion"))));
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, light.above()) >= 11,
                    "Lumen must illuminate adjacent real air through the light engine");
            helper.assertTrue(helper.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, light.above(2)) >= 10,
                    "Lumen must propagate beyond its own emission property");
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void ribCollisionFollowsTheRotatedArchOpening(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        double[][] solid = {{15, 8, 8}, {8, 15, 8}, {8, 8, 1}};
        double[][] open = {{2, 8, 8}, {8, 2, 8}, {8, 8, 14}};
        for (var axis : Direction.Axis.values()) {
            var state = block(RIB_FRAME).defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, axis);
            var shape = state.getCollisionShape(helper.getLevel(), pos);
            var support = solid[axis.ordinal()];
            var opening = open[axis.ordinal()];
            helper.assertTrue(shape.toAabbs().stream().anyMatch(box -> box.contains(support[0]/16, support[1]/16, support[2]/16)),
                    "Rotated arch must collide at its visible crown: " + axis);
            helper.assertTrue(shape.toAabbs().stream().noneMatch(box -> box.contains(opening[0]/16, opening[1]/16, opening[2]/16)),
                    "Rotated arch must leave its visible opening clear: " + axis);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void windowPlacementRefusesUnloadedHaloWithoutPayment(GameTestHelper helper) {
        var level = helper.getLevel();
        var source = level.getChunkSource();
        var origin = helper.absolutePos(BlockPos.ZERO);
        int chunkX = (origin.getX() >> 4) + 192;
        int chunkZ = origin.getZ() >> 4;
        level.getChunk(chunkX, chunkZ);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        // Exercise both a face boundary and a diagonal update boundary.
        for (int z : new int[] {8, 15}) {
            var pos = new BlockPos(chunkX * 16 + 15, origin.getY() + 4, chunkZ * 16 + z);
            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE, 0);
            int missingZ = chunkZ;
            if (z == 15) {
                level.getChunk(chunkX + 1, chunkZ);
                level.getChunk(chunkX, chunkZ + 1);
                missingZ++;
            }
            helper.assertTrue(source.getChunkNow(chunkX + 1, missingZ) == null, "Fixture boundary chunk must be absent");
            int loaded = source.getLoadedChunksCount();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(MEMBRANE_WINDOW), 2));
            var context = new net.minecraft.world.item.context.BlockPlaceContext(new UseOnContext(player,
                    InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false)));
            var result = ((BlockItem) item(MEMBRANE_WINDOW)).place(context);
            helper.assertTrue(source.getLoadedChunksCount() == loaded && source.getChunkNow(chunkX + 1, missingZ) == null,
                    "Window placement must not request an unloaded neighbor chunk");
            helper.assertTrue(!result.consumesAction() && level.getBlockState(pos).isAir()
                            && player.getMainHandItem().getCount() == 2,
                    "Unavailable window update halo must refuse without placement or payment");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void isolatedWindowCollisionAndTargetingCoverVisibleMembrane(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var state = block(MEMBRANE_WINDOW).defaultBlockState();
        helper.getLevel().setBlock(pos, state, Block.UPDATE_KNOWN_SHAPE, 0);
        for (var shape : List.of(state.getShape(helper.getLevel(), pos), state.getCollisionShape(helper.getLevel(), pos))) {
            for (double x : new double[] {2, 8, 14}) {
                helper.assertTrue(shape.toAabbs().stream().anyMatch(box -> box.contains(x / 16, .5, .5)),
                        "Isolated membrane must collide and target across its full visible width at x=" + x);
                for (int side : new int[] {-1, 1}) {
                    var start = Vec3.atLowerCornerOf(pos).add(x / 16, .5, .5 + side);
                    var end = Vec3.atLowerCornerOf(pos).add(x / 16, .5, .5 - side);
                    helper.assertTrue(shape.clip(start, end, pos) != null, "Both visible membrane faces must be targetable");
                }
            }
            helper.assertTrue(shape.toAabbs().stream().noneMatch(box -> box.contains(.125, .5, .25)),
                    "Empty space beside the isolated panel must remain open");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void windowPlacementUpdatesJoinsWithoutRecursingAcrossChunkEdge(GameTestHelper helper) {
        var level = helper.getLevel();
        var source = level.getChunkSource();
        var origin = helper.absolutePos(BlockPos.ZERO);
        int chunkX = (origin.getX() >> 4) + 208;
        int chunkZ = origin.getZ() >> 4;
        level.getChunk(chunkX, chunkZ);
        var pos = new BlockPos(chunkX * 16 + 14, origin.getY() + 4, chunkZ * 16 + 8);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE, 0);
        level.setBlock(pos.east(), block(MEMBRANE_WINDOW).defaultBlockState(), Block.UPDATE_KNOWN_SHAPE, 0);
        level.setBlock(pos.above(), block(MEMBRANE_WINDOW).defaultBlockState(), Block.UPDATE_KNOWN_SHAPE, 0);
        level.setBlock(pos.west(), Blocks.COBBLESTONE_WALL.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE, 0);
        level.setBlock(pos.south(), Blocks.GLASS_PANE.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE, 0);
        int loaded = source.getLoadedChunksCount();
        helper.assertTrue(source.getChunkNow(chunkX + 1, chunkZ) == null, "Fixture second ring must be unloaded");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(MEMBRANE_WINDOW), 2));
        player.getMainHandItem().set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                new net.minecraft.world.item.component.BlockItemStateProperties(java.util.Map.of("post", "false")));
        var context = new net.minecraft.world.item.context.BlockPlaceContext(new UseOnContext(player,
                InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false)));
        var result = ((BlockItem) item(MEMBRANE_WINDOW)).place(context);
        helper.assertTrue(source.getLoadedChunksCount() == loaded && source.getChunkNow(chunkX + 1, chunkZ) == null,
                "Reciprocal join updates must not read the unloaded second ring");
        helper.assertTrue(result.consumesAction() && player.getMainHandItem().getCount() == 1,
                "Loaded immediate halo must permit exactly one paid window");
        helper.assertTrue(level.getBlockState(pos).toString().contains("east=true")
                        && level.getBlockState(pos.east()).toString().contains("west=true")
                        && level.getBlockState(pos).toString().contains("up=true")
                        && level.getBlockState(pos.above()).toString().contains("down=true"),
                "Bounded placement must retain reciprocal horizontal and vertical joins");
        helper.assertTrue(level.getBlockState(pos.south()).getValue(net.minecraft.world.level.block.IronBarsBlock.NORTH)
                        && level.getBlockState(pos.west()).getValue(net.minecraft.world.level.block.WallBlock.EAST_WALL)
                                != net.minecraft.world.level.block.state.properties.WallSide.NONE,
                "Bounded placement must retain vanilla pane and wall joins");
        helper.succeed();
    }

    private static void useBlock(GameTestHelper helper, BlockPos pos, net.minecraft.world.entity.player.Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        if (stack.isEmpty()) helper.getLevel().getBlockState(pos).useWithoutItem(helper.getLevel(), player, hit);
        else helper.getLevel().getBlockState(pos).useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    }

    private static ItemStack craft(GameTestHelper helper, ResourceLocation id, List<ItemStack> ingredients) {
        CraftingRecipe recipe = recipe(helper, id);
        CraftingInput input = input(ingredients);
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "Recipe must match exact inputs: " + id);
        return recipe.assemble(input, helper.getLevel().registryAccess());
    }

    private static CraftingRecipe recipe(GameTestHelper helper, ResourceLocation id) {
        var holder = helper.getLevel().getRecipeManager().byKey(id)
                .orElseThrow(() -> new AssertionError("Missing recipe " + id));
        helper.assertTrue(holder.value() instanceof CraftingRecipe, "Expected crafting recipe " + id);
        return (CraftingRecipe) holder.value();
    }

    private static CraftingInput input(List<ItemStack> ingredients) {
        var slots = new ArrayList<ItemStack>(ingredients);
        while (slots.size() < 9) slots.add(ItemStack.EMPTY);
        return CraftingInput.of(3, 3, slots);
    }

    private static Item item(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id).orElseThrow(() -> new AssertionError("Missing item " + id));
    }

    private static Block block(ResourceLocation id) {
        return BuiltInRegistries.BLOCK.getOptional(id).orElseThrow(() -> new AssertionError("Missing block " + id));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("infestusfrontier", path);
    }
}
