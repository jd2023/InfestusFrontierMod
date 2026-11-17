package org.jd.infestusfrontier.mining;

import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
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
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.core.foundation.TickQuota;
import org.jd.infestusfrontier.core.mining.Fracturing;
import org.jd.infestusfrontier.core.mining.RootMutation;
import org.jd.infestusfrontier.core.mining.Surveying;
import org.jd.infestusfrontier.foundation.render.GhostBoxRenderer;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.slf4j.Logger;

/**
 * Owner of mining: the Leaching Pod and the Leached Rock it leaves, the Tunnel Root with its Root Segments, the
 * root cells it lays and the genetic constructs that mutate it, the stationary cutting organs: the Fracture Jaw, the Work Bed and the Collection Cilia, and
 * surveyed sections: the Surveyed Tissue marker that holds a plan and the Structure Grower that builds it.
 */
public final class MiningModule {
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

    /** Ground a Leaching Pod may weaken; the pod also needs it soft enough, loose of fluid and free of a block entity. */
    public static final TagKey<Block> LEACHABLE = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "leachable"));

    /** Weakened ground: translucent, solid, breaks by hand and drops nothing. */
    public static final DeferredBlock<LeachedRockBlock> LEACHED_ROCK = BLOCKS.registerBlock(
            "leached_rock",
            LeachedRockBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(LeachedRockBlock.HARDNESS)
                    .sound(SoundType.AMETHYST)
                    .noOcclusion()
                    .lightLevel(LeachedRockBlock::light)
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false));

    public static final DeferredItem<BlockItem> LEACHED_ROCK_ITEM = ITEMS.registerSimpleBlockItem(LEACHED_ROCK);

    /** A one-use organ that leaches the ground below it. Grown from an Organ Bud; never picked up again. */
    public static final DeferredBlock<LeachingPodBlock> LEACHING_POD = BLOCKS.registerBlock(
            "leaching_pod",
            LeachingPodBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(LeachingPodBlock.HARDNESS, LeachingPodBlock.BLAST_RESISTANCE)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion());

    public static final DeferredItem<BlockItem> LEACHING_POD_ITEM = ITEMS.registerSimpleBlockItem(LEACHING_POD);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LeachingPodBlockEntity>> LEACHING_POD_ENTITY =
            BLOCK_ENTITIES.register(
                    "leaching_pod",
                    () -> BlockEntityType.Builder.of(LeachingPodBlockEntity::new, LEACHING_POD.get())
                            .build(null));

    /** Plain ground a Tunnel Root may replace with a root cell in the floor of its tunnel. */
    public static final TagKey<Block> TUNNEL_FLOOR = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "tunnel_floor"));

    /** What a head lays: one segment is one root cell. Loaded into the head by hand. */
    public static final DeferredItem<Item> ROOT_SEGMENT = ITEMS.registerSimpleItem("root_segment");

    /** A cell of a root in a tunnel floor. Grown tissue: it never moves and breaking it gives nothing back. */
    public static final DeferredBlock<RootCellBlock> ROOT_CELL = BLOCKS.registerBlock(
            "root_cell",
            RootCellBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIRT)
                    .strength(RootCellBlock.HARDNESS)
                    .sound(SoundType.ROOTED_DIRT));

    public static final DeferredItem<BlockItem> ROOT_CELL_ITEM = ITEMS.registerSimpleBlockItem(ROOT_CELL);

    /** The head of a Tunnel Root: grown from an Organ Bud, fed and steered by hand, it cuts a 1x2 tunnel. */
    public static final DeferredBlock<TunnelRootBlock> TUNNEL_ROOT = BLOCKS.registerBlock(
            "tunnel_root",
            TunnelRootBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                    .sound(SoundType.ROOTED_DIRT)
                    .noOcclusion());

    public static final DeferredItem<TunnelRootItem> TUNNEL_ROOT_ITEM =
            ITEMS.registerItem("tunnel_root", properties -> new TunnelRootItem(TUNNEL_ROOT.get(), properties));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TunnelRootBlockEntity>> TUNNEL_ROOT_ENTITY =
            BLOCK_ENTITIES.register(
                    "tunnel_root",
                    () -> BlockEntityType.Builder.of(TunnelRootBlockEntity::new, TUNNEL_ROOT.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<TunnelRootMenu>> TUNNEL_ROOT_MENU =
            MENUS.register("tunnel_root", () -> IMenuTypeExtension.create(TunnelRootMenu::new));

    /** Constructs a hand stack holds at most. */
    static final int CONSTRUCT_STACK = 16;

    /** The genetic construct for Stairs, produced in a DNA Bank; applied to a head it makes a Rootstock. */
    public static final DeferredItem<ConstructItem> STAIRS_CONSTRUCT = ITEMS.registerItem(
            "stairs_construct",
            properties -> new ConstructItem(RootMutation.STAIRS, properties),
            new Item.Properties().stacksTo(CONSTRUCT_STACK));

    /** The genetic construct for Wider Section, produced in a DNA Bank; applied to a Rootstock. */
    public static final DeferredItem<ConstructItem> WIDER_SECTION_CONSTRUCT = ITEMS.registerItem(
            "wider_section_construct",
            properties -> new ConstructItem(RootMutation.WIDER_SECTION, properties),
            new Item.Properties().stacksTo(CONSTRUCT_STACK));

    /** What a broken head carries on its item: everything it held, the cells it has laid and its mutations. */
    static final DeferredHolder<DataComponentType<?>, DataComponentType<TunnelRootContents>> TUNNEL_ROOT_CONTENTS =
            COMPONENTS.registerComponentType(
                    "tunnel_root", builder -> builder.persistent(TunnelRootContents.CODEC.codec()));

    /** The cuts the Fracture Jaws of one dimension share each tick. */
    static final DeferredHolder<AttachmentType<?>, AttachmentType<TickQuota>> JAW_CUTS = ATTACHMENTS.register(
            "jaw_cuts",
            () -> AttachmentType.builder(() -> new TickQuota(Fracturing.CUTS_PER_TICK))
                    .build());

    /** A stationary cutter: it cuts the block in front of it into the Work Bed behind it. Crafted from a bud. */
    public static final DeferredBlock<FractureJawBlock> FRACTURE_JAW = BLOCKS.registerBlock(
            "fracture_jaw",
            FractureJawBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                    .sound(SoundType.BONE_BLOCK)
                    .noOcclusion());

    public static final DeferredItem<BlockItem> FRACTURE_JAW_ITEM = ITEMS.registerSimpleBlockItem(FRACTURE_JAW);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FractureJawBlockEntity>> FRACTURE_JAW_ENTITY =
            BLOCK_ENTITIES.register(
                    "fracture_jaw",
                    () -> BlockEntityType.Builder.of(FractureJawBlockEntity::new, FRACTURE_JAW.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<WorkOrganMenu>> FRACTURE_JAW_MENU = MENUS.register(
            "fracture_jaw",
            () -> IMenuTypeExtension.create(
                    (id, inventory, extra) -> new WorkOrganMenu(WorkOrganMenu.Kind.JAW, id, inventory, extra)));
    /** What a broken jaw carries on its item: its biomass and the cuts it has made. */
    static final DeferredHolder<DataComponentType<?>, DataComponentType<FractureJawContents>> FRACTURE_JAW_CONTENTS =
            COMPONENTS.registerComponentType(
                    "fracture_jaw", builder -> builder.persistent(FractureJawContents.CODEC.codec()));

    /** A living trough that holds what a jaw cuts. Crafted from a bud. */
    public static final DeferredBlock<WorkBedBlock> WORK_BED = BLOCKS.registerBlock(
            "work_bed",
            WorkBedBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_PINK)
                    .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion());

    public static final DeferredItem<BlockItem> WORK_BED_ITEM = ITEMS.registerSimpleBlockItem(WORK_BED);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkBedBlockEntity>> WORK_BED_ENTITY =
            BLOCK_ENTITIES.register(
                    "work_bed",
                    () -> BlockEntityType.Builder.of(WorkBedBlockEntity::new, WORK_BED.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<WorkBedMenu>> WORK_BED_MENU =
            MENUS.register("work_bed", () -> IMenuTypeExtension.create(WorkBedMenu::new));
    /** What a broken bed carries on its item: what it holds and what it has received. */
    static final DeferredHolder<DataComponentType<?>, DataComponentType<WorkBedContents>> WORK_BED_CONTENTS =
            COMPONENTS.registerComponentType("work_bed", builder -> builder.persistent(WorkBedContents.CODEC.codec()));

    /** Organ standing on a Mouth that sweeps a Work Bed into the Item Vein network. Crafted from a bud. */
    public static final DeferredBlock<CollectionCiliaBlock> COLLECTION_CILIA = BLOCKS.registerBlock(
            "collection_cilia",
            CollectionCiliaBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion());

    public static final DeferredItem<BlockItem> COLLECTION_CILIA_ITEM = ITEMS.registerSimpleBlockItem(COLLECTION_CILIA);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CollectionCiliaBlockEntity>>
            COLLECTION_CILIA_ENTITY = BLOCK_ENTITIES.register(
                    "collection_cilia",
                    () -> BlockEntityType.Builder.of(CollectionCiliaBlockEntity::new, COLLECTION_CILIA.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<WorkOrganMenu>> COLLECTION_CILIA_MENU = MENUS.register(
            "collection_cilia",
            () -> IMenuTypeExtension.create(
                    (id, inventory, extra) -> new WorkOrganMenu(WorkOrganMenu.Kind.CILIA, id, inventory, extra)));
    /** What broken cilia carry on their item: their biomass and the items they have sent. */
    static final DeferredHolder<DataComponentType<?>, DataComponentType<CollectionCiliaContents>>
            COLLECTION_CILIA_CONTENTS = COMPONENTS.registerComponentType(
                    "collection_cilia", builder -> builder.persistent(CollectionCiliaContents.CODEC.codec()));

    /** Stairs a Structure Grower lays as treads; it also needs them to be plain stair blocks. */
    public static final TagKey<Item> MINE_TREADS = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "mine_treads"));

    /** A flush marker at a section's entrance that holds its plan and draws its preview. Crafted. */
    public static final DeferredBlock<SurveyedTissueBlock> SURVEYED_TISSUE = BLOCKS.registerBlock(
            "surveyed_tissue",
            SurveyedTissueBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_GREEN)
                    .strength(SurveyedTissueBlock.HARDNESS)
                    .sound(SoundType.ROOTED_DIRT));

    public static final DeferredItem<BlockItem> SURVEYED_TISSUE_ITEM = ITEMS.registerSimpleBlockItem(SURVEYED_TISSUE);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SurveyedTissueBlockEntity>> SURVEYED_TISSUE_ENTITY =
            BLOCK_ENTITIES.register(
                    "surveyed_tissue",
                    () -> BlockEntityType.Builder.of(SurveyedTissueBlockEntity::new, SURVEYED_TISSUE.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<SurveyMenu>> SURVEY_MENU =
            MENUS.register("surveyed_tissue", () -> IMenuTypeExtension.create(SurveyMenu::new));

    /** The steps the Structure Growers of one dimension build each tick, together. */
    static final DeferredHolder<AttachmentType<?>, AttachmentType<TickQuota>> GROWER_STEPS = ATTACHMENTS.register(
            "grower_steps",
            () -> AttachmentType.builder(() -> new TickQuota(Surveying.STEPS_PER_TICK))
                    .build());

    /** An organ on Surveyed Tissue that builds the approved plan's root cells, treads and lights. Crafted. */
    public static final DeferredBlock<StructureGrowerBlock> STRUCTURE_GROWER = BLOCKS.registerBlock(
            "structure_grower",
            StructureGrowerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                    .sound(SoundType.ROOTED_DIRT)
                    .noOcclusion());

    public static final DeferredItem<BlockItem> STRUCTURE_GROWER_ITEM = ITEMS.registerSimpleBlockItem(STRUCTURE_GROWER);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StructureGrowerBlockEntity>>
            STRUCTURE_GROWER_ENTITY = BLOCK_ENTITIES.register(
                    "structure_grower",
                    () -> BlockEntityType.Builder.of(StructureGrowerBlockEntity::new, STRUCTURE_GROWER.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<StructureGrowerMenu>> STRUCTURE_GROWER_MENU =
            MENUS.register("structure_grower", () -> IMenuTypeExtension.create(StructureGrowerMenu::new));
    /** What a broken grower carries on its item: its stock and the steps it has built. */
    static final DeferredHolder<DataComponentType<?>, DataComponentType<StructureGrowerContents>>
            STRUCTURE_GROWER_CONTENTS = COMPONENTS.registerComponentType(
                    "structure_grower", builder -> builder.persistent(StructureGrowerContents.CODEC.codec()));

    /** Blocks from a marker within which its preview is drawn: past the far end of the longest route. */
    private static final int PREVIEW_DISTANCE = 96;

    private MiningModule() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        COMPONENTS.register(modBus);
        ATTACHMENTS.register(modBus);
        modBus.addListener((RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(BiomassCapabilities.BLOCK, FRACTURE_JAW_ENTITY.get(), (jaw, side) -> jaw);
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    FRACTURE_JAW_ENTITY.get(),
                    (jaw, side) -> BiomassFluids.handler(jaw));
            event.registerBlockEntity(BiomassCapabilities.BLOCK, COLLECTION_CILIA_ENTITY.get(), (cilia, side) -> cilia);
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK, STRUCTURE_GROWER_ENTITY.get(), (grower, side) -> grower.intake());
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    COLLECTION_CILIA_ENTITY.get(),
                    (cilia, side) -> BiomassFluids.handler(cilia));
        });
        if (FMLEnvironment.dist == Dist.CLIENT) {
            GhostBoxRenderer.register(modBus, SURVEYED_TISSUE_ENTITY, MiningModule::preview, PREVIEW_DISTANCE);
            modBus.addListener((RegisterMenuScreensEvent event) -> {
                event.register(TUNNEL_ROOT_MENU.get(), TunnelRootScreen::new);
                event.register(FRACTURE_JAW_MENU.get(), WorkOrganScreen::new);
                event.register(COLLECTION_CILIA_MENU.get(), WorkOrganScreen::new);
                event.register(WORK_BED_MENU.get(), WorkBedScreen::new);
                event.register(SURVEY_MENU.get(), SurveyScreen::new);
                event.register(STRUCTURE_GROWER_MENU.get(), StructureGrowerScreen::new);
            });
        }
    }

    /**
     * A marker's preview in colour: amber while a draft, green once approved; root cells and treads stronger than the
     * passage, lights yellow. Client only.
     */
    private static List<GhostBoxRenderer.Ghost> preview(SurveyedTissueBlockEntity marker) {
        return SurveyPreview.of(marker).stream()
                .map(box -> new GhostBoxRenderer.Ghost(box.box(), colour(box)))
                .toList();
    }

    private static int colour(SurveyPreview.Box box) {
        return switch (box.part()) {
            case ROOT, TREAD -> box.approved() ? 0x7040C070 : 0x70E0A030;
            case PASSAGE -> box.approved() ? 0x2860E090 : 0x28F0C060;
            case LIGHT -> 0xC0FFF080;
        };
    }
}
