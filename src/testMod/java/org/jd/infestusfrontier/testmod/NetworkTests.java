package org.jd.infestusfrontier.testmod;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.IOUtilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.MouthBlock;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.logistics.api.LogisticsCapabilities;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassFluids;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = "infestusfrontier_tests", bus = EventBusSubscriber.Bus.MOD)
public final class NetworkTests {
    private NetworkTests() {}

    @GameTest(template = "empty", batch = "network_budget")
    public static void fluidHandlersShareAdmissionAndSimulationReservesItsExecution(GameTestHelper h) {
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
        level.invalidateCapabilities(vein.east());
        h.assertValueEqual(
                port.fill(fluid, IFluidHandler.FluidAction.EXECUTE),
                250,
                "endpoint capability invalidation preserves admitted execution after quota is full");
        h.assertValueEqual(
                port.fill(fluid, IFluidHandler.FluidAction.EXECUTE), 0, "no second execution on the same admission");
        IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, vein.east(), Direction.WEST);
        h.assertValueEqual(tank.getFluidInTank(0).getAmount(), 250, "exactly one transfer");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "network_mixed_budget")
    public static void fluidAdmissionAlsoBoundsMouthsAndResetsNextTick(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos vein = h.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(vein, LogisticsModule.BIOMASS_VEIN.get().defaultBlockState());
        level.setBlockAndUpdate(vein.east(), StorageModule.BIOMASS_BLADDER.get().defaultBlockState());
        BlockPos sender = h.absolutePos(new BlockPos(2, 1, 6));
        BlockPos receiver = sender.east(2);
        level.setBlockAndUpdate(
                sender, LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, MouthMode.OUTPUT));
        level.setBlockAndUpdate(sender.east(), LogisticsModule.ITEM_VEIN.get().defaultBlockState());
        level.setBlockAndUpdate(receiver, LogisticsModule.MOUTH.get().defaultBlockState());
        level.setBlockAndUpdate(sender.above(), Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(receiver.above(), Blocks.CHEST.defaultBlockState());
        var source = (ChestBlockEntity) level.getBlockEntity(sender.above());
        var destination = (ChestBlockEntity) level.getBlockEntity(receiver.above());
        source.setItem(0, new ItemStack(Items.BONE, 8));
        IFluidHandler port = level.getCapability(Capabilities.FluidHandler.BLOCK, vein, Direction.WEST);
        for (int n = 0; n < 64; n++)
            h.assertValueEqual(
                    port.fill(BiomassFluids.of(Biomass.of(250)), IFluidHandler.FluidAction.SIMULATE),
                    250,
                    "fluid admission");
        level.getBlockState(sender).tick(level, sender, level.random);
        h.assertValueEqual(source.getItem(0).getCount(), 8, "Mouth cannot bypass fluid work admission");
        h.assertTrue(destination.isEmpty(), "refusal leaves destination unchanged");
        h.runAfterDelay(1, () -> {
            level.getBlockState(sender).tick(level, sender, level.random);
            h.assertTrue(source.isEmpty(), "new tick admits the Mouth");
            h.assertValueEqual(destination.getItem(0).getCount(), 8, "exactly one transfer after budget reset");
            h.succeed();
        });
    }

    @SubscribeEvent
    public static void dynamicFluidPort(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.FluidHandler.BLOCK,
                (level, pos, state, entity, side) -> {
                    if (!level.hasChunkAt(pos.above())
                            || !level.hasChunkAt(pos.east())
                            || !level.getBlockState(pos.above()).is(Blocks.REDSTONE_BLOCK)
                            || !level.getBlockState(pos.east()).is(StorageModule.BIOMASS_BLADDER.get())) return null;
                    return level.getCapability(Capabilities.FluidHandler.BLOCK, pos.east(), Direction.WEST);
                },
                Blocks.GOLD_BLOCK);
    }

    @GameTest(template = "empty", batch = "network_dynamic")
    public static void capabilityInvalidationDiscoversAnEndpointWithoutNeighborUpdates(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos vein = h.absolutePos(new BlockPos(2, 2, 2));
        BlockPos endpoint = vein.east();
        level.setBlockAndUpdate(vein, LogisticsModule.BIOMASS_VEIN.get().defaultBlockState());
        level.setBlockAndUpdate(endpoint, Blocks.GOLD_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(
                endpoint.east(), StorageModule.BIOMASS_BLADDER.get().defaultBlockState());
        IFluidHandler port = level.getCapability(Capabilities.FluidHandler.BLOCK, vein, Direction.WEST);
        FluidStack fluid = BiomassFluids.of(Biomass.of(250));
        h.assertValueEqual(port.fill(fluid, IFluidHandler.FluidAction.SIMULATE), 0, "absent endpoint discovered");
        level.setBlock(endpoint.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
        level.invalidateCapabilities(endpoint);
        h.assertValueEqual(
                port.fill(fluid, IFluidHandler.FluidAction.EXECUTE), 250, "new capability becomes reachable");
        level.setBlock(endpoint.above(), Blocks.AIR.defaultBlockState(), 2);
        level.invalidateCapabilities(endpoint);
        h.assertValueEqual(port.fill(fluid, IFluidHandler.FluidAction.EXECUTE), 0, "removed capability refuses");
        IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, endpoint.east(), Direction.WEST);
        h.assertValueEqual(tank.getFluidInTank(0).getAmount(), 250, "only the accepted operation changed storage");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "network_offer_size")
    public static void repeatedSimulationUsesTheNewAmountForFillAndDrain(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos vein = h.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(vein, LogisticsModule.BIOMASS_VEIN.get().defaultBlockState());
        level.setBlockAndUpdate(vein.east(), StorageModule.BIOMASS_BLADDER.get().defaultBlockState());
        IFluidHandler port = level.getCapability(Capabilities.FluidHandler.BLOCK, vein, Direction.WEST);
        IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, vein.east(), Direction.WEST);
        FluidStack fluid = BiomassFluids.of(Biomass.of(250));
        h.assertValueEqual(port.fill(fluid.copyWithAmount(1), IFluidHandler.FluidAction.SIMULATE), 1, "small offer");
        h.assertValueEqual(port.fill(fluid, IFluidHandler.FluidAction.SIMULATE), 250, "larger offer recomputed");
        h.assertTrue(tank.getFluidInTank(0).isEmpty(), "simulation leaves storage empty");
        h.assertValueEqual(port.fill(fluid, IFluidHandler.FluidAction.EXECUTE), 250, "matching fill");
        h.assertValueEqual(port.drain(1, IFluidHandler.FluidAction.SIMULATE).getAmount(), 1, "small drain offer");
        h.assertValueEqual(
                port.drain(250, IFluidHandler.FluidAction.SIMULATE).getAmount(), 250, "larger drain recomputed");
        h.assertValueEqual(tank.getFluidInTank(0).getAmount(), 250, "drain simulation leaves storage unchanged");
        h.assertValueEqual(port.drain(250, IFluidHandler.FluidAction.EXECUTE).getAmount(), 250, "matching drain");
        h.assertTrue(tank.getFluidInTank(0).isEmpty(), "no duplication or loss");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "network_invalid_topology")
    public static void invalidLayoutsAndDuplicatePositionsPreserveTheirSaveAndRefuseUse(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos vein = h.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(vein, LogisticsModule.BIOMASS_VEIN.get().defaultBlockState());
        level.setBlockAndUpdate(vein.east(), StorageModule.BIOMASS_BLADDER.get().defaultBlockState());
        for (int[] masks :
                new int[][] {{1}, {3}, {20}, {7}, {63, 63}, {3, 5}, {63 ^ (1 << Direction.UP.ordinal())}, {63}}) {
            CompoundTag saved = topologyAt(vein, masks);
            ListTag entries = saved.getList("connections", 10);
            if (masks.length == 1 && masks[0] == 63) entries.add(entries.get(0).copy());
            assertRefusedTopology(h, vein, saved);
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "network_valid_topology")
    public static void everyExistingTissueLayoutLoadsWithoutLockingTheNetwork(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos vein = h.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(vein, LogisticsModule.BIOMASS_VEIN.get().defaultBlockState());
        ListTag entries = new ListTag();
        for (String carrier : List.of("BIOMASS", "FLUID", "ITEM")) {
            List<int[]> layouts = new ArrayList<>();
            layouts.add(new int[] {63});
            for (Direction vertical : List.of(Direction.UP, Direction.DOWN))
                for (Direction horizontal : Direction.Plane.HORIZONTAL)
                    layouts.add(new int[] {(1 << vertical.ordinal()) | (1 << horizontal.ordinal())});
            layouts.add(new int[] {3, 12});
            layouts.add(new int[] {3, 48});
            layouts.add(new int[] {12, 48});
            if (carrier.equals("ITEM")) layouts.add(new int[] {63 ^ (1 << Direction.UP.ordinal())});
            for (int[] layout : layouts) {
                CompoundTag entry = new CompoundTag();
                entry.putIntArray("pos", new int[] {2000 + entries.size(), 80, 2000});
                CompoundTag tissue = new CompoundTag();
                tissue.putString("carrier", carrier);
                tissue.putIntArray("passages", layout);
                entry.put("tissue", tissue);
                entries.add(entry);
            }
        }
        CompoundTag saved = new CompoundTag();
        saved.put("connections", entries);
        withTopology(h, saved, restored -> {
            var status = level.getCapability(LogisticsCapabilities.NETWORK_STATUS, vein, Direction.WEST);
            h.assertValueEqual(status.state(), "ready", "all legitimate saved layouts are readable");
            ListTag roundTrip =
                    restored.save(new CompoundTag(), level.registryAccess()).getList("connections", 10);
            for (var entry : entries) h.assertTrue(roundTrip.contains(entry), "each legitimate layout survives");
        });
        h.succeed();
    }

    @GameTest(template = "empty", batch = "network_unknown_schema")
    public static void unknownSavedFieldsArePreservedAndRefused(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos vein = h.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(vein, LogisticsModule.BIOMASS_VEIN.get().defaultBlockState());
        level.setBlockAndUpdate(vein.east(), StorageModule.BIOMASS_BLADDER.get().defaultBlockState());
        for (int location = 0; location < 4; location++) {
            CompoundTag saved = topologyAt(vein, 63);
            CompoundTag entry = saved.getList("connections", 10).getCompound(0);
            CompoundTag target = switch (location) {
                case 1 -> entry;
                case 2 -> entry.getCompound("tissue");
                default -> saved;
            };
            target.putString("future_rule", "must not be ignored");
            if (location == 3) saved.remove("connections");
            h.assertValueEqual(saveAndReload(h, saved), saved, "unknown fields survive actual disk save and reload");
            assertRefusedTopology(h, vein, saved);
        }
        h.succeed();
    }

    private static CompoundTag saveAndReload(GameTestHelper h, CompoundTag saved) {
        try {
            var directory = Files.createTempDirectory("infestus-network-schema-");
            try {
                var registries = h.getLevel().registryAccess();
                var fixer = h.getLevel().getServer().getFixerUpper();
                var factory = new SavedData.Factory<>(
                        () -> LogisticsModule.restoreNetworks(new CompoundTag(), registries),
                        LogisticsModule::restoreNetworks);
                var storage = new DimensionDataStorage(directory.toFile(), fixer, registries);
                var data = LogisticsModule.restoreNetworks(saved, registries);
                data.setDirty();
                storage.set("topology", data);
                storage.save();
                IOUtilities.waitUntilIOWorkerComplete();
                var reopened = new DimensionDataStorage(directory.toFile(), fixer, registries);
                var loaded = reopened.get(factory, "topology");
                h.assertTrue(loaded != null, "topology file exists and loads");
                return loaded.save(new CompoundTag(), registries);
            } finally {
                IOUtilities.waitUntilIOWorkerComplete();
                Files.deleteIfExists(directory.resolve("topology.dat"));
                Files.delete(directory);
            }
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    private static CompoundTag topologyAt(BlockPos pos, int... masks) {
        CompoundTag entry = new CompoundTag();
        entry.putIntArray("pos", new int[] {pos.getX(), pos.getY(), pos.getZ()});
        CompoundTag tissue = new CompoundTag();
        tissue.putString("carrier", "BIOMASS");
        tissue.putIntArray("passages", masks);
        entry.put("tissue", tissue);
        ListTag entries = new ListTag();
        entries.add(entry);
        CompoundTag saved = new CompoundTag();
        saved.put("connections", entries);
        return saved;
    }

    private static void assertRefusedTopology(GameTestHelper h, BlockPos vein, CompoundTag saved) {
        withTopology(h, saved, restored -> {
            h.assertValueEqual(
                    restored.save(new CompoundTag(), h.getLevel().registryAccess()),
                    saved,
                    "the entire unreadable save is preserved");
            var status = h.getLevel().getCapability(LogisticsCapabilities.NETWORK_STATUS, vein, Direction.WEST);
            h.assertValueEqual(status.state(), "unavailable", "unreadable topology reports refusal");
            IFluidHandler port = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, vein, Direction.WEST);
            h.assertValueEqual(
                    port.fill(BiomassFluids.of(Biomass.of(250)), IFluidHandler.FluidAction.EXECUTE),
                    0,
                    "unreadable topology transfers nothing");
            h.assertValueEqual(
                    restored.save(new CompoundTag(), h.getLevel().registryAccess()),
                    saved,
                    "refused operations leave the unreadable save unchanged");
        });
    }

    private static void withTopology(GameTestHelper h, CompoundTag saved, Consumer<SavedData> check) {
        var registries = h.getLevel().registryAccess();
        var storage = h.getLevel().getDataStorage();
        var factory = new SavedData.Factory<>(
                () -> LogisticsModule.restoreNetworks(new CompoundTag(), registries), LogisticsModule::restoreNetworks);
        var original = storage.computeIfAbsent(factory, "infestusfrontier_networks");
        try {
            var restored = LogisticsModule.restoreNetworks(saved, registries);
            storage.set("infestusfrontier_networks", restored);
            check.accept(restored);
        } finally {
            storage.set("infestusfrontier_networks", original);
        }
    }

    @GameTest(template = "empty", batch = "network_ring")
    public static void aLiquidRingHasNoStorageAndTransfersDirectlyToALoadedEndpoint(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos origin = h.absolutePos(new BlockPos(2, 2, 2));
        for (BlockPos offset :
                List.of(BlockPos.ZERO, new BlockPos(1, 0, 0), new BlockPos(1, 0, 1), new BlockPos(0, 0, 1))) {
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
        forceChunks(h, first, last, z, z);
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
                    IOUtilities.waitUntilIOWorkerComplete();
                    for (int x = first + 1; x < last; x++) level.setChunkForced(x, z, false);
                })
                .thenWaitUntil(() -> h.assertFalse(level.hasChunkAt(start.east(64)), "middle unloaded"))
                .thenExecute(() -> {
                    var registries = level.registryAccess();
                    var factory = new SavedData.Factory<>(
                            () -> LogisticsModule.restoreNetworks(new CompoundTag(), registries),
                            LogisticsModule::restoreNetworks);
                    var disk = new DimensionDataStorage(
                            level.getServer()
                                    .getWorldPath(LevelResource.ROOT)
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
                })
                .thenIdle(21)
                .thenExecute(() -> {
                    h.assertValueEqual(
                            port.fill(fluid, IFluidHandler.FluidAction.EXECUTE),
                            250,
                            "unchanged unloaded connections stay ready beyond the retry interval");
                    h.assertFalse(level.hasChunkAt(start.east(64)), "idle network did not load the middle");
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
                    h.assertValueEqual(tank.getFluidInTank(0).getAmount(), 750, "no duplication or loss");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", batch = "network_persistence")
    public static void connectionKnowledgeRoundTripsAndCorruptionKeepsTheEntireSave(GameTestHelper h) {
        var entries = new ListTag();
        var entry = new CompoundTag();
        entry.putIntArray("pos", new int[] {16, 80, 0});
        var tissue = new CompoundTag();
        tissue.putString("carrier", "BIOMASS");
        tissue.putIntArray("passages", new int[] {63});
        entry.put("tissue", tissue);
        entries.add(entry);
        var saved = new CompoundTag();
        saved.put("connections", entries);
        var registries = h.getLevel().registryAccess();
        var restored = LogisticsModule.restoreNetworks(saved, registries);
        h.assertValueEqual(restored.save(new CompoundTag(), registries), saved, "valid topology survives");
        var bad = entry.copy();
        bad.getCompound("tissue").putString("carrier", "UNKNOWN");
        entries.add(bad);
        restored = LogisticsModule.restoreNetworks(saved, registries);
        h.assertValueEqual(
                restored.save(new CompoundTag(), registries),
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
        forceChunks(h, chunkX, chunkX + 9, chunkZ, chunkZ + 1);
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
        h.startSequence()
                .thenWaitUntil(() -> {
                    for (int group = 0; group < 4; group++)
                        if (!filled[group])
                            filled[group] = ports[group].fill(fluid, IFluidHandler.FluidAction.EXECUTE) == 250;
                    for (boolean done : filled)
                        h.assertTrue(done, "all four networks become available within bounded discovery");
                })
                .thenExecute(() -> {
                    for (int group = 0; group < 4; group++) {
                        var status = level.getCapability(
                                LogisticsCapabilities.NETWORK_STATUS, base.east(group * 40), Direction.WEST);
                        h.assertValueEqual(status.cells(), 1024, "complete component size");
                    }
                    level.setBlockAndUpdate(
                            base.west(), LogisticsModule.BIOMASS_VEIN.get().defaultBlockState());
                })
                .thenWaitUntil(() -> {
                    var status = level.getCapability(LogisticsCapabilities.NETWORK_STATUS, base, Direction.WEST);
                    h.assertValueEqual(status.state(), "too_large", "oversized graph explicitly refuses");
                })
                .thenExecute(() -> {
                    h.assertValueEqual(
                            ports[0].fill(fluid, IFluidHandler.FluidAction.EXECUTE), 0, "no partial network transfer");
                    IFluidHandler tank =
                            level.getCapability(Capabilities.FluidHandler.BLOCK, base.above(), Direction.DOWN);
                    h.assertValueEqual(tank.getFluidInTank(0).getAmount(), 250, "held contents survive refusal");
                })
                .thenSucceed();
    }
    /** Tickets owned by this fixture are released on both success and failure. */
    private static void forceChunks(GameTestHelper h, int minX, int maxX, int minZ, int maxZ) {
        h.testInfo.addListener(new GameTestListener() {
            private void release() {
                for (int x = minX; x <= maxX; x++)
                    for (int z = minZ; z <= maxZ; z++) h.getLevel().setChunkForced(x, z, false);
            }

            @Override
            public void testStructureLoaded(GameTestInfo test) {}

            @Override
            public void testPassed(GameTestInfo test, GameTestRunner runner) {
                release();
            }

            @Override
            public void testFailed(GameTestInfo test, GameTestRunner runner) {
                release();
            }

            @Override
            public void testAddedForRerun(GameTestInfo oldTest, GameTestInfo newTest, GameTestRunner runner) {}
        });
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++) h.getLevel().setChunkForced(x, z, true);
    }
}
