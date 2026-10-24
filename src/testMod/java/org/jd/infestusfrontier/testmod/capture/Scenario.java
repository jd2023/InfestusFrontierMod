package org.jd.infestusfrontier.testmod.capture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;

/** One scripted scene from {@code <folder>/<name>.json}: commands to run, then views to stand at. */
record Scenario(String name, List<String> commands, List<View> views) {
    static final int MAX_VIEWS = 96;
    private static final String NAME = "[a-z][a-z0-9_]{0,63}";

    /**
     * {@code use} names a block the player right-clicks before the shot, to photograph its screen, with an empty hand
     * or with the item {@code holding} names (given by the scenario's commands) selected in the hotbar;
     * {@code inventory} opens the player's own inventory instead; {@code book} opens the guide at that entry id (empty
     * for its first screen) on page pair {@code page}. A view with any of these is shot at GUI scale 3. {@code icons}
     * names what the hotbar holds, left to right, for the verifier's centring check; the client selects an empty slot
     * past them first so no selection ring touches a measured icon.
     */
    record View(
            String name,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            boolean gui,
            Optional<BlockPos> use,
            Optional<String> holding,
            boolean inventory,
            Optional<String> book,
            int page,
            List<String> icons,
            int frames,
            int frameTicks) {
        boolean opensScreen() {
            return use.isPresent() || inventory || book.isPresent();
        }
    }

    /** Loads every scenario named in a comma separated list from the test mod resources. */
    static List<Scenario> loadAll(String folder, String names) throws IOException {
        List<Scenario> scenarios = new ArrayList<>();
        for (String name : names.split(",")) {
            if (!name.isBlank()) scenarios.add(load(folder, name.trim()));
        }
        if (scenarios.isEmpty()) throw new IOException("No capture scenarios named");
        return scenarios;
    }

    static Scenario load(String folder, String name) throws IOException {
        if (!name.matches(NAME)) throw new IOException("Bad scenario name: " + name);
        String path = "/" + folder + "/" + name + ".json";
        try (InputStream in = Scenario.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("Missing scenario resource " + path);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            List<String> commands = new ArrayList<>();
            if (root.has("commands")) {
                for (JsonElement e : root.getAsJsonArray("commands")) commands.add(e.getAsString());
            }
            JsonArray viewArray = root.getAsJsonArray("views");
            if (viewArray == null || viewArray.isEmpty() || viewArray.size() > MAX_VIEWS) {
                throw new IOException(name + ": a scenario has 1 to " + MAX_VIEWS + " views");
            }
            List<View> views = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (JsonElement e : viewArray) {
                JsonObject v = e.getAsJsonObject();
                String viewName = v.get("name").getAsString();
                if (!viewName.matches(NAME) || !seen.add(viewName)) {
                    throw new IOException(name + ": bad or duplicate view name " + viewName);
                }
                Optional<BlockPos> use = Optional.empty();
                int frames = v.has("frames") ? v.get("frames").getAsInt() : 1;
                int frameTicks = v.has("frameTicks") ? v.get("frameTicks").getAsInt() : 4;
                if (frames < 1 || frames > 96 || frameTicks < 1 || frameTicks > 20)
                    throw new IOException(name + ": frames must be 1..96 and frameTicks 1..20");
                if (v.has("use")) {
                    JsonArray at = v.getAsJsonArray("use");
                    if (at.size() != 3) throw new IOException(name + ": use needs [x, y, z] in view " + viewName);
                    use = Optional.of(new BlockPos(
                            at.get(0).getAsInt(),
                            at.get(1).getAsInt(),
                            at.get(2).getAsInt()));
                }
                views.add(new View(
                        viewName,
                        v.get("x").getAsDouble(),
                        v.get("y").getAsDouble(),
                        v.get("z").getAsDouble(),
                        v.get("yaw").getAsFloat(),
                        v.get("pitch").getAsFloat(),
                        v.has("gui") && v.get("gui").getAsBoolean(),
                        use,
                        v.has("holding") ? Optional.of(v.get("holding").getAsString()) : Optional.empty(),
                        v.has("inventory") && v.get("inventory").getAsBoolean(),
                        v.has("book") ? Optional.of(v.get("book").getAsString()) : Optional.empty(),
                        v.has("page") ? v.get("page").getAsInt() : 0,
                        v.has("icons") ? icons(v.getAsJsonArray("icons")) : List.of(),
                        frames,
                        frameTicks));
            }
            return new Scenario(name, List.copyOf(commands), List.copyOf(views));
        }
    }

    private static List<String> icons(JsonArray array) {
        List<String> icons = new ArrayList<>();
        array.forEach(e -> icons.add(e.getAsString()));
        return List.copyOf(icons);
    }
}
