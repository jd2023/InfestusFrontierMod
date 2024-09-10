package org.jd.infestusfrontier.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.block.InfestusBlocks;

public class InfestusTabs {
    public static DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, InfestusFrontier.MODID);
    public static final RegistryObject<CreativeModeTab> INFESTUS_TAB = CREATIVE_MODE_TABS.register("infestusfrontier",
            () -> CreativeModeTab.builder()
                    .icon(()->new ItemStack(InfestusItems.EYE_OF_CORRUPTION.get()))
                    .title(Component.translatable("creativetab.infestus_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(InfestusItems.FERMENTED_FLESH.get());
                        pOutput.accept(InfestusItems.EYE_OF_CORRUPTION.get());
                        pOutput.accept(InfestusBlocks.ROTTEN_FLESH_BLOCK.get());
                        pOutput.accept(InfestusBlocks.FERMENTED_FLESH_BLOCK.get());
                        pOutput.accept(InfestusBlocks.INFESTUS_NETWORK.get());


                    })
                    .build());

    public static void register(IEventBus bus){
        CREATIVE_MODE_TABS.register(bus);
    }
}
