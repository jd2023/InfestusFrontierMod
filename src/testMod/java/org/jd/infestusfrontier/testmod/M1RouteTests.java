package org.jd.infestusfrontier.testmod;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.ecology.LivingSubstrateBlock;
import org.jd.infestusfrontier.ecology.SubstrateStage;

/** The whole M1 route in one test: craft cultures, convert two hosts, bone meal, mature. Substrate is only grown. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class M1RouteTests {
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 150)
    public static void m1Route(GameTestHelper h) {
        BlockPos stone = new BlockPos(1, 1, 1);
        BlockPos grass = new BlockPos(4, 1, 1);
        h.setBlock(stone, Blocks.STONE);
        h.setBlock(grass, Blocks.GRASS_BLOCK);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 1);
        ItemStack cultures = ItemStack.EMPTY;
        for (int i = 0; i < 3; i++) {
            ItemStack made = craft(
                    h,
                    List.of(
                            new ItemStack(Items.ROTTEN_FLESH),
                            new ItemStack(Items.RED_MUSHROOM),
                            new ItemStack(Items.WHEAT_SEEDS)),
                    EcologyModule.SPORE_CULTURE.get());
            if (cultures.isEmpty()) cultures = made;
            else cultures.grow(made.getCount());
        }
        h.assertValueEqual(cultures.getCount(), 3, "cultures crafted");
        player.setItemInHand(InteractionHand.MAIN_HAND, cultures);
        EcologyTests.use(h, player, stone, Direction.UP);
        EcologyTests.use(h, player, grass, Direction.UP);
        EcologyTests.assertStage(h, stone, SubstrateStage.BASIC);
        EcologyTests.assertStage(h, grass, SubstrateStage.BASIC);
        h.assertValueEqual(cultures.getCount(), 1, "cultures left after the route");
        for (BlockPos cell : new BlockPos[] {stone, grass}) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 1));
            EcologyTests.use(h, player, cell, Direction.UP);
        }
        h.runAfterDelay(2 * LivingSubstrateBlock.GROWTH_DELAY_TICKS + 2, () -> {
            EcologyTests.assertStage(h, stone, SubstrateStage.MATURE);
            EcologyTests.assertStage(h, grass, SubstrateStage.MATURE);
            h.succeed();
        });
    }

    /** The result of the crafting recipe that {@code inputs} match, assembled as a crafting table would. */
    static ItemStack craft(GameTestHelper h, List<ItemStack> inputs, Item result) {
        CraftingInput input = CraftingInput.of(inputs.size(), 1, inputs);
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
        h.assertTrue(recipe.isPresent(), "recipe present for " + result);
        ItemStack out = recipe.get().value().assemble(input, h.getLevel().registryAccess());
        h.assertTrue(out.is(result), "result is " + result);
        return out;
    }
}
