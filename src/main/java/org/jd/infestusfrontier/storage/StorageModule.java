package org.jd.infestusfrontier.storage;

import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.render.FrontStockRenderer;
import org.jd.infestusfrontier.foundation.render.JoinedTankRenderer;
import org.jd.infestusfrontier.integration.geckolib.OrganItem;
import org.jd.infestusfrontier.integration.geckolib.OrganRenderers;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.jd.infestusfrontier.storage.api.CapsuleStock;
import org.slf4j.Logger;

/**
 * Owner of item, fluid and biomass balances. Ships the biomass fluid, its bucket, the Biomass Bladder, the Reservoir
 * cell, the Capsule Stem and its Item Capsule pods.
 */
public final class StorageModule {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, InfestusFrontier.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, InfestusFrontier.MOD_ID);

    private static final ResourceLocation STILL =
            ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "block/biomass_still");
    private static final ResourceLocation FLOW =
            ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "block/biomass_flow");

    /** Thick, warm and slow: a puddle spreads two blocks and never makes new sources. */
    public static final DeferredHolder<FluidType, FluidType> BIOMASS_TYPE = FLUID_TYPES.register(
            "biomass",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.infestusfrontier.biomass")
                    .density(1200)
                    .viscosity(3000)
                    .temperature(310)
                    .canExtinguish(false)
                    .canConvertToSource(false)
                    .supportsBoating(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, FlowingFluid> BIOMASS =
            FLUIDS.register("biomass", () -> new BaseFlowingFluid.Source(fluidProperties()));
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, FlowingFluid> FLOWING_BIOMASS =
            FLUIDS.register("flowing_biomass", () -> new BaseFlowingFluid.Flowing(fluidProperties()));
    public static final DeferredBlock<LiquidBlock> BIOMASS_BLOCK = BLOCKS.register(
            "biomass",
            () -> new LiquidBlock(
                    BIOMASS.get(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GREEN)
                            .replaceable()
                            .noCollission()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid()
                            .sound(SoundType.EMPTY)));
    /** A vanilla bucket item. An empty bucket fills from any biomass block or puddle and becomes this. */
    public static final DeferredItem<BucketItem> BIOMASS_BUCKET = ITEMS.register(
            "biomass_bucket",
            () -> new BucketItem(
                    BIOMASS.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredBlock<Block> BIOMASS_BLADDER = BLOCKS.registerBlock(
            "biomass_bladder",
            BiomassBladderBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_MAGENTA)
                    .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion());
    public static final DeferredItem<OrganItem> BIOMASS_BLADDER_ITEM = ITEMS.register(
            "biomass_bladder", () -> new OrganItem(BIOMASS_BLADDER.get(), new Item.Properties(), "idle", "fill_0"));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BiomassBladderBlockEntity>> BIOMASS_BLADDER_ENTITY =
            BLOCK_ENTITIES.register(
                    "biomass_bladder",
                    () -> BlockEntityType.Builder.of(BiomassBladderBlockEntity::new, BIOMASS_BLADDER.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<BiomassBladderMenu>> BIOMASS_BLADDER_MENU =
            MENUS.register("biomass_bladder", () -> IMenuTypeExtension.create(BiomassBladderMenu::new));
    public static final DeferredBlock<ReservoirCellBlock> RESERVOIR_CELL = BLOCKS.registerBlock(
            "reservoir_cell",
            ReservoirCellBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false));
    public static final DeferredItem<BlockItem> RESERVOIR_CELL_ITEM = ITEMS.registerSimpleBlockItem(RESERVOIR_CELL);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReservoirCellBlockEntity>> RESERVOIR_CELL_ENTITY =
            BLOCK_ENTITIES.register(
                    "reservoir_cell",
                    () -> BlockEntityType.Builder.of(ReservoirCellBlockEntity::new, RESERVOIR_CELL.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<ReservoirCellMenu>> RESERVOIR_CELL_MENU =
            MENUS.register("reservoir_cell", () -> IMenuTypeExtension.create(ReservoirCellMenu::new));
    static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ReservoirCellRecipe>> RESERVOIR_CELL_RECIPE =
            RECIPE_SERIALIZERS.register("reservoir_cell", ReservoirCellRecipe.Serializer::new);
    /** The stem the pods grow on: tissue, which breaks to Dead Tissue and regrows from its root. */
    public static final DeferredBlock<CapsuleStemBlock> CAPSULE_STEM = BLOCKS.registerBlock(
            "capsule_stem",
            CapsuleStemBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false));

    public static final DeferredItem<BlockItem> CAPSULE_STEM_ITEM = ITEMS.registerSimpleBlockItem(CAPSULE_STEM);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CapsuleStemBlockEntity>> CAPSULE_STEM_ENTITY =
            BLOCK_ENTITIES.register(
                    "capsule_stem",
                    () -> BlockEntityType.Builder.of(CapsuleStemBlockEntity::new, CAPSULE_STEM.get())
                            .build(null));
    public static final DeferredBlock<ItemCapsuleBlock> ITEM_CAPSULE = BLOCKS.registerBlock(
            "item_capsule",
            ItemCapsuleBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false));
    public static final DeferredItem<OrganItem> ITEM_CAPSULE_ITEM =
            ITEMS.register("item_capsule", () -> new ItemCapsuleItem(ITEM_CAPSULE.get(), new Item.Properties()));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemCapsuleBlockEntity>> ITEM_CAPSULE_ENTITY =
            BLOCK_ENTITIES.register(
                    "item_capsule",
                    () -> BlockEntityType.Builder.of(ItemCapsuleBlockEntity::new, ITEM_CAPSULE.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<ItemCapsuleMenu>> ITEM_CAPSULE_MENU =
            MENUS.register("item_capsule", () -> IMenuTypeExtension.create(ItemCapsuleMenu::new));
    /** Blocks a Reservoir cell may also stand on: pumps that fill it from below. */
    public static final TagKey<Block> RESERVOIR_SUPPORTS = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "reservoir_supports"));
    /** The balance an organ carries on its item when broken and placed again. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BiomassTank>> TANK =
            COMPONENTS.registerComponentType(
                    "biomass_tank",
                    builder -> builder.persistent(BiomassCodecs.TANK).networkSynchronized(BiomassCodecs.TANK_STREAM));

    /** The compartment and grade a pod carries on its item when broken and placed again. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CapsuleStock>> CAPSULE_STOCK =
            COMPONENTS.registerComponentType(
                    "capsule_stock",
                    builder -> builder.persistent(CapsuleStock.CODEC).networkSynchronized(CapsuleStock.STREAM_CODEC));

    private StorageModule() {}

    private static BaseFlowingFluid.Properties fluidProperties() {
        return new BaseFlowingFluid.Properties(BIOMASS_TYPE, BIOMASS, FLOWING_BIOMASS)
                .bucket(BIOMASS_BUCKET)
                .block(BIOMASS_BLOCK)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(20);
    }

    public static void register(IEventBus modBus) {
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        COMPONENTS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        modBus.addListener((RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(
                    BiomassCapabilities.BLOCK, BIOMASS_BLADDER_ENTITY.get(), (bladder, side) -> bladder);
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    BIOMASS_BLADDER_ENTITY.get(),
                    (bladder, side) -> BiomassFluids.handler(bladder));
            event.registerBlockEntity(BiomassCapabilities.BLOCK, RESERVOIR_CELL_ENTITY.get(), (cell, side) -> cell);
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    RESERVOIR_CELL_ENTITY.get(),
                    (cell, side) -> BiomassFluids.handler(cell));
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK,
                    ITEM_CAPSULE_ENTITY.get(),
                    (pod, side) -> new CapsuleItems(() -> Stems.of(pod)));
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK,
                    CAPSULE_STEM_ENTITY.get(),
                    (cell, side) -> new CapsuleItems(() -> Stems.of(cell)));
        });
        if (FMLEnvironment.dist == Dist.CLIENT) {
            OrganRenderers.register(modBus, "biomass_bladder", BIOMASS_BLADDER_ENTITY, BIOMASS_BLADDER_ITEM);
            modBus.addListener((RegisterMenuScreensEvent event) -> {
                event.register(BIOMASS_BLADDER_MENU.get(), BiomassBladderScreen::new);
                event.register(RESERVOIR_CELL_MENU.get(), ReservoirCellScreen::new);
                event.register(ITEM_CAPSULE_MENU.get(), ItemCapsuleScreen::new);
            });
            // one pocket on the pod's outer face, the icon over its count; art/gen_item_capsule.py POCKET
            OrganRenderers.register(
                    modBus,
                    "item_capsule",
                    ITEM_CAPSULE_ENTITY,
                    ITEM_CAPSULE_ITEM,
                    FrontStockRenderer.overlay(
                            ItemCapsuleBlock.FACING,
                            ItemCapsuleBlockEntity::shown,
                            new FrontStockRenderer.Pockets(1, 8F, 9.0F, 7F, 5.5F, 0.3F, 4F, 0.95F),
                            ResourceLocation.fromNamespaceAndPath("infestusfrontier", "block/item_capsule_lining")));
            // the liquid stops inside the membrane panes, above the fleshy base and under the lid
            JoinedTankRenderer.register(
                    modBus,
                    RESERVOIR_CELL_ENTITY,
                    ReservoirCellBlock.FILL,
                    ReservoirCellBlock.FILL_STEPS,
                    ReservoirCellBlock.JOINED,
                    () -> STILL,
                    new JoinedTankRenderer.Insets(0.75F / 16, 2F / 16, 0.75F / 16));
            modBus.addListener((RegisterClientExtensionsEvent event) -> event.registerFluidType(
                    new IClientFluidTypeExtensions() {
                        @Override
                        public ResourceLocation getStillTexture() {
                            return STILL;
                        }

                        @Override
                        public ResourceLocation getFlowingTexture() {
                            return FLOW;
                        }
                    },
                    BIOMASS_TYPE.get()));
        }
    }
}
