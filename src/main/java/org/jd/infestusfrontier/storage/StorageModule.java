package org.jd.infestusfrontier.storage;

import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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
import org.jd.infestusfrontier.integration.geckolib.OrganItem;
import org.jd.infestusfrontier.integration.geckolib.OrganRenderers;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.slf4j.Logger;

/** Owner of item, fluid and biomass balances. Ships the biomass fluid, its bucket and the Biomass Bladder. */
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
                    .strength(1.0F)
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
    /** The balance an organ carries on its item when broken and placed again. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BiomassTank>> TANK =
            COMPONENTS.registerComponentType(
                    "biomass_tank",
                    builder -> builder.persistent(BiomassCodecs.TANK).networkSynchronized(BiomassCodecs.TANK_STREAM));

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
        modBus.addListener((RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(
                    BiomassCapabilities.BLOCK, BIOMASS_BLADDER_ENTITY.get(), (bladder, side) -> bladder);
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    BIOMASS_BLADDER_ENTITY.get(),
                    (bladder, side) -> BiomassFluids.handler(bladder));
        });
        if (FMLEnvironment.dist == Dist.CLIENT) {
            OrganRenderers.register(modBus, "biomass_bladder", BIOMASS_BLADDER_ENTITY, BIOMASS_BLADDER_ITEM);
            modBus.addListener((RegisterMenuScreensEvent event) ->
                    event.register(BIOMASS_BLADDER_MENU.get(), BiomassBladderScreen::new));
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
