package org.jd.infestusfrontier.testmod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Properties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.power.PowerModule;
import org.jd.infestusfrontier.vigil.VigilKeeper;
import org.jd.infestusfrontier.vigil.VigilModule;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class VigilRestartTests {
    private static final BlockPos HOME = new BlockPos(918008, 100, 918008);
    private static final BlockPos STARVING = new BlockPos(928008, 100, 928008);
    private static final BlockPos NETHER = new BlockPos(2584, 100, 2584);
    private static final String PHASE = "infestusfrontier.restart.phase";

    private static NestRig build(ServerLevel level, BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) level.getChunk(chunk.x + x, chunk.z + z);
        for (BlockPos cell : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(7, 6, 3)))
            level.setBlock(cell, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (BlockPos cell : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(6, 5, 2)))
            level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlockAndUpdate(
                pos.below(), EcologyModule.LIVING_SUBSTRATE.get().defaultBlockState());
        level.setBlockAndUpdate(pos, VigilModule.NEST.get().defaultBlockState());
        NestRig nest = new NestRig(level.getBlockEntity(pos));
        nest.insert(Biomass.of(8000), false);
        nest.seed();
        nest.insert(Biomass.of(16000), false);
        nest.action(9);
        return nest;
    }

    private static CompoundTag saved(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos).saveWithoutMetadata(level.registryAccess());
    }

    @GameTest(templateNamespace = "infestusfrontier_restart", template = "empty", timeoutTicks = 260)
    public static void beforeRestartTwoNestsKeepARealNetherPlantRunning(GameTestHelper h) {
        if (!"before".equals(System.getProperty(PHASE))) {
            h.succeed();
            return;
        }
        ServerLevel nether = h.getLevel().getServer().getLevel(Level.NETHER);
        NestRig home = build(h.getLevel(), HOME);
        NestRig remote = build(nether, NETHER);
        NestRig sheltered = build(h.getLevel(), HOME.south(6));
        ((VigilKeeper) h.getLevel().getEntity(sheltered.saved().getUUID("keeper"))).setHealth(12.5F);
        sheltered.action(9);
        h.assertTrue(home.enabled() && remote.enabled(), "both bases explicitly paid");
        BlockPos core = NETHER.east(4);
        nether.setBlockAndUpdate(
                core.below(), EcologyModule.LIVING_SUBSTRATE.get().defaultBlockState());
        nether.setBlockAndUpdate(core, PowerModule.HEART.get().defaultBlockState());
        nether.setBlockAndUpdate(
                core.east(),
                PowerModule.COLLECTOR.get().defaultBlockState().setValue(DirectionalBlock.FACING, Direction.WEST));
        nether.setBlockAndUpdate(core.east().below(), Blocks.LAVA.defaultBlockState());
        nether.setBlockAndUpdate(
                core.west(),
                PowerModule.BOILER.get().defaultBlockState().setValue(DirectionalBlock.FACING, Direction.EAST));
        nether.setBlockAndUpdate(
                core.north(),
                PowerModule.GENERATOR.get().defaultBlockState().setValue(DirectionalBlock.FACING, Direction.SOUTH));
        nether.getCapability(Capabilities.FluidHandler.BLOCK, core.west(), Direction.UP)
                .fill(
                        new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 16000),
                        IFluidHandler.FluidAction.EXECUTE);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(
                        saved(nether, core).getCompound("power").getInt("charge") > 0,
                        "plant produces in its nest-loaded chunk"))
                .thenIdle(120)
                .thenExecute(() -> {
                    h.assertTrue(home.enabled() && remote.enabled(), "both keepers survive until shutdown evidence");
                    Properties evidence = new Properties();
                    evidence.setProperty(
                            "home_keeper",
                            saved(h.getLevel(), HOME).getUUID("keeper").toString());
                    evidence.setProperty(
                            "nether_keeper",
                            saved(nether, NETHER).getUUID("keeper").toString());
                    evidence.setProperty(
                            "cycles",
                            Integer.toString(
                                    saved(nether, core).getCompound("power").getInt("cycles")));
                    try (Writer out = Files.newBufferedWriter(
                            h.getLevel().getServer().getServerDirectory().resolve("vigil-restart.properties"))) {
                        evidence.store(out, "paid nests; no vanilla force tickets");
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_restart", template = "empty", timeoutTicks = 200)
    public static void afterRestartTicketsKeepTheSameCreaturesAndPowerHistory(GameTestHelper h) {
        if (!"after".equals(System.getProperty(PHASE))) {
            h.succeed();
            return;
        }
        Properties evidence = new Properties();
        try (Reader in = Files.newBufferedReader(
                h.getLevel().getServer().getServerDirectory().resolve("vigil-restart.properties"))) {
            evidence.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        ServerLevel nether = h.getLevel().getServer().getLevel(Level.NETHER);
        h.startSequence()
                .thenWaitUntil(() -> {
                    h.assertTrue(h.getLevel().hasChunkAt(HOME), "overworld loaded only by restored nest tickets");
                    h.assertTrue(nether.hasChunkAt(NETHER), "Nether loaded only by restored nest tickets");
                })
                .thenExecute(() -> {
                    NestRig sheltered = new NestRig(h.getLevel().getBlockEntity(HOME.south(6)));
                    h.assertValueEqual(
                            sheltered.saved().getFloat("health"), 12.5F, "sheltered injuries survive server restart");
                    sheltered.action(9);
                    h.assertValueEqual(
                            ((VigilKeeper) h.getLevel()
                                            .getEntity(sheltered.saved().getUUID("keeper")))
                                    .getHealth(),
                            12.5F,
                            "waking restored nest cannot heal");
                    sheltered.action(9);
                    NestRig home = new NestRig(h.getLevel().getBlockEntity(HOME));
                    NestRig remote = new NestRig(nether.getBlockEntity(NETHER));
                    h.assertTrue(home.enabled() && remote.enabled(), "both still paid and active");
                    h.assertValueEqual(
                            saved(h.getLevel(), HOME).getUUID("keeper").toString(),
                            evidence.getProperty("home_keeper"),
                            "same saved home creature");
                    h.assertValueEqual(
                            saved(nether, NETHER).getUUID("keeper").toString(),
                            evidence.getProperty("nether_keeper"),
                            "same saved Nether creature");
                    h.assertTrue(
                            saved(nether, NETHER.east(4)).getCompound("power").getInt("cycles")
                                    >= Integer.parseInt(evidence.getProperty("cycles")),
                            "power history retained");
                    h.assertFalse(
                            h.getLevel().getForcedChunks().contains(new ChunkPos(HOME).toLong()), "not vanilla forced");
                })
                .thenIdle(45)
                .thenExecute(() -> {
                    h.assertTrue(
                            new NestRig(nether.getBlockEntity(NETHER)).enabled(),
                            "saved keeper passed startup validation");
                    new NestRig(h.getLevel().getBlockEntity(HOME)).action(9);
                    new NestRig(nether.getBlockEntity(NETHER)).action(9);
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_restart", template = "empty", timeoutTicks = 280)
    public static void beforeRestartLeaveAStarvingKeeperAlive(GameTestHelper h) {
        if (!"before".equals(System.getProperty(PHASE))) {
            h.succeed();
            return;
        }
        NestRig nest = build(h.getLevel(), STARVING);
        nest.extract(nest.tank().stored(), false);
        VigilKeeper keeper = (VigilKeeper) h.getLevel().getEntity(nest.saved().getUUID("keeper"));
        keeper.setHealth(12.5F);
        keeper.setInvulnerable(true);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(
                        nest.saved().getInt("hungry") >= 40, "starving remote keeper has begun emergency allowance"))
                .thenExecute(() -> {
                    Properties evidence = new Properties();
                    evidence.setProperty("keeper", keeper.getUUID().toString());
                    evidence.setProperty("hungry", Integer.toString(nest.saved().getInt("hungry")));
                    try (Writer out = Files.newBufferedWriter(
                            h.getLevel().getServer().getServerDirectory().resolve("vigil-hunger.properties"))) {
                        evidence.store(out, "starving keeper, no vanilla force tickets");
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "infestusfrontier_restart", template = "empty", timeoutTicks = 340)
    public static void afterRestartHungerAndInjuriesPersistUntilDeath(GameTestHelper h) {
        if (!"after".equals(System.getProperty(PHASE))) {
            h.succeed();
            return;
        }
        Properties evidence = new Properties();
        try (Reader in = Files.newBufferedReader(
                h.getLevel().getServer().getServerDirectory().resolve("vigil-hunger.properties"))) {
            evidence.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(
                        h.getLevel().hasChunkAt(STARVING), "finite emergency tickets restored remote nest"))
                .thenWaitUntil(() -> h.assertTrue(
                        h.getLevel().getEntity(java.util.UUID.fromString(evidence.getProperty("keeper")))
                                instanceof VigilKeeper,
                        "same saved entity loaded"))
                .thenExecute(() -> {
                    NestRig nest = new NestRig(h.getLevel().getBlockEntity(STARVING));
                    h.assertTrue(
                            nest.saved().getInt("hungry") >= Integer.parseInt(evidence.getProperty("hungry")) + 40,
                            "restart consumes emergency allowance instead of resetting it");
                    VigilKeeper keeper =
                            (VigilKeeper) h.getLevel().getEntity(nest.saved().getUUID("keeper"));
                    h.assertValueEqual(keeper.getHealth(), 12.5F, "active injuries survived");
                    // Startup may already spend most of the finite ticket allowance. Keep this test's
                    // chunk loaded ordinarily so it can observe damage even after those tickets expire.
                    ChunkPos chunk = new ChunkPos(STARVING);
                    h.getLevel().setChunkForced(chunk.x, chunk.z, true);
                    keeper.setInvulnerable(false);
                })
                .thenWaitUntil(() -> h.assertFalse(
                        new NestRig(h.getLevel().getBlockEntity(STARVING)).egg(),
                        "restored starvation kills keeper and consumes egg"))
                .thenExecute(() -> {
                    ChunkPos chunk = new ChunkPos(STARVING);
                    h.getLevel().setChunkForced(chunk.x, chunk.z, false);
                })
                .thenSucceed();
    }
}
