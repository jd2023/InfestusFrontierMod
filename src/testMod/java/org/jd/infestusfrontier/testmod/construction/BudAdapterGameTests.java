package org.jd.infestusfrontier.testmod.construction;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.construction.api.BudConstructionRecipe;
import org.jd.infestusfrontier.construction.api.BudConstructionResult;

/** Isolated recipe fixtures exercise the production adapter without registering gameplay recipes. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class BudAdapterGameTests {
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void unloadedBoundaryRefusesWithoutLoadingOrSpending(GameTestHelper helper) {
        var level = helper.getLevel();
        var source = level.getChunkSource();
        var origin = helper.absolutePos(BlockPos.ZERO);
        int chunkX = (origin.getX() >> 4) + 128;
        int chunkZ = origin.getZ() >> 4;
        // Explicit test setup loads one remote chunk, never the neighboring FULL chunk.
        level.getChunk(chunkX, chunkZ);
        BlockPos pos = new BlockPos(chunkX * 16 + 15, origin.getY(), chunkZ * 16 + 8);
        Block bud = bud();
        level.setBlock(pos.below(), substrate().defaultBlockState(), Block.UPDATE_KNOWN_SHAPE, 0);
        level.setBlock(pos, bud.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE, 0);
        helper.assertTrue(source.getChunkNow(chunkX + 1, chunkZ) == null, "Fixture requires an unloaded east neighbor");
        int loadedBefore = source.getLoadedChunksCount();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 2));
        var result = recipes().apply(level, pos, bud, player, player.getMainHandItem());
        helper.assertTrue(source.getLoadedChunksCount() == loadedBefore
                        && source.getChunkNow(chunkX + 1, chunkZ) == null,
                "Construction must not load chunks through indirect vanilla updates");
        helper.assertTrue(result != BudConstructionResult.SUCCESS, "Unavailable update neighborhood must refuse");
        helper.assertTrue(level.getBlockState(pos).is(bud), "Refusal must leave the bud unchanged");
        helper.assertTrue(player.getMainHandItem().getCount() == 2, "Refusal must preserve all ingredients");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void loadedConstructionPaysOnceAndIncompleteLeavesEverything(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        Block bud = bud();
        level.setBlock(pos.below(), substrate().defaultBlockState(), Block.UPDATE_KNOWN_SHAPE, 0);
        level.setBlock(pos, bud.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE, 0);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var recipes = recipes();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL));
        helper.assertTrue(recipes.apply(level, pos, bud, player, player.getMainHandItem()) == BudConstructionResult.INCOMPLETE,
                "Incomplete registered recipe must refuse");
        helper.assertTrue(level.getBlockState(pos).is(bud) && player.getMainHandItem().getCount() == 1,
                "Incomplete recipe must preserve bud and inputs");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 4));
        helper.assertTrue(recipes.apply(level, pos, bud, player, player.getMainHandItem()) == BudConstructionResult.SUCCESS,
                "Loaded registered recipe must succeed");
        helper.assertTrue(level.getBlockState(pos).is(Blocks.STONE) && player.getMainHandItem().getCount() == 2,
                "Construction must replace one bud and pay the exact remaining cost");
        helper.assertTrue(recipes.apply(level, pos, bud, player, player.getMainHandItem()) == BudConstructionResult.TARGET_CHANGED
                        && player.getMainHandItem().getCount() == 2,
                "Duplicate construction must not pay twice");
        helper.succeed();
    }

    private static Adapter recipes() {
        var recipes = new Adapter();
        recipes.register(new BudConstructionRecipe("infestusfrontier_tests:stone", "minecraft:bone_meal",
                "minecraft:stone", Map.of("minecraft:bone_meal", 2)));
        return recipes;
    }

    // Separate NeoForge modules cannot share packages. Keep reflective access confined to this fixture,
    // so production does not expose an adapter API just for tests or install test recipes globally.
    private static final class Adapter {
        private final Object registry;
        private final java.lang.reflect.Method apply;

        Adapter() {
            try {
                var type = Class.forName("org.jd.infestusfrontier.construction.BudRecipeRegistry");
                var constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                registry = constructor.newInstance();
                apply = type.getDeclaredMethod("apply", net.minecraft.world.level.Level.class, BlockPos.class,
                        Block.class, net.minecraft.world.entity.player.Player.class, ItemStack.class);
                apply.setAccessible(true);
            } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        }

        void register(BudConstructionRecipe recipe) {
            ((org.jd.infestusfrontier.construction.api.BudRecipeRegistrar) registry).register(recipe);
        }

        BudConstructionResult apply(net.minecraft.world.level.Level level, BlockPos pos, Block bud,
                net.minecraft.world.entity.player.Player player, ItemStack held) {
            try { return (BudConstructionResult) apply.invoke(registry, level, pos, bud, player, held); }
            catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        }
    }

    private static Block bud() {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse("infestusfrontier:construction/organ_bud")).orElseThrow();
    }

    private static Block substrate() {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse("infestusfrontier:ecology/living_substrate")).orElseThrow();
    }
}
