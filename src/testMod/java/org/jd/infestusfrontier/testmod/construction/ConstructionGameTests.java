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
