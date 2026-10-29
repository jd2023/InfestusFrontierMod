package org.jd.infestusfrontier.defense;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.core.foundation.TickQuota;
import org.jd.infestusfrontier.integration.geckolib.OrganItem;
import org.jd.infestusfrontier.integration.geckolib.OrganRenderers;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.slf4j.Logger;

/** Rooted ground and air defence, with shared search admission and kill drops routed to the source organ. */
public final class DefenseModule {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, InfestusFrontier.MOD_ID);

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, InfestusFrontier.MOD_ID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<TickQuota>> SCANS = ATTACHMENTS.register(
            "defense_scans",
            () -> AttachmentType.builder(() -> new TickQuota(32)).build());

    /** The sentry's strike: {@code data/infestusfrontier/damage_type/spine.json}. */
    public static final ResourceKey<DamageType> SPINE = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "spine"));

    public static final DeferredBlock<DefenseOrganBlock> SPINE_SENTRY = BLOCKS.registerBlock(
            "spine_sentry",
            properties -> new DefenseOrganBlock(DefenseKind.SPINE, properties),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion());
    public static final DeferredItem<OrganItem> SPINE_SENTRY_ITEM =
            ITEMS.register("spine_sentry", () -> new OrganItem(SPINE_SENTRY.get(), new Item.Properties(), "idle"));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DefenseOrganBlockEntity>> SPINE_SENTRY_ENTITY =
            BLOCK_ENTITIES.register(
                    "spine_sentry",
                    () -> BlockEntityType.Builder.of(
                                    (pos, state) -> new DefenseOrganBlockEntity(DefenseKind.SPINE, pos, state),
                                    SPINE_SENTRY.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<DefenseMenu>> SPINE_SENTRY_MENU = MENUS.register(
            "spine_sentry",
            () -> IMenuTypeExtension.create(
                    (id, inventory, extra) -> new DefenseMenu(DefenseKind.SPINE, id, inventory, extra)));

    public static final ResourceKey<DamageType> SPORE = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "spore"));
    private static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, InfestusFrontier.MOD_ID);
    static final DeferredHolder<
                    net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<SporePellet>>
            SPORE_PELLET = ENTITIES.register(
                    "spore_pellet",
                    () -> net.minecraft.world.entity.EntityType.Builder.<SporePellet>of(
                                    SporePellet::new, net.minecraft.world.entity.MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(8)
                            .updateInterval(10)
                            .build("infestusfrontier:spore_pellet"));
    public static final DeferredBlock<DefenseOrganBlock> AEROCYTE_BLOOM = BLOCKS.registerBlock(
            "aerocyte_bloom",
            properties -> new DefenseOrganBlock(DefenseKind.AIR, properties),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion());
    public static final DeferredItem<OrganItem> AEROCYTE_BLOOM_ITEM =
            ITEMS.register("aerocyte_bloom", () -> new OrganItem(AEROCYTE_BLOOM.get(), new Item.Properties(), "idle"));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DefenseOrganBlockEntity>> AEROCYTE_BLOOM_ENTITY =
            BLOCK_ENTITIES.register(
                    "aerocyte_bloom",
                    () -> BlockEntityType.Builder.of(
                                    (pos, state) -> new DefenseOrganBlockEntity(DefenseKind.AIR, pos, state),
                                    AEROCYTE_BLOOM.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<DefenseMenu>> AEROCYTE_BLOOM_MENU = MENUS.register(
            "aerocyte_bloom",
            () -> IMenuTypeExtension.create(
                    (id, inventory, extra) -> new DefenseMenu(DefenseKind.AIR, id, inventory, extra)));

    private DefenseModule() {}

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        ENTITIES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener((RegisterCapabilitiesEvent event) -> {
            for (var type : List.of(SPINE_SENTRY_ENTITY.get(), AEROCYTE_BLOOM_ENTITY.get())) {
                event.registerBlockEntity(BiomassCapabilities.BLOCK, type, (organ, side) -> organ);
                event.registerBlockEntity(
                        Capabilities.FluidHandler.BLOCK, type, (organ, side) -> BiomassFluids.handler(organ));
                event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (organ, side) -> organ.output());
            }
        });
        NeoForge.EVENT_BUS.addListener(DefenseModule::onDrops);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            OrganRenderers.register(
                    modBus,
                    "aerocyte_bloom",
                    AEROCYTE_BLOOM_ENTITY,
                    AEROCYTE_BLOOM_ITEM,
                    new net.minecraft.world.phys.AABB(0, 0, 0, 1, 1.8, 1));
            modBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) ->
                    event.registerEntityRenderer(
                            SPORE_PELLET.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new));
            modBus.addListener(
                    (RegisterMenuScreensEvent event) -> event.register(AEROCYTE_BLOOM_MENU.get(), DefenseScreen::new));
            OrganRenderers.register(
                    modBus,
                    "spine_sentry",
                    SPINE_SENTRY_ENTITY,
                    SPINE_SENTRY_ITEM,
                    new net.minecraft.world.phys.AABB(-0.5, 0, -0.5, 1.5, 2, 1.5));
            modBus.addListener(
                    (RegisterMenuScreensEvent event) -> event.register(SPINE_SENTRY_MENU.get(), DefenseScreen::new));
        }
    }

    /**
     * A creature killed by a sentry's strike drops into that sentry's store: the strike names the organ's position,
     * the organ keeps each stack as far as it fits, and only what it cannot keep falls.
     */
    private static void onDrops(LivingDropsEvent event) {
        if (!event.getSource().is(SPINE) && !event.getSource().is(SPORE)) return;
        Vec3 at = event.getSource().getSourcePosition();
        if (at == null) return;
        BlockPos pos = BlockPos.containing(at);
        if (!event.getEntity().level().hasChunkAt(pos)
                || !(event.getEntity().level().getBlockEntity(pos) instanceof DefenseOrganBlockEntity sentry)) return;
        List<ItemEntity> gone = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            var left = sentry.keep(drop.getItem());
            if (left.isEmpty()) gone.add(drop);
            else drop.setItem(left);
        }
        event.getDrops().removeAll(gone);
    }
}
