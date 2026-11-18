package org.jd.infestusfrontier.testmod.capture;

import com.google.gson.JsonArray;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.LoadingErrorScreen;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Ends a capture client whose mod loading failed. NeoForge then shows its loading error screen and never starts the
 * game event bus, so {@link CaptureClient} never ticks and the client would wait for a person. This watch writes the
 * loading errors as the run's failure report and stops the client. The first client tick proves loading succeeded and
 * ends the watch.
 */
final class LoadingFailureWatch {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long POLL_MILLIS = 1000;

    private final Path outputDir;
    private volatile boolean finished;

    private LoadingFailureWatch(Path outputDir) {
        this.outputDir = outputDir;
    }

    /** Starts watching; call before anything in the test mod's construction can fail. */
    static void start(Path outputDir) {
        LoadingFailureWatch watch = new LoadingFailureWatch(outputDir);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> watch.finished = true);
        Thread thread = new Thread(watch::poll, "infestus-capture-loading-watch");
        thread.setDaemon(true);
        thread.start();
    }

    private void poll() {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            while (!finished) {
                Thread.sleep(POLL_MILLIS);
                minecraft.execute(() -> check(minecraft)); // the screen belongs to the render thread
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void check(Minecraft minecraft) {
        if (finished || !ModLoader.hasErrors() || !(minecraft.screen instanceof LoadingErrorScreen)) return;
        finished = true;
        String failure = "LOADING: "
                + ModLoader.getLoadingErrors().stream()
                        .map(LoadingFailureWatch::describe)
                        .collect(Collectors.joining("; "));
        LOGGER.error("INFESTUS_CAPTURE_FAILURE {}", failure);
        try {
            CaptureClient.writeReport(outputDir, new JsonArray(), failure);
        } catch (IOException io) {
            LOGGER.error("INFESTUS_CAPTURE_FAILURE report", io);
        }
        minecraft.stop();
    }

    /** The issue's key, its mod, its exception and that exception's root cause when the message does not show it. */
    private static String describe(ModLoadingIssue issue) {
        StringBuilder text = new StringBuilder(issue.translationKey());
        if (issue.affectedMod() != null)
            text.append(" (").append(issue.affectedMod().getModId()).append(')');
        Throwable cause = issue.cause();
        if (cause != null) {
            text.append(": ").append(cause);
            Throwable root = cause;
            while (root.getCause() != null) root = root.getCause();
            if (!text.toString().contains(root.toString()))
                text.append(" caused by ").append(root);
        }
        return text.toString();
    }
}
