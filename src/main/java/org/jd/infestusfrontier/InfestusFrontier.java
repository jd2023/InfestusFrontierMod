package org.jd.infestusfrontier;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.jd.infestusfrontier.discovery.DiscoveryModule;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.equipment.EquipmentModule;
import org.jd.infestusfrontier.integration.modonomicon.GuidePages;
import org.jd.infestusfrontier.interaction.InteractionModule;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.StorageModule;

/** Composition root. Wires feature modules; owns no gameplay itself. */
@Mod(InfestusFrontier.MOD_ID)
public final class InfestusFrontier {
    public static final String MOD_ID = "infestusfrontier";

    public InfestusFrontier(IEventBus modBus) {
        EcologyModule.register(modBus);
        OrganModule.register(modBus);
        StorageModule.register(modBus);
        ProcessingModule.register(modBus);
        LogisticsModule.register(modBus);
        EquipmentModule.register(modBus);
        InteractionModule.register(modBus);
        DiscoveryModule.register(modBus);
        GuidePages.register(modBus);
    }
}
