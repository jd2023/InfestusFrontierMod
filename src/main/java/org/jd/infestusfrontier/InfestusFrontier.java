package org.jd.infestusfrontier;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import org.jd.infestusfrontier.construction.ConstructionModule;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.integration.RuntimeIdentity;
import org.jd.infestusfrontier.interaction.InteractionModule;

/** Production composition root. */
@Mod(InfestusFrontier.MOD_ID)
public final class InfestusFrontier {
    public static final String MOD_ID = "infestusfrontier";

    public InfestusFrontier(IEventBus modBus) {
        RuntimeIdentity.install();
        new org.jd.infestusfrontier.processing.ProcessingModule().register(modBus);
        new InteractionModule().register(modBus);
        var ecology = new EcologyModule();
        ecology.register(modBus);
        new ConstructionModule(ecology::applyCulture).register(modBus);
    }
}
