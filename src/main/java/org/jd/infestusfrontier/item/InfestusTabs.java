package org.jd.infestusfrontier.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.InfestusFrontierMod;

public class InfestusTabs {
    public static DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, InfestusFrontierMod.MODID);
    public static final RegistryObject<CreativeModeTab> INFESTUS_TAB = CREATIVE_MODE_TABS.register("infestusfrontier",
            () -> CreativeModeTab.builder()
                    .icon(()->new ItemStack(Items.ROTTEN_FLESH))
                    .title(Component.translatable("creativetab.infestus_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(Items.ROTTEN_FLESH);
                    })
                    .build());

    public static void register(IEventBus bus){
        CREATIVE_MODE_TABS.register(bus);
    }
}
