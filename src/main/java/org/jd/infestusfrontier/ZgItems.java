package org.jd.infestusfrontier;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.block.*;
import org.jd.infestusfrontier.item.*;

public class ZgItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, InfestusFrontier.MODID);
    public static final RegistryObject<Item> LIGHT_INFESTUS_POD = ITEMS.register(LightInfestusPod.ID,
            () -> new BlockItem(ZgBlocks.LIGHT_INFESTUS_POD.get(), new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
    public static final RegistryObject<Item> CORRUPT_CHUNK = ITEMS.register(CorruptChunk.ID,
            () -> new BlockItem(ZgBlocks.CORRUPT_CHUNK.get(), new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
    public static final RegistryObject<Item> corruption_core = ITEMS.register(CorruptionCore.ID,
            () -> new BlockItem(ZgBlocks.CORRUPTION_CORE.get(), new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
    public static final RegistryObject<Item> BIO_RESERVOIR = ITEMS.register(BioReservoir.ID,
            () -> new BlockItem(ZgBlocks.BIO_RESERVOIR.get(), new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
    public static final RegistryObject<Item> INFESTUS_POD = ITEMS.register(InfestusPod.ID,
            () -> new BlockItem(ZgBlocks.INFESTUS_POD.get(), new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
    public static final RegistryObject<Item> MEAT_SWORD = ITEMS.register(MeatSword.ID,
            () -> new SwordItem(ZgToolTiers.TIER1TOOLS, 1, 1,
                    new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
    public static final RegistryObject<Item> MEAT_AXE = ITEMS.register(MeatAxe.ID,
            () -> new AxeItem(ZgToolTiers.TIER1TOOLS, 1, 1,
                    new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
    public static final RegistryObject<Item> MEAT_HOE = ITEMS.register(MeatHoe.ID,
            () -> new HoeItem(ZgToolTiers.TIER1TOOLS, 1, 1,
                    new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
    public static final RegistryObject<Item> MEAT_SHOVEL = ITEMS.register(MeatShovel.ID,
            () -> new ShovelItem(ZgToolTiers.TIER1TOOLS, 1, 1,
                    new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
    public static final RegistryObject<Item> MEAT_PICKAXE = ITEMS.register(MeatPickaxe.ID,
            () -> new PickaxeItem(ZgToolTiers.TIER1TOOLS, 1, 1,
                    new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
    public static final RegistryObject<Item> MEAT_HELMET = ITEMS.register(MeatHelmet.ID,
            () -> new ArmorItem(ZgArmorMaterials.FLESH_TIER_ONE, EquipmentSlot.HEAD,
                    new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
    public static final RegistryObject<Item> MEAT_CHESTPLATE = ITEMS.register(MeatChestplate.ID,
            () -> new ArmorItem(ZgArmorMaterials.FLESH_TIER_ONE, EquipmentSlot.CHEST,
                    new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
    public static final RegistryObject<Item> MEAT_LEGGINGS = ITEMS.register(MeatLeggings.ID,
            () -> new ArmorItem(ZgArmorMaterials.FLESH_TIER_ONE, EquipmentSlot.LEGS,
                    new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
    public static final RegistryObject<Item> MEAT_BOOTS= ITEMS.register(MeatBoots.ID,
            () -> new ArmorItem(ZgArmorMaterials.FLESH_TIER_ONE, EquipmentSlot.FEET,
                    new Item.Properties().tab(ModCreativeModeTab.INFESTUS_TAB)));
    public static final RegistryObject<Item> INFESTED_CONNECTOR = ITEMS.register(InfestedConnector.ID, InfestedConnector::new);

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
