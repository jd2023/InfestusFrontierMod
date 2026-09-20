package org.jd.infestusfrontier.testmod.integration.client;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.ServiceLoader;
import net.minecraft.client.Minecraft;
import org.jd.infestusfrontier.testmod.integration.ContentAssertion;
import org.jd.infestusfrontier.testmod.integration.ContentRequirements;

/** Runs only eligible explicit providers, one at a time, within the harness disconnect deadline. */
public final class GuideScenarios {
    private final ArrayDeque<ContentGuideScenario> pending = new ArrayDeque<>();

    public GuideScenarios() {
        var names = ContentRequirements.read().clientAssertions();
        var providers = new HashMap<String, ContentGuideScenario>();
        for (var scenario : ServiceLoader.load(ContentGuideScenario.class)) {
            String name = scenario.assertion();
            if (providers.putIfAbsent(name, scenario) != null || providers.size() > 4096) {
                throw new IllegalStateException("Duplicate or excessive guide provider: " + name);
            }
        }
        for (String name : names) {
            var scenario = providers.get(name);
            if (scenario == null) throw new IllegalStateException("Missing guide scenario: " + name);
            pending.add(scenario);
        }
    }

    public boolean tick(Minecraft minecraft) {
        var scenario = pending.peek();
        if (scenario != null && scenario.tick(minecraft)) {
            ContentAssertion.passGuide(scenario.assertion());
            pending.remove();
        }
        return pending.isEmpty();
    }
}
