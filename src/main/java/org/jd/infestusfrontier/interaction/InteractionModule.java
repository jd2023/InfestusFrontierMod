package org.jd.infestusfrontier.interaction;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Tool;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.organ.api.Organs;

/** Owner of hand tools that read, open and remove organs. Ships the Synaptic Probe and the Surgical Blade. */
public final class InteractionModule {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);

    public static final DeferredItem<Item> SYNAPTIC_PROBE =
            ITEMS.registerItem("synaptic_probe", SynapticProbeItem::new, new Item.Properties().stacksTo(1));

    /** Uses of a Surgical Blade: one an organ cut out. */
    static final int BLADE_USES = 64;
    /**
     * The blade's speed on organs: an organ of {@link Organs#HARDNESS} comes out in 1.5 seconds instead of 18. On
     * anything else it is no faster than a hand.
     */
    static final float BLADE_SPEED = 12.0F;

    /** Cuts organs out whole and fast, through vanilla's tool rules; it does nothing else a hand cannot. */
    public static final DeferredItem<Item> SURGICAL_BLADE = ITEMS.registerSimpleItem(
            "surgical_blade",
            new Item.Properties()
                    .durability(BLADE_USES)
                    .component(
                            DataComponents.TOOL,
                            new Tool(List.of(Tool.Rule.overrideSpeed(Organs.ORGANS, BLADE_SPEED)), 1.0F, 1)));

    private InteractionModule() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
