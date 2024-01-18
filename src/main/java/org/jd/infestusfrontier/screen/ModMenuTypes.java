package org.jd.infestusfrontier.screen;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.InfestusFrontier;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, InfestusFrontier.MODID);

    public static final RegistryObject<MenuType<BioReservoirMenu>> BIO_RESERVOIR_MENU =
            registerMenuTypes(BioReservoirMenu::new, "bio_reservoir_menu");

    public static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerMenuTypes(IContainerFactory<T> factory,
                                                                                                  String name) {
        return MENUS.register(name, () -> IForgeMenuType.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
