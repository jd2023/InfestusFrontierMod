package org.jd.infestusfrontier;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Composition root. Wires feature modules; owns no gameplay itself. */
@Mod(InfestusFrontier.MOD_ID)
public final class InfestusFrontier {
    public static final String MOD_ID = "infestusfrontier";

    public InfestusFrontier(IEventBus modBus) {
        // Feature modules register here, one line each, in dependency order.
    }
}
