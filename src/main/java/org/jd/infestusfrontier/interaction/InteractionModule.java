package org.jd.infestusfrontier.interaction;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.discovery.api.DiscoveryObserver;

/** Registers the reusable interaction-owned probe; organ screens remain feature adapters. */
public final class InteractionModule {
    private final DeferredRegister.Items items = DeferredRegister.createItems("infestusfrontier");

    public InteractionModule(DiscoveryObserver discovery) {
        items.register("interaction/synaptic_probe",
                () -> new SynapticProbeItem(new Item.Properties().stacksTo(1), discovery));
    }

    public void register(IEventBus bus) {
        items.register(bus);
    }
}
