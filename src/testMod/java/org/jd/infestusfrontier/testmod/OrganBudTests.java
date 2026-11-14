package org.jd.infestusfrontier.testmod;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.OrganGrowthRecipe;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.StorageModule;

/**
 * Growing organs from an Organ Bud: the choice, the supply, the growth in place and every way it must not lose or
 * duplicate what was supplied.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class OrganBudTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos BUD = GROUND.above();
    private static final BlockPos OTHER_GROUND = new BlockPos(3, 1, 1);
    private static final BlockPos OTHER = OTHER_GROUND.above();

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("infestusfrontier", path);
    }

    /** A bud on a foundation, planted the way the world holds one. */
    private static void plant(GameTestHelper h, BlockPos ground) {
        h.setBlock(ground, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(ground.above(), OrganModule.ORGAN_BUD.get());
    }

    private static Player holding(GameTestHelper h, ItemStack stack) {
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static BlockHitResult hit(GameTestHelper h, BlockPos cell) {
        BlockPos absolute = h.absolutePos(cell);
        return new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0.5, 0), Direction.UP, absolute, false);
    }

    /** Right click with {@code stack}; the player's hand shows what is left of it. */
    private static ItemStack feed(GameTestHelper h, BlockPos cell, ItemStack stack) {
        Player player = holding(h, stack);
        EcologyTests.useHeld(h, player, cell, Direction.UP);
        return player.getMainHandItem();
    }

    /** Right click with an empty hand, sneaking or not. */
    private static void look(GameTestHelper h, BlockPos cell, boolean sneaking) {
        Player player = holding(h, ItemStack.EMPTY);
        player.setShiftKeyDown(sneaking);
        h.getBlockState(cell).useWithoutItem(h.getLevel(), player, hit(h, cell));
    }

    private static CompoundTag saved(GameTestHelper h, BlockPos cell) {
        return h.getBlockEntity(cell).saveWithFullMetadata(h.getLevel().registryAccess());
    }

    private static int supplied(GameTestHelper h, BlockPos cell) {
        int total = 0;
        for (var stack : saved(h, cell).getList("supplied", 10)) total += ((CompoundTag) stack).getInt("count");
        return total;
    }

    private record Growth(String organ, Block block, List<ItemStack> materials) {}

    private static List<Growth> growths() {
        return List.of(
                new Growth(
                        "digestive_sac",
                        ProcessingModule.DIGESTIVE_SAC.get(),
                        List.of(new ItemStack(Items.ROTTEN_FLESH, 2), new ItemStack(Items.BOWL))),
                new Growth(
                        "biomass_bladder",
                        StorageModule.BIOMASS_BLADDER.get(),
                        List.of(new ItemStack(Items.GLASS, 2), new ItemStack(Items.SLIME_BALL))),
                new Growth(
                        "culture_bowl",
                        ProcessingModule.CULTURE_BOWL.get(),
                        List.of(
                                new ItemStack(Items.FLOWER_POT),
                                new ItemStack(EcologyModule.SPORE_CULTURE.get()),
                                new ItemStack(Items.WHEAT_SEEDS))));
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void everyOrganGrowsInPlaceFromItsMaterials(GameTestHelper h) {
        int x = 1;
        for (Growth growth : growths()) {
            BlockPos ground = new BlockPos(x, 1, 1);
            x += 2;
            plant(h, ground);
            List<ItemStack> materials = growth.materials();
            for (int i = 0; i < materials.size(); i++) {
                h.assertBlockPresent(OrganModule.ORGAN_BUD.get(), ground.above());
                ItemStack left = feed(h, ground.above(), materials.get(i).copy());
                h.assertTrue(left.isEmpty(), growth.organ() + " takes " + materials.get(i));
            }
            h.assertBlockPresent(growth.block(), ground.above());
            h.assertTrue(h.getBlockEntity(ground.above()) != null, growth.organ() + " has its block entity");
        }
        h.assertValueEqual(TestDrops.dropped(h).size(), 0, "growth gives nothing back and drops nothing");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void growthTakesOnlyWhatItNeedsAndOnlyOnce(GameTestHelper h) {
        plant(h, GROUND);
        ItemStack left = feed(h, BUD, new ItemStack(Items.ROTTEN_FLESH, 5));
        h.assertValueEqual(left.getCount(), 3, "two of five rotten flesh are taken");
        h.assertValueEqual(supplied(h, BUD), 2, "the bud holds two");
        left = feed(h, BUD, new ItemStack(Items.ROTTEN_FLESH, 3));
        h.assertValueEqual(left.getCount(), 3, "a filled material takes no more");
        h.assertBlockPresent(OrganModule.ORGAN_BUD.get(), BUD);
        feed(h, BUD, new ItemStack(Items.BOWL, 2));
        h.assertBlockPresent(ProcessingModule.DIGESTIVE_SAC.get(), BUD);
        h.assertValueEqual(TestDrops.dropped(h).size(), 0, "nothing falls out of a grown organ");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void unusedItemsAndEmptyHandsChangeNothing(GameTestHelper h) {
        plant(h, GROUND);
        ItemStack left = feed(h, BUD, new ItemStack(Items.STICK, 4));
        h.assertValueEqual(left.getCount(), 4, "a stick is not a material");
        look(h, BUD, false);
        look(h, BUD, false);
        h.assertValueEqual(supplied(h, BUD), 0, "nothing was supplied");
        h.assertBlockPresent(OrganModule.ORGAN_BUD.get(), BUD);
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBudWithoutFoundationKeepsItsMaterialsAndGrowsWhenItHasOne(GameTestHelper h) {
        h.setBlock(GROUND, Blocks.DIRT);
        h.setBlock(BUD, OrganModule.ORGAN_BUD.get());
        feed(h, BUD, new ItemStack(Items.GLASS, 2));
        ItemStack left = feed(h, BUD, new ItemStack(Items.SLIME_BALL));
        h.assertTrue(left.isEmpty(), "the bud holds the last material");
        h.assertBlockPresent(OrganModule.ORGAN_BUD.get(), BUD);
        h.assertValueEqual(supplied(h, BUD), 3, "it keeps all three items");
        look(h, BUD, false);
        h.assertBlockPresent(OrganModule.ORGAN_BUD.get(), BUD);
        h.assertValueEqual(supplied(h, BUD), 3, "a refused growth loses nothing");
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        look(h, BUD, false);
        h.assertBlockPresent(StorageModule.BIOMASS_BLADDER.get(), BUD);
        h.assertValueEqual(TestDrops.dropped(h).size(), 0, "the retry spent them once");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void breakingABudGivesBackWhatItWasSupplied(GameTestHelper h) {
        plant(h, GROUND);
        feed(h, BUD, new ItemStack(Items.ROTTEN_FLESH, 2));
        h.getLevel().destroyBlock(h.absolutePos(BUD), true);
        h.runAfterDelay(2, () -> {
            Map<Item, Integer> dropped = TestDrops.dropped(h);
            h.assertValueEqual(dropped.get(Items.ROTTEN_FLESH), 2, "both rotten flesh come back, once");
            h.assertValueEqual(dropped.get(OrganModule.ORGAN_BUD_ITEM.get()), 1, "and the bud itself");
            h.assertValueEqual(dropped.size(), 2, "nothing else");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void suppliedMaterialsSurviveSaveAndLoad(GameTestHelper h) {
        plant(h, GROUND);
        plant(h, OTHER_GROUND);
        feed(h, BUD, new ItemStack(Items.ROTTEN_FLESH, 2));
        CompoundTag kept = saved(h, BUD);
        h.getBlockEntity(OTHER).loadWithComponents(kept, h.getLevel().registryAccess());
        h.assertValueEqual(supplied(h, OTHER), 2, "the loaded bud holds what the saved one did");
        h.assertValueEqual(
                saved(h, OTHER).getString("organ"),
                "infestusfrontier:digestive_sac",
                "and the organ it was told to grow");
        feed(h, OTHER, new ItemStack(Items.BOWL));
        h.assertBlockPresent(ProcessingModule.DIGESTIVE_SAC.get(), OTHER);
        h.assertBlockPresent(OrganModule.ORGAN_BUD.get(), BUD);
        h.assertValueEqual(supplied(h, BUD), 2, "the first bud is as it was");
        h.assertValueEqual(TestDrops.dropped(h).size(), 0, "no copy fell out");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void unreadableSavedDataIsKeptAndRefused(GameTestHelper h) {
        plant(h, GROUND);
        CompoundTag bad = saved(h, BUD);
        bad.put("supplied", StringTag.valueOf("not a list"));
        h.getBlockEntity(BUD).loadWithComponents(bad, h.getLevel().registryAccess());
        ItemStack left = feed(h, BUD, new ItemStack(Items.ROTTEN_FLESH, 2));
        h.assertValueEqual(left.getCount(), 2, "an unreadable bud takes nothing");
        look(h, BUD, true);
        h.assertValueEqual(
                saved(h, BUD).getString("supplied"), "not a list", "its saved bytes come back out unchanged");
        h.getLevel().destroyBlock(h.absolutePos(BUD), true);
        h.runAfterDelay(2, () -> {
            List<ItemEntity> drops = h.getEntities(EntityType.ITEM, BUD, 2.0);
            h.assertValueEqual(drops.size(), 1, "one bud item");
            h.assertTrue(
                    drops.get(0).getItem().has(OrganModule.UNREADABLE_SAVE.get()), "it carries the unreadable data");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theChoiceFollowsTheHandUntilSomethingIsSupplied(GameTestHelper h) {
        plant(h, GROUND);
        look(h, BUD, false);
        h.assertValueEqual(saved(h, BUD).getString("organ"), "infestusfrontier:biomass_bladder", "the first organ");
        look(h, BUD, true);
        h.assertValueEqual(saved(h, BUD).getString("organ"), "infestusfrontier:culture_bowl", "sneaking: the next");
        look(h, BUD, true);
        h.assertValueEqual(saved(h, BUD).getString("organ"), "infestusfrontier:digestive_sac", "and the next");
        look(h, BUD, true);
        h.assertValueEqual(saved(h, BUD).getString("organ"), "infestusfrontier:biomass_bladder", "then round again");
        feed(h, BUD, new ItemStack(Items.ROTTEN_FLESH, 1));
        h.assertValueEqual(
                saved(h, BUD).getString("organ"),
                "infestusfrontier:digestive_sac",
                "an item picks the organ that uses it");
        look(h, BUD, true);
        h.assertValueEqual(
                saved(h, BUD).getString("organ"), "infestusfrontier:digestive_sac", "supplied: the choice is fixed");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void growthRecipesAskOnlyForStarterMaterials(GameTestHelper h) {
        List<RecipeHolder<OrganGrowthRecipe>> all = OrganGrowthRecipe.all(h.getLevel());
        h.assertValueEqual(all.size(), 3, "the Sac, the Bladder and the Bowl grow from a bud");
        for (RecipeHolder<OrganGrowthRecipe> holder : all) {
            for (SizedIngredient material : holder.value().materials())
                for (ItemStack stack : material.getItems()) {
                    ResourceLocation item = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    h.assertTrue(
                            item.getNamespace().equals("minecraft") || stack.is(EcologyModule.SPORE_CULTURE.get()),
                            holder.id() + " asks for " + item + ", which a new player cannot get from plain materials");
                }
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theThreeOrgansAreNoLongerCrafted(GameTestHelper h) {
        List<Item> grown = List.of(
                ProcessingModule.DIGESTIVE_SAC_ITEM.get(),
                StorageModule.BIOMASS_BLADDER_ITEM.get(),
                ProcessingModule.CULTURE_BOWL_ITEM.get());
        for (var holder : h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            ItemStack result = holder.value().getResultItem(h.getLevel().registryAccess());
            h.assertFalse(grown.contains(result.getItem()), holder.id() + " still crafts " + result);
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void existingOrganItemsStillPlace(GameTestHelper h) {
        Player builder = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        int x = 1;
        List<Item> items = List.of(
                ProcessingModule.DIGESTIVE_SAC_ITEM.get(),
                StorageModule.BIOMASS_BLADDER_ITEM.get(),
                ProcessingModule.CULTURE_BOWL_ITEM.get());
        List<Block> blocks = List.of(
                ProcessingModule.DIGESTIVE_SAC.get(),
                StorageModule.BIOMASS_BLADDER.get(),
                ProcessingModule.CULTURE_BOWL.get());
        for (int i = 0; i < items.size(); i++) {
            BlockPos ground = new BlockPos(x, 1, 1);
            x += 2;
            h.setBlock(ground, EcologyModule.LIVING_SUBSTRATE.get());
            ItemStack stack = new ItemStack(items.get(i));
            builder.setItemInHand(InteractionHand.MAIN_HAND, stack);
            h.placeAt(builder, stack, ground, Direction.UP);
            h.assertBlockPresent(blocks.get(i), ground.above());
        }
        h.succeed();
    }

    /** Feeds {@code stack} to the bud at {@code pos} (a position in the world) as a survival player; returns the rest. */
    private static ItemStack feedAt(GameTestHelper h, BlockPos pos, ItemStack stack) {
        Player player = holding(h, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
        h.getLevel()
                .getBlockState(pos)
                .useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        return player.getMainHandItem();
    }

    /**
     * The demo's Organ Bud exhibit: a fresh bud, a half-fed one and a fed one, and the starter chest stocking every
     * material. Each bud grows its organ when used as its sign says.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 300)
    public static void theDemoExhibitGrowsBudsAndStocksTheirMaterials(GameTestHelper h) {
        DemoExhibit yard = new DemoExhibit(new BlockPos(13, -61, -42), new BlockPos(19, -59, -41), 50);
        DemoExhibit stock = new DemoExhibit(new BlockPos(14, -61, -37), new BlockPos(14, -59, -35), 51);
        yard.force(h);
        stock.force(h);
        h.startSequence()
                .thenIdle(DemoExhibit.ENTITIES_LOAD_TICKS)
                .thenExecute(() -> {
                    yard.build(h);
                    stock.build(h);
                    var level = h.getLevel();
                    BlockPos fresh = yard.at(new BlockPos(14, -60, -41));
                    BlockPos half = yard.at(new BlockPos(16, -60, -41));
                    BlockPos fed = yard.at(new BlockPos(18, -60, -41));
                    for (BlockPos bud : List.of(fresh, half, fed))
                        h.assertTrue(level.getBlockState(bud).is(OrganModule.ORGAN_BUD.get()), "a bud at " + bud);
                    var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity)
                            level.getBlockEntity(stock.at(new BlockPos(14, -60, -36)));
                    for (Item material :
                            List.of(Items.ROTTEN_FLESH, Items.BOWL, Items.GLASS, Items.SLIME_BALL, Items.FLOWER_POT))
                        h.assertTrue(chest != null && chest.countItem(material) >= 2, "the chest holds " + material);
                    feedAt(h, half, new ItemStack(Items.BOWL));
                    h.assertTrue(
                            level.getBlockState(half).is(ProcessingModule.DIGESTIVE_SAC.get()),
                            "the half-fed bud needs only its bowl");
                    Player player = holding(h, ItemStack.EMPTY);
                    level.getBlockState(fed)
                            .useWithoutItem(
                                    level,
                                    player,
                                    new BlockHitResult(Vec3.atCenterOf(fed).add(0, 0.5, 0), Direction.UP, fed, false));
                    h.assertTrue(
                            level.getBlockState(fed).is(StorageModule.BIOMASS_BLADDER.get()),
                            "the fed bud grows when clicked");
                    for (ItemStack material : growths().get(2).materials())
                        h.assertTrue(feedAt(h, fresh, material.copy()).isEmpty(), "the fresh bud takes " + material);
                    h.assertTrue(
                            level.getBlockState(fresh).is(ProcessingModule.CULTURE_BOWL.get()),
                            "the fresh bud grows a bowl");
                    // The grown organs leave the way a player takes them out, so the network forgets them.
                    for (BlockPos organ : List.of(fresh, half, fed)) level.destroyBlock(organ, false);
                    yard.tidy(h);
                    stock.tidy(h);
                    yard.forget(h);
                    stock.forget(h);
                })
                .thenSucceed();
    }
}
