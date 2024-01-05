package org.jd.infestusfrontier;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.blocks.LightSpreader;
import org.jd.infestusfrontier.blocks.Nest;

public class ZgItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, InfestusFrontier.MODID);
    public static final RegistryObject<Item> LIGHT_SPREADER = ITEMS.register(LightSpreader.ID,
            () -> new BlockItem(ZgBlocks.LIGHT_SPREADER.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));
    public static final RegistryObject<Item> NEST = ITEMS.register(Nest.ID,
            () -> new BlockItem(ZgBlocks.NEST.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
