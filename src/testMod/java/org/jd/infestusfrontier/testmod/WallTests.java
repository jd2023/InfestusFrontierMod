package org.jd.infestusfrontier.testmod;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.ecology.LivingSubstrateBlock;
import org.jd.infestusfrontier.ecology.SubstrateStage;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.OrganBatchRecipe;

/**
 * Living walls: started by sneaking with bone meal on a mature cell, grown from their top face, hardened by grafts, an
 * explosion against the hardest tier, and that they never move. Ground cells sit at y=1.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class WallTests {
    private static final int Y = 1;

    private WallTests() {}

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void sneakingWithBoneMealOnAMatureCellStartsAWall(GameTestHelper h) {
        BlockPos cell = new BlockPos(2, Y, 2);
        BlockPos plain = new BlockPos(5, Y, 2);
        h.setBlock(cell, EcologyTests.mature());
        h.setBlock(plain, EcologyTests.mature());
        FakePlayer player = gardener(h, 3);
        use(h, player, plain, Direction.UP, false);
        h.assertBlockPresent(Blocks.AIR, plain.above());
        h.assertValueEqual(
                player.getMainHandItem().getCount(), 3, "plain bone meal on a dormant mature cell starts no wall");
        use(h, player, cell, Direction.UP, true);
        h.assertBlockPresent(EcologyModule.LIVING_WALL.get(), cell.above());
        h.assertValueEqual(player.getMainHandItem().getCount(), 2, "one bone meal a block");
        EcologyTests.assertStage(h, cell, org.jd.infestusfrontier.ecology.SubstrateStage.MATURE);
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void onlyTheTopOfAMatureCellStartsAWall(GameTestHelper h) {
        BlockPos young = new BlockPos(2, Y, 2);
        BlockPos mature = new BlockPos(5, Y, 2);
        h.setBlock(
                young,
                EcologyModule.LIVING_SUBSTRATE
                        .get()
                        .defaultBlockState()
                        .setValue(LivingSubstrateBlock.STAGE, SubstrateStage.YOUNG));
        h.setBlock(mature, EcologyTests.mature());
        FakePlayer player = gardener(h, 3);
        use(h, player, young, Direction.UP, true);
        use(h, player, mature, Direction.NORTH, true);
        h.assertFalse(h.getBlockState(young.above()).is(EcologyModule.LIVING_WALL.get()), "no wall on a young cell");
        h.assertBlockPresent(Blocks.AIR, mature.above());
        h.assertBlockPresent(Blocks.AIR, mature.north());
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void spectatorsAndAdventurersGrowNoWall(GameTestHelper h) {
        BlockPos cell = new BlockPos(2, Y, 2);
        BlockPos wall = new BlockPos(5, Y + 1, 2);
        h.setBlock(cell, EcologyTests.mature());
        h.setBlock(wall.below(), EcologyTests.mature());
        h.setBlock(wall, EcologyModule.LIVING_WALL.get());
        for (GameType mode : List.of(GameType.ADVENTURE, GameType.SPECTATOR)) {
            FakePlayer player = gardener(h, 2);
            player.setGameMode(mode);
            use(h, player, cell, Direction.UP, true);
            use(h, player, wall, Direction.UP, false);
            h.assertBlockPresent(Blocks.AIR, cell.above());
            h.assertBlockPresent(Blocks.AIR, wall.above());
            h.assertValueEqual(player.getMainHandItem().getCount(), 2, mode + " spends no bone meal");
            player.setGameMode(GameType.SURVIVAL);
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void creativeSpendsNeitherBoneMealNorGrafts(GameTestHelper h) {
        BlockPos cell = new BlockPos(2, Y, 2);
        h.setBlock(cell, EcologyTests.mature());
        FakePlayer player = gardener(h, 1);
        player.setGameMode(GameType.CREATIVE);
        use(h, player, cell, Direction.UP, true);
        h.assertBlockPresent(EcologyModule.LIVING_WALL.get(), cell.above());
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "the bone meal is kept");
        player.setGameMode(GameType.SURVIVAL);
        var hand = EcologyTests.holding(h, GameType.CREATIVE, EcologyModule.SKELETAL_GRAFT.get(), 1);
        EcologyTests.use(h, hand, cell.above(), Direction.NORTH);
        h.assertBlockPresent(EcologyModule.RIBBED_WALL.get(), cell.above());
        h.assertValueEqual(hand.getMainHandItem().getCount(), 1, "the graft is kept");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void boneMealOnAWallsTopGrowsItAndItsSideDoesNot(GameTestHelper h) {
        BlockPos wall = new BlockPos(2, Y + 1, 2);
        h.setBlock(wall.below(), EcologyTests.mature());
        h.setBlock(wall, EcologyModule.LIVING_WALL.get());
        FakePlayer player = gardener(h, 4);
        use(h, player, wall, Direction.NORTH, false);
        h.assertBlockPresent(Blocks.AIR, wall.north());
        h.assertBlockPresent(Blocks.AIR, wall.above());
        h.assertValueEqual(player.getMainHandItem().getCount(), 4, "a side face grows nothing");
        use(h, player, wall, Direction.UP, false);
        use(h, player, wall.above(), Direction.UP, false);
        h.assertBlockPresent(EcologyModule.LIVING_WALL.get(), wall.above());
        h.assertBlockPresent(EcologyModule.LIVING_WALL.get(), wall.above(2));
        h.assertValueEqual(player.getMainHandItem().getCount(), 2, "two blocks, two bone meal");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aWallNeverGrowsIntoSomeoneStandingThere(GameTestHelper h) {
        BlockPos wall = new BlockPos(2, Y + 1, 2);
        h.setBlock(wall.below(), EcologyTests.mature());
        h.setBlock(wall, EcologyModule.LIVING_WALL.get());
        h.spawn(EntityType.PIG, wall.above());
        FakePlayer player = gardener(h, 1);
        use(h, player, wall, Direction.UP, false);
        h.assertBlockPresent(Blocks.AIR, wall.above());
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "nothing spent");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void graftsHardenAWallOneTierAtATime(GameTestHelper h) {
        BlockPos wall = new BlockPos(2, Y + 1, 2);
        h.setBlock(wall, EcologyModule.LIVING_WALL.get());
        var hand = EcologyTests.holding(
                h, net.minecraft.world.level.GameType.SURVIVAL, EcologyModule.CALCIFYING_GRAFT.get(), 1);
        EcologyTests.use(h, hand, wall, Direction.NORTH);
        h.assertBlockPresent(EcologyModule.LIVING_WALL.get(), wall);
        h.assertValueEqual(hand.getMainHandItem().getCount(), 1, "a Calcifying Graft needs a Ribbed Wall");
        hand.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EcologyModule.SKELETAL_GRAFT.get()));
        EcologyTests.use(h, hand, wall, Direction.NORTH);
        h.assertBlockPresent(EcologyModule.RIBBED_WALL.get(), wall);
        h.assertTrue(hand.getMainHandItem().isEmpty(), "the Skeletal Graft is spent");
        hand.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EcologyModule.CALCIFYING_GRAFT.get()));
        EcologyTests.use(h, hand, wall, Direction.NORTH);
        h.assertBlockPresent(EcologyModule.CALCIFIED_WALL.get(), wall);
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCalcifiedWallSurvivesTheBlastThatTakesALivingWall(GameTestHelper h) {
        BlockPos blast = new BlockPos(4, Y + 1, 4);
        BlockPos soft = blast.east();
        BlockPos hard = blast.west();
        h.setBlock(soft, EcologyModule.LIVING_WALL.get());
        h.setBlock(hard, EcologyModule.CALCIFIED_WALL.get());
        BlockPos at = h.absolutePos(blast);
        h.getLevel()
                .explode(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 4.0F, Level.ExplosionInteraction.TNT);
        h.assertBlockPresent(Blocks.AIR, soft);
        h.assertBlockPresent(EcologyModule.CALCIFIED_WALL.get(), hard);
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void wallsDieIntoDeadTissueAndNoPistonMovesThem(GameTestHelper h) {
        BlockPos at = h.absolutePos(new BlockPos(1, Y, 1));
        List<Block> walls = List.of(
                EcologyModule.LIVING_WALL.get(), EcologyModule.RIBBED_WALL.get(), EcologyModule.CALCIFIED_WALL.get());
        for (Block wall : walls) {
            List<ItemStack> drops =
                    Block.getDrops(wall.defaultBlockState(), h.getLevel(), at, null, null, ItemStack.EMPTY);
            h.assertTrue(
                    drops.size() == 1 && drops.getFirst().is(EcologyModule.DEAD_TISSUE.get()),
                    wall + " gives one Dead Tissue, not " + drops);
        }
        for (int i = 0; i < walls.size(); i++) {
            BlockPos piston = new BlockPos(1, Y + 1, 3 + 2 * i);
            h.setBlock(piston, Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST));
            h.setBlock(piston.east(), walls.get(i));
            h.setBlock(piston.west(), Blocks.REDSTONE_BLOCK);
        }
        h.runAfterDelay(10, () -> {
            for (int i = 0; i < walls.size(); i++) {
                BlockPos wall = new BlockPos(2, Y + 1, 3 + 2 * i);
                h.assertBlockPresent(walls.get(i), wall);
                h.assertBlockPresent(Blocks.AIR, wall.east());
            }
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theCystMakesBothGrafts(GameTestHelper h) {
        for (String graft : List.of("skeletal_graft", "calcifying_graft")) {
            var holder = h.getLevel()
                    .getRecipeManager()
                    .byKey(ResourceLocation.fromNamespaceAndPath("infestusfrontier", "activating/" + graft));
            h.assertTrue(holder.isPresent(), graft + " recipe loaded");
            var recipe = holder.get().value();
            h.assertTrue(recipe instanceof OrganBatchRecipe, graft + " is an organ batch");
            h.assertValueEqual(recipe.getType(), ProcessingModule.ORGAN_BATCH.get(), graft + " runs in an organ");
            h.assertTrue(
                    recipe.getResultItem(h.getLevel().registryAccess())
                            .is(
                                    graft.equals("skeletal_graft")
                                            ? EcologyModule.SKELETAL_GRAFT.get()
                                            : EcologyModule.CALCIFYING_GRAFT.get()),
                    graft + " makes the graft");
        }
        h.succeed();
    }

    // --- fixtures ---

    /** A server player holding {@code count} bone meal, so clicks go through the real interaction and its events. */
    private static FakePlayer gardener(GameTestHelper h, int count) {
        FakePlayer player = FakePlayerFactory.get(
                h.getLevel(), new GameProfile(UUID.fromString("7a1c33f2-0b5e-4f7e-9d0e-5b8a2f4c6e10"), "wall_tests"));
        player.setShiftKeyDown(false);
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, count));
        return player;
    }

    private static void use(GameTestHelper h, ServerPlayer player, BlockPos cell, Direction face, boolean sneaking) {
        BlockPos absolute = h.absolutePos(cell);
        Vec3 hit = Vec3.atCenterOf(absolute)
                .add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
        player.setShiftKeyDown(sneaking);
        player.gameMode.useItemOn(
                player,
                h.getLevel(),
                player.getMainHandItem(),
                InteractionHand.MAIN_HAND,
                new BlockHitResult(hit, face, absolute, false));
        player.setShiftKeyDown(false);
    }
}
