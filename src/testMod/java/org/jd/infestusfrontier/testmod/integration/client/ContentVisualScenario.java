package org.jd.infestusfrontier.testmod.integration.client;

import net.minecraft.client.Minecraft;

/** Observes an owner's server-prepared scene before the shared world capture. */
public interface ContentVisualScenario {
    boolean ready(Minecraft minecraft);
}
