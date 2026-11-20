package org.jd.infestusfrontier.vigil;

import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.*;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.integration.vigil.KeeperRenderer;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.slf4j.Logger;

public final class VigilModule {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, InfestusFrontier.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, InfestusFrontier.MOD_ID);
    public static final DeferredBlock<VigilNestBlock> NEST =
            BLOCKS.register("vigil_nest", () -> new VigilNestBlock(properties()));
    public static final DeferredBlock<Block> FEEDER = BLOCKS.register("vigil_feeder", () -> new Block(properties()));
    public static final DeferredItem<BlockItem> NEST_ITEM =
            ITEMS.registerSimpleBlockItem(NEST, new Item.Properties().stacksTo(1));
    public static final DeferredItem<BlockItem> FEEDER_ITEM = ITEMS.registerSimpleBlockItem(FEEDER);
    public static final DeferredItem<Item> EGG =
            ITEMS.registerSimpleItem("vigil_egg", new Item.Properties().stacksTo(1));
    public static final DeferredHolder<EntityType<?>, EntityType<VigilKeeper>> KEEPER = ENTITIES.register(
            "vigil_keeper",
            () -> EntityType.Builder.of(VigilKeeper::new, MobCategory.CREATURE)
                    .sized(1.2F, 1.2F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("infestusfrontier:vigil_keeper"));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VigilNestBlockEntity>> NEST_ENTITY =
            BLOCK_ENTITIES.register(
                    "vigil_nest",
                    () -> BlockEntityType.Builder.of(VigilNestBlockEntity::new, NEST.get())
                            .build(null));
    static final DeferredHolder<MenuType<?>, MenuType<VigilNestMenu>> MENU =
            MENUS.register("vigil_nest", () -> IMenuTypeExtension.create(VigilNestMenu::new));
    static final DeferredHolder<DataComponentType<?>, DataComponentType<NestContents>> CONTENTS =
            COMPONENTS.registerComponentType("vigil_nest", b -> b.persistent(NestContents.CODEC.codec()));
    static final DeferredHolder<AttachmentType<?>, AttachmentType<VigilTickets>> TERRITORIES = ATTACHMENTS.register(
            "vigil_territories", () -> AttachmentType.builder(VigilTickets::new).build());

    private VigilModule() {}

    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of()
                .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                .sound(SoundType.SLIME_BLOCK)
                .noOcclusion();
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        ENTITIES.register(bus);
        ATTACHMENTS.register(bus);
        COMPONENTS.register(bus);
        bus.addListener((RegisterTicketControllersEvent e) -> e.register(VigilTickets.CONTROLLER));
        bus.addListener((EntityAttributeCreationEvent e) ->
                e.put(KEEPER.get(), VigilKeeper.attributes().build()));
        bus.addListener((RegisterCapabilitiesEvent e) -> {
            e.registerBlockEntity(BiomassCapabilities.BLOCK, NEST_ENTITY.get(), (nest, side) -> nest);
            e.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK, NEST_ENTITY.get(), (nest, side) -> BiomassFluids.handler(nest));
        });
        if (FMLEnvironment.dist == Dist.CLIENT) {
            bus.addListener((RegisterMenuScreensEvent e) -> e.register(MENU.get(), VigilNestScreen::new));
            bus.addListener((EntityRenderersEvent.RegisterRenderers e) ->
                    e.registerEntityRenderer(KEEPER.get(), KeeperRenderer::new));
            bus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) ->
                    e.registerLayerDefinition(KeeperRenderer.LAYER, KeeperRenderer::layer));
        }
    }
}
