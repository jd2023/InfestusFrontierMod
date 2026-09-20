package org.jd.infestusfrontier.testmod.integration;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.jd.infestusfrontier.testmod.integration.client.GuideScenarios;

/** Disposable-client automation used only by the integration harness. */
@Mod(value = "infestusfrontier_client", dist = Dist.CLIENT)
public final class BootstrapClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Path captureDirectory = Path.of(environment("INFESTUS_CAPTURE_DIR", "build/integration/capture"));
    private final String address = environment("INFESTUS_SERVER", "127.0.0.1:25565");
    private Stage stage = Stage.TITLE_LOAD;
    private boolean worldRendered;
    private GuideScenarios guideScenarios;

    public BootstrapClient() {
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::worldRendered);
        NeoForge.EVENT_BUS.addListener(this::frameRendered);
    }

    private static String environment(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private void tick(ClientTickEvent.Post ignored) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            switch (stage) {
                case TITLE_LOAD -> prepareTitle(minecraft);
                case CONNECT -> connect(minecraft);
                case WORLD -> captureWorld(minecraft);
                case GUIDES -> {
                    if (guideScenarios == null) guideScenarios = new GuideScenarios();
                    if (guideScenarios.tick(minecraft)) stage = Stage.DISCONNECT;
                }
                case DISCONNECT -> disconnect(minecraft);
                case STOP -> stop(minecraft);
                case TITLE, WAIT, WORLD_RENDER, DONE -> { }
            }
        } catch (Exception exception) {
            LOGGER.error("INFESTUS_CLIENT_FAILURE stage={}", stage, exception);
            minecraft.stop();
            stage = Stage.DONE;
        }
    }

    private void prepareTitle(Minecraft minecraft) {
        if (minecraft.isGameLoadFinished() && minecraft.screen instanceof TitleScreen && minecraft.getOverlay() == null) {
            minecraft.setScreen(new TitleScreen(false));
            stage = Stage.TITLE;
        }
    }

    private void captureTitle(Minecraft minecraft) throws IOException {
        if (!minecraft.isGameLoadFinished() || !(minecraft.screen instanceof TitleScreen) || minecraft.getOverlay() != null) {
            return;
        }
        if (minecraft.getWindow().getWidth() != 1280 || minecraft.getWindow().getHeight() != 720) {
            minecraft.getWindow().setWindowed(1280, 720);
            return;
        }
        Files.createDirectories(captureDirectory);
        LOGGER.info("INFESTUS_CLIENT_TITLE_READY screen={}", minecraft.screen.getClass().getName());
        grab(minecraft, "bootstrap-title.png", () -> stage = Stage.CONNECT);
        stage = Stage.WAIT;
    }

    private void connect(Minecraft minecraft) {
        ServerData server = new ServerData("Infestus integration", address, ServerData.Type.OTHER);
        ConnectScreen.startConnecting(new TitleScreen(), minecraft, ServerAddress.parseString(address), server, false, null);
        LOGGER.info("INFESTUS_CLIENT_CONNECT_BEGIN address={}", address);
        stage = Stage.WORLD;
    }

    private void captureWorld(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null
                || !minecraft.player.onGround()) return;
        minecraft.player.setYRot(0);
        minecraft.player.setXRot(15);
        minecraft.player.yRotO = 0;
        minecraft.player.xRotO = 15;
        minecraft.options.hideGui = false;
        LOGGER.info("INFESTUS_CLIENT_PLAY_ENTER name={} uuid={}", minecraft.player.getGameProfile().getName(), minecraft.player.getUUID());
        stage = Stage.WORLD_RENDER;
    }

    private void worldRendered(RenderLevelStageEvent event) {
        if (stage == Stage.WORLD_RENDER && event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL
                && worldReady(Minecraft.getInstance())
                && event.getLevelRenderer().countRenderedSections() > 0
                && event.getLevelRenderer().hasRenderedAllSections()
                && event.getCamera().getYRot() == 0 && event.getCamera().getXRot() == 15) {
            worldRendered = true;
        }
    }

    private boolean worldReady(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) return false;
        var chunk = minecraft.player.chunkPosition();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (!minecraft.level.hasChunk(chunk.x + x, chunk.z + z)) return false;
            }
        }
        return true;
    }

    private void frameRendered(RenderFrameEvent.Post ignored) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (stage == Stage.TITLE) captureTitle(minecraft);
            if (stage == Stage.WORLD_RENDER && worldRendered && minecraft.screen == null) {
                LOGGER.info("INFESTUS_CLIENT_CAMERA_RENDERED yaw=0.0 pitch=15.0");
                stage = Stage.WAIT;
                grab(minecraft, "bootstrap-world.png", () -> stage = Stage.GUIDES);
            }
        } catch (Exception exception) {
            LOGGER.error("INFESTUS_CLIENT_FAILURE render", exception);
            stage = Stage.DONE;
            minecraft.stop();
        }
        worldRendered = false;
    }

    private void disconnect(Minecraft minecraft) {
        minecraft.disconnect(new TitleScreen());
        LOGGER.info("INFESTUS_CLIENT_PLAY_EXIT");
        stage = Stage.STOP;
    }

    private void stop(Minecraft minecraft) {
        if (minecraft.level != null || minecraft.player != null || !(minecraft.screen instanceof TitleScreen)) return;
        stage = Stage.DONE;
        minecraft.stop();
    }

    private void grab(Minecraft minecraft, String name, Runnable complete) {
        Path temporary = captureDirectory.resolve("screenshots").resolve(name);
        Screenshot.grab(captureDirectory.toFile(), name, minecraft.getMainRenderTarget(), message -> {
            try {
                Files.move(temporary, captureDirectory.resolve(name), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("INFESTUS_CLIENT_CAPTURE name={} message={}", name, message.getString());
                minecraft.execute(complete);
            } catch (IOException exception) {
                LOGGER.error("INFESTUS_CLIENT_FAILURE capture={}", name, exception);
                minecraft.stop();
            }
        });
    }

    private enum Stage { TITLE_LOAD, TITLE, WAIT, CONNECT, WORLD, WORLD_RENDER, GUIDES, DISCONNECT, STOP, DONE }
}
