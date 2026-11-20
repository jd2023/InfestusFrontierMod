package org.jd.infestusfrontier.testmod;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.power.PowerStore;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.mining.MiningModule;
import org.jd.infestusfrontier.power.PowerModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class PowerTests {
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void existingSealedVeinsDeliverSteamWithoutAnotherMod(GameTestHelper h) {
        PowerRig source = heart(h, PowerModule.RECEIVER.get());
        store(source, new PowerStore(0, 0, 400, 0, 0));
        BlockPos outlet = part(source, PowerModule.OUTLET.get(), Direction.EAST);
        BlockPos pump = outlet.east();
        PowerRig driver = place(h.getLevel(), pump.east(2), PowerModule.DRIVER.get());
        var vein = org.jd.infestusfrontier.logistics.LogisticsModule.FLUID_VEIN
                .get()
                .defaultBlockState()
                .setValue(org.jd.infestusfrontier.logistics.VeinBlock.SEALED, true);
        for (int x = 0; x <= 2; x++) h.getLevel().setBlockAndUpdate(pump.east(x).below(), vein);
        h.getLevel()
                .setBlockAndUpdate(
                        pump,
                        org.jd.infestusfrontier.logistics.LogisticsModule.VASCULAR_JUNCTION
                                .get()
                                .defaultBlockState()
                                .setValue(
                                        org.jd.infestusfrontier.logistics.VascularJunctionBlock.PORT.get(
                                                Direction.WEST),
                                        org.jd.infestusfrontier.logistics.Port.INTAKE)
                                .setValue(
                                        org.jd.infestusfrontier.logistics.VascularJunctionBlock.PORT.get(
                                                Direction.DOWN),
                                        org.jd.infestusfrontier.logistics.Port.OUTLET));
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(
                        driver.store().steam(), 400, "steam reaches driver through existing pump and sealed veins"))
                .thenExecute(() -> h.assertValueEqual(source.store().steam(), 0, "all steam left source"))
                .thenSucceed();
    }

    private static PowerRig heart(GameTestHelper h, Block type) {
        return place(h.getLevel(), h.absolutePos(new BlockPos(3, 2, 3)), type);
    }

    private static PowerRig place(ServerLevel level, BlockPos pos, Block type) {
        level.setBlockAndUpdate(
                pos.below(), EcologyModule.LIVING_SUBSTRATE.get().defaultBlockState());
        level.setBlockAndUpdate(pos, type.defaultBlockState());
        return new PowerRig(level.getBlockEntity(pos));
    }

    private static BlockPos part(PowerRig heart, Block block, Direction side) {
        BlockPos pos = heart.getBlockPos().relative(side);
        heart.getLevel()
                .setBlockAndUpdate(
                        pos, block.defaultBlockState().setValue(DirectionalBlock.FACING, side.getOpposite()));
        return pos;
    }

    private static void store(PowerRig heart, PowerStore store) {
        var registries = heart.getLevel().registryAccess();
        CompoundTag tag = heart.saveWithoutMetadata(registries);
        CompoundTag power = new CompoundTag();
        power.putInt("heat", store.heat());
        power.putInt("water", store.water());
        power.putInt("steam", store.steam());
        power.putInt("charge", store.charge());
        power.putInt("cycles", store.cycles());
        tag.put("power", power);
        heart.loadWithComponents(tag, registries);
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void lavaAndCollectorCannotProduceHeatOutsideTheNether(GameTestHelper h) {
        PowerRig heart = heart(h, PowerModule.HEART.get());
        BlockPos collector = part(heart, PowerModule.COLLECTOR.get(), Direction.EAST);
        h.getLevel().setBlockAndUpdate(collector.below(), Blocks.LAVA.defaultBlockState());
        h.startSequence()
                .thenIdle(25)
                .thenExecute(() -> {
                    h.assertValueEqual(heart.store().heat(), 0, "no overworld heat production");
                    h.assertValueEqual(heart.stop(), 2, "dimension reason");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aRealNetherPlantProducesHeatThenSteamAndElectricity(GameTestHelper h) {
        ServerLevel level = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(level != null, "Nether available");
        BlockPos pos = new BlockPos(1288, 100, 1288);
        var chunk = new net.minecraft.world.level.ChunkPos(pos);
        level.setChunkForced(chunk.x, chunk.z, true);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) level.getChunk(chunk.x + x, chunk.z + z);
        for (BlockPos cell : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(3, 4, 3)))
            level.setBlock(cell, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (BlockPos cell : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 3, 2)))
            level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        PowerRig heart = place(level, pos, PowerModule.HEART.get());
        BlockPos collector = part(heart, PowerModule.COLLECTOR.get(), Direction.EAST);
        level.setBlockAndUpdate(collector.below(), Blocks.LAVA.defaultBlockState());
        BlockPos boiler = part(heart, PowerModule.BOILER.get(), Direction.WEST);
        part(heart, PowerModule.GENERATOR.get(), Direction.NORTH);
        var water = level.getCapability(Capabilities.FluidHandler.BLOCK, boiler, Direction.WEST);
        h.assertValueEqual(
                water.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE),
                1000,
                "supplied water");
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(heart.store().charge() > 0, "Nether generator works"))
                .thenExecute(() -> {
                    h.assertTrue(heart.store().water() < 1000, "water consumed");
                    h.assertTrue(heart.store().steam() > 0, "remaining real steam");
                    level.removeBlock(pos, false);
                    level.setChunkForced(chunk.x, chunk.z, false);
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void boilerAndGeneratorConserveAndFullProductsPause(GameTestHelper h) {
        PowerRig heart = heart(h, PowerModule.RECEIVER.get());
        part(heart, PowerModule.BOILER.get(), Direction.WEST);
        part(heart, PowerModule.GENERATOR.get(), Direction.NORTH);
        store(heart, new PowerStore(10, 10, 0, 0, 0));
        h.startSequence()
                .thenWaitUntil(() ->
                        h.assertValueEqual(heart.store().charge(), 80, "one boiler batch yields two generator batches"))
                .thenExecute(() -> {
                    h.assertValueEqual(heart.store(), new PowerStore(0, 0, 0, 80, 3), "conservative complete route");
                    store(heart, new PowerStore(10, 10, 16000, 16000, 3));
                })
                .thenIdle(25)
                .thenExecute(() -> h.assertValueEqual(
                        heart.store(), new PowerStore(10, 10, 16000, 16000, 3), "full products retain all inputs"))
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void capsulesMoveOnlyTheirStoredHeatAndReturnTheirShell(GameTestHelper h) {
        PowerRig source = heart(h, PowerModule.HEART.get());
        PowerRig destination = place(h.getLevel(), source.getBlockPos().offset(3, 0, 0), PowerModule.RECEIVER.get());
        store(source, new PowerStore(1000, 0, 0, 0, 0));
        source.items().insertItem(0, new ItemStack(PowerModule.HEAT_CAPSULE.get()), false);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(
                        source.items().getStackInSlot(2).is(PowerModule.HOT_CAPSULE.get()), "source charges capsule"))
                .thenExecute(() -> {
                    h.assertValueEqual(source.store().heat(), 0, "heat left source");
                    destination.items().insertItem(0, source.items().extractItem(2, 1, false), false);
                })
                .thenWaitUntil(() -> h.assertValueEqual(destination.store().heat(), 1000, "same heat arrives"))
                .thenExecute(() -> h.assertTrue(
                        destination.items().getStackInSlot(2).is(PowerModule.HEAT_CAPSULE.get()),
                        "empty shell returned"))
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void cellRoundTripAndBlockedOutputNeverCreateCharge(GameTestHelper h) {
        PowerRig heart = heart(h, PowerModule.RECEIVER.get());
        store(heart, new PowerStore(0, 0, 0, 1000, 0));
        heart.items().insertItem(1, new ItemStack(PowerModule.CELL.get()), false);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(
                        heart.items().getStackInSlot(3).is(PowerModule.CHARGED_CELL.get()), "cell charged"))
                .thenExecute(() -> {
                    h.assertValueEqual(heart.store().charge(), 0, "charge left store");
                    heart.toggle();
                    heart.items().insertItem(1, heart.items().extractItem(3, 1, false), false);
                    heart.items().setStackInSlot(3, new ItemStack(Items.STONE));
                })
                .thenIdle(25)
                .thenExecute(() -> {
                    h.assertValueEqual(heart.store().charge(), 0, "blocked output prevents discharge");
                    h.assertTrue(
                            heart.items().getStackInSlot(1).is(PowerModule.CHARGED_CELL.get()),
                            "charged input retained");
                    heart.items().extractItem(3, 1, false);
                })
                .thenWaitUntil(() ->
                        h.assertValueEqual(heart.store().charge(), 1000, "round trip restores exactly original charge"))
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void detachedCachedPortsRefuseEveryTransfer(GameTestHelper h) {
        PowerRig heart = heart(h, PowerModule.RECEIVER.get());
        BlockPos boiler = part(heart, PowerModule.BOILER.get(), Direction.WEST);
        BlockPos generator = part(heart, PowerModule.GENERATOR.get(), Direction.NORTH);
        store(heart, new PowerStore(0, 0, 0, 1000, 0));
        var fluid = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, boiler, Direction.WEST);
        var energy = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, generator, Direction.NORTH);
        h.assertValueEqual(
                fluid.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.SIMULATE),
                1000,
                "fluid simulation");
        h.assertValueEqual(heart.store().water(), 0, "simulation changes nothing");
        h.assertValueEqual(energy.extractEnergy(1000, true), 1000, "energy simulation");
        h.assertValueEqual(heart.store().charge(), 1000, "energy simulation changes nothing");
        h.getLevel().removeBlock(boiler, false);
        h.getLevel().removeBlock(generator, false);
        part(heart, PowerModule.BOILER.get(), Direction.EAST);
        part(heart, PowerModule.GENERATOR.get(), Direction.SOUTH);
        h.assertValueEqual(
                fluid.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE),
                0,
                "old port stays closed even with replacement elsewhere");
        h.assertValueEqual(energy.extractEnergy(1000, false), 0, "old energy port closed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void corruptPowerStoreRefusesAndPreservesUnknownData(GameTestHelper h) {
        PowerRig heart = heart(h, PowerModule.DRIVER.get());
        CompoundTag raw = heart.saveWithoutMetadata(h.getLevel().registryAccess());
        raw.getCompound("power").putInt("steam", -1);
        raw.putString("future", "preserve");
        heart.loadWithComponents(raw, h.getLevel().registryAccess());
        h.assertTrue(heart.pay(false) instanceof Outcome.Refused<?>, "corrupt drive refuses");
        h.assertValueEqual(
                heart.fluids(false)
                        .fill(new FluidStack(PowerModule.STEAM.get(), 100), IFluidHandler.FluidAction.EXECUTE),
                0,
                "corrupt store refuses input");
        h.assertValueEqual(heart.saveWithoutMetadata(h.getLevel().registryAccess()), raw, "raw state preserved");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void steamEnhancesTheExistingJawAndPaysOnlyForASuccessfulCut(GameTestHelper h) {
        PowerRig driver = heart(h, PowerModule.DRIVER.get());
        BlockPos jaw = driver.getBlockPos().west();
        BlockPos bed = jaw.south();
        for (BlockPos pos : List.of(jaw, bed))
            h.getLevel()
                    .setBlockAndUpdate(
                            pos.below(), EcologyModule.LIVING_SUBSTRATE.get().defaultBlockState());
        h.getLevel()
                .setBlockAndUpdate(
                        jaw,
                        MiningModule.FRACTURE_JAW
                                .get()
                                .defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        h.getLevel().setBlockAndUpdate(bed, MiningModule.WORK_BED.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(jaw.north(), Blocks.DIAMOND_ORE.defaultBlockState());
        h.getLevel().getCapability(BiomassCapabilities.BLOCK, jaw, Direction.UP).insert(Biomass.of(1000), false);
        driver.fluids(false).fill(new FluidStack(PowerModule.STEAM.get(), 80), IFluidHandler.FluidAction.EXECUTE);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(
                        h.getLevel().getBlockState(jaw.north()).isAir(), "iron grade harvests diamond ore"))
                .thenExecute(() -> {
                    h.assertValueEqual(driver.store().steam(), 40, "one cut payment");
                    var slots = h.getLevel()
                            .getBlockEntity(bed)
                            .saveWithoutMetadata(h.getLevel().registryAccess())
                            .getList("slots", 10);
                    boolean diamond = false;
                    for (int i = 0; i < slots.size(); i++)
                        diamond |= ItemStack.parseOptional(h.getLevel().registryAccess(), slots.getCompound(i))
                                .is(Items.DIAMOND);
                    h.assertTrue(diamond, "real iron-tool loot");
                })
                .thenIdle(25)
                .thenExecute(() -> h.assertValueEqual(driver.store().steam(), 40, "empty target charges nothing"))
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void cachedItemHandlersCannotDrainRemovedOrgans(GameTestHelper h) {
        PowerRig heart = heart(h, PowerModule.RECEIVER.get());
        var handler = heart.items();
        handler.insertItem(0, new ItemStack(PowerModule.HOT_CAPSULE.get()), false);
        h.getLevel().removeBlock(heart.getBlockPos(), false);
        h.assertTrue(handler.extractItem(0, 1, false).isEmpty(), "old owner cannot give copied contents");
        h.assertFalse(
                handler.insertItem(1, new ItemStack(PowerModule.CELL.get()), false)
                        .isEmpty(),
                "old owner refuses input");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aRebuiltCoreInvalidatesCachedPartCapabilities(GameTestHelper h) {
        PowerRig heart = heart(h, PowerModule.RECEIVER.get());
        BlockPos boiler = part(heart, PowerModule.BOILER.get(), Direction.WEST);
        var cache = net.neoforged.neoforge.capabilities.BlockCapabilityCache.create(
                Capabilities.FluidHandler.BLOCK, h.getLevel(), boiler, Direction.WEST);
        h.assertTrue(cache.getCapability() != null, "initial connection");
        h.getLevel().removeBlock(heart.getBlockPos(), false);
        h.assertTrue(cache.getCapability() == null, "removed core invalidates cache");
        PowerRig next = place(h.getLevel(), heart.getBlockPos(), PowerModule.RECEIVER.get());
        h.assertTrue(cache.getCapability() != null, "replacement reconnects existing port");
        cache.getCapability().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        h.assertValueEqual(next.store().water(), 1000, "water belongs to replacement");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void supplyChangesCannotAccelerateSavedProductionCadence(GameTestHelper h) {
        PowerRig heart = heart(h, PowerModule.RECEIVER.get());
        part(heart, PowerModule.BOILER.get(), Direction.WEST);
        store(heart, new PowerStore(1000, 1000, 0, 0, 0));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(heart.store().cycles() > 0, "first pulse"))
                .thenExecute(() -> {
                    int ticks = heart.contents().ticks();
                    h.assertTrue(ticks > 0, "next pulse scheduled");
                    for (int i = 0; i < 100; i++) {
                        heart.fluids(true).fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE);
                        heart.items().insertItem(1, new ItemStack(PowerModule.CELL.get()), false);
                        heart.items().extractItem(1, 1, false);
                    }
                    h.assertValueEqual(heart.contents().ticks(), ticks, "supplies preserve cadence");
                    CompoundTag tag = heart.saveWithoutMetadata(h.getLevel().registryAccess());
                    heart.loadWithComponents(tag, h.getLevel().registryAccess());
                    h.assertValueEqual(heart.contents().ticks(), ticks, "save/load preserves cadence");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void aBrokenPowerCoreCarriesItsStoresInventoryAndHistoryOnce(GameTestHelper h) {
        PowerRig heart = heart(h, PowerModule.RECEIVER.get());
        PowerStore before = new PowerStore(600, 700, 800, 900, 123);
        store(heart, before);
        heart.items().insertItem(0, new ItemStack(PowerModule.HOT_CAPSULE.get()), false);
        var drops = Block.getDrops(
                h.getLevel().getBlockState(heart.getBlockPos()), h.getLevel(), heart.getBlockPos(), heart.entity());
        h.assertValueEqual(drops.size(), 1, "one organ with no loose products");
        ItemStack carried = drops.getFirst();
        var old = heart.items();
        h.getLevel().removeBlock(heart.getBlockPos(), false);
        PowerRig replacement = place(h.getLevel(), heart.getBlockPos(), PowerModule.RECEIVER.get());
        replacement
                .entity()
                .applyComponents(carried.getComponents(), net.minecraft.core.component.DataComponentPatch.EMPTY);
        h.assertValueEqual(replacement.store(), before, "every store and work history travels with core");
        h.assertTrue(
                replacement.items().getStackInSlot(0).is(PowerModule.HOT_CAPSULE.get()), "stored capsule preserved");
        h.assertTrue(old.extractItem(0, 1, false).isEmpty(), "old owner cannot duplicate it");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theMiningDriverDiesWhenItsSupportIsRemoved(GameTestHelper h) {
        PowerRig driver = heart(h, PowerModule.DRIVER.get());
        store(driver, new PowerStore(0, 0, 1000, 0, 0));
        h.getLevel().removeBlock(driver.getBlockPos().below(), false);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(
                        h.getLevel().getBlockState(driver.getBlockPos()).isAir(), "unsupported driver dies"))
                .thenExecute(() -> h.assertTrue(
                        h.getLevel()
                                .getEntitiesOfClass(
                                        net.minecraft.world.entity.item.ItemEntity.class,
                                        new net.minecraft.world.phys.AABB(driver.getBlockPos()))
                                .isEmpty(),
                        "no organ or steam drops"))
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "steam_gallery")
    public static void twoGalleryFacesReportSteamOnTheAlreadySelectedHardCut(GameTestHelper h) {
        GalleryRig rig = GalleryRig.of(h, 40);
        rig.minehead(4000, "stairs");
        rig.face(
                GalleryRig.Face.WEST,
                2,
                org.jd.infestusfrontier.core.mining.Turn.NONE,
                org.jd.infestusfrontier.core.mining.Turn.NONE,
                false,
                8);
        rig.face(
                GalleryRig.Face.NORTH,
                2,
                org.jd.infestusfrontier.core.mining.Turn.NONE,
                org.jd.infestusfrontier.core.mining.Turn.NONE,
                false,
                8);
        rig.cutter(GalleryRig.Cutter.WEST_RIGHT);
        rig.cutter(GalleryRig.Cutter.NORTH_RIGHT);
        for (var face : List.of(GalleryRig.Face.WEST, GalleryRig.Face.NORTH))
            for (var step : rig.plan(face).steps())
                for (var cell : step.cleared()) rig.set(GalleryRig.plan(face, cell), "minecraft:diamond_ore");
        place(rig.level, rig.at(GalleryRig.Cutter.WEST_RIGHT.jaw).west(), PowerModule.DRIVER.get());
        place(rig.level, rig.at(GalleryRig.Cutter.NORTH_RIGHT.jaw).north(), PowerModule.DRIVER.get());
        var player = StarterAdvancementTests.player(h);
        h.startSequence()
                .thenWaitUntil(() -> {
                    h.assertTrue(
                            rig.faceStop(player, 0)
                                    .filter("infestusfrontier.refusal.power.no_steam"::equals)
                                    .isPresent(),
                            "first face names steam instead of losing its turn");
                    h.assertTrue(
                            rig.faceStop(player, 1)
                                    .filter("infestusfrontier.refusal.power.no_steam"::equals)
                                    .isPresent(),
                            "second face names steam");
                })
                .thenExecute(rig::release)
                .thenSucceed();
    }
}
