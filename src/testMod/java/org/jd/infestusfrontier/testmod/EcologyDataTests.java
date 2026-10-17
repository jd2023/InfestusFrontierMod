package org.jd.infestusfrontier.testmod;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.ecology.LivingSubstrateBlock;
import org.jd.infestusfrontier.ecology.SubstrateStage;

/** Recipes and loot of the Spore Culture and Living Substrate. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class EcologyDataTests {
    private static final BlockPos CELL = new BlockPos(1, 1, 1);

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void sporeCultureRecipeResolves(GameTestHelper h) {
        assertCrafts(
                h,
                List.of(
                        new ItemStack(Items.ROTTEN_FLESH),
                        new ItemStack(Items.RED_MUSHROOM),
                        new ItemStack(Items.WHEAT_SEEDS)),
                EcologyModule.SPORE_CULTURE.get());
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void livingSubstrateRecipeResolves(GameTestHelper h) {
        assertCrafts(
                h,
                List.of(new ItemStack(Items.DIRT), new ItemStack(EcologyModule.SPORE_CULTURE.get())),
                EcologyModule.LIVING_SUBSTRATE_ITEM.get());
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void substrateDropsItself(GameTestHelper h) {
        h.setBlock(CELL, mature());
        h.getLevel().destroyBlock(h.absolutePos(CELL), true);
        h.assertItemEntityPresent(EcologyModule.LIVING_SUBSTRATE_ITEM.get(), CELL, 2.0);
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void substrateKeepsStage(GameTestHelper h) {
        h.setBlock(CELL, mature());
        h.getLevel().destroyBlock(h.absolutePos(CELL), true);
        List<ItemEntity> drops =
                h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds().inflate(1));
        h.assertTrue(drops.size() == 1, "one drop");
        ItemStack drop = drops.get(0).getItem();
        h.assertTrue(
                drop.get(DataComponents.BLOCK_STATE) != null
                        && "mature"
                                .equals(drop.get(DataComponents.BLOCK_STATE)
                                        .properties()
                                        .get("stage")),
                "the drop carries stage=mature");
        BlockPos ground = new BlockPos(4, 1, 1);
        h.setBlock(ground, Blocks.STONE);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, drop.getItem(), 1);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, drop.copy());
        h.placeAt(player, player.getMainHandItem(), ground, Direction.UP);
        BlockState placed = h.getBlockState(ground.above());
        h.assertTrue(placed.is(EcologyModule.LIVING_SUBSTRATE.get()), "substrate placed");
        h.assertTrue(placed.getValue(LivingSubstrateBlock.STAGE) == SubstrateStage.MATURE, "placed cell is mature");
        h.succeed();
    }

    private static void assertCrafts(GameTestHelper h, List<ItemStack> inputs, net.minecraft.world.item.Item result) {
        var recipe = h.getLevel()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(inputs.size(), 1, inputs), h.getLevel());
        h.assertTrue(recipe.isPresent(), "recipe present");
        h.assertTrue(
                recipe.get()
                        .value()
                        .getResultItem(h.getLevel().registryAccess())
                        .is(result),
                "result is " + result);
    }

    private static BlockState mature() {
        return EcologyTests.mature();
    }
}
