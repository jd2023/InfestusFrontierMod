package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.ecology.LivingSubstrateBlock;
import org.jd.infestusfrontier.ecology.SubstrateStage;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class EcologyTests {
    private static final BlockPos CELL = new BlockPos(1, 1, 1);

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void cultureConvertsExposedGrass(GameTestHelper h) {
        h.setBlock(CELL, Blocks.GRASS_BLOCK);
        Player player = holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 2);
        use(h, player, CELL, Direction.UP);
        assertStage(h, CELL, SubstrateStage.BASIC);
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "culture left in hand");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void cultureRefusesCoveredGround(GameTestHelper h) {
        h.setBlock(CELL, Blocks.DIRT);
        h.setBlock(CELL.above(), Blocks.STONE);
        h.setBlock(CELL.north(), Blocks.STONE);
        Player player = holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 2);
        use(h, player, CELL, Direction.UP);
        use(h, player, CELL, Direction.NORTH);
        h.assertBlockPresent(Blocks.DIRT, CELL);
        h.assertValueEqual(player.getMainHandItem().getCount(), 2, "culture left in hand");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void cultureRefusesNonHost(GameTestHelper h) {
        h.setBlock(CELL, Blocks.SAND);
        Player player = holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 2);
        use(h, player, CELL, Direction.UP);
        h.assertBlockPresent(Blocks.SAND, CELL);
        h.assertValueEqual(player.getMainHandItem().getCount(), 2, "culture left in hand");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void creativeKeepsTheCulture(GameTestHelper h) {
        h.setBlock(CELL, Blocks.STONE);
        Player player = holding(h, GameType.CREATIVE, EcologyModule.SPORE_CULTURE.get(), 2);
        use(h, player, CELL, Direction.UP);
        assertStage(h, CELL, SubstrateStage.BASIC);
        h.assertValueEqual(player.getMainHandItem().getCount(), 2, "culture left in hand");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void conversionDropsNothing(GameTestHelper h) {
        h.setBlock(CELL, Blocks.GRASS_BLOCK);
        Player player = holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 1);
        use(h, player, CELL, Direction.UP);
        assertStage(h, CELL, SubstrateStage.BASIC);
        h.runAfterDelay(5, () -> {
            for (Item item : new Item[] {
                EcologyModule.SPORE_CULTURE.get(),
                EcologyModule.LIVING_SUBSTRATE_ITEM.get(),
                Items.DIRT,
                Items.GRASS_BLOCK
            }) {
                h.assertItemEntityNotPresent(item, CELL, 2.0);
            }
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void boneMealMaturesOneStageEveryThirtyTicks(GameTestHelper h) {
        h.setBlock(CELL, EcologyModule.LIVING_SUBSTRATE.get());
        for (Direction side : Direction.Plane.HORIZONTAL) h.setBlock(CELL.relative(side), Blocks.OAK_PLANKS);
        Player player = holding(h, GameType.SURVIVAL, Items.BONE_MEAL, 2);
        use(h, player, CELL, Direction.UP);
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "one bone meal used");
        assertStage(h, CELL, SubstrateStage.BASIC);
        int step = LivingSubstrateBlock.GROWTH_DELAY_TICKS;
        h.runAfterDelay(step - 2, () -> assertStage(h, CELL, SubstrateStage.BASIC));
        h.runAfterDelay(step + 2, () -> assertStage(h, CELL, SubstrateStage.YOUNG));
        h.runAfterDelay(2 * step + 2, () -> assertStage(h, CELL, SubstrateStage.MATURE));
        h.runAfterDelay(3 * step + 2, () -> {
            assertVigor(h, CELL, 0);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 800)
    public static void oneBoneMealSpreadsAtMostFifteenCellsThenStops(GameTestHelper h) {
        BlockPos center = new BlockPos(7, 1, 7);
        for (int x = 2; x <= 12; x++) for (int z = 2; z <= 12; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
        h.setBlock(center, mature());
        Player player = holding(h, GameType.SURVIVAL, Items.BONE_MEAL, 1);
        use(h, player, center, Direction.UP);
        h.assertValueEqual(player.getMainHandItem().getCount(), 0, "bone meal used on a mature cell with hosts");
        int step = LivingSubstrateBlock.GROWTH_DELAY_TICKS;
        h.runAfterDelay(step + 2, () -> {
            h.assertValueEqual(countCells(h), 3, "the first wave converts exactly two hosts");
            assertVigor(h, center, 0);
        });
        h.runAfterDelay(12 * step, () -> {
            int settled = countCells(h);
            h.assertTrue(settled >= 7 && settled <= 15, "the wave reached " + settled + " cells, expected 7 to 15");
            h.assertTrue(countVigor(h) == 0, "every cell is dormant");
            h.runAfterDelay(4 * step, () -> {
                h.assertValueEqual(countCells(h), settled, "nothing grows after the wave");
                h.succeed();
            });
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void boneMealIsKeptOnAMatureCellWithNoExposedHost(GameTestHelper h) {
        h.setBlock(CELL, mature());
        for (Direction side : Direction.Plane.HORIZONTAL) {
            h.setBlock(CELL.relative(side), Blocks.DIRT);
            h.setBlock(CELL.relative(side).above(), Blocks.OAK_PLANKS);
        }
        Player player = holding(h, GameType.SURVIVAL, Items.BONE_MEAL, 2);
        use(h, player, CELL, Direction.UP);
        h.assertValueEqual(player.getMainHandItem().getCount(), 2, "bone meal kept");
        assertVigor(h, CELL, 0);
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 100)
    public static void spreadFollowsOneStepUpAndDown(GameTestHelper h) {
        BlockPos cell = new BlockPos(4, 2, 4);
        h.setBlock(cell, mature());
        h.setBlock(cell.north(), Blocks.OAK_PLANKS);
        h.setBlock(cell.south(), Blocks.OAK_PLANKS);
        h.setBlock(cell.east(), Blocks.OAK_PLANKS); // planks are not hosts
        h.setBlock(cell.east().above(), Blocks.DIRT);
        h.setBlock(cell.west().below(), Blocks.DIRT);
        Player player = holding(h, GameType.SURVIVAL, Items.BONE_MEAL, 1);
        use(h, player, cell, Direction.UP);
        h.runAfterDelay(LivingSubstrateBlock.GROWTH_DELAY_TICKS + 2, () -> {
            assertStage(h, cell.east().above(), SubstrateStage.YOUNG);
            assertStage(h, cell.west().below(), SubstrateStage.YOUNG);
            assertVigor(h, cell.east().above(), 2);
            h.assertBlockPresent(Blocks.OAK_PLANKS, cell.east());
            h.succeed();
        });
    }

    static BlockState mature() {
        return EcologyModule.LIVING_SUBSTRATE
                .get()
                .defaultBlockState()
                .setValue(LivingSubstrateBlock.STAGE, SubstrateStage.MATURE);
    }

    static int countCells(GameTestHelper h) {
        int count = 0;
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++)
                if (h.getBlockState(new BlockPos(x, 1, z)).is(EcologyModule.LIVING_SUBSTRATE.get())) count++;
        return count;
    }

    static int countVigor(GameTestHelper h) {
        int sum = 0;
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                BlockState state = h.getBlockState(new BlockPos(x, 1, z));
                if (state.is(EcologyModule.LIVING_SUBSTRATE.get())) sum += state.getValue(LivingSubstrateBlock.VIGOR);
            }
        return sum;
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void tileFollowsPosition(GameTestHelper h) {
        Player player = holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 8);
        BlockPos[] cells = {new BlockPos(1, 1, 1), new BlockPos(4, 1, 1), new BlockPos(1, 1, 4), new BlockPos(3, 1, 2)};
        for (BlockPos cell : cells) {
            h.setBlock(cell, Blocks.GRASS_BLOCK);
            use(h, player, cell, Direction.UP);
            assertTile(h, cell);
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void wrongTileIsCorrectedOnPlace(GameTestHelper h) {
        int wrong = (LivingSubstrateBlock.tileAt(h.absolutePos(CELL)) + 1) % 16;
        h.setBlock(
                CELL,
                EcologyModule.LIVING_SUBSTRATE.get().defaultBlockState().setValue(LivingSubstrateBlock.TILE, wrong));
        assertTile(h, CELL);
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 100)
    public static void tileSurvivesGrowth(GameTestHelper h) {
        h.setBlock(CELL, EcologyModule.LIVING_SUBSTRATE.get());
        for (Direction side : Direction.Plane.HORIZONTAL) h.setBlock(CELL.relative(side), Blocks.OAK_PLANKS);
        Player player = holding(h, GameType.SURVIVAL, Items.BONE_MEAL, 1);
        use(h, player, CELL, Direction.UP);
        h.runAfterDelay(LivingSubstrateBlock.GROWTH_DELAY_TICKS + 2, () -> {
            assertStage(h, CELL, SubstrateStage.YOUNG);
            assertTile(h, CELL);
            h.succeed();
        });
    }

    static void assertTile(GameTestHelper h, BlockPos cell) {
        int tile = LivingSubstrateBlock.tileAt(h.absolutePos(cell));
        h.assertBlockState(
                cell, (BlockState s) -> s.getValue(LivingSubstrateBlock.TILE) == tile, () -> "expected tile " + tile);
    }

    static void assertVigor(GameTestHelper h, BlockPos cell, int vigor) {
        h.assertBlockState(
                cell,
                (BlockState s) -> s.getValue(LivingSubstrateBlock.VIGOR) == vigor,
                () -> "expected vigor " + vigor);
    }

    static Player holding(GameTestHelper h, GameType mode, Item item, int count) {
        Player player = h.makeMockPlayer(mode);
        // The mock player answers isCreative() but keeps survival abilities; items check the abilities like vanilla.
        player.getAbilities().instabuild = mode.isCreative();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, count));
        return player;
    }

    /** The block's own answer to the held item, before vanilla falls back to item use. */
    static ItemInteractionResult useHeld(GameTestHelper h, Player player, BlockPos cell, Direction face) {
        BlockPos absolute = h.absolutePos(cell);
        Vec3 hit = Vec3.atCenterOf(absolute)
                .add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
        return h.getBlockState(cell)
                .useItemOn(
                        player.getMainHandItem(),
                        h.getLevel(),
                        player,
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(hit, face, absolute, false));
    }

    static void use(GameTestHelper h, Player player, BlockPos cell, Direction face) {
        BlockPos absolute = h.absolutePos(cell);
        Vec3 hit = Vec3.atCenterOf(absolute)
                .add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
        h.useBlock(cell, player, new BlockHitResult(hit, face, absolute, false));
    }

    static void assertStage(GameTestHelper h, BlockPos cell, SubstrateStage stage) {
        Block substrate = EcologyModule.LIVING_SUBSTRATE.get();
        h.assertBlockPresent(substrate, cell);
        h.assertBlockState(
                cell,
                (BlockState s) -> s.getValue(LivingSubstrateBlock.STAGE) == stage,
                () -> "expected stage " + stage.getSerializedName());
    }
}
