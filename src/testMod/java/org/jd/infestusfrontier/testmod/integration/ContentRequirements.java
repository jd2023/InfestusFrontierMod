package org.jd.infestusfrontier.testmod.integration;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;

/** The same generated working-tree requirements are packaged for both test runtimes. */
public record ContentRequirements(boolean emptyRegistry, List<String> clientAssertions,
        List<GuideRequirement> guideRequirements) {
    public ContentRequirements {
        clientAssertions = List.copyOf(clientAssertions);
        guideRequirements = List.copyOf(guideRequirements);
    }

    public static ContentRequirements read() {
        try (var stream = ContentRequirements.class.getResourceAsStream("/content-requirements.json")) {
            if (stream == null) throw new IllegalStateException("Missing generated content requirements");
            byte[] bytes = stream.readNBytes(1024 * 1024 + 1);
            if (bytes.length > 1024 * 1024) throw new IllegalStateException("Content requirements exceed 1 MiB");
            var requirements = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            var names = new LinkedHashSet<String>();
            for (String section : new String[] {"harnessAssertions", "assertions"}) {
                for (var name : requirements.getAsJsonObject(section).getAsJsonArray("client")) {
                    if (!names.add(name.getAsString()) || names.size() > 4096) {
                        throw new IllegalStateException("Duplicate or excessive guide assertion: " + name);
                    }
                }
            }
            var guides = new java.util.ArrayList<GuideRequirement>();
            for (var value : requirements.getAsJsonArray("representations")) {
                var representation = value.getAsJsonObject();
                guides.add(new GuideRequirement(
                        representation.get("entry").getAsString(),
                        representation.getAsJsonObject("assertions").get("guide").getAsString()));
            }
            return new ContentRequirements(requirements.get("emptyRegistry").getAsBoolean(),
                    List.copyOf(names), List.copyOf(guides));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read content requirements", exception);
        }
    }

    public record GuideRequirement(String entry, String assertion) {}
}
