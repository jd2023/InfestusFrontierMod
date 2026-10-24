package org.jd.infestusfrontier.testmod;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.control.ControlModule;
import org.jd.infestusfrontier.control.TreadReceptorBlock;
import org.jd.infestusfrontier.ecology.EcologyModule;

/**
 * The Tread Receptor: a graft grows it in an exposed mature cell; a creature standing on it presses it as on a stone
 * plate, an item does not; it springs back when the creature leaves; pressed, it opens a Clenched Door standing on the
 * cell beside it; broken, it dies into Dead Tissue and no piston moves it. Ground cells sit at y=1.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class TreadReceptorTests {
    private static final int Y = 1;

    private TreadReceptorTests() {}

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theGraftGrowsAReceptorOnlyInAnExposedMatureCell(GameTestHelper h) {
        BlockPos open = new BlockPos(2, Y, 2);
        BlockPos covered = new BlockPos(5, Y, 2);
        h.setBlock(open, EcologyTests.mature());
        h.setBlock(covered, EcologyTests.mature());
        h.setBlock(covered.above(), Blocks.STONE);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, ControlModule.TREAD_RECEPTOR_ITEM.get(), 2);
        EcologyTests.use(h, player, covered, Direction.UP);
        h.assertBlockPresent(EcologyModule.LIVING_SUBSTRATE.get(), covered);
        h.assertValueEqual(player.getMainHandItem().getCount(), 2, "a covered cell takes no graft");
        EcologyTests.use(h, player, open, Direction.UP);
        h.assertBlockPresent(ControlModule.TREAD_RECEPTOR.get(), open);
        h.assertFalse(h.getBlockState(open).getValue(TreadReceptorBlock.POWERED), "it grows unpressed");
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "one graft a receptor");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aCreatureStandingOnItPressesItAndItSpringsBackWhenItLeaves(GameTestHelper h) {
        BlockPos receptor = penned(h, new BlockPos(3, Y, 3));
        Pig pig = h.spawn(EntityType.PIG, receptor.above());
        h.runAfterDelay(10, () -> {
            h.assertTrue(h.getBlockState(receptor).getValue(TreadReceptorBlock.POWERED), "the pig presses it");
            h.runAfterDelay(60, () -> {
                h.assertTrue(
                        h.getBlockState(receptor).getValue(TreadReceptorBlock.POWERED),
                        "it stays pressed while the pig stands there");
                pig.discard();
                h.runAfterDelay(30, () -> {
                    h.assertFalse(
                            h.getBlockState(receptor).getValue(TreadReceptorBlock.POWERED),
                            "it springs back once nobody stands on it");
                    h.succeed();
                });
            });
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anItemDoesNotPressIt(GameTestHelper h) {
        BlockPos receptor = new BlockPos(3, Y, 3);
        h.setBlock(receptor, ControlModule.TREAD_RECEPTOR.get());
        ItemEntity item = h.spawnItem(Items.WHEAT, receptor.getX() + 0.5F, Y + 1.2F, receptor.getZ() + 0.5F);
        h.runAfterDelay(20, () -> {
            h.assertTrue(item.isAlive() && item.onGround(), "the item lies on the receptor");
            h.assertFalse(h.getBlockState(receptor).getValue(TreadReceptorBlock.POWERED), "items do not press it");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void pressedItOpensAClenchedDoorStandingOnTheCellBesideIt(GameTestHelper h) {
        BlockPos receptor = penned(h, new BlockPos(3, Y, 3));
        BlockPos floor = receptor.south();
        BlockPos lower = floor.above();
        h.setBlock(floor, EcologyTests.mature());
        BlockState door = EcologyModule.CLENCHED_DOOR
                .get()
                .defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        h.setBlock(lower, door);
        h.setBlock(lower.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        h.setBlock(lower.east(), Blocks.GLASS);
        h.setBlock(lower.west(), Blocks.GLASS);
        h.setBlock(lower.south(), Blocks.GLASS);
        h.assertFalse(h.getBlockState(lower).getValue(DoorBlock.OPEN), "closed before anyone comes");
        h.spawn(EntityType.PIG, receptor.above());
        h.succeedWhen(() -> {
            h.assertTrue(h.getBlockState(lower).getValue(DoorBlock.OPEN), "the receptor opens the door");
            h.assertTrue(h.getBlockState(lower.above()).getValue(DoorBlock.OPEN), "both halves");
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theReceptorDiesIntoDeadTissueAndNoPistonMovesIt(GameTestHelper h) {
        List<ItemStack> drops = Block.getDrops(
                ControlModule.TREAD_RECEPTOR.get().defaultBlockState(),
                h.getLevel(),
                h.absolutePos(new BlockPos(1, Y, 1)),
                null,
                null,
                ItemStack.EMPTY);
        h.assertTrue(
                drops.size() == 1 && drops.getFirst().is(EcologyModule.DEAD_TISSUE.get()),
                "one Dead Tissue, not " + drops);
        BlockPos piston = new BlockPos(1, Y + 1, 4);
        h.setBlock(piston, Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST));
        h.setBlock(piston.east(), ControlModule.TREAD_RECEPTOR.get());
        h.setBlock(piston.west(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(10, () -> {
            h.assertBlockPresent(ControlModule.TREAD_RECEPTOR.get(), piston.east());
            h.assertBlockPresent(Blocks.AIR, piston.east(2));
            h.succeed();
        });
    }

    /** A receptor with glass round the block above it on three sides, so a creature put on it stays there. */
    private static BlockPos penned(GameTestHelper h, BlockPos receptor) {
        h.setBlock(receptor, ControlModule.TREAD_RECEPTOR.get());
        BlockPos above = receptor.above();
        for (Direction side : List.of(Direction.NORTH, Direction.EAST, Direction.WEST, Direction.SOUTH))
            if (h.getBlockState(above.relative(side)).isAir()) h.setBlock(above.relative(side), Blocks.GLASS);
        for (Direction side : List.of(Direction.NORTH, Direction.EAST, Direction.WEST, Direction.SOUTH))
            h.setBlock(above.relative(side).above(), Blocks.GLASS);
        return receptor;
    }
}
