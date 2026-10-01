package org.jd.infestusfrontier;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import org.jd.infestusfrontier.construction.ConstructionModule;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.discovery.DiscoveryModule;
import org.jd.infestusfrontier.integration.RuntimeIdentity;
import org.jd.infestusfrontier.interaction.InteractionModule;

/** Production composition root. */
@Mod(InfestusFrontier.MOD_ID)
public final class InfestusFrontier {
    public static final String MOD_ID = "infestusfrontier";

    public InfestusFrontier(IEventBus modBus) {
        RuntimeIdentity.install();
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            org.jd.infestusfrontier.integration.modonomicon.BowlGuidePages.register();
            org.jd.infestusfrontier.integration.modonomicon.PreparationGuidePages.register();
        }));
        var discovery = new DiscoveryModule();
        discovery.register();
        var observer = discovery.observer();
        new InteractionModule(observer).register(modBus);
        var ecology = new EcologyModule(observer);
        ecology.register(modBus);
        var construction = new ConstructionModule(ecology::applyCulture);
        new org.jd.infestusfrontier.processing.ProcessingModule(observer, construction.budRecipes()).register(modBus);
        new org.jd.infestusfrontier.processing.digestion.DigestionModule(construction.budRecipes(), observer).register(modBus);
        construction.register(modBus);
    }
}
