package org.jd.infestusfrontier.equipment;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.slf4j.Logger;

/**
 * Armor piece identity and fuel. Four dormant pieces are crafted from rotten flesh (G0 in ARMOR_EVOLUTION.md); the
 * Cradle awakens each into its awakened form (G1), which carries a {@link #RESERVE} of biomass. The Fuel Papilla
 * fills that reserve from the ground.
 */
public final class EquipmentModule {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<ArmorMaterial> MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, InfestusFrontier.MOD_ID);
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

    public static final DeferredItem<BlockItem> FUEL_PAPILLA_ITEM = ITEMS.registerSimpleBlockItem(FUEL_PAPILLA);
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
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, layer))),
                0.0F,
                0.0F);
    }

    private static DeferredItem<ArmorItem> dormant(String name, ArmorItem.Type type) {
        return ITEMS.register(
                name,
                () -> new ArmorItem(
                        DORMANT, type, new Item.Properties().durability(type.getDurability(DORMANT_DURABILITY))));
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
        modBus.addListener((RegisterCapabilitiesEvent event) -> {
            event.registerItem(
                    Capabilities.FluidHandler.ITEM,
                    (stack, context) -> new ReserveHandler(stack),
                    AWAKENED_HOOD.get(),
                    AWAKENED_THORAX.get(),
                    AWAKENED_LEGWRAPS.get(),
                    AWAKENED_TREADS.get());
            event.registerBlockEntity(BiomassCapabilities.BLOCK, FUEL_PAPILLA_ENTITY.get(), (papilla, side) -> papilla);
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    FUEL_PAPILLA_ENTITY.get(),
                    (papilla, side) -> BiomassFluids.handler(papilla));
        });
    }
}
