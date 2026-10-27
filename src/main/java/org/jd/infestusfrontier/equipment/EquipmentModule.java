package org.jd.infestusfrontier.equipment;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.render.LivingArmor;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.slf4j.Logger;

/**
 * Armor and tool identity and fuel. Four dormant pieces are crafted from rotten flesh (G0 in ARMOR_EVOLUTION.md); the
 * Cradle awakens each into its awakened form (G1), which carries a {@link #RESERVE} of biomass. The Fuel Papilla
 * fills that reserve from the ground. Four dormant tools are crafted from Bone Plates (ITEM_CATALOG I015); the Cradle
 * wakes each into its living form (I016), which carries the same reserve, mends from it and takes one
 * {@link #TOOL_MUTATION} by a graft in the Cradle; the Dormant and Living Fang (I017, I018) are the same for a blade.
 */
public final class EquipmentModule {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<ArmorMaterial> MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, InfestusFrontier.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<FuelPapillaMenu>> FUEL_PAPILLA_MENU =
            MENUS.register("fuel_papilla", () -> IMenuTypeExtension.create(FuelPapillaMenu::new));
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, InfestusFrontier.MOD_ID);

    /** Durability multipliers from ARMOR_EVOLUTION.md "Frame statistics": 33/48/45/39 and 88/128/120/104. */
    static final int DORMANT_DURABILITY = 3;

    static final int AWAKENED_DURABILITY = 8;
    /** Every awakened piece starts with this reserve, empty. */
    public static final Biomass RESERVE_CAPACITY = Biomass.of(40);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BiomassTank>> RESERVE =
            COMPONENTS.registerComponentType(
                    "biomass_reserve",
                    builder -> builder.persistent(BiomassCodecs.TANK).networkSynchronized(BiomassCodecs.TANK_STREAM));
    /** The branch a living tool has taken; absent on a tool that has none. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolMutation>> TOOL_MUTATION =
            COMPONENTS.registerComponentType(
                    "tool_mutation",
                    builder -> builder.persistent(ToolMutation.CODEC).networkSynchronized(ToolMutation.STREAM_CODEC));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> DORMANT =
            MATERIALS.register("dormant", () -> material("dormant", 0, 1, 1, 0));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> AWAKENED =
            MATERIALS.register("awakened", () -> material("awakened", 1, 2, 2, 1));

    /** The refuelling cell: a substrate cell that fills the worn pieces of whoever stands on it. */
    public static final DeferredBlock<FuelPapillaBlock> FUEL_PAPILLA = BLOCKS.registerBlock(
            "fuel_papilla",
            FuelPapillaBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_PINK)
                    .strength(0.6F)
                    .sound(SoundType.GRAVEL));

    /** The papilla as a graft: used on exposed mature substrate it grows the cell. */
    public static final DeferredItem<Item> FUEL_PAPILLA_ITEM = ITEMS.registerItem("fuel_papilla", FuelPapillaItem::new);

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FuelPapillaBlockEntity>> FUEL_PAPILLA_ENTITY =
            BLOCK_ENTITIES.register(
                    "fuel_papilla",
                    () -> BlockEntityType.Builder.of(FuelPapillaBlockEntity::new, FUEL_PAPILLA.get())
                            .build(null));

    public static final DeferredItem<ArmorItem> DORMANT_HOOD = dormant("dormant_hood", ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> DORMANT_THORAX = dormant("dormant_thorax", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> DORMANT_LEGWRAPS = dormant("dormant_legwraps", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> DORMANT_TREADS = dormant("dormant_treads", ArmorItem.Type.BOOTS);
    public static final DeferredItem<ArmorItem> AWAKENED_HOOD = awakened("awakened_hood", ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> AWAKENED_THORAX =
            awakened("awakened_thorax", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> AWAKENED_LEGWRAPS =
            awakened("awakened_legwraps", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> AWAKENED_TREADS = awakened("awakened_treads", ArmorItem.Type.BOOTS);

    // the tools, with the stone tools' attack and speed figures
    public static final DeferredItem<PickaxeItem> DORMANT_PICK = ITEMS.register(
            "dormant_pick",
            () -> new PickaxeItem(
                    LivingTiers.DORMANT,
                    new Item.Properties().attributes(PickaxeItem.createAttributes(LivingTiers.DORMANT, 1.0F, -2.8F))));
    public static final DeferredItem<AxeItem> DORMANT_HATCHET = ITEMS.register(
            "dormant_hatchet",
            () -> new AxeItem(
                    LivingTiers.DORMANT,
                    new Item.Properties().attributes(AxeItem.createAttributes(LivingTiers.DORMANT, 7.0F, -3.2F))));
    public static final DeferredItem<ShovelItem> DORMANT_SPADE = ITEMS.register(
            "dormant_spade",
            () -> new ShovelItem(
                    LivingTiers.DORMANT,
                    new Item.Properties().attributes(ShovelItem.createAttributes(LivingTiers.DORMANT, 1.5F, -3.0F))));
    public static final DeferredItem<HoeItem> DORMANT_HOE = ITEMS.register(
            "dormant_hoe",
            () -> new HoeItem(
                    LivingTiers.DORMANT,
                    new Item.Properties().attributes(HoeItem.createAttributes(LivingTiers.DORMANT, -1.0F, -2.0F))));
    public static final DeferredItem<PickaxeItem> LIVING_PICK = ITEMS.register(
            "living_pick",
            () -> new LivingPickItem(living(PickaxeItem.createAttributes(LivingTiers.LIVING, 1.0F, -2.8F))));
    public static final DeferredItem<AxeItem> LIVING_HATCHET = ITEMS.register(
            "living_hatchet",
            () -> new LivingHatchetItem(living(AxeItem.createAttributes(LivingTiers.LIVING, 7.0F, -3.2F))));
    public static final DeferredItem<ShovelItem> LIVING_SPADE = ITEMS.register(
            "living_spade",
            () -> new LivingSpadeItem(living(ShovelItem.createAttributes(LivingTiers.LIVING, 1.5F, -3.0F))));
    public static final DeferredItem<HoeItem> LIVING_HOE = ITEMS.register(
            "living_hoe", () -> new LivingHoeItem(living(HoeItem.createAttributes(LivingTiers.LIVING, -1.0F, -2.0F))));

    // the fangs: a weak blade, a wooden sword's bite when dormant
    public static final DeferredItem<SwordItem> DORMANT_FANG = ITEMS.register(
            "dormant_fang",
            () -> new SwordItem(
                    LivingTiers.DORMANT,
                    new Item.Properties().attributes(SwordItem.createAttributes(LivingTiers.DORMANT, 2, -2.4F))));
    public static final DeferredItem<SwordItem> LIVING_FANG = ITEMS.register(
            "living_fang", () -> new LivingFangItem(living(SwordItem.createAttributes(LivingTiers.LIVING, 2, -2.4F))));

    /** The dormant tools and the fang in the catalog's order. */
    public static final List<DeferredItem<? extends Item>> DORMANT_TOOLS =
            List.of(DORMANT_PICK, DORMANT_HATCHET, DORMANT_SPADE, DORMANT_HOE, DORMANT_FANG);
    /** The living tools and the fang in the same order. */
    public static final List<DeferredItem<? extends Item>> LIVING_TOOLS =
            List.of(LIVING_PICK, LIVING_HATCHET, LIVING_SPADE, LIVING_HOE, LIVING_FANG);

    private EquipmentModule() {}

    private static ArmorMaterial material(String layer, int hood, int thorax, int legwraps, int treads) {
        return new ArmorMaterial(
                Map.of(
                        ArmorItem.Type.HELMET, hood,
                        ArmorItem.Type.CHESTPLATE, thorax,
                        ArmorItem.Type.LEGGINGS, legwraps,
                        ArmorItem.Type.BOOTS, treads),
                5,
                SoundEvents.ARMOR_EQUIP_LEATHER,
                () -> Ingredient.of(Items.ROTTEN_FLESH),
                // one dyeable layer: dye tints it as it tints leather, and trims draw on its vanilla shape
                List.of(new ArmorMaterial.Layer(
                        ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, layer), "", true)),
                0.0F,
                0.0F);
    }

    private static DeferredItem<ArmorItem> dormant(String name, ArmorItem.Type type) {
        return ITEMS.register(
                name,
                () -> new ArmorItem(
                        DORMANT, type, new Item.Properties().durability(type.getDurability(DORMANT_DURABILITY))));
    }

    /** A living tool's properties: its attributes and an empty reserve. */
    private static Item.Properties living(ItemAttributeModifiers attributes) {
        return new Item.Properties()
                .attributes(attributes)
                .component(RESERVE.get(), BiomassTank.empty(RESERVE_CAPACITY));
    }

    private static DeferredItem<ArmorItem> awakened(String name, ArmorItem.Type type) {
        return ITEMS.register(
                name,
                () -> new AwakenedPieceItem(
                        AWAKENED,
                        type,
                        new Item.Properties()
                                .durability(type.getDurability(AWAKENED_DURABILITY))
                                .component(RESERVE.get(), BiomassTank.empty(RESERVE_CAPACITY))));
    }

    public static void register(IEventBus modBus) {
        MATERIALS.register(modBus);
        COMPONENTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener((RegisterMenuScreensEvent event) ->
                    event.register(FUEL_PAPILLA_MENU.get(), FuelPapillaScreen::new));
            LivingArmor.register(
                    modBus,
                    ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "dormant"),
                    DORMANT_HOOD,
                    DORMANT_THORAX,
                    DORMANT_LEGWRAPS,
                    DORMANT_TREADS);
            LivingArmor.register(
                    modBus,
                    ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "awakened"),
                    AWAKENED_HOOD,
                    AWAKENED_THORAX,
                    AWAKENED_LEGWRAPS,
                    AWAKENED_TREADS);
        }
        // a water cauldron washes the dye off, as it washes leather: vanilla's own interaction, listed for these items
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            for (var piece : List.of(
                    DORMANT_HOOD,
                    DORMANT_THORAX,
                    DORMANT_LEGWRAPS,
                    DORMANT_TREADS,
                    AWAKENED_HOOD,
                    AWAKENED_THORAX,
                    AWAKENED_LEGWRAPS,
                    AWAKENED_TREADS)) CauldronInteraction.WATER.map().put(piece.get(), CauldronInteraction.DYED_ITEM);
        }));
        modBus.addListener((RegisterCapabilitiesEvent event) -> {
            event.registerItem(
                    Capabilities.FluidHandler.ITEM,
                    (stack, context) -> new ReserveHandler(stack),
                    AWAKENED_HOOD,
                    AWAKENED_THORAX,
                    AWAKENED_LEGWRAPS,
                    AWAKENED_TREADS,
                    LIVING_PICK,
                    LIVING_HATCHET,
                    LIVING_SPADE,
                    LIVING_HOE,
                    LIVING_FANG);
            event.registerBlockEntity(BiomassCapabilities.BLOCK, FUEL_PAPILLA_ENTITY.get(), (papilla, side) -> papilla);
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    FUEL_PAPILLA_ENTITY.get(),
                    (papilla, side) -> BiomassFluids.handler(papilla));
        });
    }
}
