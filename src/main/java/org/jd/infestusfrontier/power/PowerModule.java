package org.jd.infestusfrontier.power;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.*;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.core.foundation.TickQuota;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.power.api.SteamDrive;
import org.slf4j.Logger;

public final class PowerModule {
    static final Logger LOGGER = LogUtils.getLogger();
    static final int DIMENSION_PULSES = 16;
    static final int SERVER_PULSES = 32;
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, InfestusFrontier.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, InfestusFrontier.MOD_ID);
    public static final DeferredHolder<FluidType, FluidType> STEAM_TYPE = FLUID_TYPES.register(
            "steam",
            () -> new FluidType(FluidType.Properties.create()
                    .density(-100)
                    .viscosity(100)
                    .temperature(450)
                    .canConvertToSource(false)));
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, FlowingFluid> STEAM =
            FLUIDS.register("steam", () -> new BaseFlowingFluid.Source(fluidProperties()));
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, FlowingFluid> FLOWING_STEAM =
            FLUIDS.register("flowing_steam", () -> new BaseFlowingFluid.Flowing(fluidProperties()));
    public static final DeferredBlock<ThermalHeartBlock> HEART = core("thermal_heart");
    public static final DeferredBlock<ThermalHeartBlock> RECEIVER = core("thermal_receiver");
    public static final DeferredBlock<ThermalHeartBlock> DRIVER = core("steam_driver");
    public static final DeferredBlock<PowerPartBlock> COLLECTOR = part("heat_collector");
    public static final DeferredBlock<PowerPartBlock> BOILER = part("boiler_chamber");
    public static final DeferredBlock<PowerPartBlock> GENERATOR = part("generating_chamber");
    public static final DeferredBlock<PowerPartBlock> OUTLET = part("pressure_outlet");
    public static final DeferredItem<Item> HEAT_CAPSULE =
            ITEMS.registerSimpleItem("heat_capsule", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> HOT_CAPSULE =
            ITEMS.registerSimpleItem("hot_heat_capsule", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> CELL =
            ITEMS.registerSimpleItem("charge_cell", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> CHARGED_CELL =
            ITEMS.registerSimpleItem("charged_cell", new Item.Properties().stacksTo(1));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThermalHeartBlockEntity>> CORE_ENTITY =
            BLOCK_ENTITIES.register(
                    "thermal_heart",
                    () -> BlockEntityType.Builder.of(
                                    ThermalHeartBlockEntity::new, HEART.get(), RECEIVER.get(), DRIVER.get())
                            .build(null));
    static final DeferredHolder<MenuType<?>, MenuType<PowerMenu>> MENU =
            MENUS.register("thermal_heart", () -> IMenuTypeExtension.create(PowerMenu::new));
    static final DeferredHolder<DataComponentType<?>, DataComponentType<PowerContents>> CONTENTS =
            COMPONENTS.registerComponentType("power", b -> b.persistent(PowerContents.CODEC.codec()));
    static final DeferredHolder<AttachmentType<?>, AttachmentType<TickQuota>> DIMENSION_WORK = ATTACHMENTS.register(
            "power_work",
            () -> AttachmentType.builder(() -> new TickQuota(DIMENSION_PULSES)).build());
    static final DeferredHolder<AttachmentType<?>, AttachmentType<TickQuota>> SERVER_WORK = ATTACHMENTS.register(
            "server_power_work",
            () -> AttachmentType.builder(() -> new TickQuota(SERVER_PULSES)).build());

    private PowerModule() {}

    private static BaseFlowingFluid.Properties fluidProperties() {
        return new BaseFlowingFluid.Properties(STEAM_TYPE, STEAM, FLOWING_STEAM);
    }

    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of()
                .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                .sound(SoundType.SLIME_BLOCK)
                .noOcclusion();
    }

    private static DeferredBlock<ThermalHeartBlock> core(String name) {
        var block = BLOCKS.register(name, () -> new ThermalHeartBlock(properties()));
        ITEMS.registerSimpleBlockItem(block, new Item.Properties().stacksTo(1));
        return block;
    }

    private static DeferredBlock<PowerPartBlock> part(String name) {
        var block = BLOCKS.register(name, () -> new PowerPartBlock(properties()));
        ITEMS.registerSimpleBlockItem(block);
        return block;
    }

    static Optional<ThermalHeartBlockEntity> coreAt(Level level, BlockPos part) {
        if (!level.hasChunkAt(part)) return Optional.empty();
        var state = level.getBlockState(part);
        if (!(state.getBlock() instanceof PowerPartBlock)) return Optional.empty();
        BlockPos core = part.relative(state.getValue(PowerPartBlock.FACING));
        return level.hasChunkAt(core)
                        && level.getBlockEntity(core) instanceof ThermalHeartBlockEntity heart
                        && !heart.driver()
                ? Optional.of(heart)
                : Optional.empty();
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        ATTACHMENTS.register(bus);
        COMPONENTS.register(bus);
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
        bus.addListener((RegisterCapabilitiesEvent e) -> {
            e.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK,
                    CORE_ENTITY.get(),
                    (heart, side) -> heart.driver() ? null : heart.items());
            e.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    CORE_ENTITY.get(),
                    (heart, side) -> heart.driver() ? heart.fluids(false) : null);
            e.registerBlockEntity(SteamDrive.BLOCK, CORE_ENTITY.get(), (heart, side) -> heart.driver() ? heart : null);
            e.registerBlock(
                    Capabilities.FluidHandler.BLOCK,
                    (level, pos, state, entity, side) -> side == state.getValue(PowerPartBlock.FACING)
                            ? null
                            : coreAt(level, pos)
                                    .map(heart -> heart.fluids(
                                            state.is(BOILER.get()),
                                            () -> level.hasChunkAt(pos)
                                                    && level.getBlockState(pos).equals(state)
                                                    && coreAt(level, pos)
                                                            .filter(core -> core == heart)
                                                            .isPresent()))
                                    .orElse(null),
                    BOILER.get(),
                    OUTLET.get());
            e.registerBlock(
                    Capabilities.EnergyStorage.BLOCK,
                    (level, pos, state, entity, side) -> side == state.getValue(PowerPartBlock.FACING)
                            ? null
                            : coreAt(level, pos)
                                    .map(heart -> heart.energy(() -> level.hasChunkAt(pos)
                                            && level.getBlockState(pos).equals(state)
                                            && coreAt(level, pos)
                                                    .filter(core -> core == heart)
                                                    .isPresent()))
                                    .orElse(null),
                    GENERATOR.get());
        });
        if (FMLEnvironment.dist == Dist.CLIENT) {
            bus.addListener((RegisterMenuScreensEvent e) -> e.register(MENU.get(), PowerScreen::new));
            bus.addListener((net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e) ->
                    e.registerFluidType(
                            new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
                                @Override
                                public net.minecraft.resources.ResourceLocation getStillTexture() {
                                    return net.minecraft.resources.ResourceLocation.withDefaultNamespace(
                                            "block/water_still");
                                }

                                @Override
                                public net.minecraft.resources.ResourceLocation getFlowingTexture() {
                                    return net.minecraft.resources.ResourceLocation.withDefaultNamespace(
                                            "block/water_flow");
                                }

                                @Override
                                public int getTintColor() {
                                    return 0xFFCBD9C5;
                                }
                            },
                            STEAM_TYPE.get()));
        }
    }
}
