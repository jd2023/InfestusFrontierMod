package org.jd.infestusfrontier.screen;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.InfestusFrontier;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, InfestusFrontier.MODID);

    public static final RegistryObject<MenuType<CorruptionCoreMenu>> CORRUPTION_CORE_MENU =
            MENUS.register("corruption_core_menu",() -> new MenuType<>(CorruptionCoreMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
