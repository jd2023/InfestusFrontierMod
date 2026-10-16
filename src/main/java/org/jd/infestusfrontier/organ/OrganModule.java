package org.jd.infestusfrontier.organ;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.organ.api.OrganCodecs;

/** Owner of organ identity and history. An organ's history travels on its item when it is broken and placed again. */
public final class OrganModule {
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, InfestusFrontier.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<OrganHistory>> HISTORY =
            COMPONENTS.registerComponentType(
                    "organ_history",
                    builder -> builder.persistent(OrganCodecs.HISTORY).networkSynchronized(OrganCodecs.HISTORY_STREAM));

    private OrganModule() {}

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
