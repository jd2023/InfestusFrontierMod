package org.jd.infestusfrontier.testmod.integration;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.ServiceLoader;
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
import org.jd.infestusfrontier.testmod.integration.client.ContentVisualScenario;

/** Disposable-client automation used only by the integration harness. */
@Mod(value = "infestusfrontier_client", dist = Dist.CLIENT)
public final class BootstrapClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Path captureDirectory = Path.of(environment("INFESTUS_CAPTURE_DIR", "build/integration/capture"));
    private final String address = environment("INFESTUS_SERVER", "127.0.0.1:25565");
    private final String probeRole = environment("INFESTUS_PROBE_ROLE", "");
    private final Path coordination = Path.of(environment("INFESTUS_COORDINATION", "."));
    private final List<org.jd.infestusfrontier.testmod.integration.client.ContentMultiplayerScenario> multiplayer =
            ServiceLoader.load(org.jd.infestusfrontier.testmod.integration.client.ContentMultiplayerScenario.class)
                    .stream().map(ServiceLoader.Provider::get).toList();
    private Stage stage = Stage.TITLE_LOAD;
    private boolean worldRendered;
    private GuideScenarios guideScenarios;
    private final List<ContentVisualScenario> visuals = ServiceLoader.load(ContentVisualScenario.class)
            .stream().map(ServiceLoader.Provider::get).toList();

    private List<DetailCapture> detailViews = List.of();
    private int detailIndex;
    private List<UiCapture> uiCaptures = List.of();
    private int uiIndex;

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
                case WORLD_SETUP -> {
                    boolean ready = true;
                    for (var scene : visuals) ready &= scene.ready(minecraft);
                    if (ready) {
                        detailViews = visuals.stream().flatMap(scene -> scene.detailViews().stream()
                                .map(view -> new DetailCapture(scene, view))).toList();
                        uiCaptures = visuals.stream().flatMap(scene -> scene.uiViews().stream()
                                .map(view -> new UiCapture(scene, view))).toList();
                        var names = java.util.stream.Stream.concat(detailViews.stream().map(capture -> capture.view().filename()),
                                uiCaptures.stream().map(capture -> capture.view().filename())).toList();
                        if (names.size() > 28 || names.stream().distinct().count() != names.size()) {
                            throw new IllegalStateException("Excessive or duplicate detail captures");
                        }
                        stage = Stage.WORLD_RENDER;
                    }
                }
                case DETAILS -> {
                    if (detailIndex == detailViews.size()) stage = Stage.UI_SETUP;
                    else {
                        var capture = detailViews.get(detailIndex);
                        if (!capture.scenario().prepareView(minecraft, capture.view())) break;
                        var view = capture.view();
                        minecraft.player.setYRot(view.yaw());
                        minecraft.player.setXRot(view.pitch());
                        minecraft.player.yRotO = view.yaw();
                        minecraft.player.xRotO = view.pitch();
                        stage = Stage.DETAIL_RENDER;
                    }
                }
                case UI_SETUP -> prepareUi(minecraft);
                case MULTIPLAYER -> {
                    if (multiplayer.size() != 1) throw new IllegalStateException("Expected one multiplayer contributor");
                    if (multiplayer.getFirst().tick(minecraft, probeRole, coordination))
                        stage = probeRole.equals("observer") ? Stage.DISCONNECT : Stage.GUIDES;
                }
                case GUIDES -> {
                    if (guideScenarios == null) guideScenarios = new GuideScenarios();
                    if (guideScenarios.tick(minecraft)) stage = Stage.DISCONNECT;
                }
                case DISCONNECT -> disconnect(minecraft);
                case STOP -> stop(minecraft);
                case TITLE, WAIT, WORLD_RENDER, DETAIL_RENDER, UI_RENDER, DONE -> { }
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
        stage = probeRole.equals("observer") ? Stage.MULTIPLAYER : Stage.WORLD_SETUP;
    }

    private void worldRendered(RenderLevelStageEvent event) {
        boolean detail = stage == Stage.DETAIL_RENDER;
        float yaw = detail ? detailViews.get(detailIndex).view().yaw() : 0;
        float pitch = detail ? detailViews.get(detailIndex).view().pitch() : 15;
        if ((stage == Stage.WORLD_RENDER || detail) && event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL
                && worldReady(Minecraft.getInstance())
                && event.getLevelRenderer().countRenderedSections() > 0
                && event.getLevelRenderer().hasRenderedAllSections()
                && event.getCamera().getYRot() == yaw && event.getCamera().getXRot() == pitch) {
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
                grab(minecraft, "bootstrap-world.png", () -> stage = Stage.DETAILS);
            }
            if (stage == Stage.DETAIL_RENDER && worldRendered && minecraft.screen == null) {
                var view = detailViews.get(detailIndex).view();
                LOGGER.info("INFESTUS_CLIENT_DETAIL_RENDERED name={} yaw={} pitch={}",
                        view.filename(), view.yaw(), view.pitch());
                stage = Stage.WAIT;
                grab(minecraft, view.filename(), () -> {
                    detailViews.get(detailIndex).scenario().finishView(minecraft, view);
                    detailIndex++;
                    stage = Stage.DETAILS;
                });
            }
            if (stage == Stage.UI_RENDER && minecraft.screen != null) {
                var capture = uiCaptures.get(uiIndex);
                LOGGER.info("INFESTUS_CLIENT_UI_RENDERED name={} scale={}", capture.view().filename(), capture.view().guiScale());
                stage = Stage.WAIT;
                grab(minecraft, capture.view().filename(), () -> {
                    uiIndex++;
                    stage = Stage.UI_SETUP;
                });
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

    private void prepareUi(Minecraft minecraft) {
        if (uiIndex == uiCaptures.size()) {
            if (minecraft.screen != null) minecraft.setScreen(null);
            minecraft.resizeDisplay();
            stage = probeRole.isEmpty() ? Stage.GUIDES : Stage.MULTIPLAYER;
            return;
        }
        var capture = uiCaptures.get(uiIndex);
        if (minecraft.getWindow().getGuiScale() != capture.view().guiScale()) {
            minecraft.getWindow().setGuiScale(capture.view().guiScale());
            if (minecraft.screen != null) minecraft.screen.resize(minecraft,
                    minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        }
        if (capture.scenario().prepareUi(minecraft, capture.view())) stage = Stage.UI_RENDER;
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

    private record UiCapture(ContentVisualScenario scenario, ContentVisualScenario.UiView view) {}
    private record DetailCapture(ContentVisualScenario scenario, ContentVisualScenario.View view) {}
    private enum Stage { TITLE_LOAD, TITLE, WAIT, CONNECT, WORLD, WORLD_SETUP, WORLD_RENDER, DETAILS, DETAIL_RENDER, UI_SETUP, UI_RENDER, MULTIPLAYER, GUIDES, DISCONNECT, STOP, DONE }
}
