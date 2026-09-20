package org.jd.infestusfrontier.interaction;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the reusable interaction-owned probe; organ screens remain feature adapters. */
public final class InteractionModule {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("infestusfrontier");
    public static final java.util.function.Supplier<Item> SYNAPTIC_PROBE = ITEMS.register("interaction/synaptic_probe",
            () -> new SynapticProbeItem(new Item.Properties().stacksTo(1)));

    public void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
