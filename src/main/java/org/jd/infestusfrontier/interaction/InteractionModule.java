package org.jd.infestusfrontier.interaction;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.InfestusFrontier;

/** Owner of hand tools that read and open organs. Ships the Synaptic Probe. */
public final class InteractionModule {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);

    public static final DeferredItem<Item> SYNAPTIC_PROBE =
            ITEMS.registerItem("synaptic_probe", SynapticProbeItem::new, new Item.Properties().stacksTo(1));

    private InteractionModule() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
