package org.jd.infestusfrontier.testmod;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.vigil.Territory;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.vigil.VigilKeeper;
import org.jd.infestusfrontier.vigil.VigilModule;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class VigilTests {
    private static NestRig nest(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
        ServerLevel level = h.getLevel();
        for (BlockPos cell : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 5, 1)))
            level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlockAndUpdate(
                pos.below(),
                EcologyModule.LIVING_SUBSTRATE
                        .get()
                        .defaultBlockState()
                        .setValue(
                                org.jd.infestusfrontier.ecology.LivingSubstrateBlock.STAGE,
                                org.jd.infestusfrontier.ecology.SubstrateStage.MATURE));
        level.setBlockAndUpdate(pos, VigilModule.NEST.get().defaultBlockState());
        return new NestRig(level.getBlockEntity(pos));
    }

    private static void supply(NestRig nest, int fuel) {
        nest.insert(Biomass.of(8000), false);
        nest.seed();
        nest.insert(Biomass.of(fuel), false);
    }

    @GameTest(template = "empty")
    public static void startPaysExactlyOnceAndRestSheltersTheKeeper(GameTestHelper h) {
        NestRig nest = nest(h);
        supply(nest, 100);
        nest.action(9);
        h.assertTrue(nest.enabled(), "nest wakes");
        h.assertValueEqual(nest.tank().stored().units(), 90, "one chunk prepays ten BU");
        h.assertValueEqual(nest.paid(), 200, "two hundred loaded ticks paid");
        CompoundTag state = nest.saveWithoutMetadata(h.getLevel().registryAccess());
        h.assertTrue(h.getLevel().getEntity(state.getUUID("keeper")) instanceof VigilKeeper, "real bound creature");
        nest.action(9);
        h.assertFalse(nest.enabled(), "rest releases service");
        h.assertTrue(nest.egg(), "living keeper sheltered");
        h.assertTrue(h.getLevel().getEntity(state.getUUID("keeper")) == null, "no orphan left");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void missingSupplyConsumesNothingAndFeedersBoundSelection(GameTestHelper h) {
        NestRig nest = nest(h);
        supply(nest, 9);
        nest.action(9);
        h.assertFalse(nest.enabled(), "cannot afford payment");
        h.assertValueEqual(nest.tank().stored().units(), 9, "fuel unchanged");
        nest.action(0);
        h.assertValueEqual(nest.mask(), Territory.CENTRE, "bare nest cannot select second chunk");
        h.getLevel()
                .setBlockAndUpdate(
                        nest.getBlockPos().east(), VigilModule.FEEDER.get().defaultBlockState());
        nest.action(0);
        nest.action(1);
        nest.action(2);
        h.assertValueEqual(Integer.bitCount(nest.mask()), 3, "one part permits three, refuses fourth");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void keeperDeathConsumesTheEggAndStopsLoading(GameTestHelper h) {
        NestRig nest = nest(h);
        supply(nest, 100);
        nest.action(9);
        CompoundTag state = nest.saveWithoutMetadata(h.getLevel().registryAccess());
        h.getLevel().getEntity(state.getUUID("keeper")).kill();
        h.startSequence()
                .thenWaitUntil(() -> h.assertFalse(nest.enabled(), "death stops service"))
                .thenExecute(() -> {
                    h.assertFalse(nest.egg(), "death needs a replacement egg");
                    nest.action(9);
                    h.assertFalse(nest.enabled(), "cannot silently respawn");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void growthChargesEightThousandOnceAndRefusalKeepsTheEgg(GameTestHelper h) {
        NestRig nest = nest(h);
        nest.insert(Biomass.of(7999), false);
        h.assertFalse(nest.seed(), "insufficient growth biomass keeps egg");
        h.assertValueEqual(nest.tank().stored().units(), 7999, "refusal takes no biomass");
        h.assertValueEqual(nest.stop(), 10, "growth cost explained");
        nest.insert(Biomass.of(1), false);
        h.assertTrue(nest.seed(), "funded growth consumes egg");
        h.assertValueEqual(nest.tank().stored().units(), 0, "growth costs 8000 BU");
        h.assertFalse(nest.seed(), "existing keeper cannot consume another egg");
        nest.insert(Biomass.of(100), false);
        nest.action(9);
        h.assertTrue(nest.enabled(), "grown keeper needs only upkeep to wake");
        nest.action(9);
        nest.action(9);
        h.assertTrue(nest.enabled(), "rest does not repeat growth cost");
        h.assertValueEqual(nest.tank().stored().units(), 80, "two ordinary wake payments");
        nest.action(9);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 850)
    public static void exhaustedFuelInjuresThenKillsInsteadOfSheltering(GameTestHelper h) {
        NestRig nest = nest(h);
        supply(nest, 10);
        nest.action(9);
        VigilKeeper keeper = (VigilKeeper) h.getLevel().getEntity(nest.saved().getUUID("keeper"));
        h.startSequence()
                .thenIdle(180)
                .thenExecute(() -> h.assertValueEqual(keeper.getHealth(), 30F, "prepaid time feeds keeper"))
                .thenWaitUntil(() -> h.assertTrue(keeper.getHealth() < 30, "starvation visibly injures"))
                .thenExecute(() -> {
                    h.assertTrue(nest.enabled(), "starving keeper is not sheltered");
                    h.assertValueEqual(nest.stop(), 4, "missing food named");
                })
                .thenWaitUntil(() -> h.assertFalse(nest.enabled(), "starvation eventually kills"))
                .thenExecute(() -> {
                    h.assertFalse(keeper.isAlive(), "real entity died");
                    h.assertFalse(nest.egg(), "no free replacement after starvation");
                    h.assertValueEqual(nest.tank().stored().units(), 0, "no fuel debt");
                    h.assertFalse(nest.seed(), "replacement also requires growth biomass");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void restoringARequiredFeederStopsDamageWithoutHealing(GameTestHelper h) {
        NestRig nest = nest(h);
        supply(nest, 100);
        BlockPos part = nest.getBlockPos().east();
        h.getLevel().setBlockAndUpdate(part, VigilModule.FEEDER.get().defaultBlockState());
        nest.action(0);
        nest.action(9);
        VigilKeeper keeper = (VigilKeeper) h.getLevel().getEntity(nest.saved().getUUID("keeper"));
        h.getLevel().removeBlock(part, false);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(keeper.getHealth() < 30, "missing feeder causes damage"))
                .thenExecute(() -> {
                    h.assertTrue(nest.enabled(), "lost part does not shelter");
                    h.assertValueEqual(nest.stop(), 3, "missing part named");
                    keeper.setHealth(12.5F);
                    h.getLevel()
                            .setBlockAndUpdate(part, VigilModule.FEEDER.get().defaultBlockState());
                })
                .thenIdle(45)
                .thenExecute(() -> {
                    h.assertValueEqual(keeper.getHealth(), 12.5F, "repair stops damage without healing");
                    h.assertValueEqual(nest.saved().getInt("hungry"), 0, "supplied again");
                    nest.action(9);
                    h.assertValueEqual(nest.saved().getFloat("health"), 12.5F, "rest preserves fractional health");
                    nest.action(9);
                    VigilKeeper awake =
                            (VigilKeeper) h.getLevel().getEntity(nest.saved().getUUID("keeper"));
                    h.assertValueEqual(awake.getHealth(), 12.5F, "waking cannot heal");
                    var drops = Block.getDrops(
                            h.getLevel().getBlockState(nest.getBlockPos()),
                            h.getLevel(),
                            nest.getBlockPos(),
                            nest.entity());
                    var components = ((CompoundTag)
                                    drops.getFirst().save(h.getLevel().registryAccess()))
                            .getCompound("components");
                    h.assertValueEqual(
                            components
                                    .getCompound("infestusfrontier:vigil_nest")
                                    .getFloat("health"),
                            12.5F,
                            "carrying preserves injuries");
                    h.getLevel().removeBlock(nest.getBlockPos(), false);
                    h.getLevel()
                            .setBlockAndUpdate(
                                    nest.getBlockPos(), VigilModule.NEST.get().defaultBlockState());
                    var placed = h.getLevel().getBlockEntity(nest.getBlockPos());
                    placed.applyComponents(
                            drops.getFirst().getComponents(), net.minecraft.core.component.DataComponentPatch.EMPTY);
                    NestRig moved = new NestRig(placed);
                    moved.action(9);
                    h.assertTrue(moved.enabled(), "carried keeper wakes without a second growth charge");
                    h.assertValueEqual(
                            ((VigilKeeper) h.getLevel().getEntity(moved.saved().getUUID("keeper"))).getHealth(),
                            12.5F,
                            "placing carried nest cannot heal");
                    moved.action(9);
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void feedingAStarvingKeeperSavesItWithoutHealing(GameTestHelper h) {
        NestRig nest = nest(h);
        supply(nest, 10);
        nest.action(9);
        VigilKeeper keeper = (VigilKeeper) h.getLevel().getEntity(nest.saved().getUUID("keeper"));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(keeper.getHealth() < 30, "food ran out"))
                .thenExecute(() -> {
                    keeper.setHealth(12.5F);
                    nest.insert(Biomass.of(10), false);
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(nest.enabled(), "refill saves keeper");
                    h.assertValueEqual(keeper.getHealth(), 12.5F, "food is not free healing");
                    h.assertValueEqual(nest.tank().stored().units(), 0, "rescue paid normal upkeep");
                    nest.action(9);
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 1400, batch = "vigil_starvation_unload")
    public static void evenAnInvulnerableKeeperCannotLoadForeverWithoutFood(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos pos = new BlockPos(830008, 100, 830008);
        ChunkPos chunk = new ChunkPos(pos);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) level.getChunk(chunk.x + x, chunk.z + z);
        level.setBlockAndUpdate(
                pos.below(), EcologyModule.LIVING_SUBSTRATE.get().defaultBlockState());
        for (BlockPos at : BlockPos.betweenClosed(pos, pos.above(5)))
            level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlockAndUpdate(pos, VigilModule.NEST.get().defaultBlockState());
        NestRig nest = new NestRig(level.getBlockEntity(pos));
        supply(nest, 10);
        nest.action(9);
        VigilKeeper keeper = (VigilKeeper) level.getEntity(nest.saved().getUUID("keeper"));
        keeper.setInvulnerable(true);
        h.startSequence()
                .thenWaitUntil(() ->
                        h.assertTrue(nest.saved().getInt("hungry") >= 40, "remote chunk ticks through food exhaustion"))
                .thenExecute(() -> {
                    h.assertTrue(level.hasChunkAt(pos), "emergency tickets keep remote keeper present");
                    h.assertTrue(nest.enabled(), "not automatically sheltered");
                })
                .thenWaitUntil(() ->
                        h.assertValueEqual(nest.saved().getInt("hungry"), 800, "finite starvation allowance expires"))
                .thenExecute(() -> {
                    h.assertTrue(keeper.isAlive() && nest.enabled(), "expiry does not shelter or discard creature");
                    level.save(null, true, false);
                })
                .thenWaitUntil(() -> h.assertFalse(
                        level.hasChunkAt(pos), "unpaid remote chunk actually unloads despite healing immunity"))
                .thenSucceed();
    }

    @GameTest(template = "empty", batch = "vigil_limits")
    public static void dimensionQuotaRejectsTheFirstExcessNest(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        List<NestRig> nests = new ArrayList<>();
        BlockPos origin = h.absolutePos(new BlockPos(2, 2, 2));
        try {
            for (int i = 0; i <= Territory.DIMENSION_CHUNKS; i++) {
                BlockPos at = origin.offset((i % 7) * 3, 0, (i / 7) * 3);
                for (BlockPos cell : BlockPos.betweenClosed(at.offset(-1, 0, -1), at.offset(1, 5, 1)))
                    level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlockAndUpdate(
                        at.below(), EcologyModule.LIVING_SUBSTRATE.get().defaultBlockState());
                level.setBlockAndUpdate(at, VigilModule.NEST.get().defaultBlockState());
                NestRig nest = new NestRig(level.getBlockEntity(at));
                nests.add(nest);
                supply(nest, 100);
                nest.action(9);
                h.assertTrue(
                        nest.enabled() == (i < Territory.DIMENSION_CHUNKS),
                        "dimension cap at nest " + i + "; stop=" + nest.stop());
            }
        } finally {
            for (NestRig nest : nests) level.removeBlock(nest.getBlockPos(), false);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void unreadableNestKeepsBytesAndRefusesFuelAndHatching(GameTestHelper h) {
        NestRig nest = nest(h);
        CompoundTag raw = nest.saveWithoutMetadata(h.getLevel().registryAccess());
        raw.putInt("mask", 0);
        raw.putString("future_data", "keep me");
        nest.loadWithComponents(raw, h.getLevel().registryAccess());
        h.assertTrue(nest.insert(Biomass.of(100), false) instanceof Outcome.Refused<?>, "corrupt owner refuses fuel");
        h.assertFalse(nest.seed(), "corrupt owner refuses egg");
        nest.action(9);
        h.assertValueEqual(nest.saveWithoutMetadata(h.getLevel().registryAccess()), raw, "raw bytes retained");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void activeTerritoryCannotBeEditedWithoutResting(GameTestHelper h) {
        NestRig nest = nest(h);
        supply(nest, 100);
        nest.action(9);
        nest.action(0);
        h.assertValueEqual(nest.mask(), Territory.CENTRE, "active selection unchanged");
        h.assertValueEqual(nest.tank().stored().units(), 90, "edit consumes no fuel");
        nest.action(9);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 1000, batch = "vigil_unload")
    public static void onlyThePaidNestKeepsItsRemoteChunkTicking(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos pos = new BlockPos(810008, 100, 810008);
        ChunkPos chunk = new ChunkPos(pos);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) level.getChunk(chunk.x + x, chunk.z + z);
        level.setBlockAndUpdate(
                pos.below(),
                org.jd.infestusfrontier.ecology.EcologyModule.LIVING_SUBSTRATE
                        .get()
                        .defaultBlockState()
                        .setValue(
                                org.jd.infestusfrontier.ecology.LivingSubstrateBlock.STAGE,
                                org.jd.infestusfrontier.ecology.SubstrateStage.MATURE));
        for (BlockPos at : BlockPos.betweenClosed(pos, pos.above(5)))
            level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlockAndUpdate(pos, VigilModule.NEST.get().defaultBlockState());
        NestRig nest = new NestRig(level.getBlockEntity(pos));
        supply(nest, 100);
        nest.action(9);
        h.startSequence()
                .thenIdle(220)
                .thenExecute(() -> {
                    h.assertTrue(level.hasChunkAt(pos), "nest retained remote chunk without vanilla forcing");
                    h.assertTrue(nest.tank().stored().units() < 90, "remote nest really ticks and pays next period");
                    nest.action(9);
                    level.save(null, true, false);
                })
                .thenWaitUntil(() -> h.assertFalse(level.hasChunkAt(pos), "rest releases remote chunk to unload"))
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void cachedBiomassHandlerCannotDrainARemovedNest(GameTestHelper h) {
        NestRig nest = nest(h);
        supply(nest, 100);
        h.getLevel().removeBlock(nest.getBlockPos(), false);
        h.assertTrue(
                nest.extract(Biomass.of(100), false) instanceof Outcome.Refused<?>,
                "removed owner cannot give copied fuel");
        h.assertTrue(
                nest.insert(Biomass.of(100), false) instanceof Outcome.Refused<?>, "removed owner cannot accept fuel");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 180, batch = "vigil_nether")
    public static void aNetherKeeperRemainsAliveWhileItsPlantRuns(GameTestHelper h) {
        ServerLevel level = h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        BlockPos pos = new BlockPos(3080, 100, 3080);
        ChunkPos chunk = new ChunkPos(pos);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) level.getChunk(chunk.x + x, chunk.z + z);
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(7, 6, 3)))
            level.setBlock(at, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(6, 5, 2)))
            level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlockAndUpdate(
                pos.below(), EcologyModule.LIVING_SUBSTRATE.get().defaultBlockState());
        level.setBlockAndUpdate(pos, VigilModule.NEST.get().defaultBlockState());
        NestRig nest = new NestRig(level.getBlockEntity(pos));
        supply(nest, 1000);
        nest.action(9);
        h.assertTrue(nest.enabled(), "starts in Nether");
        var creature = (VigilKeeper) level.getEntity(nest.saved().getUUID("keeper"));
        h.startSequence()
                .thenIdle(120)
                .thenExecute(() -> {
                    h.assertTrue(
                            nest.enabled(),
                            "Nether nest remains awake; stop=" + nest.stop() + " keeper health=" + creature.getHealth()
                                    + " removal=" + creature.getRemovalReason() + " at=" + creature.position());
                    nest.action(9);
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void replacingAnActiveSaveReleasesTheOldKeeperAndTerritory(GameTestHelper h) {
        NestRig nest = nest(h);
        supply(nest, 100);
        nest.action(9);
        CompoundTag raw = nest.saved();
        var keeper = h.getLevel().getEntity(raw.getUUID("keeper"));
        raw.putInt("mask", 0);
        nest.loadWithComponents(raw, h.getLevel().registryAccess());
        h.assertTrue(keeper.isRemoved(), "in-place corrupt reload releases the old living service");
        h.assertValueEqual(nest.saved(), raw, "unreadable replacement preserved exactly");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 1050)
    public static void replacingAnExpiredHungryNestStillRemovesItsOldKeeper(GameTestHelper h) {
        NestRig nest = nest(h);
        supply(nest, 10);
        nest.action(9);
        VigilKeeper keeper = (VigilKeeper) h.getLevel().getEntity(nest.saved().getUUID("keeper"));
        keeper.setInvulnerable(true);
        h.startSequence()
                .thenWaitUntil(
                        () -> h.assertValueEqual(nest.saved().getInt("hungry"), 800, "starvation tickets expired"))
                .thenExecute(() -> {
                    h.assertTrue(keeper.isAlive(), "living keeper remains bound without tickets");
                    CompoundTag raw = nest.saved();
                    raw.putInt("mask", 0);
                    nest.loadWithComponents(raw, h.getLevel().registryAccess());
                    h.assertTrue(keeper.isRemoved(), "in-place replacement cleans old creature even without tickets");
                    h.assertValueEqual(nest.saved(), raw, "corrupt replacement retained exactly");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void breakingImmediatelyAfterKeeperDeathDoesNotResurrectItsEgg(GameTestHelper h) {
        NestRig nest = nest(h);
        supply(nest, 100);
        nest.action(9);
        h.getLevel().getEntity(nest.saved().getUUID("keeper")).kill();
        var drops = Block.getDrops(
                h.getLevel().getBlockState(nest.getBlockPos()), h.getLevel(), nest.getBlockPos(), nest.entity());
        h.assertValueEqual(drops.size(), 1, "one carried nest");
        var components = ((CompoundTag) drops.getFirst().save(h.getLevel().registryAccess())).getCompound("components");
        h.assertFalse(
                components.getCompound("infestusfrontier:vigil_nest").getBoolean("egg"),
                "dead keeper is not carried as a living egg");
        h.getLevel().removeBlock(nest.getBlockPos(), false);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "vigil_server_limit", timeoutTicks = 200)
    public static void threeDimensionsShareOneServerTerritoryCeiling(GameTestHelper h) {
        List<NestRig> nests = new ArrayList<>();
        try {
            for (var dimension : List.of(
                    net.minecraft.world.level.Level.OVERWORLD,
                    net.minecraft.world.level.Level.NETHER,
                    net.minecraft.world.level.Level.END)) {
                ServerLevel level = h.getLevel().getServer().getLevel(dimension);
                BlockPos origin = new BlockPos(4008, 100, 4008);
                ChunkPos chunk = new ChunkPos(origin);
                for (int x = -2; x <= 3; x++) for (int z = -2; z <= 2; z++) level.getChunk(chunk.x + x, chunk.z + z);
                for (int i = 0; i < 18; i++) {
                    BlockPos pos = origin.offset(i % 6 * 3, 0, i / 6 * 3);
                    for (BlockPos at : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 5, 1)))
                        level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    level.setBlockAndUpdate(
                            pos.below(), EcologyModule.LIVING_SUBSTRATE.get().defaultBlockState());
                    level.setBlockAndUpdate(pos, VigilModule.NEST.get().defaultBlockState());
                    NestRig nest = new NestRig(level.getBlockEntity(pos));
                    nests.add(nest);
                    supply(nest, 100);
                    nest.action(9);
                    h.assertTrue(nest.enabled(), "within shared server cap: " + nests.size() + " stop=" + nest.stop());
                }
            }
            NestRig excess = nest(h);
            nests.add(excess);
            supply(excess, 100);
            excess.action(9);
            h.assertFalse(excess.enabled(), "fifty-fifth chunk refused despite local room");
            h.assertValueEqual(excess.stop(), 6, "server limit named");
        } finally {
            for (NestRig nest : nests) nest.level().removeBlock(nest.getBlockPos(), false);
        }
        h.succeed();
    }
}
