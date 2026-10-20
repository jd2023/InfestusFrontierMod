package org.jd.infestusfrontier.cultivation;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
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
import org.jd.infestusfrontier.integration.geckolib.OrganItem;
import org.jd.infestusfrontier.integration.geckolib.OrganRenderers;
import org.jd.infestusfrontier.logistics.GraftItem;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.slf4j.Logger;

/**
 * Owner of crop growth, allocated harvest and planting reserves. Ships Cultivation Tissue and the Rooting Gel that
 * grows it, the Seed Pouch, the Planting Proboscis and the Harvest Corolla.
 */
public final class CultivationModule {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, InfestusFrontier.MOD_ID);

    public static final DeferredBlock<CultivationTissueBlock> CULTIVATION_TISSUE = BLOCKS.registerBlock(
            "cultivation_tissue",
            CultivationTissueBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(0.6F)
                    .sound(SoundType.GRAVEL)
                    .randomTicks());
    public static final DeferredItem<BlockItem> CULTIVATION_TISSUE_ITEM =
            ITEMS.registerSimpleBlockItem(CULTIVATION_TISSUE);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CultivationTissueBlockEntity>>
            CULTIVATION_TISSUE_ENTITY = BLOCK_ENTITIES.register(
                    "cultivation_tissue",
                    () -> BlockEntityType.Builder.of(CultivationTissueBlockEntity::new, CULTIVATION_TISSUE.get())
                            .build(null));
    /** Grows a bed out of a mature cell; the Bowl and the Cyst make it (ITEM_CATALOG I033). */
    public static final DeferredItem<Item> ROOTING_GEL =
            ITEMS.registerItem("rooting_gel", properties -> new GraftItem(properties, () -> CULTIVATION_TISSUE.get()));

    public static final DeferredBlock<SeedPouchBlock> SEED_POUCH = BLOCKS.registerBlock(
            "seed_pouch",
            SeedPouchBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.8F)
                    .sound(SoundType.WOOL)
                    .noOcclusion());
    public static final DeferredItem<OrganItem> SEED_POUCH_ITEM =
            ITEMS.register("seed_pouch", () -> new OrganItem(SEED_POUCH.get(), new Item.Properties(), "idle"));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SeedPouchBlockEntity>> SEED_POUCH_ENTITY =
            BLOCK_ENTITIES.register(
                    "seed_pouch",
                    () -> BlockEntityType.Builder.of(SeedPouchBlockEntity::new, SEED_POUCH.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<SeedPouchMenu>> SEED_POUCH_MENU =
            MENUS.register("seed_pouch", () -> IMenuTypeExtension.create(SeedPouchMenu::new));

    public static final DeferredBlock<PlotOrganBlock> PLANTING_PROBOSCIS = BLOCKS.registerBlock(
            "planting_proboscis",
            properties -> new PlotOrganBlock(
                    properties,
                    () -> CultivationModule.PLANTING_PROBOSCIS_ENTITY.get(),
                    Block.box(2, 0, 2, 14, 14, 14)),
            organ(MapColor.COLOR_GREEN));
    public static final DeferredItem<OrganItem> PLANTING_PROBOSCIS_ITEM = ITEMS.register(
            "planting_proboscis", () -> new OrganItem(PLANTING_PROBOSCIS.get(), new Item.Properties(), "idle"));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlantingProboscisBlockEntity>>
            PLANTING_PROBOSCIS_ENTITY = BLOCK_ENTITIES.register(
                    "planting_proboscis",
                    () -> BlockEntityType.Builder.of(PlantingProboscisBlockEntity::new, PLANTING_PROBOSCIS.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<PlotOrganMenu>> PLANTING_PROBOSCIS_MENU =
            MENUS.register("planting_proboscis", () -> IMenuTypeExtension.create(PlotOrganMenu::planter));

    public static final DeferredBlock<PlotOrganBlock> HARVEST_COROLLA = BLOCKS.registerBlock(
            "harvest_corolla",
            properties -> new PlotOrganBlock(
                    properties, () -> CultivationModule.HARVEST_COROLLA_ENTITY.get(), Block.box(1, 0, 1, 15, 14, 15)),
            organ(MapColor.COLOR_PINK));
    public static final DeferredItem<OrganItem> HARVEST_COROLLA_ITEM = ITEMS.register(
            "harvest_corolla", () -> new OrganItem(HARVEST_COROLLA.get(), new Item.Properties(), "idle"));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HarvestCorollaBlockEntity>> HARVEST_COROLLA_ENTITY =
            BLOCK_ENTITIES.register(
                    "harvest_corolla",
                    () -> BlockEntityType.Builder.of(HarvestCorollaBlockEntity::new, HARVEST_COROLLA.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<PlotOrganMenu>> HARVEST_COROLLA_MENU =
            MENUS.register("harvest_corolla", () -> IMenuTypeExtension.create(PlotOrganMenu::harvester));

    private CultivationModule() {}

    private static BlockBehaviour.Properties organ(MapColor colour) {
        return BlockBehaviour.Properties.of()
                .mapColor(colour)
                .strength(1.0F)
                .sound(SoundType.SLIME_BLOCK)
                .noOcclusion();
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener((RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK, CULTIVATION_TISSUE_ENTITY.get(), (bed, side) -> bed.water());
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK, SEED_POUCH_ENTITY.get(), (pouch, side) -> pouch.surplus());
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK, PLANTING_PROBOSCIS_ENTITY.get(), (organ, side) -> organ.items());
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK, HARVEST_COROLLA_ENTITY.get(), (organ, side) -> organ.output());
            for (var entity : java.util.List.of(PLANTING_PROBOSCIS_ENTITY, HARVEST_COROLLA_ENTITY)) {
                event.registerBlockEntity(BiomassCapabilities.BLOCK, entity.get(), (organ, side) -> organ);
                event.registerBlockEntity(
                        Capabilities.FluidHandler.BLOCK, entity.get(), (organ, side) -> BiomassFluids.handler(organ));
            }
        });
        if (FMLEnvironment.dist == Dist.CLIENT) {
            OrganRenderers.register(modBus, "seed_pouch", SEED_POUCH_ENTITY, SEED_POUCH_ITEM);
            OrganRenderers.register(modBus, "planting_proboscis", PLANTING_PROBOSCIS_ENTITY, PLANTING_PROBOSCIS_ITEM);
            OrganRenderers.register(modBus, "harvest_corolla", HARVEST_COROLLA_ENTITY, HARVEST_COROLLA_ITEM);
            modBus.addListener((RegisterMenuScreensEvent event) -> {
                event.register(SEED_POUCH_MENU.get(), SeedPouchScreen::new);
                event.register(PLANTING_PROBOSCIS_MENU.get(), PlotOrganScreen::planter);
                event.register(HARVEST_COROLLA_MENU.get(), PlotOrganScreen::harvester);
            });
        }
    }
}
