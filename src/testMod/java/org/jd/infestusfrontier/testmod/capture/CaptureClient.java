package org.jd.infestusfrontier.testmod.capture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.klikli_dev.modonomicon.book.Book;
import com.klikli_dev.modonomicon.book.entries.BookEntry;
import com.klikli_dev.modonomicon.bookstate.BookUnlockStateManager;
import com.klikli_dev.modonomicon.client.gui.BookGuiManager;
import com.klikli_dev.modonomicon.client.gui.book.BookAddress;
import com.klikli_dev.modonomicon.data.BookDataManager;
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
import java.util.Optional;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Drives a disposable client through the capture scenarios: fresh flat world, commands, one screenshot per view,
 * then exit. Started by {@link TestClient} only when {@code infestusfrontier.capture.dir} is set.
 */
final class CaptureClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String WORLD = "capture";
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int STAGE_TIMEOUT_TICKS = 600;
    private static final int SETTLE_TICKS = 40;
    private static final int MOVE_TICKS = 10;
    private static final int WORLD_SCALE = 2; // the HUD stays small in a world shot
    private static final int SCREEN_SCALE = 3; // a screen is shot the size a player sees it
    private static final int READY_RADIUS = 2; // chunks around the view that must have arrived from the server
    private static final int READY_FRAMES = 10; // consecutive frames with nothing left to compile
    private static final List<String> BASELINE = List.of(
            "gamerule sendCommandFeedback false",
            "gamerule doDaylightCycle false",
            "gamerule doWeatherCycle false",
            "gamerule doMobSpawning false",
            "time set noon",
            "weather clear");
    /** Runs before every scenario's own commands, so nothing carries over from the scenario before. */
    private static final String RESET = "clear @s";

    private final Path outputDir;
    private final List<Scenario> scenarios;
    private final Deque<String> commandQueue = new ArrayDeque<>();
    private final JsonArray report = new JsonArray();
    private Stage stage = Stage.TITLE;
    private int ticksInStage;
    private int scenarioIndex;
    private int viewIndex;
    private boolean frameReady;
    private int readyFrames;
    private boolean resized;

    CaptureClient(Path outputDir, List<Scenario> scenarios) {
        this.outputDir = outputDir;
        this.scenarios = scenarios;
        NeoForge.EVENT_BUS.addListener(this::onTick);
        NeoForge.EVENT_BUS.addListener(this::onLevelRendered);
        NeoForge.EVENT_BUS.addListener(this::onFrame);
    }

    private void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (++ticksInStage > STAGE_TIMEOUT_TICKS && stage != Stage.DONE) {
                throw new IllegalStateException("Timed out in stage " + stage + where(minecraft));
            }
            switch (stage) {
                case TITLE -> createWorld(minecraft);
                case JOIN -> join(minecraft);
                case COMMANDS -> sendNextCommand(minecraft);
                case SETTLE -> {
                    if (ticksInStage >= SETTLE_TICKS) moveToView(minecraft);
                }
                case MOVE -> {
                    holdInAir(minecraft);
                    if (ticksInStage >= MOVE_TICKS) {
                        currentView()
                                .use()
                                .ifPresent(block ->
                                        useBlock(minecraft, block, currentView().holding()));
                        if (currentView().inventory()) minecraft.setScreen(new InventoryScreen(minecraft.player));
                        currentView()
                                .book()
                                .ifPresent(
                                        entry -> openBook(entry, currentView().page()));
                        // The selection ring would be measured as icon pixels: park it on the last slot.
                        if (!currentView().icons().isEmpty()) minecraft.player.getInventory().selected = 8;
                        enter(Stage.FRAME);
                    }
                }
                case FRAME, GRABBING -> holdInAir(minecraft);
                case DISCONNECT -> disconnect(minecraft);
                case STOP -> stop(minecraft);
                case DONE -> {}
            }
        } catch (Exception e) {
            fail(minecraft, e);
        }
    }

    private void createWorld(Minecraft minecraft) {
        if (!FlatWorld.atTitle(minecraft)) {
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
        guiScale(minecraft, WORLD_SCALE);
        LOGGER.info("INFESTUS_CAPTURE_WORLD_CREATE scenarios={}", scenarios.size());
        FlatWorld.create(minecraft, WORLD);
        enter(Stage.JOIN);
    }

    private void join(Minecraft minecraft) {
        if (!FlatWorld.joined(minecraft)) return;
        LOGGER.info("INFESTUS_CAPTURE_JOINED");
        commandQueue.addAll(BASELINE);
        commandQueue.add(RESET);
        commandQueue.addAll(scenarios.get(scenarioIndex).commands());
        enter(Stage.COMMANDS);
    }

    private void sendNextCommand(Minecraft minecraft) {
        String command = commandQueue.poll();
        if (command == null) {
            enter(Stage.SETTLE);
            return;
        }
        FlatWorld.command(minecraft, command);
    }

    private void moveToView(Minecraft minecraft) {
        Scenario scenario = scenarios.get(scenarioIndex);
        Scenario.View view = scenario.views().get(viewIndex);
        String tp = String.format(
                Locale.ROOT, "tp @s %.3f %.3f %.3f %.2f %.2f", view.x(), view.y(), view.z(), view.yaw(), view.pitch());
        FlatWorld.command(minecraft, tp);
        minecraft.options.hideGui = !view.gui();
        guiScale(minecraft, view.opensScreen() ? SCREEN_SCALE : WORLD_SCALE);
        minecraft.gui.getChat().clearMessages(true);
        minecraft.getToasts().clear(); // "New recipes unlocked" after a give
        frameReady = false;
        readyFrames = 0;
        enter(Stage.MOVE);
    }

    private static void guiScale(Minecraft minecraft, int scale) {
        if (minecraft.options.guiScale().get() == scale) return;
        minecraft.options.guiScale().set(scale);
        minecraft.resizeDisplay();
    }

    private Scenario.View currentView() {
        return scenarios.get(scenarioIndex).views().get(viewIndex);
    }

    /** Opens the guide at {@code entry} (an entry id, or empty for the book's first screen) on page pair {@code page}. */
    private static void openBook(String entry, int page) {
        ResourceLocation bookId = ResourceLocation.fromNamespaceAndPath("infestusfrontier", "the_waking_genome");
        Book book = BookDataManager.get().getBook(bookId);
        if (book == null) throw new IllegalStateException("book " + bookId + " not loaded");
        if (entry.isEmpty()) {
            BookGuiManager.get().openBook(BookAddress.defaultFor(book));
            return;
        }
        ResourceLocation entryId = ResourceLocation.parse(entry);
        String category = entryId.getPath().substring(0, entryId.getPath().indexOf('/'));
        BookEntry bookEntry = book.getCategory(ResourceLocation.fromNamespaceAndPath(entryId.getNamespace(), category))
                .getEntries()
                .get(entryId);
        if (bookEntry == null) throw new IllegalStateException("entry " + entry + " not in " + bookId);
        if (!BookUnlockStateManager.get().isUnlockedFor(Minecraft.getInstance().player, bookEntry))
            throw new IllegalStateException(entry + " is locked for the capture player");
        BookGuiManager.get()
                .openEntry(bookEntry, BookAddress.of(bookId, bookEntry.getCategoryId(), bookEntry.getId(), page));
    }

    /**
     * Right-clicks {@code block} the way a player opens an organ's screen: with an empty hotbar slot selected, or
     * with the hotbar slot holding the item {@code holding} names.
     */
    private static void useBlock(Minecraft minecraft, BlockPos block, Optional<String> holding) {
        var player = minecraft.player;
        var inventory = player.getInventory();
        int chosen = -1;
        for (int slot = 0; slot < Inventory.getSelectionSize() && chosen < 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            boolean matches = holding.isPresent()
                    ? BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(holding.get())
                    : stack.isEmpty();
            if (matches) chosen = slot;
        }
        if (chosen < 0)
            throw new IllegalStateException("No hotbar slot "
                    + holding.map(i -> "holding " + i).orElse("empty") + " to use " + block + " with");
        inventory.selected = chosen;
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        Vec3 hit = Vec3.atCenterOf(block).add(0, 0, -0.5);
        minecraft.gameMode.useItemOn(
                player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.NORTH, block, false));
    }

    /**
     * A Creative player stops flying the moment it stands on the ground and falls when it is in the air. So a view
     * on the ground just stands, and a view in the air flies and is pinned to its exact position once the teleport
     * has arrived.
     */
    private void holdInAir(Minecraft minecraft) {
        var player = minecraft.player;
        if (player == null || player.onGround()) return;
        Scenario.View view = scenarios.get(scenarioIndex).views().get(viewIndex);
        if (!player.getAbilities().flying) FlatWorld.fly(minecraft);
        if (player.position().distanceToSqr(view.x(), view.y(), view.z()) < 1.0) {
            player.setDeltaMovement(Vec3.ZERO);
            player.setPos(view.x(), view.y(), view.z());
        }
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
        // A newly arrived chunk is scheduled for compiling on a later frame, so the empty queue must hold for a while.
        boolean ready = positioned
                && aimed
                && renderer.countRenderedSections() > 0
                && renderer.hasRenderedAllSections()
                && chunksArrived(minecraft, view)
                && renderer.isSectionCompiled(BlockPos.containing(view.x(), view.y(), view.z()));
        readyFrames = ready ? readyFrames + 1 : 0;
        if (readyFrames >= READY_FRAMES) frameReady = true;
    }

    /** The chunks around the view have arrived from the server. Sections behind the camera are never compiled. */
    private static boolean chunksArrived(Minecraft minecraft, Scenario.View view) {
        int cx = SectionPos.blockToSectionCoord(Mth.floor(view.x()));
        int cz = SectionPos.blockToSectionCoord(Mth.floor(view.z()));
        for (int dx = -READY_RADIUS; dx <= READY_RADIUS; dx++) {
            for (int dz = -READY_RADIUS; dz <= READY_RADIUS; dz++) {
                if (!minecraft.level.hasChunk(cx + dx, cz + dz)) return false;
            }
        }
        return true;
    }

    private void onFrame(RenderFrameEvent.Post event) {
        if (stage != Stage.FRAME || !frameReady) return;
        Minecraft minecraft = Minecraft.getInstance();
        Scenario scenario = scenarios.get(scenarioIndex);
        Scenario.View view = scenario.views().get(viewIndex);
        // A view that used a block waits for its container screen; every other view waits for no screen at all.
        boolean screenOpen = view.book().isPresent()
                ? minecraft.screen != null
                : minecraft.screen instanceof AbstractContainerScreen<?>;
        if (view.opensScreen() ? !screenOpen || ticksInStage < MOVE_TICKS : minecraft.screen != null) return;
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
        if (minecraft.screen instanceof AbstractContainerScreen<?> && minecraft.player != null)
            minecraft.player.closeContainer();
        else if (minecraft.screen != null) minecraft.setScreen(null);
        viewIndex++;
        if (viewIndex < scenarios.get(scenarioIndex).views().size()) {
            moveToView(minecraft);
            return;
        }
        viewIndex = 0;
        scenarioIndex++;
        if (scenarioIndex < scenarios.size()) {
            commandQueue.add(RESET);
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

    /** Player position, camera angles and render state, for the timeout message. */
    private static String where(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) return " (not in a world)";
        var renderer = minecraft.levelRenderer;
        var camera = minecraft.gameRenderer.getMainCamera();
        return String.format(
                Locale.ROOT,
                " at %.3f %.3f %.3f yaw %.2f pitch %.2f flying=%s sections=%d all=%s screen=%s",
                minecraft.player.getX(),
                minecraft.player.getY(),
                minecraft.player.getZ(),
                camera.getYRot(),
                camera.getXRot(),
                minecraft.player.getAbilities().flying,
                renderer.countRenderedSections(),
                renderer.hasRenderedAllSections(),
                minecraft.screen == null ? null : minecraft.screen.getClass().getSimpleName());
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
