package org.jd.infestusfrontier.testmod;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.interaction.InteractionModule;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.MouthBlock;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.logistics.VeinBlock;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassFluids;

/**
 * Septum Crossings and Vascular Stitches: grafting a crossing, two streams through it unmixed, the probe turning it,
 * its bound and its drops; a stitch carrying biomass up, down and under a ceiling, placed by hand, letting go of a
 * lost end, and items routed over it. Ground cells sit at y=1.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class CrossingTests {
    private static final int Y = 1;
    private static final int TRANSFER = 250;

    private CrossingTests() {}

    // --- the Septum Crossing ---

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theGraftTurnsAnIdleSealedVeinIntoACrossingOfItsCarrier(GameTestHelper h) {
        BlockPos biomass = new BlockPos(1, Y, 1);
        BlockPos unsealed = new BlockPos(3, Y, 1);
        BlockPos item = new BlockPos(7, Y, 1);
        h.setBlock(biomass, LogisticsModule.BIOMASS_VEIN.get());
        h.setBlock(unsealed, LogisticsModule.FLUID_VEIN.get());
        h.setBlock(item, LogisticsModule.ITEM_VEIN.get());
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, LogisticsModule.SEPTUM_CROSSING_ITEM.get(), 4);
        EcologyTests.use(h, player, unsealed, Direction.UP);
        h.assertBlockPresent(LogisticsModule.FLUID_VEIN.get(), unsealed);
        h.assertValueEqual(player.getMainHandItem().getCount(), 4, "nothing spent on an unsealed cell");
        EcologyTests.use(h, player, biomass, Direction.UP);
        EcologyTests.use(h, player, item, Direction.NORTH);
        h.assertBlockPresent(LogisticsModule.SEPTUM_CROSSING.get(), biomass);
        h.assertValueEqual(value(h.getBlockState(biomass), "carrier"), "biomass", "the vein's carrier");
        h.assertValueEqual(
                h.getBlockState(biomass).getValue(BlockStateProperties.AXIS),
                Direction.Axis.Y,
                "the clicked top shows both lumens");
        h.assertValueEqual(value(h.getBlockState(item), "carrier"), "item", "an item crossing");
        h.assertValueEqual(
                h.getBlockState(item).getValue(BlockStateProperties.AXIS),
                Direction.Axis.Z,
                "turned to the clicked side");
        h.assertValueEqual(player.getMainHandItem().getCount(), 2, "one graft a crossing");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void twoBiomassStreamsCrossWithoutMixing(GameTestHelper h) {
        // west to east along z=3 and north to south along x=3, through one crossing at (3, 3); a tank on each last cell
        BlockPos crossing = new BlockPos(3, Y, 3);
        h.setBlock(crossing, crossing(LogisticsModule.BIOMASS_VEIN.get(), Direction.Axis.Y));
        for (int i : new int[] {1, 2, 4, 5}) {
            h.setBlock(new BlockPos(i, Y, 3), LogisticsModule.BIOMASS_VEIN.get());
            h.setBlock(new BlockPos(3, Y, i), LogisticsModule.BIOMASS_VEIN.get());
        }
        BlockPos east = new BlockPos(5, Y + 1, 3);
        BlockPos south = new BlockPos(3, Y + 1, 5);
        h.setBlock(east, StorageModule.BIOMASS_BLADDER.get());
        h.setBlock(south, StorageModule.BIOMASS_BLADDER.get());
        h.assertTrue(port(h, crossing, Direction.UP) == null, "no passage through the septum's own faces");
        IFluidHandler west = port(h, new BlockPos(1, Y, 3), Direction.WEST);
        IFluidHandler north = port(h, new BlockPos(3, Y, 1), Direction.NORTH);
        for (int transfer = 0; transfer < 2; transfer++)
            h.runAtTickTime(
                    1 + transfer * 12,
                    () -> h.assertValueEqual(
                            west.fill(biomass(TRANSFER), IFluidHandler.FluidAction.EXECUTE), TRANSFER, "west took it"));
        h.runAtTickTime(
                2,
                () -> h.assertValueEqual(
                        north.fill(biomass(TRANSFER), IFluidHandler.FluidAction.EXECUTE), TRANSFER, "north took it"));
        h.runAtTickTime(90, () -> {
            h.assertValueEqual(stored(h, east), 2 * TRANSFER, "both west transfers went straight on east");
            h.assertValueEqual(stored(h, south), TRANSFER, "the north transfer went straight on south");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aFluidCrossingPassesWaterOnAndRefusesBiomass(GameTestHelper h) {
        BlockPos crossing = new BlockPos(2, Y, 2);
        BlockPos far = crossing.east();
        h.setBlock(crossing, crossing(LogisticsModule.FLUID_VEIN.get(), Direction.Axis.Y));
        h.setBlock(far, LogisticsModule.FLUID_VEIN.get().defaultBlockState().setValue(VeinBlock.SEALED, true));
        h.setBlock(far.above(), ProcessingModule.CULTURE_BOWL.get());
        IFluidHandler entry = port(h, crossing, Direction.WEST);
        h.assertValueEqual(
                entry.fill(biomass(TRANSFER), IFluidHandler.FluidAction.EXECUTE),
                0,
                "a fluid crossing never passes biomass");
        h.assertValueEqual(
                entry.fill(new FluidStack(Fluids.WATER, TRANSFER), IFluidHandler.FluidAction.EXECUTE),
                TRANSFER,
                "water passes");
        h.assertValueEqual(
                port(h, far.above(), null).getFluidInTank(0).getAmount(), TRANSFER, "the endpoint holds the water");
        h.assertValueEqual(entry.getFluidInTank(0).getAmount(), 0, "the crossing itself holds nothing");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theProbeTurnsACrossingSoTheClickedFaceShowsBothLumens(GameTestHelper h) {
        BlockPos crossing = new BlockPos(2, Y, 2);
        h.setBlock(crossing.north(), LogisticsModule.BIOMASS_VEIN.get());
        h.setBlock(crossing.east(), LogisticsModule.BIOMASS_VEIN.get());
        h.setBlock(crossing, crossing(LogisticsModule.BIOMASS_VEIN.get(), Direction.Axis.Y));
        h.assertTrue(h.getBlockState(crossing.north()).getValue(PipeBlock.SOUTH), "the north vein joins a passage");
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, InteractionModule.SYNAPTIC_PROBE.get(), 1);
        EcologyTests.use(h, player, crossing, Direction.NORTH);
        h.assertValueEqual(
                h.getBlockState(crossing).getValue(BlockStateProperties.AXIS),
                Direction.Axis.Z,
                "turned to the north face");
        h.assertFalse(
                h.getBlockState(crossing.north()).getValue(PipeBlock.SOUTH), "the north vein meets the septum now");
        h.assertTrue(h.getBlockState(crossing.east()).getValue(PipeBlock.WEST), "the east vein still joins");
        h.assertTrue(port(h, crossing, Direction.NORTH) == null, "no passage north now");
        h.assertTrue(port(h, crossing, Direction.UP) != null, "a passage up and down instead");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aNetworkPassesLongCrossingChainsWithoutRecursiveForwarding(GameTestHelper h) {
        // Both chains are ordinary passive network components.
        BlockPos near = row(h, 1, 9);
        BlockPos far = row(h, 3, 10);
        h.assertValueEqual(
                port(h, near, Direction.WEST).fill(biomass(TRANSFER), IFluidHandler.FluidAction.EXECUTE),
                TRANSFER,
                "nine pass");
        h.assertValueEqual(
                port(h, far, Direction.WEST).fill(biomass(TRANSFER), IFluidHandler.FluidAction.EXECUTE),
                TRANSFER,
                "ten pass without a forwarding-depth limit");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void itemsCrossWithoutMixing(GameTestHelper h) {
        // bones go west to east and sticks north to south through one item crossing at (3, 3)
        BlockPos crossing = new BlockPos(3, Y, 3);
        h.setBlock(crossing, crossing(LogisticsModule.ITEM_VEIN.get(), Direction.Axis.Y));
        h.setBlock(new BlockPos(2, Y, 3), LogisticsModule.ITEM_VEIN.get());
        h.setBlock(new BlockPos(4, Y, 3), LogisticsModule.ITEM_VEIN.get());
        h.setBlock(new BlockPos(3, Y, 2), LogisticsModule.ITEM_VEIN.get());
        h.setBlock(new BlockPos(3, Y, 4), LogisticsModule.ITEM_VEIN.get());
        BlockPos west = new BlockPos(1, Y, 3);
        BlockPos east = new BlockPos(5, Y, 3);
        BlockPos north = new BlockPos(3, Y, 1);
        BlockPos south = new BlockPos(3, Y, 5);
        store(h, west, new ItemStack(Items.BONE, 16));
        store(h, north, new ItemStack(Items.STICK, 16));
        store(h, east);
        store(h, south);
        mouth(h, west, MouthMode.OUTPUT);
        mouth(h, north, MouthMode.OUTPUT);
        mouth(h, east, MouthMode.INTAKE);
        mouth(h, south, MouthMode.INTAKE);
        h.runAfterDelay(120, () -> {
            h.assertValueEqual(count(h, east, Items.BONE), 16, "every bone went east");
            h.assertValueEqual(count(h, south, Items.STICK), 16, "every stick went south");
            h.assertValueEqual(count(h, east, Items.STICK) + count(h, south, Items.BONE), 0, "nothing turned off");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBrokenCrossingDiesIntoDeadTissue(GameTestHelper h) {
        BlockPos crossing = new BlockPos(2, Y, 2);
        h.setBlock(crossing, crossing(LogisticsModule.FLUID_VEIN.get(), Direction.Axis.Y));
        h.getLevel().destroyBlock(h.absolutePos(crossing), true);
        h.runAfterDelay(2, () -> {
            List<ItemEntity> drops = h.getEntities(EntityType.ITEM, crossing, 2.0);
            h.assertValueEqual(drops.size(), 1, "one drop");
            h.assertTrue(drops.getFirst().getItem().is(EcologyModule.DEAD_TISSUE.get()), "Dead Tissue, nothing back");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "crossing_boundary")
    public static void aCrossingOrStitchAtAChunkEdgeNeverLoadsTheFarChunk(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        BlockPos edge = new BlockPos((origin.getX() & ~15) + 232015, 80, (origin.getZ() & ~15) + 232008);
        level.getChunkAt(edge);
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(edge, crossing(LogisticsModule.BIOMASS_VEIN.get(), Direction.Axis.Y), quiet);
        BlockPos above = edge.above(2);
        level.setBlock(above, stitch(LogisticsModule.BIOMASS_VEIN.get(), Direction.EAST, false), quiet);
        h.assertFalse(level.hasChunkAt(edge.east()), "the far chunk starts unloaded");
        IFluidHandler crossing = level.getCapability(Capabilities.FluidHandler.BLOCK, edge, Direction.WEST);
        h.assertValueEqual(
                crossing.fill(biomass(TRANSFER), IFluidHandler.FluidAction.EXECUTE), 0, "the crossing passes nothing");
        IFluidHandler stitch = level.getCapability(Capabilities.FluidHandler.BLOCK, above, Direction.DOWN);
        h.assertValueEqual(stitch.fill(biomass(TRANSFER), IFluidHandler.FluidAction.EXECUTE), 0, "nor does the stitch");
        h.assertFalse(level.hasChunkAt(edge.east()), "and nothing loaded the far chunk");
        h.succeed();
    }

    // --- the Vascular Stitch ---

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aStitchCarriesBiomassUpAndDownAStep(GameTestHelper h) {
        // up: a low cell at (1, 1, 1), a high cell at (2, 2, 1) on substrate, a tank on the high cell
        BlockPos low = new BlockPos(1, Y, 1);
        BlockPos high = new BlockPos(2, Y + 1, 1);
        step(h, low, high, Direction.EAST);
        BlockPos top = high.above();
        h.setBlock(top, StorageModule.BIOMASS_BLADDER.get());
        // down: a high cell at (1, 2, 4), a low cell at (2, 1, 4), a tank under the low cell
        BlockPos high2 = new BlockPos(1, Y + 1, 4);
        BlockPos low2 = new BlockPos(2, Y, 4);
        step(h, low2, high2, Direction.WEST);
        BlockPos bottom = low2.below();
        h.setBlock(bottom, StorageModule.BIOMASS_BLADDER.get());
        h.assertTrue(
                h.getBlockState(low).getValue(PipeBlock.UP)
                        && h.getBlockState(high).getValue(PipeBlock.WEST),
                "both cells link toward the stitch");
        h.assertValueEqual(
                port(h, low, Direction.WEST).fill(biomass(TRANSFER), IFluidHandler.FluidAction.EXECUTE),
                TRANSFER,
                "into the low cell");
        h.assertValueEqual(
                port(h, high2, Direction.WEST).fill(biomass(TRANSFER), IFluidHandler.FluidAction.EXECUTE),
                TRANSFER,
                "into the high cell");
        h.runAtTickTime(40, () -> {
            h.assertValueEqual(stored(h, top), TRANSFER, "the transfer climbed the step");
            h.assertValueEqual(stored(h, bottom), TRANSFER, "the transfer came down the step");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aStitchUnderACeilingCarriesBiomass(GameTestHelper h) {
        // a ceiling cell at (1, 3, 1), a lower ceiling cell at (2, 2, 1) under substrate, a tank under the lower cell
        BlockPos upper = new BlockPos(1, Y + 2, 1);
        BlockPos lower = new BlockPos(2, Y + 1, 1);
        h.setBlock(upper, LogisticsModule.BIOMASS_VEIN.get());
        h.setBlock(lower, LogisticsModule.BIOMASS_VEIN.get());
        h.setBlock(lower.above(), EcologyTests.mature());
        h.setBlock(new BlockPos(1, Y + 1, 1), stitch(LogisticsModule.BIOMASS_VEIN.get(), Direction.EAST, true));
        BlockPos tank = lower.below();
        h.setBlock(tank, StorageModule.BIOMASS_BLADDER.get());
        h.assertValueEqual(
                port(h, upper, Direction.WEST).fill(biomass(TRANSFER), IFluidHandler.FluidAction.EXECUTE),
                TRANSFER,
                "into the upper cell");
        h.runAtTickTime(40, () -> {
            h.assertValueEqual(stored(h, tank), TRANSFER, "the transfer crossed the step overhead");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aStitchPlacedOnTheLowCellReachesForTheCellThePlayerFaces(GameTestHelper h) {
        BlockPos low = new BlockPos(2, Y, 2);
        h.setBlock(low, LogisticsModule.BIOMASS_VEIN.get());
        h.setBlock(low.east(), EcologyTests.mature());
        h.setBlock(low.east().above(), LogisticsModule.BIOMASS_VEIN.get());
        h.setBlock(low.west(), EcologyTests.mature());
        h.setBlock(low.west().above(), LogisticsModule.BIOMASS_VEIN.get());
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, LogisticsModule.VASCULAR_STITCH_ITEM.get(), 3);
        player.setYRot(Direction.EAST.toYRot());
        h.placeAt(player, player.getMainHandItem(), low, Direction.UP);
        BlockState placed = h.getBlockState(low.above());
        h.assertBlockPresent(LogisticsModule.VASCULAR_STITCH.get(), low.above());
        h.assertValueEqual(
                placed.getValue(BlockStateProperties.HORIZONTAL_FACING), Direction.EAST, "toward the faced step");
        h.assertValueEqual(value(placed, "face"), "floor", "on the floor");
        h.assertValueEqual(value(placed, "carrier"), "biomass", "of the cells' carrier");
        h.assertValueEqual(player.getMainHandItem().getCount(), 2, "one stitch spent");
        // a step whose cells carry different things takes no stitch
        BlockPos other = new BlockPos(5, Y, 2);
        h.setBlock(other, LogisticsModule.BIOMASS_VEIN.get());
        h.setBlock(other.east(), EcologyTests.mature());
        h.setBlock(other.east().above(), LogisticsModule.ITEM_VEIN.get());
        h.placeAt(player, player.getMainHandItem(), other, Direction.UP);
        h.assertBlockPresent(Blocks.AIR, other.above());
        h.assertValueEqual(player.getMainHandItem().getCount(), 2, "nothing spent on a mixed step");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aStitchLetsGoWhenAnEndCellGoes(GameTestHelper h) {
        BlockPos low = new BlockPos(1, Y, 1);
        BlockPos high = new BlockPos(2, Y + 1, 1);
        step(h, low, high, Direction.EAST);
        h.setBlock(high, Blocks.AIR);
        h.assertBlockPresent(Blocks.AIR, low.above());
        h.assertItemEntityPresent(EcologyModule.DEAD_TISSUE.get(), low.above(), 2.0);
        h.assertItemEntityNotPresent(LogisticsModule.VASCULAR_STITCH_ITEM.get(), low.above(), 2.0);
        h.assertFalse(h.getBlockState(low).getValue(PipeBlock.UP), "the low cell closes its top again");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void mouthsRouteItemsOverAStitch(GameTestHelper h) {
        // an output Mouth on the ground, a step up through a stitch, an intake Mouth on the higher level
        BlockPos source = new BlockPos(1, Y, 1);
        BlockPos low = new BlockPos(2, Y, 1);
        BlockPos high = new BlockPos(3, Y + 1, 1);
        BlockPos target = new BlockPos(4, Y + 1, 1);
        h.setBlock(low, LogisticsModule.ITEM_VEIN.get());
        h.setBlock(high, LogisticsModule.ITEM_VEIN.get());
        h.setBlock(high.below(), EcologyTests.mature());
        store(h, source, new ItemStack(Items.BONE, 16));
        store(h, target);
        mouth(h, source, MouthMode.OUTPUT);
        mouth(h, target, MouthMode.INTAKE);
        h.runAfterDelay(30, () -> {
            h.assertValueEqual(count(h, target, Items.BONE), 0, "no route before the stitch");
            h.setBlock(low.above(), stitch(LogisticsModule.ITEM_VEIN.get(), Direction.EAST, false));
            h.runAfterDelay(100, () -> {
                h.assertValueEqual(count(h, target, Items.BONE), 16, "every bone went up the step");
                h.succeed();
            });
        });
    }

    // --- fixtures ---

    private static BlockState crossing(Block vein, Direction.Axis axis) {
        String carrier = vein == LogisticsModule.FLUID_VEIN.get()
                ? "fluid"
                : vein == LogisticsModule.ITEM_VEIN.get() ? "item" : "biomass";
        return with(LogisticsModule.SEPTUM_CROSSING.get().defaultBlockState(), "carrier", carrier)
                .setValue(BlockStateProperties.AXIS, axis);
    }

    private static BlockState stitch(Block vein, Direction facing, boolean ceiling) {
        String carrier = vein == LogisticsModule.FLUID_VEIN.get()
                ? "fluid"
                : vein == LogisticsModule.ITEM_VEIN.get() ? "item" : "biomass";
        BlockState state = LogisticsModule.VASCULAR_STITCH
                .get()
                .defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        return with(with(state, "carrier", carrier), "face", ceiling ? "ceiling" : "floor");
    }

    /** A low cell, a high cell beside and above it on substrate, and the stitch in the corner, facing the high cell. */
    private static void step(GameTestHelper h, BlockPos low, BlockPos high, Direction towardHigh) {
        h.setBlock(low, LogisticsModule.BIOMASS_VEIN.get());
        h.setBlock(high, LogisticsModule.BIOMASS_VEIN.get());
        h.setBlock(high.below(), EcologyTests.mature());
        h.setBlock(low.above(), stitch(LogisticsModule.BIOMASS_VEIN.get(), towardHigh, false));
    }

    /** {@code crossings} biomass crossings in a row along z, eastward from x=1, then a tank; returns the first. */
    private static BlockPos row(GameTestHelper h, int z, int crossings) {
        for (int x = 1; x <= crossings; x++)
            h.setBlock(new BlockPos(x, Y, z), crossing(LogisticsModule.BIOMASS_VEIN.get(), Direction.Axis.Y));
        h.setBlock(new BlockPos(crossings + 1, Y, z), StorageModule.BIOMASS_BLADDER.get());
        return new BlockPos(1, Y, z);
    }

    private static IFluidHandler port(GameTestHelper h, BlockPos pos, Direction side) {
        return h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), side);
    }

    private static FluidStack biomass(int units) {
        return BiomassFluids.of(Biomass.of(units));
    }

    private static int stored(GameTestHelper h, BlockPos tank) {
        return ProcessingTests.handler(h, tank).tank().stored().units();
    }

    /** The serialized value of the property named {@code name}, for properties whose type the mod keeps private. */
    private static String value(BlockState state, String name) {
        for (Property<?> property : state.getProperties())
            if (property.getName().equals(name)) return serialized(state, property);
        throw new IllegalArgumentException("No property " + name);
    }

    private static <T extends Comparable<T>> String serialized(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static BlockState with(BlockState state, String name, String value) {
        for (Property<?> property : state.getProperties())
            if (property.getName().equals(name)) return set(state, property, value);
        throw new IllegalArgumentException("No property " + name);
    }

    private static <T extends Comparable<T>> BlockState set(BlockState state, Property<T> property, String value) {
        return state.setValue(property, property.getValue(value).orElseThrow());
    }

    private static void mouth(GameTestHelper h, BlockPos at, MouthMode mode) {
        h.setBlock(at, LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, mode));
    }

    private static void store(GameTestHelper h, BlockPos at, ItemStack... contents) {
        h.setBlock(at.above(), Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) h.getBlockEntity(at.above());
        for (int slot = 0; slot < contents.length; slot++) chest.setItem(slot, contents[slot]);
    }

    private static int count(GameTestHelper h, BlockPos at, Item item) {
        var handler = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(at.above()), null);
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++)
            if (handler.getStackInSlot(slot).is(item))
                total += handler.getStackInSlot(slot).getCount();
        return total;
    }
}
