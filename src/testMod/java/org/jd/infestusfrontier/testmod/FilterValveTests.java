package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.logistics.FilterValveBlock;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.Port;
import org.jd.infestusfrontier.logistics.VascularJunctionBlock;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassFluids;

/** The Filter Valve: a Junction drains a Bladder through it down to the reserve, a stick sets the reserve, filling goes through, and an unloaded store is no store. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class FilterValveTests {
    private static final int Y = 1;
    private static final BlockPos SOURCE = new BlockPos(1, Y + 1, 2);
    private static final BlockPos VALVE = SOURCE.east();
    private static final BlockPos JUNCTION = VALVE.east();
    private static final BlockPos SINK = JUNCTION.east();

    private FilterValveTests() {}

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 300)
    public static void aJunctionDrainsABladderThroughTheValveDownToItsReserve(GameTestHelper h) {
        for (int x = 1; x <= 4; x++) h.setBlock(new BlockPos(x, Y, 2), EcologyTests.mature());
        h.setBlock(SOURCE, StorageModule.BIOMASS_BLADDER.get());
        h.setBlock(SINK, StorageModule.BIOMASS_BLADDER.get());
        fill(h, SOURCE, 3000);
        valve(h, 3); // 1000 mB
        h.setBlock(
                JUNCTION,
                LogisticsModule.VASCULAR_JUNCTION
                        .get()
                        .defaultBlockState()
                        .setValue(VascularJunctionBlock.PORT.get(Direction.WEST), Port.INTAKE)
                        .setValue(VascularJunctionBlock.PORT.get(Direction.EAST), Port.OUTLET));
        h.runAfterDelay(250, () -> {
            h.assertValueEqual(stored(h, SOURCE), 1000, "the source keeps its reserve exactly");
            h.assertValueEqual(stored(h, SINK) + stored(h, JUNCTION), 2000, "the rest went on, no more");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aStickStepsTheReserveAndWraps(GameTestHelper h) {
        h.setBlock(VALVE.below(), EcologyTests.mature());
        valve(h, 0);
        Player setter = EcologyTests.holding(h, GameType.SURVIVAL, Items.STICK, 1);
        EcologyTests.use(h, setter, VALVE, Direction.UP);
        h.assertValueEqual(h.getBlockState(VALVE).getValue(FilterValveBlock.RESERVE), 1, "one step up");
        for (int click = 0; click < 7; click++) EcologyTests.use(h, setter, VALVE, Direction.UP);
        h.assertValueEqual(h.getBlockState(VALVE).getValue(FilterValveBlock.RESERVE), 0, "back to none");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void fillingGoesThroughAndTheStoreSideOffersNothing(GameTestHelper h) {
        h.setBlock(SOURCE.below(), EcologyTests.mature());
        h.setBlock(VALVE.below(), EcologyTests.mature());
        h.setBlock(SOURCE, StorageModule.BIOMASS_BLADDER.get());
        valve(h, 7);
        IFluidHandler through =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(VALVE), Direction.EAST);
        h.assertValueEqual(
                through.fill(BiomassFluids.of(Biomass.of(500)), IFluidHandler.FluidAction.EXECUTE), 500, "filled");
        h.assertValueEqual(stored(h, SOURCE), 500, "into the store");
        h.assertTrue(through.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "500 is under a 16000 reserve");
        h.assertTrue(
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(VALVE), Direction.WEST)
                        == null,
                "the side facing the store has no handler");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "valve_boundary")
    public static void aValveAtAChunkEdgeNeverLoadsItsStoresChunk(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(BlockPos.ZERO);
        BlockPos edge = new BlockPos((origin.getX() & ~15) + 224015, 80, (origin.getZ() & ~15) + 224008);
        level.getChunkAt(edge);
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(edge.below(), Blocks.STONE.defaultBlockState(), quiet);
        level.setBlock(
                edge,
                LogisticsModule.FILTER_VALVE
                        .get()
                        .defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST),
                quiet);
        h.assertFalse(level.hasChunkAt(edge.east()), "the store's chunk starts unloaded");
        IFluidHandler valve = level.getCapability(Capabilities.FluidHandler.BLOCK, edge, Direction.WEST);
        h.assertTrue(valve.drain(250, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "nothing drains");
        h.assertValueEqual(valve.getTanks(), 0, "no tanks");
        h.assertFalse(level.hasChunkAt(edge.east()), "and nothing loaded the chunk");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aRingOfValvesAnswersNothingAndNeverLoops(GameTestHelper h) {
        BlockPos a = new BlockPos(1, Y + 1, 1);
        BlockPos b = a.east();
        BlockPos c = b.south();
        BlockPos d = a.south();
        Direction[] faces = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
        BlockPos[] ring = {a, b, c, d};
        for (int i = 0; i < 4; i++) {
            h.setBlock(ring[i].below(), EcologyTests.mature());
            h.setBlock(
                    ring[i],
                    LogisticsModule.FILTER_VALVE
                            .get()
                            .defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, faces[i]));
        }
        IFluidHandler handler =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(a), Direction.WEST);
        h.assertValueEqual(handler.getTanks(), 0, "a valve facing a valve has no store");
        h.assertTrue(handler.drain(250, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "and drains nothing");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aRingOfValvesAndCrossingsAnswersNothingAndNeverLoops(GameTestHelper h) {
        // valves at the corners of a 3 by 3 ring, each facing a biomass crossing whose passage leads to the next valve
        Direction[] faces = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
        BlockPos[] valves = {
            new BlockPos(1, Y + 1, 1), new BlockPos(3, Y + 1, 1), new BlockPos(3, Y + 1, 3), new BlockPos(1, Y + 1, 3)
        };
        BlockState crossing = LogisticsModule.SEPTUM_CROSSING.get().defaultBlockState();
        for (int i = 0; i < 4; i++) {
            BlockPos between = valves[i].relative(faces[i]);
            h.setBlock(valves[i].below(), EcologyTests.mature());
            h.setBlock(between.below(), EcologyTests.mature());
            h.setBlock(
                    valves[i],
                    LogisticsModule.FILTER_VALVE
                            .get()
                            .defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, faces[i]));
            h.setBlock(between, crossing);
        }
        IFluidHandler handler =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(valves[0]), Direction.WEST);
        h.assertValueEqual(
                handler.fill(BiomassFluids.of(Biomass.of(250)), IFluidHandler.FluidAction.SIMULATE),
                0,
                "a valve facing a crossing has no store");
        h.assertValueEqual(handler.getTanks(), 0, "and no tanks");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBucketDrainsThroughTheValveAndStopsAtTheReserve(GameTestHelper h) {
        h.setBlock(SOURCE.below(), EcologyTests.mature());
        h.setBlock(VALVE.below(), EcologyTests.mature());
        h.setBlock(SOURCE, StorageModule.BIOMASS_BLADDER.get());
        fill(h, SOURCE, 2500);
        valve(h, 3); // 1000 mB
        Player drawer = EcologyTests.holding(h, GameType.SURVIVAL, Items.BUCKET, 1);
        EcologyTests.use(h, drawer, VALVE, Direction.EAST);
        h.assertTrue(drawer.getMainHandItem().is(StorageModule.BIOMASS_BUCKET.get()), "the first bucket filled");
        h.assertValueEqual(stored(h, SOURCE), 1500, "from the store");
        Player second = EcologyTests.holding(h, GameType.SURVIVAL, Items.BUCKET, 1);
        EcologyTests.use(h, second, VALVE, Direction.EAST);
        h.assertTrue(second.getMainHandItem().is(Items.BUCKET), "a second would cross the reserve, so it stays empty");
        h.assertValueEqual(stored(h, SOURCE), 1500, "the store keeps more than its reserve");
        h.succeed();
    }

    private static void valve(GameTestHelper h, int reserve) {
        BlockState state = LogisticsModule.FILTER_VALVE
                .get()
                .defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST)
                .setValue(FilterValveBlock.RESERVE, reserve);
        h.setBlock(VALVE, state);
    }

    private static void fill(GameTestHelper h, BlockPos tank, int mb) {
        IFluidHandler handler =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(tank), Direction.UP);
        h.assertValueEqual(
                handler.fill(BiomassFluids.of(Biomass.of(mb)), IFluidHandler.FluidAction.EXECUTE), mb, "filled " + mb);
    }

    private static int stored(GameTestHelper h, BlockPos tank) {
        return LogisticsTests.biomassIn(h, tank);
    }
}
