package org.jd.infestusfrontier.testmod;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.EcologyModule;

/**
 * Openings mutated from living walls: a window graft and joined panes, a door graft over two wall cells, the door
 * opened by hand and by redstone, which half draws it, and what both leave when broken. Grafts harden the door into
 * the Clenched and Calcified Doors, which only redstone opens, as the vanilla iron door. Ground cells sit at y=1.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class OpeningTests {
    private static final int Y = 1;

    private OpeningTests() {}

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aWindowGraftTurnsAWallCellOfAnyTierIntoAWindow(GameTestHelper h) {
        BlockPos living = new BlockPos(1, Y + 1, 2);
        BlockPos ribbed = new BlockPos(3, Y + 1, 2);
        BlockPos ground = new BlockPos(5, Y, 2);
        h.setBlock(living, EcologyModule.LIVING_WALL.get());
        h.setBlock(ribbed, EcologyModule.RIBBED_WALL.get());
        h.setBlock(ground, EcologyTests.mature());
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.MEMBRANE_WINDOW_ITEM.get(), 3);
        EcologyTests.use(h, player, ground, Direction.UP);
        h.assertValueEqual(player.getMainHandItem().getCount(), 3, "substrate takes no window");
        EcologyTests.use(h, player, living, Direction.NORTH);
        EcologyTests.use(h, player, ribbed, Direction.NORTH);
        h.assertBlockPresent(EcologyModule.MEMBRANE_WINDOW.get(), living);
        h.assertBlockPresent(EcologyModule.MEMBRANE_WINDOW.get(), ribbed);
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "one graft a window");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void neighbouringWindowsJoinIntoOnePane(GameTestHelper h) {
        BlockPos left = new BlockPos(2, Y + 1, 2);
        BlockPos right = left.east();
        h.setBlock(left, EcologyModule.LIVING_WALL.get());
        h.setBlock(right, EcologyModule.LIVING_WALL.get());
        h.setBlock(left.above(), EcologyModule.LIVING_WALL.get());
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.MEMBRANE_WINDOW_ITEM.get(), 2);
        EcologyTests.use(h, player, left, Direction.NORTH);
        h.assertFalse(h.getBlockState(left).getValue(PipeBlock.EAST), "a wall beside is no pane");
        EcologyTests.use(h, player, right, Direction.NORTH);
        h.assertTrue(
                h.getBlockState(left).getValue(PipeBlock.EAST)
                        && h.getBlockState(right).getValue(PipeBlock.WEST),
                "the two windows join");
        h.assertFalse(h.getBlockState(left).getValue(PipeBlock.UP), "the wall above keeps its frame edge");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aDoorGraftTurnsATwoHighWallIntoADoorway(GameTestHelper h) {
        BlockPos lower = new BlockPos(2, Y + 1, 2);
        BlockPos single = new BlockPos(5, Y + 1, 2);
        h.setBlock(lower.below(), EcologyTests.mature());
        h.setBlock(single.below(), EcologyTests.mature());
        h.setBlock(lower, EcologyModule.LIVING_WALL.get());
        h.setBlock(lower.above(), EcologyModule.CALCIFIED_WALL.get());
        h.setBlock(single, EcologyModule.LIVING_WALL.get());
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPHINCTER_DOOR_ITEM.get(), 2);
        EcologyTests.use(h, player, single, Direction.NORTH);
        h.assertBlockPresent(EcologyModule.LIVING_WALL.get(), single);
        h.assertValueEqual(player.getMainHandItem().getCount(), 2, "a one-high wall takes no door");
        EcologyTests.use(h, player, lower.above(), Direction.NORTH);
        BlockState bottom = h.getBlockState(lower);
        BlockState top = h.getBlockState(lower.above());
        h.assertTrue(
                bottom.is(EcologyModule.SPHINCTER_DOOR.get()) && top.is(EcologyModule.SPHINCTER_DOOR.get()),
                "both cells became the doorway");
        h.assertValueEqual(bottom.getValue(DoorBlock.HALF), DoubleBlockHalf.LOWER, "the lower half below");
        h.assertValueEqual(top.getValue(DoorBlock.HALF), DoubleBlockHalf.UPPER, "the upper half above");
        h.assertValueEqual(bottom.getValue(DoorBlock.FACING), Direction.NORTH, "facing the clicked side");
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "one graft a door");
        h.assertTrue(h.getBlockEntity(lower) != null, "the lower half draws the iris");
        h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(lower.above())) == null, "the upper half draws nothing");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theDoorOpensByHandAndByRedstoneAndOnlyBlocksWhenClosed(GameTestHelper h) {
        BlockPos lower = door(h, new BlockPos(2, Y + 1, 2));
        BlockPos other = door(h, new BlockPos(6, Y + 1, 2));
        h.assertFalse(
                h.getBlockState(lower)
                        .getCollisionShape(h.getLevel(), h.absolutePos(lower), CollisionContext.empty())
                        .isEmpty(),
                "a closed door stops a walker");
        Player hand = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        EcologyTests.use(h, hand, lower, Direction.NORTH);
        h.assertTrue(h.getBlockState(lower).getValue(DoorBlock.OPEN), "a hand opens it");
        h.assertTrue(h.getBlockState(lower.above()).getValue(DoorBlock.OPEN), "both halves open");
        h.assertTrue(
                h.getBlockState(lower)
                        .getCollisionShape(h.getLevel(), h.absolutePos(lower), CollisionContext.empty())
                        .isEmpty(),
                "an open door stops nothing");
        h.setBlock(other.east(), Blocks.REDSTONE_BLOCK);
        h.assertTrue(h.getBlockState(other).getValue(DoorBlock.OPEN), "redstone opens it");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aDoorGraftedBesideASignalStartsOpen(GameTestHelper h) {
        BlockPos lower = new BlockPos(2, Y + 1, 2);
        h.setBlock(lower.east(), Blocks.REDSTONE_BLOCK);
        door(h, lower);
        h.assertTrue(h.getBlockState(lower).getValue(DoorBlock.OPEN), "the signal already on opens it");
        h.assertTrue(h.getBlockState(lower).getValue(DoorBlock.POWERED), "and it knows it is powered");
        h.setBlock(lower.east(), Blocks.AIR);
        h.assertFalse(h.getBlockState(lower).getValue(DoorBlock.OPEN), "the signal going off closes it");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aWindowUnlinksWhenItsNeighbourGoesAndNoPistonMovesIt(GameTestHelper h) {
        BlockPos left = new BlockPos(2, Y + 1, 2);
        BlockPos right = left.east();
        h.setBlock(left, EcologyModule.MEMBRANE_WINDOW.get());
        h.setBlock(right, EcologyModule.MEMBRANE_WINDOW.get());
        h.assertTrue(h.getBlockState(left).getValue(PipeBlock.EAST), "joined");
        h.setBlock(right, Blocks.AIR);
        h.assertFalse(h.getBlockState(left).getValue(PipeBlock.EAST), "the frame closes again");
        BlockPos piston = new BlockPos(1, Y + 1, 5);
        h.setBlock(piston, Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST));
        h.setBlock(piston.east(), EcologyModule.MEMBRANE_WINDOW.get());
        h.setBlock(piston.west(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(10, () -> {
            h.assertBlockPresent(EcologyModule.MEMBRANE_WINDOW.get(), piston.east());
            h.assertBlockPresent(Blocks.AIR, piston.east(2));
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void openingsDieIntoDeadTissue(GameTestHelper h) {
        BlockPos at = h.absolutePos(new BlockPos(1, Y, 1));
        List<ItemStack> window = Block.getDrops(
                EcologyModule.MEMBRANE_WINDOW.get().defaultBlockState(), h.getLevel(), at, null, null, ItemStack.EMPTY);
        h.assertTrue(
                window.size() == 1 && window.getFirst().is(EcologyModule.DEAD_TISSUE.get()),
                "a window gives one Dead Tissue, not " + window);
        BlockState door = EcologyModule.SPHINCTER_DOOR.get().defaultBlockState();
        List<ItemStack> bottom = Block.getDrops(
                door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), h.getLevel(), at, null, null, ItemStack.EMPTY);
        List<ItemStack> top = Block.getDrops(
                door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), h.getLevel(), at, null, null, ItemStack.EMPTY);
        h.assertTrue(
                bottom.size() == 1
                        && bottom.getFirst().is(EcologyModule.DEAD_TISSUE.get())
                        && bottom.getFirst().getCount() == 2,
                "a door gives two Dead Tissue, its two cells, not " + bottom);
        h.assertTrue(top.isEmpty(), "and only once, from its lower half");
        BlockPos lower = door(h, new BlockPos(4, Y + 1, 3));
        h.getLevel().destroyBlock(h.absolutePos(lower.above()), true);
        h.assertBlockPresent(Blocks.AIR, lower);
        h.assertBlockPresent(Blocks.AIR, lower.above());
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void graftsHardenADoorWholeOneTierAtATime(GameTestHelper h) {
        BlockPos lower = door(h, new BlockPos(2, Y + 1, 2));
        Player hand = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.CALCIFYING_GRAFT.get(), 1);
        EcologyTests.use(h, hand, lower, Direction.NORTH);
        h.assertBlockPresent(EcologyModule.SPHINCTER_DOOR.get(), lower);
        h.assertValueEqual(hand.getMainHandItem().getCount(), 1, "a Calcifying Graft needs a Clenched Door");
        hand.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EcologyModule.SKELETAL_GRAFT.get(), 2));
        EcologyTests.use(h, hand, lower.above(), Direction.NORTH);
        assertDoor(h, lower, EcologyModule.CLENCHED_DOOR.get());
        h.assertValueEqual(hand.getMainHandItem().getCount(), 1, "one graft hardens the whole doorway");
        h.assertValueEqual(
                BuiltInRegistries.BLOCK_ENTITY_TYPE
                        .getKey(h.getBlockEntity(lower).getType())
                        .getPath(),
                "clenched_door",
                "the lower half draws the clenched iris");
        EcologyTests.use(h, hand, lower, Direction.NORTH);
        h.assertBlockPresent(EcologyModule.CLENCHED_DOOR.get(), lower);
        h.assertValueEqual(hand.getMainHandItem().getCount(), 1, "a second Skeletal Graft does nothing");
        hand.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EcologyModule.CALCIFYING_GRAFT.get()));
        EcologyTests.use(h, hand, lower, Direction.NORTH);
        assertDoor(h, lower, EcologyModule.CALCIFIED_DOOR.get());
        h.assertTrue(hand.getMainHandItem().isEmpty(), "the Calcifying Graft is spent");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aHardenedDoorIgnoresHandsAndAButtonOpensIt(GameTestHelper h) {
        BlockPos lower = hardened(h, new BlockPos(2, Y + 1, 3), 1);
        BlockPos other = hardened(h, new BlockPos(6, Y + 1, 3), 2);
        Player hand = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        EcologyTests.use(h, hand, lower, Direction.NORTH);
        EcologyTests.use(h, hand, other.above(), Direction.NORTH);
        h.assertFalse(h.getBlockState(lower).getValue(DoorBlock.OPEN), "a hand does not open a Clenched Door");
        h.assertFalse(h.getBlockState(other).getValue(DoorBlock.OPEN), "nor a Calcified Door");
        BlockPos button = lower.north();
        h.setBlock(button.below(), Blocks.STONE);
        h.setBlock(button, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.FLOOR));
        EcologyTests.use(h, hand, button, Direction.UP);
        h.assertTrue(h.getBlockState(lower).getValue(DoorBlock.OPEN), "a button opens it");
        h.assertTrue(h.getBlockState(lower.above()).getValue(DoorBlock.OPEN), "both halves open");
        h.setBlock(other.east(), Blocks.REDSTONE_BLOCK);
        h.assertTrue(h.getBlockState(other).getValue(DoorBlock.OPEN), "redstone opens the Calcified Door");
        h.runAfterDelay(30, () -> {
            h.assertFalse(h.getBlockState(lower).getValue(DoorBlock.OPEN), "it closes when the button springs back");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void villagersAndZombiesTreatOnlyTheSphincterDoorAsWooden(GameTestHelper h) {
        h.assertTrue(
                DoorBlock.isWoodenDoor(EcologyModule.SPHINCTER_DOOR.get().defaultBlockState()),
                "the Sphincter Door is a simple door that villagers open and zombies break");
        for (Block hard : List.of(EcologyModule.CLENCHED_DOOR.get(), EcologyModule.CALCIFIED_DOOR.get()))
            h.assertFalse(DoorBlock.isWoodenDoor(hard.defaultBlockState()), hard + " is an iron door to mobs");
        BlockPos at = h.absolutePos(new BlockPos(1, Y, 1));
        h.assertValueEqual(
                EcologyModule.CLENCHED_DOOR.get().defaultBlockState().getDestroySpeed(h.getLevel(), at),
                EcologyModule.RIBBED_WALL.get().defaultBlockState().getDestroySpeed(h.getLevel(), at),
                "a Clenched Door is as hard as a Ribbed Wall");
        h.assertValueEqual(
                EcologyModule.CALCIFIED_DOOR.get().getExplosionResistance(),
                EcologyModule.CALCIFIED_WALL.get().getExplosionResistance(),
                "a Calcified Door resists blasts as a Calcified Wall");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCalcifiedDoorOnAHardFloorSurvivesTheBlastThatTakesASphincterDoor(GameTestHelper h) {
        BlockPos blast = new BlockPos(4, Y + 1, 4);
        BlockPos soft = door(h, blast.east(2));
        BlockPos hard = hardened(h, blast.west(2), 2);
        h.setBlock(hard.below(), EcologyModule.CALCIFIED_WALL.get());
        BlockPos at = h.absolutePos(blast);
        h.getLevel()
                .explode(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 4.0F, Level.ExplosionInteraction.TNT);
        h.assertBlockPresent(Blocks.AIR, soft);
        assertDoor(h, hard, EcologyModule.CALCIFIED_DOOR.get());
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aDoorOpenedByHandClenchesShutAndAPoweredOneStaysOpen(GameTestHelper h) {
        BlockPos byHand = door(h, new BlockPos(2, Y + 1, 2));
        BlockPos powered = new BlockPos(6, Y + 1, 2);
        h.setBlock(powered.east(), Blocks.REDSTONE_BLOCK);
        door(h, powered);
        EcologyTests.use(h, EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0), byHand, Direction.NORTH);
        h.assertTrue(h.getBlockState(byHand).getValue(DoorBlock.OPEN), "open by hand");
        Player graft = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SKELETAL_GRAFT.get(), 2);
        EcologyTests.use(h, graft, byHand, Direction.NORTH);
        EcologyTests.use(h, graft, powered, Direction.NORTH);
        h.assertFalse(h.getBlockState(byHand).getValue(DoorBlock.OPEN), "no signal holds it, so it clenches shut");
        h.assertFalse(h.getBlockState(byHand.above()).getValue(DoorBlock.OPEN), "both halves shut");
        h.assertTrue(h.getBlockState(powered).getValue(DoorBlock.OPEN), "the signal holds the other open");
        h.assertTrue(h.getBlockState(powered).getValue(DoorBlock.POWERED), "and it knows it is powered");
        h.setBlock(powered.east(), Blocks.AIR);
        h.assertFalse(h.getBlockState(powered).getValue(DoorBlock.OPEN), "the signal going off closes it");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void hardenedDoorsDieIntoDeadTissueAndNoPistonMovesThem(GameTestHelper h) {
        BlockPos at = h.absolutePos(new BlockPos(1, Y, 1));
        for (Block hard : List.of(EcologyModule.CLENCHED_DOOR.get(), EcologyModule.CALCIFIED_DOOR.get())) {
            BlockState door = hard.defaultBlockState();
            List<ItemStack> bottom = Block.getDrops(
                    door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER),
                    h.getLevel(),
                    at,
                    null,
                    null,
                    ItemStack.EMPTY);
            List<ItemStack> top = Block.getDrops(
                    door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER),
                    h.getLevel(),
                    at,
                    null,
                    null,
                    ItemStack.EMPTY);
            h.assertTrue(
                    bottom.size() == 1
                            && bottom.getFirst().is(EcologyModule.DEAD_TISSUE.get())
                            && bottom.getFirst().getCount() == 2,
                    hard + " gives two Dead Tissue, not " + bottom);
            h.assertTrue(top.isEmpty(), hard + " gives them once, from its lower half");
        }
        BlockPos piston = new BlockPos(1, Y + 1, 4);
        BlockPos lower = hardened(h, piston.east(), 1);
        h.setBlock(piston, Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST));
        h.setBlock(piston.west(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(10, () -> {
            assertDoor(h, lower, EcologyModule.CLENCHED_DOOR.get());
            h.assertBlockPresent(Blocks.AIR, lower.east());
            h.succeed();
        });
    }

    /** Both halves of the doorway at {@code lower} are {@code door}, facing north. */
    private static void assertDoor(GameTestHelper h, BlockPos lower, Block door) {
        BlockState bottom = h.getBlockState(lower);
        BlockState top = h.getBlockState(lower.above());
        h.assertTrue(bottom.is(door) && top.is(door), "both halves are " + door + ": " + bottom + ", " + top);
        h.assertValueEqual(bottom.getValue(DoorBlock.HALF), DoubleBlockHalf.LOWER, "the lower half below");
        h.assertValueEqual(top.getValue(DoorBlock.HALF), DoubleBlockHalf.UPPER, "the upper half above");
        h.assertValueEqual(bottom.getValue(DoorBlock.FACING), Direction.NORTH, "still facing north");
        h.assertValueEqual(top.getValue(DoorBlock.FACING), Direction.NORTH, "both halves");
    }

    /** A door hardened once (Clenched) or twice (Calcified) by real grafts; returns its lower half. */
    private static BlockPos hardened(GameTestHelper h, BlockPos at, int tiers) {
        BlockPos lower = door(h, at);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SKELETAL_GRAFT.get(), 1);
        EcologyTests.use(h, player, lower, Direction.NORTH);
        if (tiers > 1) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EcologyModule.CALCIFYING_GRAFT.get()));
            EcologyTests.use(h, player, lower, Direction.NORTH);
        }
        assertDoor(h, lower, tiers > 1 ? EcologyModule.CALCIFIED_DOOR.get() : EcologyModule.CLENCHED_DOOR.get());
        return lower;
    }

    /** A two-high wall on substrate grafted into a door facing north; returns its lower half. */
    private static BlockPos door(GameTestHelper h, BlockPos lower) {
        h.setBlock(lower.below(), EcologyTests.mature());
        h.setBlock(lower, EcologyModule.LIVING_WALL.get());
        h.setBlock(lower.above(), EcologyModule.LIVING_WALL.get());
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPHINCTER_DOOR_ITEM.get(), 1);
        EcologyTests.use(h, player, lower, Direction.NORTH);
        h.assertBlockPresent(EcologyModule.SPHINCTER_DOOR.get(), lower);
        return lower;
    }
}
