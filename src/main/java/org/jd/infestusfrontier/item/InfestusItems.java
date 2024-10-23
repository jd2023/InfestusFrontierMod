package org.jd.infestusfrontier.item;

import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class InfestusItems {
    public static DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "infestusfrontier");

    public static RegistryObject<Item> FERMENTED_FLESH = ITEMS.register("fermented_flesh",
            ()->new Item(new Item.Properties().stacksTo(64)));
    public static RegistryObject<Item> EYE_OF_CORRUPTION = ITEMS.register("eye_of_corruption",
            ()->new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> MEAT_SWORD = ITEMS.register("meat_sword",
            () -> new SwordItem(InfestusToolTiers.TIER1TOOLS, 1, 1, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MEAT_AXE = ITEMS.register("meat_axe",
            () -> new AxeItem(InfestusToolTiers.TIER1TOOLS, 1, 1,
                    new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MEAT_HOE = ITEMS.register("meat_hoe",
            () -> new HoeItem(InfestusToolTiers.TIER1TOOLS, 1, 1,
                    new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MEAT_SHOVEL = ITEMS.register("meat_shovel",
            () -> new ShovelItem(InfestusToolTiers.TIER1TOOLS, 1, 1,
                    new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MEAT_PICKAXE = ITEMS.register("meat_pickaxe",
            () -> new PickaxeItem(InfestusToolTiers.TIER1TOOLS, 1, 1, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MEAT_CHESTPLATE = ITEMS.register("meat_chestplate",
            () -> new ArmorItem(InfestusArmorMaterials.FLESH_TIER_ONE, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> MEAT_HELMET = ITEMS.register("meat_helmet",
            () -> new ArmorItem(InfestusArmorMaterials.FLESH_TIER_ONE, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> MEAT_BOOTS = ITEMS.register("meat_boots",
            () -> new ArmorItem(InfestusArmorMaterials.FLESH_TIER_ONE, ArmorItem.Type.BOOTS, new Item.Properties()));
    public static final RegistryObject<Item> MEAT_LEGGINGS = ITEMS.register("meat_leggings",
            () -> new ArmorItem(InfestusArmorMaterials.FLESH_TIER_ONE, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

}
