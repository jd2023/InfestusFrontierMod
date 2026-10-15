package org.jd.infestusfrontier.testmod.capture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Drives a disposable client through the capture scenarios: fresh flat world, commands, one screenshot per view,
 * then exit. Active only when {@code infestusfrontier.capture.dir} is set; a normal client run never sees it.
 */
@Mod(value = "infestusfrontier_tests", dist = Dist.CLIENT)
public final class CaptureClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String WORLD = "capture";
    private static final int WIDTH = 854;
    private static final int HEIGHT = 480;
    private static final int STAGE_TIMEOUT_TICKS = 600;
    private static final int SETTLE_TICKS = 40;
    private static final int MOVE_TICKS = 10;
    private static final List<String> BASELINE = List.of(
            "gamerule sendCommandFeedback false",
            "gamerule doDaylightCycle false",
            "gamerule doWeatherCycle false",
            "gamerule doMobSpawning false",
            "time set noon",
            "weather clear",
            "clear @s");

    private final Path outputDir;
    private final List<Scenario> scenarios;
    private final Deque<String> commandQueue = new ArrayDeque<>();
    private final JsonArray report = new JsonArray();
    private Stage stage = Stage.TITLE;
    private int ticksInStage;
    private int scenarioIndex;
    private int viewIndex;
    private boolean frameReady;
    private boolean resized;

    public CaptureClient() {
        String dir = System.getProperty("infestusfrontier.capture.dir");
        if (dir == null) {
            outputDir = null;
            scenarios = List.of();
            return;
        }
        outputDir = Path.of(dir);
        List<Scenario> loaded;
        try {
            loaded = Scenario.loadAll(System.getProperty("infestusfrontier.capture.scenarios", ""));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        scenarios = loaded;
        NeoForge.EVENT_BUS.addListener(this::onTick);
        NeoForge.EVENT_BUS.addListener(this::onLevelRendered);
        NeoForge.EVENT_BUS.addListener(this::onFrame);
    }

    private void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (++ticksInStage > STAGE_TIMEOUT_TICKS && stage != Stage.DONE) {
                throw new IllegalStateException("Timed out in stage " + stage);
            }
            switch (stage) {
                case TITLE -> createWorld(minecraft);
                case JOIN -> join(minecraft);
                case COMMANDS -> sendNextCommand(minecraft);
                case SETTLE -> {
                    if (ticksInStage >= SETTLE_TICKS) moveToView(minecraft);
                }
                case MOVE -> {
                    if (ticksInStage >= MOVE_TICKS) enter(Stage.FRAME);
                }
                case FRAME, GRABBING -> {}
                case DISCONNECT -> disconnect(minecraft);
                case STOP -> stop(minecraft);
                case DONE -> {}
            }
        } catch (Exception e) {
            fail(minecraft, e);
        }
    }

    private void createWorld(Minecraft minecraft) {
        if (!minecraft.isGameLoadFinished()
                || !(minecraft.screen instanceof TitleScreen)
                || minecraft.getOverlay() != null) {
            if (ticksInStage % 100 == 0) {
                LOGGER.info(
                        "INFESTUS_CAPTURE_WAIT loaded={} screen={} overlay={}",
                        minecraft.isGameLoadFinished(),
                        minecraft.screen == null
                                ? null
                                : minecraft.screen.getClass().getSimpleName(),
                        minecraft.getOverlay() == null
                                ? null
                                : minecraft.getOverlay().getClass().getSimpleName());
            }
            return;
        }
        if (!resized) {
            // One attempt only: a scaled or tiled desktop may give another size, which is still usable.
            resized = true;
            minecraft.getWindow().setWindowed(WIDTH, HEIGHT);
            return;
        }
        LOGGER.info(
                "INFESTUS_CAPTURE_TITLE window={}x{}",
                minecraft.getWindow().getWidth(),
                minecraft.getWindow().getHeight());
        minecraft.options.cloudStatus().set(CloudStatus.OFF);
        minecraft.options.bobView().set(false);
        minecraft.options.guiScale().set(2);
        minecraft.resizeDisplay();
        LevelSettings settings = new LevelSettings(
                WORLD,
                GameType.CREATIVE,
                false,
                Difficulty.PEACEFUL,
                true,
                new GameRules(),
                WorldDataConfiguration.DEFAULT);
        WorldOptions options = new WorldOptions(0L, false, false);
        LOGGER.info("INFESTUS_CAPTURE_WORLD_CREATE scenarios={}", scenarios.size());
        minecraft
                .createWorldOpenFlows()
                .createFreshLevel(
                        WORLD,
                        settings,
                        options,
                        registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT)
                                .value()
                                .createWorldDimensions(),
                        new TitleScreen());
        enter(Stage.JOIN);
    }

    private void join(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null) return;
        var chunk = minecraft.player.chunkPosition();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (!minecraft.level.hasChunk(chunk.x + x, chunk.z + z)) return;
            }
        }
        LOGGER.info("INFESTUS_CAPTURE_JOINED");
        commandQueue.addAll(BASELINE);
        commandQueue.addAll(scenarios.get(scenarioIndex).commands());
        enter(Stage.COMMANDS);
    }

    private void sendNextCommand(Minecraft minecraft) {
        String command = commandQueue.poll();
        if (command == null) {
            enter(Stage.SETTLE);
            return;
        }
        if (!minecraft.player.connection.sendUnsignedCommand(command)) {
            throw new IllegalStateException("Command rejected by the client parser: " + command);
        }
    }

    private void moveToView(Minecraft minecraft) {
        Scenario scenario = scenarios.get(scenarioIndex);
        Scenario.View view = scenario.views().get(viewIndex);
        String tp = String.format(
                Locale.ROOT, "tp @s %.3f %.3f %.3f %.2f %.2f", view.x(), view.y(), view.z(), view.yaw(), view.pitch());
        if (!minecraft.player.connection.sendUnsignedCommand(tp)) throw new IllegalStateException("Bad view: " + tp);
        minecraft.options.hideGui = !view.gui();
        minecraft.gui.getChat().clearMessages(true);
        frameReady = false;
        enter(Stage.MOVE);
    }

    private void onLevelRendered(RenderLevelStageEvent event) {
        if (stage != Stage.FRAME || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        Scenario.View view = scenarios.get(scenarioIndex).views().get(viewIndex);
        boolean positioned = minecraft.player.position().distanceToSqr(view.x(), view.y(), view.z()) < 0.01;
        boolean aimed = Math.abs(wrap(event.getCamera().getYRot() - view.yaw())) < 0.01f
                && Math.abs(event.getCamera().getXRot() - view.pitch()) < 0.01f;
        var renderer = event.getLevelRenderer();
        if (positioned && aimed && renderer.countRenderedSections() > 0 && renderer.hasRenderedAllSections()) {
            frameReady = true;
        }
    }

    private void onFrame(RenderFrameEvent.Post event) {
        if (stage != Stage.FRAME || !frameReady) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) return;
        Scenario scenario = scenarios.get(scenarioIndex);
        Scenario.View view = scenario.views().get(viewIndex);
        enter(Stage.GRABBING);
        try {
            Path scenarioDir = outputDir.resolve(scenario.name());
            Files.createDirectories(scenarioDir);
            String file = view.name() + ".png";
            Screenshot.grab(
                    scenarioDir.toFile(),
                    file,
                    minecraft.getMainRenderTarget(),
                    message -> minecraft.execute(() -> {
                        try {
                            Path grabbed = scenarioDir.resolve("screenshots");
                            Files.move(
                                    grabbed.resolve(file),
                                    scenarioDir.resolve(file),
                                    StandardCopyOption.REPLACE_EXISTING);
                            Files.deleteIfExists(grabbed);
                            JsonObject entry = new JsonObject();
                            entry.addProperty("scenario", scenario.name());
                            entry.addProperty("view", view.name());
                            entry.addProperty("file", scenarioDir.resolve(file).toString());
                            report.add(entry);
                            LOGGER.info("INFESTUS_CAPTURE scenario={} view={}", scenario.name(), view.name());
                            advance(minecraft);
                        } catch (IOException e) {
                            fail(minecraft, e);
                        }
                    }));
        } catch (IOException e) {
            fail(minecraft, e);
        }
    }

    private void advance(Minecraft minecraft) {
        viewIndex++;
        if (viewIndex < scenarios.get(scenarioIndex).views().size()) {
            moveToView(minecraft);
            return;
        }
        viewIndex = 0;
        scenarioIndex++;
        if (scenarioIndex < scenarios.size()) {
            commandQueue.addAll(scenarios.get(scenarioIndex).commands());
            enter(Stage.COMMANDS);
            return;
        }
        enter(Stage.DISCONNECT);
    }

    private void disconnect(Minecraft minecraft) {
        minecraft.options.hideGui = false;
        if (minecraft.level != null) minecraft.level.disconnect();
        minecraft.disconnect(new TitleScreen());
        enter(Stage.STOP);
    }

    private void stop(Minecraft minecraft) throws IOException {
        if (minecraft.level != null || minecraft.player != null || !(minecraft.screen instanceof TitleScreen)) return;
        writeReport(null);
        LOGGER.info("INFESTUS_CAPTURE_DONE captures={}", report.size());
        enter(Stage.DONE);
        minecraft.stop();
    }

    private void fail(Minecraft minecraft, Exception e) {
        LOGGER.error("INFESTUS_CAPTURE_FAILURE stage={}", stage, e);
        try {
            writeReport(stage + ": " + e);
        } catch (IOException io) {
            LOGGER.error("INFESTUS_CAPTURE_FAILURE report", io);
        }
        stage = Stage.DONE;
        minecraft.stop();
    }

    private void writeReport(String failure) throws IOException {
        Files.createDirectories(outputDir);
        JsonObject root = new JsonObject();
        root.add("captures", report);
        if (failure != null) root.addProperty("failure", failure);
        Files.writeString(outputDir.resolve("report.json"), root.toString(), StandardCharsets.UTF_8);
    }

    private void enter(Stage next) {
        stage = next;
        ticksInStage = 0;
    }

    private static float wrap(float degrees) {
        float d = degrees % 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    private enum Stage {
        TITLE,
        JOIN,
        COMMANDS,
        SETTLE,
        MOVE,
        FRAME,
        GRABBING,
        DISCONNECT,
        STOP,
        DONE
    }
}
