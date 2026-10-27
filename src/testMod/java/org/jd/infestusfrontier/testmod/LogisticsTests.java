package org.jd.infestusfrontier.testmod;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
import org.jd.infestusfrontier.logistics.Port;
import org.jd.infestusfrontier.logistics.VascularJunctionBlock;
import org.jd.infestusfrontier.logistics.VeinBlock;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassFluids;

/** Veins and the Vascular Junction: grafting, pumping, pushing on, holding back, breaking, and the probe on a port. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class LogisticsTests {
    private static final int Y = 1;

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void capillaryGelGrowsAVeinOnlyFromExposedMatureSubstrate(GameTestHelper h) {
        BlockPos basic = new BlockPos(1, Y, 1);
        BlockPos mature = new BlockPos(3, Y, 1);
        BlockPos covered = new BlockPos(5, Y, 1);
        h.setBlock(basic, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(mature, EcologyTests.mature());
        h.setBlock(covered, EcologyTests.mature());
        h.setBlock(covered.above(), Blocks.STONE);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, LogisticsModule.CAPILLARY_GEL.get(), 3);
        EcologyTests.use(h, player, basic, Direction.UP);
        EcologyTests.use(h, player, covered, Direction.NORTH);
        h.assertBlockPresent(EcologyModule.LIVING_SUBSTRATE.get(), basic);
        h.assertBlockPresent(EcologyModule.LIVING_SUBSTRATE.get(), covered);
        h.assertValueEqual(player.getMainHandItem().getCount(), 3, "nothing spent on a basic or a covered cell");
        EcologyTests.use(h, player, mature, Direction.UP);
        h.assertBlockPresent(LogisticsModule.BIOMASS_VEIN.get(), mature);
        h.assertValueEqual(player.getMainHandItem().getCount(), 2, "one gel spent");
        h.setBlock(mature.east(), LogisticsModule.BIOMASS_VEIN.get());
        h.assertTrue(
                h.getBlockState(mature).getValue(PipeBlock.EAST)
                        && h.getBlockState(mature.east()).getValue(PipeBlock.WEST),
                "the two cells connect in the block state");
        h.assertFalse(h.getBlockState(mature).getValue(PipeBlock.WEST), "no vein to the west");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aJunctionPumpsABladderThroughVeinsIntoALoomAndOnToABladder(GameTestHelper h) {
        // ground row at y=1 from x=1 to x=6; the source bladder at x=1, the junction at x=2, veins under x=2..5, the
        // loom on x=4, a tank on x=5
        for (int x = 1; x <= 6; x++) h.setBlock(new BlockPos(x, Y, 1), EcologyTests.mature());
        BlockPos source = new BlockPos(1, Y + 1, 1);
        BlockPos junction = new BlockPos(2, Y + 1, 1);
        BlockPos loom = new BlockPos(4, Y + 1, 1);
        BlockPos sink = new BlockPos(5, Y + 1, 1);
        h.setBlock(source, StorageModule.BIOMASS_BLADDER.get());
        fill(h, source, 3000);
        h.setBlock(
                junction,
                LogisticsModule.VASCULAR_JUNCTION
                        .get()
                        .defaultBlockState()
                        .setValue(VascularJunctionBlock.PORT.get(Direction.WEST), Port.INTAKE)
                        .setValue(VascularJunctionBlock.PORT.get(Direction.DOWN), Port.OUTLET));
        for (int x = 2; x <= 5; x++) h.setBlock(new BlockPos(x, Y, 1), LogisticsModule.BIOMASS_VEIN.get());
        h.setBlock(loom, ProcessingModule.BONE_LOOM.get());
        h.setBlock(sink, StorageModule.BIOMASS_BLADDER.get());
        h.runAfterDelay(150, () -> {
            int loomHas = ProcessingTests.handler(h, loom).tank().stored().units();
            int sinkHas = ProcessingTests.handler(h, sink).tank().stored().units();
            int sourceHas = ProcessingTests.handler(h, source).tank().stored().units();
            h.assertValueEqual(loomHas, 1000, "the loom on the vein filled first, to its capacity");
            h.assertTrue(sinkHas > 0, "the rest flows on to the tank at the end");
            int inTransit = 0;
            for (int x = 2; x <= 5; x++) inTransit += biomassIn(h, new BlockPos(x, Y, 1));
            inTransit += biomassIn(h, junction);
            h.assertValueEqual(sourceHas + loomHas + sinkHas + inTransit, 3000, "every BU is somewhere");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aFullDestinationRefusesAndThePassiveNetworkResumesWhenRoomReturns(GameTestHelper h) {
        BlockPos cell = new BlockPos(1, Y, 1), next = cell.east(), tank = next.above();
        h.setBlock(cell, LogisticsModule.BIOMASS_VEIN.get());
        h.setBlock(next, LogisticsModule.BIOMASS_VEIN.get());
        h.setBlock(tank, StorageModule.BIOMASS_BLADDER.get());
        fill(h, tank, 4000);
        IFluidHandler port =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(cell), Direction.UP);
        h.assertValueEqual(
                port.fill(BiomassFluids.of(Biomass.of(250)), IFluidHandler.FluidAction.EXECUTE),
                0,
                "full destination refuses");
        h.assertTrue(
                h.getLevel().getBlockEntity(h.absolutePos(cell)) == null
                        && h.getLevel().getBlockEntity(h.absolutePos(next)) == null,
                "passive cells have no storage or block entity");
        IFluidHandler out =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(tank), Direction.UP);
        out.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        h.assertValueEqual(
                port.fill(BiomassFluids.of(Biomass.of(250)), IFluidHandler.FluidAction.SIMULATE),
                250,
                "room is visible immediately");
        h.assertValueEqual(biomassIn(h, tank), 3000, "simulation changes nothing");
        h.assertValueEqual(
                port.fill(BiomassFluids.of(Biomass.of(250)), IFluidHandler.FluidAction.EXECUTE),
                250,
                "direct transfer");
        h.assertValueEqual(biomassIn(h, tank), 3250, "destination owns every transferred unit");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aForkServesItsArmsInTurn(GameTestHelper h) {
        // transfers enter the fork from the west; one arm leads north, one south, each to a tank on its last cell
        BlockPos fork = new BlockPos(2, Y, 2);
        BlockPos north = fork.north();
        BlockPos south = fork.south();
        for (BlockPos cell : List.of(fork, north, south)) h.setBlock(cell, LogisticsModule.BIOMASS_VEIN.get());
        h.setBlock(north.above(), StorageModule.BIOMASS_BLADDER.get());
        h.setBlock(south.above(), StorageModule.BIOMASS_BLADDER.get());
        IFluidHandler entry =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(fork), Direction.WEST);
        for (int transfer = 0; transfer < 4; transfer++)
            h.runAtTickTime(
                    1 + transfer * 12,
                    () -> h.assertValueEqual(
                            entry.fill(BiomassFluids.of(Biomass.of(250)), IFluidHandler.FluidAction.EXECUTE),
                            250,
                            "the fork took the transfer"));
        h.runAtTickTime(80, () -> {
            h.assertValueEqual(
                    ProcessingTests.handler(h, north.above()).tank().stored().units(), 500, "north half");
            h.assertValueEqual(
                    ProcessingTests.handler(h, south.above()).tank().stored().units(), 500, "south half");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aVeinCarriesBiomassUpAWallOverAndDownAgain(GameTestHelper h) {
        // up three cells at x=1, across the top, down three cells at x=3, into a tank under the last cell; the gap at
        // x=2 below the top leaves the route as the only way
        List<BlockPos> route = List.of(
                new BlockPos(1, Y, 1),
                new BlockPos(1, Y + 1, 1),
                new BlockPos(1, Y + 2, 1),
                new BlockPos(2, Y + 2, 1),
                new BlockPos(3, Y + 2, 1),
                new BlockPos(3, Y + 1, 1),
                new BlockPos(3, Y, 1));
        for (BlockPos cell : route) h.setBlock(cell, LogisticsModule.BIOMASS_VEIN.get());
        BlockPos tank = new BlockPos(3, Y - 1, 1);
        h.setBlock(tank, StorageModule.BIOMASS_BLADDER.get());
        h.assertTrue(
                h.getBlockState(route.get(1)).getValue(PipeBlock.UP)
                        && h.getBlockState(route.get(1)).getValue(PipeBlock.DOWN),
                "a cell in the climb links up and down");
        IFluidHandler entry = h.getLevel()
                .getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(route.getFirst()), Direction.WEST);
        h.assertValueEqual(
                entry.fill(BiomassFluids.of(Biomass.of(250)), IFluidHandler.FluidAction.EXECUTE),
                250,
                "one transfer in");
        h.runAtTickTime(60, () -> {
            h.assertValueEqual(ProcessingTests.handler(h, tank).tank().stored().units(), 250, "the transfer came down");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anItemVeinIsAPassageThatHoldsNothing(GameTestHelper h) {
        BlockPos cell = new BlockPos(1, Y, 1);
        BlockPos hopper = cell.above();
        h.setBlock(cell, LogisticsModule.ITEM_VEIN.get());
        h.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        ((HopperBlockEntity) h.getBlockEntity(hopper)).setItem(0, new ItemStack(Items.BONE, 20));
        h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(cell)) == null, "no block entity");
        h.assertTrue(
                h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(cell), Direction.UP) == null,
                "nothing to insert into");
        h.runAfterDelay(40, () -> {
            h.assertValueEqual(
                    ((HopperBlockEntity) h.getBlockEntity(hopper)).countItem(Items.BONE), 20, "the hopper keeps all");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aSealedFluidVeinCarriesWaterFromASourceToABowlAndRefusesBiomass(GameTestHelper h) {
        BlockPos water = new BlockPos(1, Y + 1, 1);
        BlockPos junctionGround = new BlockPos(2, Y, 1);
        BlockPos junction = junctionGround.above();
        BlockPos vein = new BlockPos(3, Y, 1);
        BlockPos bowlGround = new BlockPos(4, Y, 1);
        BlockPos bowl = bowlGround.above();
        h.setBlock(water.below(), Blocks.STONE);
        h.setBlock(water, Blocks.WATER);
        BlockState sealedVein =
                LogisticsModule.FLUID_VEIN.get().defaultBlockState().setValue(VeinBlock.SEALED, true);
        h.setBlock(junctionGround, sealedVein);
        h.setBlock(vein, EcologyTests.mature());
        h.setBlock(bowlGround, sealedVein);
        h.setBlock(bowl, ProcessingModule.CULTURE_BOWL.get());
        Player grafter = EcologyTests.holding(h, GameType.SURVIVAL, LogisticsModule.FILTER_MEMBRANE.get(), 1);
        EcologyTests.use(h, grafter, vein, Direction.UP);
        h.assertBlockPresent(LogisticsModule.FLUID_VEIN.get(), vein);
        h.assertFalse(h.getBlockState(vein).getValue(VeinBlock.SEALED), "unsealed at first");
        IFluidHandler unsealed =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(vein), Direction.UP);
        h.assertTrue(unsealed == null, "an unsealed vein exposes no network access");
        Player sealer = EcologyTests.holding(h, GameType.SURVIVAL, LogisticsModule.SEALING_RESIN.get(), 1);
        EcologyTests.use(h, sealer, vein, Direction.UP);
        h.assertTrue(h.getBlockState(vein).getValue(VeinBlock.SEALED), "sealed");
        h.assertTrue(sealer.getMainHandItem().isEmpty(), "the resin is spent");
        Player sealAgain = EcologyTests.holding(h, GameType.SURVIVAL, LogisticsModule.SEALING_RESIN.get(), 1);
        EcologyTests.use(h, sealAgain, vein, Direction.UP);
        h.assertValueEqual(sealAgain.getMainHandItem().getCount(), 1, "a sealed vein takes no second resin");
        IFluidHandler sealed =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(vein), Direction.UP);
        h.assertValueEqual(
                sealed.fill(BiomassFluids.of(Biomass.of(100)), IFluidHandler.FluidAction.SIMULATE), 0, "never biomass");
        h.assertValueEqual(
                sealed.fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.SIMULATE), 0, "never lava");
        h.setBlock(
                junction,
                LogisticsModule.VASCULAR_JUNCTION
                        .get()
                        .defaultBlockState()
                        .setValue(VascularJunctionBlock.PORT.get(Direction.WEST), Port.INTAKE)
                        .setValue(VascularJunctionBlock.PORT.get(Direction.DOWN), Port.OUTLET));
        h.runAfterDelay(120, () -> {
            IFluidHandler bowlTanks =
                    h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(bowl), Direction.UP);
            int bowlWater = 0;
            for (int tank = 0; tank < bowlTanks.getTanks(); tank++)
                if (bowlTanks.getFluidInTank(tank).is(Fluids.WATER))
                    bowlWater += bowlTanks.getFluidInTank(tank).getAmount();
            h.assertTrue(bowlWater >= 1000, "the bowl got at least the one bucket the source gave: " + bowlWater);
            h.assertBlockPresent(Blocks.AIR, water);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBrokenPassiveCellLeavesDeadTissue(GameTestHelper h) {
        BlockPos cell = new BlockPos(1, Y, 1);
        h.setBlock(cell, LogisticsModule.BIOMASS_VEIN.get());
        h.getLevel()
                .getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(cell), Direction.UP)
                .fill(BiomassFluids.of(Biomass.of(200)), IFluidHandler.FluidAction.EXECUTE);
        h.getLevel().destroyBlock(h.absolutePos(cell), true);
        h.runAfterDelay(2, () -> {
            List<ItemEntity> drops = h.getEntities(EntityType.ITEM, cell, 2.0);
            h.assertValueEqual(drops.size(), 1, "one drop");
            h.assertTrue(drops.getFirst().getItem().is(EcologyModule.DEAD_TISSUE.get()), "Dead Tissue, not the cell");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCreativeBreakLeavesNothing(GameTestHelper h) {
        BlockPos cell = new BlockPos(1, Y, 1);
        h.setBlock(cell, LogisticsModule.BIOMASS_VEIN.get());
        h.getLevel()
                .getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(cell), Direction.UP)
                .fill(BiomassFluids.of(Biomass.of(200)), IFluidHandler.FluidAction.EXECUTE);
        Player creative = EcologyTests.holding(h, GameType.CREATIVE, Items.AIR, 0);
        BlockState state = h.getBlockState(cell);
        state.getBlock().playerWillDestroy(h.getLevel(), h.absolutePos(cell), state, creative);
        h.getLevel().removeBlock(h.absolutePos(cell), false);
        h.runAfterDelay(2, () -> {
            h.assertValueEqual(h.getEntities(EntityType.ITEM, cell, 2.0).size(), 0, "a Creative break drops nothing");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theProbeCyclesAJunctionPortAndSneakingReadsIt(GameTestHelper h) {
        BlockPos ground = new BlockPos(1, Y, 1);
        BlockPos junction = ground.above();
        h.setBlock(ground, EcologyTests.mature());
        h.setBlock(junction, LogisticsModule.VASCULAR_JUNCTION.get());
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, InteractionModule.SYNAPTIC_PROBE.get(), 1);
        var north = VascularJunctionBlock.PORT.get(Direction.NORTH);
        h.assertValueEqual(h.getBlockState(junction).getValue(north), Port.CLOSED, "closed at first");
        EcologyTests.use(h, player, junction, Direction.NORTH);
        h.assertValueEqual(h.getBlockState(junction).getValue(north), Port.INTAKE, "then intake");
        EcologyTests.use(h, player, junction, Direction.NORTH);
        h.assertValueEqual(h.getBlockState(junction).getValue(north), Port.OUTLET, "then outlet");
        EcologyTests.use(h, player, junction, Direction.NORTH);
        h.assertValueEqual(h.getBlockState(junction).getValue(north), Port.CLOSED, "then closed again");
        h.assertValueEqual(
                h.getBlockState(junction).getValue(VascularJunctionBlock.PORT.get(Direction.UP)),
                Port.CLOSED,
                "other faces untouched");
        player.setShiftKeyDown(true);
        EcologyTests.use(h, player, junction, Direction.NORTH);
        h.assertValueEqual(h.getBlockState(junction).getValue(north), Port.CLOSED, "sneaking reads, it does not cycle");
        IFluidHandler byHand =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(junction), null);
        h.assertValueEqual(
                byHand.fill(BiomassFluids.of(Biomass.BUCKET), IFluidHandler.FluidAction.EXECUTE),
                1000,
                "a bucket fills the tank by hand");
        IFluidHandler closedFace =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(junction), Direction.NORTH);
        h.assertValueEqual(
                closedFace.drain(250, IFluidHandler.FluidAction.SIMULATE).getAmount(),
                0,
                "a closed face gives nothing");
        h.assertValueEqual(
                byHand.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.SIMULATE),
                0,
                "one fluid at a time");
        h.succeed();
    }

    private static void fill(GameTestHelper h, BlockPos tank, int mb) {
        IFluidHandler handler =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(tank), Direction.UP);
        h.assertValueEqual(
                handler.fill(BiomassFluids.of(Biomass.of(mb)), IFluidHandler.FluidAction.EXECUTE), mb, "filled " + mb);
    }

    static int biomassIn(GameTestHelper h, BlockPos pos) {
        IFluidHandler handler = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), null);
        if (handler == null) return 0;
        int total = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++)
            if (BiomassFluids.isBiomass(handler.getFluidInTank(tank)))
                total += handler.getFluidInTank(tank).getAmount();
        return total;
    }
}
