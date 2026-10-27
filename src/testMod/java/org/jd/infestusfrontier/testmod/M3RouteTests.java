package org.jd.infestusfrontier.testmod;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.cultivation.CultivationModule;
import org.jd.infestusfrontier.logistics.FilterValveBlock;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.MouthBlock;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.logistics.Port;
import org.jd.infestusfrontier.logistics.VascularJunctionBlock;
import org.jd.infestusfrontier.logistics.VeinBlock;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.processing.DigestiveSacBlock;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.slf4j.Logger;

/**
 * The M3 qualifying run: a reference base supplies itself for six cycles. Wheat grows on five beds, a corolla cuts it,
 * a Mouth carries it to the Sac, the Sac fills the Reservoir, and a Junction behind a Filter Valve pumps the biomass
 * back to the farm organs and a Bio-Furnace. Seeds go back to two Seed Pouches, a second Junction waters the beds from
 * a pool. Three cycles run the farm alone; three more add a declared furnace workload of 8 raw iron a cycle.
 *
 * <p>A cycle is at least five harvests of its own, one crop a bed, so even a furnace cycle earns more than its
 * 320 BU: 500 BU of wheat less at most 75 BU of acts. At each cycle's end the test reads every tank in the base and
 * checks the ledger exactly: the start, plus 100 BU a digested wheat, less 5 BU a planting, 5 a harvest and 40 a
 * smelt. Each cycle must end with more biomass than it started with. Crops grow by vanilla random ticks at an
 * accelerated {@code randomTickSpeed} of {@value #RANDOM_TICK_SPEED} (vanilla 3) in chunks the test forces to tick, so
 * the run takes seconds instead of hours.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class M3RouteTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BATCH = "m3_route";
    private static final int RANDOM_TICK_SPEED = 64;
    /** Forces the run's chunks to tick, which random ticks need when no player is near. */
    private static final TicketController TICKETS = new TicketController(
            ResourceLocation.fromNamespaceAndPath("infestusfrontier_tests", BATCH),
            (level, tickets) -> List.copyOf(tickets.getBlockTickets().keySet()).forEach(tickets::removeAllTickets));

    private static final List<Forced> FORCED = new ArrayList<>();

    private static final int BEDS = 5;
    private static final int FARM_CYCLES = 3;
    private static final int FURNACE_CYCLES = 3;
    private static final int IRON_PER_CYCLE = 8;
    private static final int WHEAT_BU = 100;
    private static final int ACT_BU = 5;
    private static final int SMELT_BU = 40;
    private static final int START_BU = 6000;
    private static final int RESTART_RESERVE_BU = 1000;
    private static final int SEEDS_PER_POUCH = 4;

    private static final int G = 1;
    private static final int U = 0;
    private static final int O = 2;

    // The farm: two planters either side of a corolla, a pouch at each end, the corolla's yield lobe south.
    private static final BlockPos POUCH_A = new BlockPos(2, O, 8);
    private static final BlockPos PLANTER_A = new BlockPos(3, O, 8);
    private static final BlockPos COROLLA = new BlockPos(4, O, 8);
    private static final BlockPos PLANTER_B = new BlockPos(5, O, 8);
    private static final BlockPos POUCH_B = new BlockPos(6, O, 8);
    private static final BlockPos LOBE = new BlockPos(4, O, 9);
    private static final List<BlockPos> BEDS_AT = List.of(
            new BlockPos(3, G, 7),
            new BlockPos(4, G, 7),
            new BlockPos(5, G, 7),
            new BlockPos(3, G, 9),
            new BlockPos(5, G, 9));

    // The store: the Sac on its Mouth feeds the Reservoir beside it; a valve keeps the restart reserve.
    private static final BlockPos SAC = new BlockPos(14, O, 10);
    private static final BlockPos RESERVOIR = new BlockPos(14, O, 9);
    private static final BlockPos RESERVOIR_BACK = new BlockPos(14, O, 8);
    private static final BlockPos VALVE = new BlockPos(13, O, 8);
    private static final BlockPos PUMP = new BlockPos(12, O, 8);
    private static final BlockPos FURNACE = new BlockPos(9, O, 8);
    private static final BlockPos HOPPER = FURNACE.above();

    // The water: a pool of three sources, a Junction drinking from its middle.
    private static final BlockPos WATER_PUMP = new BlockPos(2, O, 10);

    private static final List<BlockPos> BIOMASS_VEINS = List.of(
            new BlockPos(12, G, 8),
            new BlockPos(11, G, 8),
            new BlockPos(10, G, 8),
            new BlockPos(9, G, 8),
            new BlockPos(8, G, 8),
            new BlockPos(7, G, 8),
            new BlockPos(7, U, 8),
            new BlockPos(6, U, 8),
            new BlockPos(5, U, 8),
            new BlockPos(5, G, 8),
            new BlockPos(4, G, 8),
            new BlockPos(3, G, 8));
    /** Under the beds; it forks at (2, 0, 9) into the north and the south row. */
    private static final List<BlockPos> FLUID_VEINS = List.of(
            new BlockPos(2, G, 10),
            new BlockPos(2, U, 10),
            new BlockPos(2, U, 9),
            new BlockPos(2, U, 8),
            new BlockPos(2, U, 7),
            new BlockPos(3, U, 7),
            new BlockPos(4, U, 7),
            new BlockPos(5, U, 7),
            new BlockPos(3, U, 9),
            new BlockPos(4, U, 9),
            new BlockPos(5, U, 9));

    private static final List<BlockPos> ITEM_VEINS = itemVeins();

    private M3RouteTests() {}

    /** Called from the test mod's constructor. */
    static void registerTickets(RegisterTicketControllersEvent event) {
        event.register(TICKETS);
    }

    @BeforeBatch(batch = BATCH)
    public static void accelerate(ServerLevel level) {
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(RANDOM_TICK_SPEED, level.getServer());
    }

    @AfterBatch(batch = BATCH)
    public static void restore(ServerLevel level) {
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
        for (Forced forced : FORCED)
            TICKETS.forceChunk(level, forced.owner(), forced.chunk().x, forced.chunk().z, false, true);
        FORCED.clear();
    }

    private record Forced(BlockPos owner, ChunkPos chunk) {}

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 100000)
    public static void m3Route(GameTestHelper h) {
        build(h);
        tick(h);
        Run run = new Run(h);
        GameTestSequence sequence = h.startSequence();
        sequence.thenExecuteAfter(5, run::start);
        for (int k = 1; k <= FARM_CYCLES + FURNACE_CYCLES; k++) {
            int cycle = k;
            sequence.thenWaitUntil(() -> run.awaitCycle(cycle)).thenExecute(() -> run.close(cycle));
        }
        sequence.thenExecute(run::finish).thenSucceed();
    }

    /** Forces every chunk under the test area to tick, owned by the test's origin. */
    private static void tick(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos owner = h.absolutePos(BlockPos.ZERO);
        ChunkPos low = new ChunkPos(h.absolutePos(new BlockPos(0, 0, 0)));
        ChunkPos high = new ChunkPos(h.absolutePos(new BlockPos(15, 0, 15)));
        for (int x = Math.min(low.x, high.x); x <= Math.max(low.x, high.x); x++)
            for (int z = Math.min(low.z, high.z); z <= Math.max(low.z, high.z); z++) {
                TICKETS.forceChunk(level, owner, x, z, true, true);
                FORCED.add(new Forced(owner, new ChunkPos(x, z)));
            }
    }

    // --- the base ---

    private static void build(GameTestHelper h) {
        BlockState sealed = LogisticsModule.FLUID_VEIN.get().defaultBlockState().setValue(VeinBlock.SEALED, true);
        for (BlockPos vein : FLUID_VEINS) h.setBlock(vein, sealed);
        for (BlockPos vein : BIOMASS_VEINS) h.setBlock(vein, LogisticsModule.BIOMASS_VEIN.get());
        for (BlockPos vein : ITEM_VEINS) h.setBlock(vein, LogisticsModule.ITEM_VEIN.get());
        for (BlockPos bed : BEDS_AT) h.setBlock(bed, CultivationModule.CULTIVATION_TISSUE.get());
        h.setBlock(new BlockPos(2, G, 9), EcologyTests.mature());

        h.setBlock(POUCH_A.below(), mouth(MouthMode.INTAKE));
        h.setBlock(POUCH_B.below(), mouth(MouthMode.INTAKE));
        h.setBlock(LOBE.below(), mouth(MouthMode.OUTPUT));
        h.setBlock(SAC.below(), mouth(MouthMode.INTAKE));
        h.setBlock(POUCH_A, CultivationModule.SEED_POUCH.get());
        h.setBlock(POUCH_B, CultivationModule.SEED_POUCH.get());
        h.setBlock(PLANTER_A, CultivationModule.PLANTING_PROBOSCIS.get());
        h.setBlock(COROLLA, CultivationModule.HARVEST_COROLLA.get());
        h.setBlock(PLANTER_B, CultivationModule.PLANTING_PROBOSCIS.get());
        h.setBlock(
                LOBE,
                LogisticsModule.YIELD_LOBE
                        .get()
                        .defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        for (BlockPos pouch : List.of(POUCH_A, POUCH_B))
            items(h, pouch).insertItem(0, new ItemStack(Items.WHEAT_SEEDS, SEEDS_PER_POUCH), false);

        for (BlockPos ground : List.of(RESERVOIR.below(), RESERVOIR_BACK.below(), VALVE.below()))
            h.setBlock(ground, EcologyTests.mature());
        h.setBlock(SAC, ProcessingModule.DIGESTIVE_SAC.get());
        h.setBlock(RESERVOIR, StorageModule.RESERVOIR_CELL.get());
        h.setBlock(RESERVOIR_BACK, StorageModule.RESERVOIR_CELL.get());
        h.setBlock(
                VALVE,
                LogisticsModule.FILTER_VALVE
                        .get()
                        .defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)
                        .setValue(FilterValveBlock.RESERVE, 3));
        h.setBlock(PUMP, junction(Direction.EAST));
        h.setBlock(FURNACE, ProcessingModule.BIO_FURNACE.get());
        h.setBlock(HOPPER, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));

        for (int x = 1; x <= 3; x++) {
            h.setBlock(new BlockPos(x, G, 11), Blocks.STONE);
            h.setBlock(new BlockPos(x, O, 12), Blocks.STONE);
        }
        h.setBlock(new BlockPos(0, O, 11), Blocks.STONE);
        h.setBlock(new BlockPos(4, O, 11), Blocks.STONE);
        h.setBlock(new BlockPos(1, O, 10), Blocks.STONE);
        h.setBlock(new BlockPos(3, O, 10), Blocks.STONE);
        for (int x = 1; x <= 3; x++) h.setBlock(new BlockPos(x, O, 11), Blocks.WATER);
        h.setBlock(WATER_PUMP, junction(Direction.SOUTH));
    }

    /** From the pouches round the beds' north side, and from the lobe east to the Sac. */
    private static List<BlockPos> itemVeins() {
        List<BlockPos> veins = new ArrayList<>();
        veins.add(new BlockPos(2, G, 7));
        for (int x = 2; x <= 6; x++) veins.add(new BlockPos(x, G, 6));
        veins.add(new BlockPos(6, G, 7));
        veins.add(new BlockPos(6, G, 9));
        for (int x = 4; x <= 13; x++) veins.add(new BlockPos(x, G, 10));
        return List.copyOf(veins);
    }

    private static BlockState mouth(MouthMode mode) {
        return LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, mode);
    }

    private static BlockState junction(Direction intake) {
        return LogisticsModule.VASCULAR_JUNCTION
                .get()
                .defaultBlockState()
                .setValue(VascularJunctionBlock.PORT.get(intake), Port.INTAKE)
                .setValue(VascularJunctionBlock.PORT.get(Direction.DOWN), Port.OUTLET);
    }

    private static IItemHandler items(GameTestHelper h, BlockPos pos) {
        IItemHandler handler =
                h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pos), Direction.UP);
        if (handler == null) h.fail("no item handler", pos);
        return handler;
    }

    // --- the run ---

    /** The ledger and the readings of one run. */
    private static final class Run {
        private final GameTestHelper h;
        private int start;
        private int last;
        /** Wheat digested when the last cycle closed; a cycle needs five more of its own. */
        private int harvested;

        Run(GameTestHelper h) {
            this.h = h;
        }

        void start() {
            IFluidHandler reservoir =
                    h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(RESERVOIR), Direction.UP);
            h.assertValueEqual(
                    reservoir.fill(BiomassFluids.of(Biomass.of(START_BU)), IFluidHandler.FluidAction.EXECUTE),
                    START_BU,
                    "the Reservoir takes the starting biomass");
            start = total();
            last = start;
            h.assertValueEqual(start, START_BU, "the base holds only the starting biomass");
            LOGGER.info("INFESTUS_M3_START total={} randomTickSpeed={}", start, RANDOM_TICK_SPEED);
        }

        /** Passes once cycle {@code k} is complete and nothing is on its way; the message says what it waits for. */
        void awaitCycle(int k) {
            int digested = digested();
            int wheat = harvested + BEDS;
            h.assertTrue(digested >= wheat, "cycle " + k + ": " + digested + " of " + wheat + " wheat digested");
            h.assertTrue(quiet(), "cycle " + k + ": wheat still on its way to the Sac");
            if (k > FARM_CYCLES) {
                BatchOrganContents furnace = furnace();
                int due = IRON_PER_CYCLE * (k - FARM_CYCLES);
                h.assertTrue(
                        furnace.history().completedBatches() >= due
                                && furnace.batch().isEmpty(),
                        "cycle " + k + ": " + furnace.history().completedBatches() + " of " + due + " smelts done");
            }
        }

        void close(int k) {
            int total = total();
            int digested = digested();
            int standing = standing();
            int smelts = smelts();
            int expected = start + WHEAT_BU * digested - ACT_BU * (2 * digested + standing) - SMELT_BU * smelts;
            LOGGER.info(
                    "INFESTUS_M3_CYCLE cycle={} tick={} harvests={} standing={} smelts={} total={} surplus={}",
                    k,
                    h.getTick(),
                    digested,
                    standing,
                    smelts,
                    total,
                    total - last);
            h.assertValueEqual(total, expected, "cycle " + k + ": the ledger, nothing spilled or made twice");
            h.assertTrue(
                    total > last, "cycle " + k + " ends with more biomass than it began: " + last + " -> " + total);
            h.assertTrue(
                    reservoir() >= RESTART_RESERVE_BU,
                    "cycle " + k + ": the valve keeps the restart reserve, " + reservoir());
            last = total;
            harvested = digested;
            if (k >= FARM_CYCLES && k < FARM_CYCLES + FURNACE_CYCLES)
                h.assertTrue(
                        items(h, HOPPER)
                                .insertItem(0, new ItemStack(Items.RAW_IRON, IRON_PER_CYCLE), false)
                                .isEmpty(),
                        "the furnace's workload for the next cycle goes into its hopper");
        }

        void finish() {
            for (BlockPos pouch : List.of(POUCH_A, POUCH_B))
                h.assertTrue(
                        items(h, pouch).getStackInSlot(0).getCount() >= 1,
                        "the pouch keeps its seed reserve: " + items(h, pouch).getStackInSlot(0));
            for (BlockPos bed : BEDS_AT) {
                IFluidHandler water =
                        h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(bed), Direction.UP);
                h.assertTrue(water.getFluidInTank(0).is(Fluids.WATER), "every bed is still watered");
            }
            h.assertTrue(
                    furnace().output().is(Items.IRON_INGOT)
                            && furnace().output().getCount() == IRON_PER_CYCLE * FURNACE_CYCLES,
                    "the declared workload was smelted: " + furnace().output());
            LOGGER.info("INFESTUS_M3_DONE start={} end={} gained={}", start, last, last - start);
        }

        /** Every biomass the base holds: Reservoir, Sac, organs and pump. */
        private int total() {
            int sum = reservoir();
            for (BlockPos pos : List.of(SAC, PLANTER_A, COROLLA, PLANTER_B, FURNACE, PUMP)) sum += stored(pos);
            return sum;
        }

        private int reservoir() {
            return stored(RESERVOIR);
        }

        private int stored(BlockPos pos) {
            return ProcessingTests.handler(h, pos).tank().stored().units();
        }

        private int digested() {
            OrganHistory history = h.getBlockEntity(SAC).collectComponents().get(OrganModule.HISTORY.get());
            return history == null ? 0 : history.completedBatches();
        }

        /** No wheat between the beds and the finished batch: so every harvest so far has been digested. */
        private boolean quiet() {
            if (h.getBlockState(SAC).getValue(DigestiveSacBlock.WORKING)) return false;
            if (!items(h, SAC).getStackInSlot(0).isEmpty()) return false;
            IItemHandler corolla = items(h, COROLLA);
            for (int slot = 0; slot < corolla.getSlots(); slot++)
                if (corolla.getStackInSlot(slot).is(Items.WHEAT)) return false;
            return !h.getBlockEntity(LOBE.below())
                    .saveWithoutMetadata(h.getLevel().registryAccess())
                    .contains("remainder");
        }

        private int standing() {
            int crops = 0;
            for (BlockPos bed : BEDS_AT) if (h.getBlockState(bed.above()).is(Blocks.WHEAT)) crops++;
            return crops;
        }

        private int smelts() {
            BatchOrganContents furnace = furnace();
            return furnace.history().completedBatches() + (furnace.batch().isPresent() ? 1 : 0);
        }

        private BatchOrganContents furnace() {
            BatchOrganContents contents =
                    h.getBlockEntity(FURNACE).collectComponents().get(ProcessingModule.BATCH_CONTENTS.get());
            if (contents == null) h.fail("no furnace contents", FURNACE);
            return contents;
        }
    }
}
