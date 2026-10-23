package org.jd.infestusfrontier.logistics;

import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.core.foundation.TickQuota;
import org.jd.infestusfrontier.integration.geckolib.OrganItem;
import org.jd.infestusfrontier.integration.geckolib.OrganRenderers;
import org.jd.infestusfrontier.logistics.api.LogisticsCapabilities;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.slf4j.Logger;

/**
 * Owner of routes, ports and the movement of items, fluids and biomass. Ships the three veins, the treatments that
 * grow them, the Septum Crossing and the Vascular Stitch, and the Vascular Junction.
 */
public final class LogisticsModule {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, InfestusFrontier.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, InfestusFrontier.MOD_ID);

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, InfestusFrontier.MOD_ID);
    /** Mouth pulses admitted per dimension per tick. */
    static final DeferredHolder<AttachmentType<?>, AttachmentType<TickQuota>> MOUTH_QUOTA = ATTACHMENTS.register(
            "mouth_quota", () -> AttachmentType.builder(() -> new TickQuota(64)).build());

    static final DeferredHolder<AttachmentType<?>, AttachmentType<RouteEpoch>> ROUTE_EPOCH = ATTACHMENTS.register(
            "route_epoch", () -> AttachmentType.builder(RouteEpoch::new).build());
    public static final DeferredBlock<MouthBlock> MOUTH = BLOCKS.registerBlock(
            "mouth",
            MouthBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_PINK)
                    .strength(0.6F)
                    .sound(SoundType.GRAVEL));
    public static final DeferredItem<BlockItem> MOUTH_ITEM = ITEMS.registerSimpleBlockItem(MOUTH);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MouthBlockEntity>> MOUTH_ENTITY =
            BLOCK_ENTITIES.register(
                    "mouth",
                    () -> BlockEntityType.Builder.of(MouthBlockEntity::new, MOUTH.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<MouthMenu>> MOUTH_MENU =
            MENUS.register("mouth", () -> IMenuTypeExtension.create(MouthMenu::new));

    /** The port organs: a Feeding Lobe puts items into the organ it faces, a Yield Lobe takes its products. */
    public static final DeferredBlock<LobeBlock> FEEDING_LOBE = lobe("feeding_lobe", true);

    public static final DeferredBlock<LobeBlock> YIELD_LOBE = lobe("yield_lobe", false);
    public static final DeferredItem<BlockItem> FEEDING_LOBE_ITEM = ITEMS.registerSimpleBlockItem(FEEDING_LOBE);
    public static final DeferredItem<BlockItem> YIELD_LOBE_ITEM = ITEMS.registerSimpleBlockItem(YIELD_LOBE);

    private static DeferredBlock<LobeBlock> lobe(String id, boolean feeds) {
        return BLOCKS.registerBlock(
                id,
                properties -> new LobeBlock(feeds, properties),
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_PINK)
                        .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                        .sound(SoundType.SLIME_BLOCK)
                        .noOcclusion());
    }

    /** Stands between a store and what drains it, and keeps the reserve. */
    public static final DeferredBlock<FilterValveBlock> FILTER_VALVE = BLOCKS.registerBlock(
            "filter_valve",
            FilterValveBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                    .sound(SoundType.BONE_BLOCK)
                    .noOcclusion());

    public static final DeferredItem<BlockItem> FILTER_VALVE_ITEM = ITEMS.registerSimpleBlockItem(FILTER_VALVE);

    public static final DeferredBlock<VeinBlock> BIOMASS_VEIN = vein(Carrier.BIOMASS);
    public static final DeferredBlock<VeinBlock> FLUID_VEIN = vein(Carrier.FLUID);
    public static final DeferredBlock<VeinBlock> ITEM_VEIN = vein(Carrier.ITEM);
    public static final DeferredItem<BlockItem> BIOMASS_VEIN_ITEM = ITEMS.registerSimpleBlockItem(BIOMASS_VEIN);
    public static final DeferredItem<BlockItem> FLUID_VEIN_ITEM = ITEMS.registerSimpleBlockItem(FLUID_VEIN);
    public static final DeferredItem<BlockItem> ITEM_VEIN_ITEM = ITEMS.registerSimpleBlockItem(ITEM_VEIN);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VeinBlockEntity>> VEIN_ENTITY =
            BLOCK_ENTITIES.register(
                    "vein",
                    () -> BlockEntityType.Builder.of(VeinBlockEntity::new, BIOMASS_VEIN.get(), FLUID_VEIN.get())
                            .build(null));

    /** Two passages of one carrier through one cell, grafted onto a vein; it holds nothing. */
    public static final DeferredBlock<SeptumCrossingBlock> SEPTUM_CROSSING = BLOCKS.registerBlock(
            "septum_crossing",
            SeptumCrossingBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_PINK)
                    .strength(0.6F)
                    .sound(SoundType.GRAVEL)
                    .pushReaction(PushReaction.BLOCK));

    public static final DeferredItem<Item> SEPTUM_CROSSING_ITEM =
            ITEMS.registerItem("septum_crossing", SeptumCrossingItem::new);

    /** A tendon over a one-block step between two vein cells; it holds nothing. */
    public static final DeferredBlock<VascularStitchBlock> VASCULAR_STITCH = BLOCKS.registerBlock(
            "vascular_stitch",
            VascularStitchBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(0.2F)
                    .sound(SoundType.SLIME_BLOCK)
                    .noCollission()
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredItem<BlockItem> VASCULAR_STITCH_ITEM = ITEMS.registerSimpleBlockItem(VASCULAR_STITCH);

    /** The treatments that grow a vein out of a mature cell; the Cyst makes them (ITEM_CATALOG I021 to I023, I028). */
    public static final DeferredItem<Item> CAPILLARY_GEL =
            ITEMS.registerItem("capillary_gel", properties -> new GraftItem(properties, () -> BIOMASS_VEIN.get()));

    public static final DeferredItem<Item> CONTRACTILE_FIBER =
            ITEMS.registerItem("contractile_fiber", properties -> new GraftItem(properties, () -> ITEM_VEIN.get()));
    public static final DeferredItem<Item> FILTER_MEMBRANE =
            ITEMS.registerItem("filter_membrane", properties -> new GraftItem(properties, () -> FLUID_VEIN.get()));
    public static final DeferredItem<Item> SEALING_RESIN = ITEMS.registerItem("sealing_resin", SealingResinItem::new);

    public static final DeferredBlock<VascularJunctionBlock> VASCULAR_JUNCTION = BLOCKS.registerBlock(
            "vascular_junction",
            VascularJunctionBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_MAGENTA)
                    .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion());
    public static final DeferredItem<OrganItem> VASCULAR_JUNCTION_ITEM = ITEMS.register(
            "vascular_junction",
            () -> new OrganItem(
                    VASCULAR_JUNCTION.get(),
                    new Item.Properties(),
                    "idle",
                    "up_closed",
                    "down_closed",
                    "north_closed",
                    "south_closed",
                    "east_closed",
                    "west_closed"));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VascularJunctionBlockEntity>>
            VASCULAR_JUNCTION_ENTITY = BLOCK_ENTITIES.register(
                    "vascular_junction",
                    () -> BlockEntityType.Builder.of(VascularJunctionBlockEntity::new, VASCULAR_JUNCTION.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<VascularJunctionMenu>> VASCULAR_JUNCTION_MENU =
            MENUS.register("vascular_junction", () -> IMenuTypeExtension.create(VascularJunctionMenu::new));
    /** The fluid a broken vein cell or junction carries on its item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID_CONTENT =
            COMPONENTS.registerComponentType(
                    "fluid_content",
                    builder -> builder.persistent(SimpleFluidContent.CODEC)
                            .networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    private LogisticsModule() {}

    private static DeferredBlock<VeinBlock> vein(Carrier carrier) {
        return BLOCKS.registerBlock(
                carrier.id(),
                properties -> new VeinBlock(carrier, properties),
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.TERRACOTTA_PINK)
                        .strength(0.6F)
                        .sound(SoundType.GRAVEL)
                        .pushReaction(PushReaction.BLOCK));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        COMPONENTS.register(modBus);
        ATTACHMENTS.register(modBus);
        modBus.addListener((RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, VEIN_ENTITY.get(), VeinBlockEntity::fluidPort);
            event.registerBlockEntity(LogisticsCapabilities.MOUTH_SCREEN, MOUTH_ENTITY.get(), (mouth, none) -> mouth);
            event.registerBlock(
                    Capabilities.FluidHandler.BLOCK,
                    (level, pos, state, entity, side) -> side == state.getValue(FilterValveBlock.FACING)
                            ? null
                            : FILTER_VALVE.get().handler(level, pos),
                    FILTER_VALVE.get());
            event.registerBlock(
                    Capabilities.ItemHandler.BLOCK,
                    (level, pos, state, entity, side) -> ((LobeBlock) state.getBlock()).handler(level, pos),
                    FEEDING_LOBE.get(),
                    YIELD_LOBE.get());
            event.registerBlock(
                    Capabilities.FluidHandler.BLOCK,
                    (level, pos, state, entity, side) -> SeptumCrossingBlock.fluid(level, pos, state, side),
                    SEPTUM_CROSSING.get());
            event.registerBlock(
                    LogisticsCapabilities.PORT_SWITCH,
                    (level, pos, state, entity, side) -> SeptumCrossingBlock.ports(level, pos),
                    SEPTUM_CROSSING.get());
            event.registerBlock(
                    Capabilities.FluidHandler.BLOCK,
                    (level, pos, state, entity, side) -> VascularStitchBlock.fluid(level, pos, state, side),
                    VASCULAR_STITCH.get());
            event.registerBlockEntity(
                    BiomassCapabilities.BLOCK,
                    VEIN_ENTITY.get(),
                    (vein, side) -> vein.carrier() == Carrier.BIOMASS ? vein : null);
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    VASCULAR_JUNCTION_ENTITY.get(),
                    VascularJunctionBlockEntity::fluidPort);
            event.registerBlockEntity(
                    BiomassCapabilities.BLOCK, VASCULAR_JUNCTION_ENTITY.get(), (junction, side) -> junction);
            event.registerBlockEntity(
                    LogisticsCapabilities.PORT_SWITCH, VASCULAR_JUNCTION_ENTITY.get(), (junction, side) -> junction);
        });
        if (FMLEnvironment.dist == Dist.CLIENT) {
            OrganRenderers.register(modBus, "vascular_junction", VASCULAR_JUNCTION_ENTITY, VASCULAR_JUNCTION_ITEM);
            modBus.addListener((RegisterMenuScreensEvent event) -> {
                event.register(VASCULAR_JUNCTION_MENU.get(), VascularJunctionScreen::new);
                event.register(MOUTH_MENU.get(), MouthScreen::new);
            });
        }
    }

    /** A cell of some item network in {@code level} was placed or removed, or a Mouth's routing changed. */
    static void routesChanged(Level level) {
        level.getData(ROUTE_EPOCH).changed();
    }

    /** The veins, for anyone listing them. */
    public static List<DeferredBlock<VeinBlock>> veins() {
        return List.of(BIOMASS_VEIN, FLUID_VEIN, ITEM_VEIN);
    }
}
