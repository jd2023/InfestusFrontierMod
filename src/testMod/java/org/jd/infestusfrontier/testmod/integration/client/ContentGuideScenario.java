package org.jd.infestusfrontier.testmod.integration.client;

import net.minecraft.client.Minecraft;

/** Explicit testMod service provider; tick performs bounded client work and returns true only after verification. */
public interface ContentGuideScenario {
    String assertion();
    boolean tick(Minecraft minecraft);
}
