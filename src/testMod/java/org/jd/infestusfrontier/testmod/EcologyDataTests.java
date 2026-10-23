package org.jd.infestusfrontier.testmod;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.cultivation.CultivationModule;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.ecology.LivingSubstrateBlock;
import org.jd.infestusfrontier.ecology.SubstrateStage;
import org.jd.infestusfrontier.equipment.EquipmentModule;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.processing.ProcessingModule;

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
    public static void dirtAndCultureNoLongerCraftSubstrate(GameTestHelper h) {
        List<ItemStack> inputs = List.of(new ItemStack(Items.DIRT), new ItemStack(EcologyModule.SPORE_CULTURE.get()));
        h.assertTrue(
                h.getLevel()
                        .getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 1, inputs), h.getLevel())
                        .isEmpty(),
                "substrate is grown from culture, never crafted");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void substrateDiesIntoDeadTissueAtEveryStage(GameTestHelper h) {
        int x = 1;
        for (SubstrateStage stage : SubstrateStage.values()) {
            BlockPos cell = new BlockPos(x, 1, 1);
            h.setBlock(
                    cell,
                    EcologyModule.LIVING_SUBSTRATE
                            .get()
                            .defaultBlockState()
                            .setValue(LivingSubstrateBlock.STAGE, stage));
            h.getLevel().destroyBlock(h.absolutePos(cell), true);
            x += 4;
        }
        List<ItemEntity> drops =
                h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds().inflate(1));
        h.assertValueEqual(drops.size(), SubstrateStage.values().length, "one drop a cell");
        for (ItemEntity drop : drops)
            h.assertTrue(drop.getItem().is(EcologyModule.DEAD_TISSUE.get()), "each is Dead Tissue");
        h.succeed();
    }

    /**
     * Living Substrate and every cell grown in it give Dead Tissue only, even to a Silk Touch tool: nothing grown can
     * be picked up and placed again.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void everyGrownCellGivesOnlyDeadTissueEvenToSilkTouch(GameTestHelper h) {
        ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
        silk.enchant(
                h.getLevel()
                        .registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SILK_TOUCH),
                1);
        BlockPos at = h.absolutePos(CELL);
        for (Block grown : List.of(
                EcologyModule.LIVING_SUBSTRATE.get(),
                LogisticsModule.BIOMASS_VEIN.get(),
                LogisticsModule.FLUID_VEIN.get(),
                LogisticsModule.ITEM_VEIN.get(),
                LogisticsModule.SEPTUM_CROSSING.get(),
                LogisticsModule.VASCULAR_STITCH.get(),
                LogisticsModule.MOUTH.get(),
                CultivationModule.CULTIVATION_TISSUE.get(),
                EquipmentModule.FUEL_PAPILLA.get())) {
            for (ItemStack tool : List.of(ItemStack.EMPTY, silk)) {
                List<ItemStack> drops = Block.getDrops(grown.defaultBlockState(), h.getLevel(), at, null, null, tool);
                h.assertTrue(
                        drops.size() == 1
                                && drops.getFirst().is(EcologyModule.DEAD_TISSUE.get())
                                && drops.getFirst().getCount() == 1,
                        grown + " gives one Dead Tissue " + (tool.isEmpty() ? "by hand" : "to Silk Touch") + ", not "
                                + drops);
            }
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theSacDigestsDeadTissue(GameTestHelper h) {
        var recipe = h.getLevel()
                .getRecipeManager()
                .getRecipeFor(
                        ProcessingModule.DIGESTION.get(),
                        new SingleRecipeInput(new ItemStack(EcologyModule.DEAD_TISSUE.get())),
                        h.getLevel());
        h.assertTrue(recipe.isPresent(), "Dead Tissue is feed");
        h.assertValueEqual(recipe.get().value().biomass().units(), 5, "5 BU");
        h.assertValueEqual(recipe.get().value().ticks(), 80, "in 80 ticks");
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
