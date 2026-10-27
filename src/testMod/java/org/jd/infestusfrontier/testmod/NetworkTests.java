package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassFluids;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class NetworkTests {
    private NetworkTests() {}

    @GameTest(template = "empty", batch = "network_budget")
    public static void allCarriersShareAdmissionAndSimulationReservesItsExecution(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos vein = h.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(vein, LogisticsModule.BIOMASS_VEIN.get().defaultBlockState());
        level.setBlockAndUpdate(vein.east(), StorageModule.BIOMASS_BLADDER.get().defaultBlockState());
        IFluidHandler port = level.getCapability(Capabilities.FluidHandler.BLOCK, vein, Direction.WEST);
        FluidStack fluid = BiomassFluids.of(Biomass.of(250));
        h.assertValueEqual(port.fill(fluid, IFluidHandler.FluidAction.SIMULATE), 250, "admitted simulated offer");
        int remaining = 0;
        while (remaining < 65 && port.fill(fluid, IFluidHandler.FluidAction.SIMULATE) > 0) remaining++;
        h.assertValueEqual(remaining, 63, "each simulation on the retained handler spends admission");
        h.assertValueEqual(
                level.getCapability(Capabilities.FluidHandler.BLOCK, vein, Direction.WEST)
                        .fill(fluid, IFluidHandler.FluidAction.SIMULATE),
                0,
                "a new handler shares the exhausted admission budget");
        h.assertValueEqual(
                port.fill(fluid, IFluidHandler.FluidAction.EXECUTE),
                250,
                "execution honors admission after quota is full");
        h.assertValueEqual(
                port.fill(fluid, IFluidHandler.FluidAction.EXECUTE), 0, "no second execution on the same admission");
        IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, vein.east(), Direction.WEST);
        h.assertValueEqual(tank.getFluidInTank(0).getAmount(), 250, "exactly one transfer");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "network_ring")
    public static void aLiquidRingHasNoStorageAndTransfersDirectlyToALoadedEndpoint(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(new BlockPos(2, 2, 2));
        for (BlockPos offset :
                java.util.List.of(BlockPos.ZERO, new BlockPos(1, 0, 0), new BlockPos(1, 0, 1), new BlockPos(0, 0, 1))) {
            BlockPos cell = origin.offset(offset);
            level.setBlockAndUpdate(cell, LogisticsModule.BIOMASS_VEIN.get().defaultBlockState());
            h.assertTrue(level.getBlockEntity(cell) == null, "passive cell");
        }
        IFluidHandler port = level.getCapability(Capabilities.FluidHandler.BLOCK, origin, Direction.WEST);
        FluidStack fluid = BiomassFluids.of(Biomass.of(250));
        h.assertValueEqual(port.fill(fluid, IFluidHandler.FluidAction.EXECUTE), 0, "sinkless ring refuses");
        h.assertValueEqual(port.getTanks(), 0, "route owns no tank");
        level.setBlockAndUpdate(
                origin.south().above(), StorageModule.BIOMASS_BLADDER.get().defaultBlockState());
        h.assertValueEqual(port.fill(fluid, IFluidHandler.FluidAction.EXECUTE), 250, "new endpoint becomes reachable");
        h.assertValueEqual(
                port.drain(fluid, IFluidHandler.FluidAction.EXECUTE).getAmount(), 250, "pull uses the same graph");
        IFluidHandler tank = level.getCapability(
                Capabilities.FluidHandler.BLOCK, origin.south().above(), Direction.DOWN);
        h.assertTrue(tank.getFluidInTank(0).isEmpty(), "all contents were returned");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "network_chunks", timeoutTicks = 1200)
    public static void knownUnloadedConnectionsWorkButChangedReloadedConnectionsDoNot(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos start = new BlockPos(320002, 80, 320008);
        BlockPos end = start.east(128);
        int first = new ChunkPos(start).x, last = new ChunkPos(end).x, z = new ChunkPos(start).z;
        for (int x = first; x <= last; x++) level.setChunkForced(x, z, true);
        for (int x = 0; x <= 128; x++)
            level.setBlockAndUpdate(
                    start.east(x), LogisticsModule.BIOMASS_VEIN.get().defaultBlockState());
        level.setBlockAndUpdate(end.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(end.above(), StorageModule.BIOMASS_BLADDER.get().defaultBlockState());
        IFluidHandler port = level.getCapability(Capabilities.FluidHandler.BLOCK, start, Direction.WEST);
        FluidStack fluid = BiomassFluids.of(Biomass.of(250));
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(
                        port.fill(fluid, IFluidHandler.FluidAction.SIMULATE), 250, "network discovered"))
                .thenExecute(() -> {
                    h.assertValueEqual(port.fill(fluid, IFluidHandler.FluidAction.EXECUTE), 250, "first transfer");
                    level.getDataStorage().save();
                    net.neoforged.neoforge.common.IOUtilities.waitUntilIOWorkerComplete();
                    for (int x = first + 1; x < last; x++) level.setChunkForced(x, z, false);
                })
                .thenWaitUntil(() -> h.assertFalse(level.hasChunkAt(start.east(64)), "middle unloaded"))
                .thenExecute(() -> {
                    var registries = level.registryAccess();
                    var factory = new net.minecraft.world.level.saveddata.SavedData.Factory<>(
                            () -> LogisticsModule.restoreNetworks(new net.minecraft.nbt.CompoundTag(), registries),
                            LogisticsModule::restoreNetworks);
                    var disk = new net.minecraft.world.level.storage.DimensionDataStorage(
                            level.getServer()
                                    .getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                                    .resolve("data")
                                    .toFile(),
                            level.getServer().getFixerUpper(),
                            registries);
                    var restored = disk.get(factory, "infestusfrontier_networks");
                    h.assertTrue(restored != null, "topology was read from the saved dimension file");
                    h.assertTrue(
                            restored != level.getDataStorage().get(factory, "infestusfrontier_networks"),
                            "restored topology has no in-memory graph from before the save");
                    level.getDataStorage().set("infestusfrontier_networks", restored);
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        port.fill(fluid, IFluidHandler.FluidAction.SIMULATE),
                        250,
                        "disk-restored connections rebuild across the unloaded middle"))
                .thenExecute(() -> {
                    h.assertValueEqual(
                            port.fill(fluid, IFluidHandler.FluidAction.EXECUTE),
                            250,
                            "known connection works while middle is unloaded");
                    h.assertFalse(level.hasChunkAt(start.east(64)), "transfer did not load the middle");
                    level.setChunkForced(first + 4, z, true);
                })
                .thenWaitUntil(() -> h.assertTrue(level.hasChunkAt(start.east(64)), "middle loaded"))
                .thenExecute(() -> level.setBlockAndUpdate(start.east(64), Blocks.AIR.defaultBlockState()))
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertValueEqual(
                            port.fill(fluid, IFluidHandler.FluidAction.EXECUTE),
                            0,
                            "broken route cannot use stale connectivity");
                    IFluidHandler tank =
                            level.getCapability(Capabilities.FluidHandler.BLOCK, end.above(), Direction.DOWN);
                    h.assertValueEqual(tank.getFluidInTank(0).getAmount(), 500, "no duplication or loss");
                    for (int x = first; x <= last; x++) level.setChunkForced(x, z, false);
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", batch = "network_persistence")
    public static void connectionKnowledgeRoundTripsAndCorruptionKeepsTheEntireSave(GameTestHelper h) {
        var entries = new net.minecraft.nbt.ListTag();
        var entry = new net.minecraft.nbt.CompoundTag();
        entry.putIntArray("pos", new int[] {16, 80, 0});
        var tissue = new net.minecraft.nbt.CompoundTag();
        tissue.putString("carrier", "BIOMASS");
        tissue.putIntArray("passages", new int[] {63});
        entry.put("tissue", tissue);
        entries.add(entry);
        var saved = new net.minecraft.nbt.CompoundTag();
        saved.put("connections", entries);
        var registries = h.getLevel().registryAccess();
        var restored = LogisticsModule.restoreNetworks(saved, registries);
        h.assertValueEqual(
                restored.save(new net.minecraft.nbt.CompoundTag(), registries), saved, "valid topology survives");
        var bad = entry.copy();
        bad.getCompound("tissue").putString("carrier", "UNKNOWN");
        entries.add(bad);
        restored = LogisticsModule.restoreNetworks(saved, registries);
        h.assertValueEqual(
                restored.save(new net.minecraft.nbt.CompoundTag(), registries),
                saved,
                "corrupt list preserved whole, not partially accepted");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "network_stale_offer")
    public static void repeatedSimulationReadsCurrentCapacity(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos vein = h.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(vein, LogisticsModule.BIOMASS_VEIN.get().defaultBlockState());
        level.setBlockAndUpdate(vein.east(), StorageModule.BIOMASS_BLADDER.get().defaultBlockState());
        IFluidHandler port = level.getCapability(Capabilities.FluidHandler.BLOCK, vein, Direction.WEST);
        IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, vein.east(), Direction.WEST);
        FluidStack fluid = BiomassFluids.of(Biomass.of(250));
        h.assertValueEqual(port.fill(fluid, IFluidHandler.FluidAction.SIMULATE), 250, "first offer");
        tank.fill(BiomassFluids.of(Biomass.of(4000)), IFluidHandler.FluidAction.EXECUTE);
        h.assertValueEqual(
                port.fill(fluid, IFluidHandler.FluidAction.SIMULATE),
                0,
                "full endpoint invalidates the previous capacity");
        h.assertValueEqual(tank.getFluidInTank(0).getAmount(), 4000, "simulation never changes contents");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "network_dense", timeoutTicks = 400)
    public static void fourThousandPassiveCellsServeFourNetworksAndRefuseAnOversizedOne(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos base = new BlockPos(400000, 80, 400000);
        int chunkX = new ChunkPos(base).x, chunkZ = new ChunkPos(base).z;
        for (int x = 0; x < 10; x++) for (int z = 0; z < 2; z++) level.setChunkForced(chunkX + x, chunkZ + z, true);
        IFluidHandler[] ports = new IFluidHandler[4];
        for (int group = 0; group < 4; group++) {
            BlockPos root = base.east(group * 40);
            level.setBlockAndUpdate(root.west(), Blocks.AIR.defaultBlockState());
            for (int x = 0; x < 32; x++)
                for (int z = 0; z < 32; z++)
                    level.setBlockAndUpdate(
                            root.offset(x, 0, z),
                            LogisticsModule.BIOMASS_VEIN.get().defaultBlockState());
            level.setBlockAndUpdate(root.above(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(
                    root.above(), StorageModule.BIOMASS_BLADDER.get().defaultBlockState());
            ports[group] = level.getCapability(Capabilities.FluidHandler.BLOCK, root, Direction.WEST);
        }
        FluidStack fluid = BiomassFluids.of(Biomass.of(250));
        boolean[] filled = new boolean[4];
        long[] nanos = {0};
        h.startSequence()
                .thenWaitUntil(() -> {
                    long started = System.nanoTime();
                    for (int group = 0; group < 4; group++)
                        if (!filled[group])
                            filled[group] = ports[group].fill(fluid, IFluidHandler.FluidAction.EXECUTE) == 250;
                    nanos[0] += System.nanoTime() - started;
                    for (boolean done : filled)
                        h.assertTrue(done, "all four networks become available within bounded discovery");
                })
                .thenExecute(() -> {
                    System.out.println(
                            "4096 passive cells, cumulative endpoint request time: " + nanos[0] / 1_000_000.0 + " ms");
                    for (int group = 0; group < 4; group++) {
                        var status = level.getCapability(
                                org.jd.infestusfrontier.logistics.api.LogisticsCapabilities.NETWORK_STATUS,
                                base.east(group * 40),
                                Direction.WEST);
                        h.assertValueEqual(status.cells(), 1024, "complete component size");
                    }
                    level.setBlockAndUpdate(
                            base.west(), LogisticsModule.BIOMASS_VEIN.get().defaultBlockState());
                })
                .thenWaitUntil(() -> {
                    var status = level.getCapability(
                            org.jd.infestusfrontier.logistics.api.LogisticsCapabilities.NETWORK_STATUS,
                            base,
                            Direction.WEST);
                    h.assertValueEqual(status.state(), "too_large", "oversized graph explicitly refuses");
                })
                .thenExecute(() -> {
                    h.assertValueEqual(
                            ports[0].fill(fluid, IFluidHandler.FluidAction.EXECUTE), 0, "no partial network transfer");
                    IFluidHandler tank =
                            level.getCapability(Capabilities.FluidHandler.BLOCK, base.above(), Direction.DOWN);
                    h.assertValueEqual(tank.getFluidInTank(0).getAmount(), 250, "held contents survive refusal");
                    for (int x = 0; x < 10; x++)
                        for (int z = 0; z < 2; z++) level.setChunkForced(chunkX + x, chunkZ + z, false);
                })
                .thenSucceed();
    }
}
