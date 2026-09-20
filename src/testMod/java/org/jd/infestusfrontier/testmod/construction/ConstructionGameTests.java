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
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOWL, 2));
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
