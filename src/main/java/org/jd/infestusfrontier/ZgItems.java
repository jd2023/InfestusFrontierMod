package org.jd.infestusfrontier;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.block.*;
import org.jd.infestusfrontier.item.ModCreativeModeTab;

public class ZgItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, InfestusFrontier.MODID);
    public static final RegistryObject<Item> LIGHT_SPREADER = ITEMS.register(LightSpreader.ID,
            () -> new BlockItem(ZgBlocks.LIGHT_SPREADER.get(), new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
     public static final RegistryObject<Item> FLESH_BLOCK = ITEMS.register(FleshBlock.ID,
            () -> new BlockItem(ZgBlocks.FLESH_BLOCK.get(), new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
    public static final RegistryObject<Item> NEST = ITEMS.register(Nest.ID,
            () -> new BlockItem(ZgBlocks.NEST.get(), new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
    public static final RegistryObject<Item> BIO_RESERVOIR = ITEMS.register(BioReservoir.ID,
            () -> new BlockItem(ZgBlocks.BIO_RESERVOIR.get(), new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
    public static final RegistryObject<Item> TUMOR = ITEMS.register(Tumor.ID,
            () -> new BlockItem(ZgBlocks.TUMOR.get(), new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
