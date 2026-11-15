package org.jd.infestusfrontier.testmod;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;

/**
 * A loaded idle batch organ notices a completed server data reload and looks again at its unchanged inputs once. Each
 * test runs in a batch of its own because it changes the server's recipes for a while; see {@link RecipeReloads}.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class RecipeReloadTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos RACK = GROUND.above();
    private static final int SHEET_TICKS = 400;
    private static final String AFFORD = "recipe_reload_afford";
    private static final String IN_FLIGHT = "recipe_reload_in_flight";

    private RecipeReloadTests() {}

    @AfterBatch(batch = AFFORD)
    public static void restoreAfterAfford(ServerLevel level) {
        RecipeReloads.restore(level.getServer());
    }

    @AfterBatch(batch = IN_FLIGHT)
    public static void restoreAfterInFlight(ServerLevel level) {
        RecipeReloads.restore(level.getServer());
    }

    /** The fixture: a Rack that holds two batches of ingredients and 50 mB of water, which the shipped recipe refuses. */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = AFFORD,
            timeoutTicks = SHEET_TICKS + 200)
    public static void reloadedCheaperRecipeStartsASleepingRack(GameTestHelper h) {
        placeRack(h, 2, 50);
        CompletableFuture<?>[] reload = new CompletableFuture<?>[1];
        BatchOrganContents[] refused = new BatchOrganContents[1];
        h.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    refused[0] = contents(h);
                    h.assertTrue(refused[0].batch().isEmpty(), "100 mB of water are asked and 50 are held");
                    h.assertValueEqual(refused[0].waterMb(), 50, "the water is untouched");
                    reload[0] = RecipeReloads.reload(h.getLevel().getServer());
                })
                .thenWaitUntil(() -> done(h, reload[0]))
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertValueEqual(
                            contents(h), refused[0], "a reload that changes nothing starts nothing and spends nothing");
                    reload[0] = RecipeReloads.reloadWithWater(h.getLevel().getServer(), 50);
                })
                .thenWaitUntil(() -> done(h, reload[0]))
                .thenIdle(2)
                .thenExecute(() -> {
                    BatchOrganContents started = contents(h);
                    h.assertTrue(started.batch().isPresent(), "the reload made the batch affordable and it started");
                    h.assertValueEqual(started.waterMb(), 0, "the reduced 50 mB were spent");
                    h.assertValueEqual(started.ingredients().get(0).getCount(), 1, "one flesh spent");
                    h.assertValueEqual(started.ingredients().get(1).getCount(), 1, "one string spent");
                    h.assertTrue(started.output().isEmpty(), "nothing is made at the start");
                })
                .thenIdle(SHEET_TICKS)
                .thenExecute(() -> {
                    BatchOrganContents done = contents(h);
                    h.assertTrue(done.batch().isEmpty(), "the one batch finished and no second one started");
                    h.assertTrue(done.output().is(ProcessingModule.MEMBRANE_SHEET.get()), "a Membrane Sheet");
                    h.assertValueEqual(done.output().getCount(), 1, "exactly one batch's output");
                    h.assertValueEqual(done.history().completedBatches(), 1, "one batch counted");
                    h.assertValueEqual(done.waterMb(), 0, "no more water was spent");
                    h.assertValueEqual(done.ingredients().get(0).getCount(), 1, "one flesh left");
                    h.assertValueEqual(done.ingredients().get(1).getCount(), 1, "one string left");
                    reload[0] = RecipeReloads.restore(h.getLevel().getServer());
                })
                .thenWaitUntil(() -> done(h, reload[0]))
                .thenExecute(() -> {
                    fill(h, 50);
                    h.assertTrue(
                            contents(h).batch().isEmpty(), "the shipped recipe is back: 50 mB cannot pay it again");
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertTrue(contents(h).batch().isEmpty(), "the restored recipe still asks 100 mB");
                    h.succeed();
                });
    }

    /** A batch already running keeps what it was started with; only the next one is read from the reloaded recipe. */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = IN_FLIGHT,
            timeoutTicks = SHEET_TICKS + 200)
    public static void reloadLeavesARunningBatchAlone(GameTestHelper h) {
        placeRack(h, 2, 1000);
        CompletableFuture<?>[] reload = new CompletableFuture<?>[1];
        h.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    BatchOrganContents running = contents(h);
                    h.assertTrue(running.batch().isPresent(), "a batch runs");
                    h.assertValueEqual(running.waterMb(), 900, "it was started at 100 mB");
                    reload[0] = RecipeReloads.reloadWithWater(h.getLevel().getServer(), 50);
                })
                .thenWaitUntil(() -> done(h, reload[0]))
                .thenIdle(5)
                .thenExecute(() -> {
                    BatchOrganContents running = contents(h);
                    h.assertTrue(running.batch().isPresent(), "the same batch still runs");
                    h.assertValueEqual(running.waterMb(), 900, "the reload spent and refunded nothing");
                    h.assertValueEqual(running.ingredients().get(0).getCount(), 1, "one flesh is spent");
                })
                .thenIdle(SHEET_TICKS)
                .thenExecute(() -> {
                    BatchOrganContents next = contents(h);
                    h.assertValueEqual(next.output().getCount(), 1, "the saved batch finished once");
                    h.assertValueEqual(next.history().completedBatches(), 1, "and was counted once");
                    h.assertTrue(next.batch().isPresent(), "the next batch started");
                    h.assertValueEqual(next.waterMb(), 850, "the next batch costs the reloaded 50 mB");
                    reload[0] = RecipeReloads.restore(h.getLevel().getServer());
                })
                .thenWaitUntil(() -> done(h, reload[0]))
                .thenSucceed();
    }

    private static void done(GameTestHelper h, CompletableFuture<?> reload) {
        if (reload.isCompletedExceptionally()) h.fail("the reload failed");
        h.assertTrue(reload.isDone(), "the reload has not completed");
    }

    private static void placeRack(GameTestHelper h, int sets, int waterMb) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(RACK, ProcessingModule.MEMBRANE_RACK.get());
        load(h, Items.ROTTEN_FLESH, sets);
        load(h, Items.STRING, sets);
        fill(h, waterMb);
    }

    private static void load(GameTestHelper h, Item item, int count) {
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, item, count);
        for (int i = 0; i < count; i++) EcologyTests.use(h, player, RACK, Direction.UP);
    }

    private static void fill(GameTestHelper h, int waterMb) {
        IFluidHandler tank =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(RACK), Direction.UP);
        h.assertValueEqual(
                tank.fill(new FluidStack(Fluids.WATER, waterMb), IFluidHandler.FluidAction.EXECUTE),
                waterMb,
                "the water went in");
    }

    private static BatchOrganContents contents(GameTestHelper h) {
        var tag = h.getBlockEntity(RACK).saveWithoutMetadata(h.getLevel().registryAccess());
        return BatchOrganContents.CODEC
                .parse(
                        h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                        tag.get("contents"))
                .getOrThrow();
    }
}
