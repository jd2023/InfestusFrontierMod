package org.jd.infestusfrontier.testmod.integration.client;

import java.nio.file.Path;
import net.minecraft.client.Minecraft;

/** Bounded real-client action contributor; coordination never substitutes for received state. */
public interface ContentMultiplayerScenario {
    boolean tick(Minecraft game, String role, Path coordination) throws Exception;
}
