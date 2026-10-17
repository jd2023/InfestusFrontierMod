package org.jd.infestusfrontier.discovery;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.StorageModule;

/** Owner of recipe reveal, advancements and guide prerequisites. Ships the creative tab. */
public final class DiscoveryModule {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, InfestusFrontier.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.infestusfrontier.main"))
                    .icon(() -> new ItemStack(EcologyModule.SPORE_CULTURE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(EcologyModule.SPORE_CULTURE.get());
                        output.accept(EcologyModule.LIVING_SUBSTRATE_ITEM.get());
                        output.accept(OrganModule.ORGAN_BUD_ITEM.get());
                        output.accept(ProcessingModule.DIGESTIVE_SAC_ITEM.get());
                        output.accept(StorageModule.BIOMASS_BLADDER_ITEM.get());
                        output.accept(StorageModule.BIOMASS_BUCKET.get());
                        output.accept(ProcessingModule.CULTURE_BOWL_ITEM.get());
                    })
                    .build());

    private DiscoveryModule() {}

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
