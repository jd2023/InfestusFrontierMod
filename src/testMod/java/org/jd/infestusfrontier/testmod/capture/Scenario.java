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
import java.util.Set;

/** One capture scenario from {@code captures/<name>.json}: commands to run, then views to photograph. */
record Scenario(String name, List<String> commands, List<View> views) {
    static final int MAX_VIEWS = 32;
    private static final String NAME = "[a-z][a-z0-9_]{0,63}";

    record View(String name, double x, double y, double z, float yaw, float pitch, boolean gui) {}

    /** Loads every scenario named in a comma separated list from the test mod resources. */
    static List<Scenario> loadAll(String names) throws IOException {
        List<Scenario> scenarios = new ArrayList<>();
        for (String name : names.split(",")) {
            if (!name.isBlank()) scenarios.add(load(name.trim()));
        }
        if (scenarios.isEmpty()) throw new IOException("No capture scenarios named");
        return scenarios;
    }

    static Scenario load(String name) throws IOException {
        if (!name.matches(NAME)) throw new IOException("Bad scenario name: " + name);
        String path = "/captures/" + name + ".json";
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
                views.add(new View(
                        viewName,
                        v.get("x").getAsDouble(),
                        v.get("y").getAsDouble(),
                        v.get("z").getAsDouble(),
                        v.get("yaw").getAsFloat(),
                        v.get("pitch").getAsFloat(),
                        v.has("gui") && v.get("gui").getAsBoolean()));
            }
            return new Scenario(name, List.copyOf(commands), List.copyOf(views));
        }
    }
}
