package org.jd.infestusfrontier.testmod.kit;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class KitGameTests {
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void chunkEdgeFixtureDetectsALoad(GameTestHelper helper) {
        var fixture = ChunkEdge.prepare(helper);
        helper.getLevel().getChunk(fixture.neighborChunkX(), fixture.neighborChunkZ());
        boolean threw = false;
        try {
            ChunkEdge.assertNothingLoaded(helper, fixture);
        } catch (GameTestAssertException expected) {
            threw = true;
        }
        helper.assertTrue(threw, "Chunk-edge fixture detects a neighbor chunk load");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void chunkEdgeFixtureStaysQuietWithoutAccess(GameTestHelper helper) {
        var fixture = ChunkEdge.prepare(helper);
        ChunkEdge.assertNothingLoaded(helper, fixture);
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void reloadRoundTripsAVanillaChest(GameTestHelper helper) {
        var relative = new BlockPos(1, 1, 1);
        var pos = helper.absolutePos(relative);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        ((Container) helper.getLevel().getBlockEntity(pos)).setItem(0, new ItemStack(Items.APPLE));
        var reloaded = (Container) SaveReload.reload(helper, relative);
        helper.assertTrue(reloaded.getItem(0).is(Items.APPLE), "Reload preserves a vanilla chest item");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void rejectedDataCheckFailsWhenAKeyIsDropped(GameTestHelper helper) {
        var relative = new BlockPos(1, 1, 1);
        helper.getLevel().setBlockAndUpdate(helper.absolutePos(relative), Blocks.CHEST.defaultBlockState());
        var hostile = SaveReload.save(helper, relative).copy();
        hostile.putString("unknown_recovery_key", "must_survive");
        boolean threw = false;
        try {
            SaveReload.assertRetainsRejectedData(helper, relative, hostile);
        } catch (GameTestAssertException expected) {
            threw = true;
        }
        helper.assertTrue(threw, "Vanilla chest drops an unknown key during load");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void offhandPlacementPassesThroughAVanillaBlock(GameTestHelper helper) {
        var relative = new BlockPos(1, 1, 1);
        helper.getLevel().setBlockAndUpdate(helper.absolutePos(relative), Blocks.STONE.defaultBlockState());
        InteractionResult result = OffhandUse.useWithOffhand(helper, relative, new ItemStack(Items.COBBLESTONE));
        helper.assertTrue(result.consumesAction(), "Offhand use consumes placement against vanilla stone");
        OffhandUse.assertPlacedBeside(helper, relative, Blocks.COBBLESTONE);
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void offhandPlacementStopsWhenMainHandConsumes(GameTestHelper helper) {
        var relative = new BlockPos(1, 1, 1);
        var pos = helper.absolutePos(relative);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        var offhand = new ItemStack(Items.COBBLESTONE);
        InteractionResult result = OffhandUse.useWithOffhand(helper, relative, offhand);
        helper.assertTrue(helper.getLevel().getBlockState(pos.east()).isAir(),
                "Chest consumes the main-hand click before offhand placement");
        helper.assertTrue(offhand.getCount() == 1, "Consumed main-hand click preserves the offhand item");
        helper.assertTrue(result == InteractionResult.CONSUME, "Returns the chest's main-hand result");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void offhandFixtureLeavesSharedPlayerUntouched(GameTestHelper helper) {
        var shared = FakePlayerFactory.getMinecraft(helper.getLevel());
        var mainBefore = shared.getMainHandItem();
        var offhandBefore = shared.getOffhandItem();
        var relative = new BlockPos(1, 1, 1);
        helper.getLevel().setBlockAndUpdate(helper.absolutePos(relative), Blocks.CHEST.defaultBlockState());
        try {
            OffhandUse.useWithOffhand(helper, relative, new ItemStack(Items.COBBLESTONE));
            helper.assertTrue(shared.getMainHandItem() == mainBefore && shared.getOffhandItem() == offhandBefore,
                    "Offhand fixture must not change another test's shared player hands");
        } finally {
            shared.setItemInHand(InteractionHand.MAIN_HAND, mainBefore);
            shared.setItemInHand(InteractionHand.OFF_HAND, offhandBefore);
        }
        helper.succeed();
    }
}
