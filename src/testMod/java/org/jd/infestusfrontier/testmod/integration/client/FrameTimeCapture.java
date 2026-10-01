package org.jd.infestusfrontier.testmod.integration.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Fixed storage and sample count; records delivered frame intervals, including swap and tick work. */
public final class FrameTimeCapture {
    private static final int WARMUP_FRAMES = 30;
    private final long[] nanos = new long[120];
    private int frames;
    private long previous;
    private boolean vsync;
    private int limit;

    public void begin(Minecraft game) {
        frames = 0;
        previous = 0;
        vsync = game.options.enableVsync().get();
        limit = game.options.framerateLimit().get();
        game.options.enableVsync().set(false);
        game.options.framerateLimit().set(260);
    }

    public boolean renderedFrame(Minecraft game, Path output, String name, JsonObject scene) {
        if (frames > WARMUP_FRAMES + nanos.length) return true;
        long now = System.nanoTime();
        if (frames > WARMUP_FRAMES) nanos[frames - WARMUP_FRAMES - 1] = now - previous;
        previous = now;
        frames++;
        if (frames <= WARMUP_FRAMES + nanos.length) return false;
        var result = new JsonObject();
        result.addProperty("schema", 1);
        result.addProperty("measurement", "consecutive rendered-frame intervals; includes CPU, GPU, swap and client tick work");
        result.addProperty("warmupFrames", WARMUP_FRAMES);
        result.addProperty("width", game.getWindow().getWidth());
        result.addProperty("height", game.getWindow().getHeight());
        result.addProperty("renderer", GL11.glGetString(GL11.GL_RENDERER));
        result.addProperty("openGl", GL11.glGetString(GL11.GL_VERSION));
        result.addProperty("graphics", game.options.graphicsMode().get().toString());
        result.addProperty("fieldOfView", game.options.fov().get());
        result.addProperty("cameraEye", game.gameRenderer.getMainCamera().getPosition().toString());
        result.addProperty("renderDistance", game.options.renderDistance().get());
        result.addProperty("vsync", game.options.enableVsync().get());
        result.addProperty("frameLimit", game.options.framerateLimit().get());
        result.add("scene", scene);
        var samples = new JsonArray();
        for (long value : nanos) {
            if (value <= 0) throw new IllegalStateException("Nonpositive frame interval");
            samples.add(value / 1_000_000.0);
        }
        result.add("frameMilliseconds", samples);
        long[] sorted = nanos.clone();
        Arrays.sort(sorted);
        result.addProperty("meanMs", Arrays.stream(nanos).average().orElseThrow() / 1_000_000.0);
        result.addProperty("p50Ms", sorted[59] / 1_000_000.0);
        result.addProperty("p95Ms", sorted[113] / 1_000_000.0);
        result.addProperty("p99Ms", sorted[118] / 1_000_000.0);
        result.addProperty("maxMs", sorted[119] / 1_000_000.0);
        try {
            Files.writeString(output.resolve(name + ".json"), result.toString());
        } catch (IOException error) {
            throw new IllegalStateException("Cannot save frame timing evidence", error);
        }
        return true;
    }

    public void finish(Minecraft game) {
        game.options.enableVsync().set(vsync);
        game.options.framerateLimit().set(limit);
    }
}
