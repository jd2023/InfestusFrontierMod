package org.jd.infestusfrontier.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class InfestusItems {
    public static DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "infestusfrontier");

    public static RegistryObject<Item> FERMENTED_FLESH = ITEMS.register("fermented_flesh",
            ()->new Item(new Item.Properties().stacksTo(64)));
    public static RegistryObject<Item> EYE_OF_THE_SWARM = ITEMS.register("eye_of_the_swarm",
            ()->new Item(new Item.Properties().stacksTo(16)));


    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

}
