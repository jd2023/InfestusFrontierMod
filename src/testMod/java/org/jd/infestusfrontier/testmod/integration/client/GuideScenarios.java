package org.jd.infestusfrontier.testmod.integration.client;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.ServiceLoader;
import net.minecraft.client.Minecraft;
import org.jd.infestusfrontier.testmod.integration.ContentAssertion;
import org.jd.infestusfrontier.testmod.integration.ContentRequirements;

/** Runs only eligible explicit providers, one at a time, within the harness disconnect deadline. */
public final class GuideScenarios {
    private final ArrayDeque<Run> pending = new ArrayDeque<>();

    public GuideScenarios() {
        var names = ContentRequirements.read().clientAssertions();
        var providers = new HashMap<String, ContentGuideScenario>();
        for (var scenario : ServiceLoader.load(ContentGuideScenario.class)) {
            for (String name : scenario.assertions()) {
                if (providers.putIfAbsent(name, scenario) != null || providers.size() > 4096) {
                    throw new IllegalStateException("Duplicate or excessive guide provider: " + name);
                }
            }
        }
        var runs = new IdentityHashMap<ContentGuideScenario, java.util.ArrayList<String>>();
        for (String name : names) {
            var scenario = providers.get(name);
            if (scenario == null) throw new IllegalStateException("Missing guide scenario: " + name);
            runs.computeIfAbsent(scenario, ignored -> new java.util.ArrayList<>()).add(name);
        }
        runs.forEach((scenario, assertions) -> pending.add(new Run(scenario, List.copyOf(assertions))));
    }

    public boolean tick(Minecraft minecraft) {
        var run = pending.peek();
        if (run != null && run.scenario().tick(minecraft)) {
            run.assertions().forEach(ContentAssertion::passGuide);
            pending.remove();
        }
        return pending.isEmpty();
    }

    private record Run(ContentGuideScenario scenario, List<String> assertions) {}
}
