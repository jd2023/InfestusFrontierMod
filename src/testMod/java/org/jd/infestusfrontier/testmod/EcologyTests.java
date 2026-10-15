package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
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

    static Player holding(GameTestHelper h, GameType mode, Item item, int count) {
        Player player = h.makeMockPlayer(mode);
        // The mock player answers isCreative() but keeps survival abilities; items check the abilities like vanilla.
        player.getAbilities().instabuild = mode.isCreative();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, count));
        return player;
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
