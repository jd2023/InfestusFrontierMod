package org.jd.infestusfrontier.testmod.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.testmod.integration.ContentAssertion;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class EcologyGameTests {
    private static final ResourceLocation CULTURE = id("construction/spore_culture");
    private static final ResourceLocation SUBSTRATE = id("ecology/living_substrate");

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void visibleSelectedGroundConvertsAndPaysExactlyOnce(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(CULTURE), 2));

        var result = useHeldOn(player, pos, Direction.UP);

        helper.assertTrue(result.consumesAction(), "Visible eligible ground must accept selected Culture use");
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(block(SUBSTRATE)),
                "Culture must replace only the selected ground cell");
        helper.assertTrue(player.getMainHandItem().getCount() == 1,
                "One successful conversion must spend exactly one Culture");
        helper.assertTrue(!useHeldOn(player, pos, Direction.UP).consumesAction()
                        && player.getMainHandItem().getCount() == 1,
                "Soil tag membership must not allow culture to convert living substrate again");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:ecology.living_substrate.obtain");
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void hiddenProtectedAndUnpaidConversionsAreAtomic(GameTestHelper helper) {
        BlockPos hidden = helper.absolutePos(BlockPos.ZERO);
        helper.getLevel().setBlockAndUpdate(hidden, Blocks.DIRT.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(hidden.above(), Blocks.STONE.defaultBlockState());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(CULTURE), 3));
        helper.assertTrue(!useHeldOn(player, hidden, Direction.UP).consumesAction(),
                "A fully covered clicked face must refuse conversion");
        helper.assertTrue(helper.getLevel().getBlockState(hidden).is(Blocks.DIRT)
                        && player.getMainHandItem().getCount() == 3,
                "Hidden-target refusal must preserve ground and inventory");

        BlockPos protectedPos = hidden.east(2);
        helper.getLevel().setBlockAndUpdate(protectedPos, Blocks.DIRT.defaultBlockState());
        player.getAbilities().mayBuild = false;
        helper.assertTrue(!useHeldOn(player, protectedPos, Direction.UP).consumesAction(),
                "A protected target must refuse conversion");
        helper.assertTrue(helper.getLevel().getBlockState(protectedPos).is(Blocks.DIRT)
                        && player.getMainHandItem().getCount() == 3,
                "Protected-target refusal must preserve ground and inventory");

        player.getAbilities().mayBuild = true;
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(!item(CULTURE).useOn(context(player, protectedPos, Direction.UP)).consumesAction(),
                "A conversion without Culture payment must refuse");
        helper.assertTrue(helper.getLevel().getBlockState(protectedPos).is(Blocks.DIRT),
                "Unpaid refusal must preserve the target");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void selectedSoilConversionDoesNotConsumeLivingCover(GameTestHelper helper) {
        BlockPos soil = helper.absolutePos(BlockPos.ZERO);
        helper.getLevel().setBlockAndUpdate(soil, Blocks.DIRT.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(soil.above(), Blocks.SHORT_GRASS.defaultBlockState());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(CULTURE)));

        helper.assertTrue(useHeldOn(player, soil, Direction.EAST).consumesAction(),
                "An exposed side may deliberately select covered soil");
        helper.assertTrue(helper.getLevel().getBlockState(soil).is(block(SUBSTRATE)),
                "Only selected soil should convert");
        helper.assertTrue(helper.getLevel().getBlockState(soil.above()).is(Blocks.SHORT_GRASS),
                "Conversion must not break or consume supported grass");

        helper.getLevel().setBlockAndUpdate(soil.above().north(), Blocks.STONE.defaultBlockState());
        helper.assertTrue(helper.getLevel().getBlockState(soil.above()).is(Blocks.SHORT_GRASS),
                "Living cover must survive an actual neighboring block update after conversion");
        helper.assertTrue(Blocks.SHORT_GRASS.defaultBlockState().canSurvive(helper.getLevel(), soil.above())
                        && Blocks.OAK_SAPLING.defaultBlockState().canSurvive(helper.getLevel(), soil.above()),
                "Converted ground must remain valid soil for grass and saplings");

        BlockPos tree = soil.east(2);
        helper.getLevel().setBlockAndUpdate(tree, Blocks.OAK_LOG.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(CULTURE)));
        helper.assertTrue(!useHeldOn(player, tree, Direction.UP).consumesAction(),
                "A tree is not eligible host ground");
        helper.assertTrue(helper.getLevel().getBlockState(tree).is(Blocks.OAK_LOG)
                        && player.getMainHandItem().getCount() == 1,
                "Ineligible vegetation must remain unchanged and unpaid");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void ownerCanMatureAndDyeWhileAnotherPlayerCannot(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        var owner = helper.makeMockPlayer(GameType.SURVIVAL);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(CULTURE)));
        helper.assertTrue(useHeldOn(owner, pos, Direction.UP).consumesAction(), "Fixture conversion must succeed");

        var other = helper.makeMockPlayer(GameType.SURVIVAL);
        other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 2));
        var denied = helper.getLevel().getBlockState(pos).useItemOn(
                other.getMainHandItem(), helper.getLevel(), other, InteractionHand.MAIN_HAND, hit(pos, Direction.UP));
        helper.assertTrue(!denied.consumesAction() && other.getMainHandItem().getCount() == 2,
                "Ownership must survive conversion and refuse another player's mutation without payment");

        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 2));
        for (int step = 0; step < 2; step++) {
            var result = helper.getLevel().getBlockState(pos).useItemOn(
                    owner.getMainHandItem(), helper.getLevel(), owner, InteractionHand.MAIN_HAND, hit(pos, Direction.UP));
            helper.assertTrue(result.consumesAction(), "Owner's selected maturity treatment must succeed");
        }
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CYAN_DYE));
        helper.getLevel().getBlockState(pos).useItemOn(
                owner.getMainHandItem(), helper.getLevel(), owner, InteractionHand.MAIN_HAND, hit(pos, Direction.UP));
        String state = helper.getLevel().getBlockState(pos).toString();
        helper.assertTrue(state.contains("stage=mature") && state.contains("pigment=cyan"),
                "Maturity and pigment must coexist in the same static cell state: " + state);
        helper.assertTrue(owner.getMainHandItem().isEmpty(), "Dye must be paid exactly once");
        other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_DYE));
        var stillDenied = helper.getLevel().getBlockState(pos).useItemOn(
                other.getMainHandItem(), helper.getLevel(), other, InteractionHand.MAIN_HAND, hit(pos, Direction.UP));
        helper.assertTrue(!stillDenied.consumesAction() && other.getMainHandItem().getCount() == 1
                        && helper.getLevel().getBlockState(pos).toString().contains("pigment=cyan"),
                "Ownership must remain unchanged through growth and pigment mutation");

        other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(CULTURE)));
        helper.assertTrue(!useHeldOn(other, pos, Direction.UP).consumesAction()
                        && other.getMainHandItem().getCount() == 1
                        && helper.getLevel().getBlockState(pos).toString().equals(state),
                "Culture must not reset a grown cell's maturity, pigment or ownership through its soil tag");

        var drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos, null, owner, ItemStack.EMPTY);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem() instanceof BlockItem,
                "Normal dismantling must recover one substrate cell");
        helper.getLevel().removeBlock(pos, false);
        BlockPos support = pos.east(2);
        helper.getLevel().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
        owner.setItemInHand(InteractionHand.MAIN_HAND, drops.getFirst());
        var placed = owner.getMainHandItem().useOn(new UseOnContext(
                owner, InteractionHand.MAIN_HAND, hit(support, Direction.UP)));
        String recovered = helper.getLevel().getBlockState(support.above()).toString();
        helper.assertTrue(placed.consumesAction() && recovered.contains("stage=mature") && recovered.contains("pigment=cyan"),
                "Recovered substrate must retain maturity and pigment: " + recovered);
        helper.assertTrue(helper.getLevel().getBlockEntity(support.above()) == null
                        && !(block(SUBSTRATE) instanceof net.minecraft.world.level.block.EntityBlock),
                "Static substrate must not create a block entity");
        helper.assertTrue(!helper.getLevel().getBlockState(support.above()).isRandomlyTicking(),
                "Static substrate must not acquire a random ticker");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:ecology.living_substrate.use");
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void everyNeighborDirectionConnectsWithoutStoredRandomState(GameTestHelper helper) {
        Block substrate = block(SUBSTRATE);
        var state = substrate.defaultBlockState();
        for (Direction direction : Direction.values()) {
            helper.assertTrue(state.skipRendering(state, direction),
                    "Substrate must cull its connected " + direction + " face");
        }
        helper.assertTrue(state.getValues().size() == 2,
                "Only anatomy stage and pigment may be stored; visual variation must not be random history");
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        helper.assertTrue(state.getSeed(pos) == state.getSeed(pos),
                "Variant selection must be deterministic for a position");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 20)
    public static void sharedTickAdmissionStopsTheSeventeenthConversion(GameTestHelper helper) {
        helper.runAfterDelay(5, () -> {
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(CULTURE), 17));
            int admitted = 0;
            for (int index = 0; index < 17; index++) {
                BlockPos pos = helper.absolutePos(new BlockPos(index % 5, 0, index / 5));
                helper.getLevel().setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                if (useHeldOn(player, pos, Direction.UP).consumesAction()) admitted++;
            }
            helper.assertTrue(admitted == 16, "Shared ecology quota must admit exactly 16 conversions per server tick");
            helper.assertTrue(player.getMainHandItem().getCount() == 1,
                    "A quota refusal must preserve the seventeenth Culture payment");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void unloadedTargetRefusesWithoutLoadingOrSpending(GameTestHelper helper) {
        var level = helper.getLevel();
        var source = level.getChunkSource();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        BlockPos target = new BlockPos(origin.getX() + 8192, -61, origin.getZ());
        int chunkX = target.getX() >> 4;
        int chunkZ = target.getZ() >> 4;
        helper.assertTrue(source.getChunkNow(chunkX, chunkZ) == null, "Target fixture must be unloaded");
        int loadedBefore = source.getLoadedChunksCount();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(CULTURE), 2));
        var result = useHeldOn(player, target, Direction.UP);
        helper.assertTrue(source.getChunkNow(chunkX, chunkZ) == null
                        && source.getLoadedChunksCount() == loadedBefore,
                "Culture must not load its target chunk, even before eligibility checks");
        helper.assertTrue(!result.consumesAction() && player.getMainHandItem().getCount() == 2,
                "An unloaded target must refuse without payment");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void unloadedClickedFaceNeighborRefusesWithoutLoadingOrSpending(GameTestHelper helper) {
        var level = helper.getLevel();
        var source = level.getChunkSource();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        int chunkX = (origin.getX() >> 4) + 256;
        int chunkZ = origin.getZ() >> 4;
        level.getChunk(chunkX, chunkZ);
        BlockPos target = new BlockPos(chunkX * 16 + 15, origin.getY(), chunkZ * 16 + 8);
        level.setBlock(target, Blocks.DIRT.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE, 0);
        helper.assertTrue(source.getChunkNow(chunkX + 1, chunkZ) == null,
                "Clicked east face fixture must have an unloaded adjacent chunk");
        int loadedBefore = source.getLoadedChunksCount();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(CULTURE), 2));
        var result = useHeldOn(player, target, Direction.EAST);
        helper.assertTrue(source.getChunkNow(chunkX + 1, chunkZ) == null
                        && source.getLoadedChunksCount() == loadedBefore,
                "Visibility checks must not load the adjacent chunk");
        helper.assertTrue(!result.consumesAction() && player.getMainHandItem().getCount() == 2
                        && level.getBlockState(target).is(Blocks.DIRT),
                "Unavailable clicked face must preserve target and payment");
        helper.succeed();
    }

    private static net.minecraft.world.InteractionResult useHeldOn(
            net.minecraft.world.entity.player.Player player, BlockPos pos, Direction face) {
        return player.getMainHandItem().getItem().useOn(context(player, pos, face));
    }

    private static UseOnContext context(net.minecraft.world.entity.player.Player player, BlockPos pos, Direction face) {
        return new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos, face));
    }

    private static BlockHitResult hit(BlockPos pos, Direction face) {
        return new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
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
