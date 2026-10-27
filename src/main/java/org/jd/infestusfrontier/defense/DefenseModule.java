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
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.integration.geckolib.OrganItem;
import org.jd.infestusfrontier.integration.geckolib.OrganRenderers;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.slf4j.Logger;

/**
 * Base defence: the Spine Sentry (BLOCK_CATALOG T2-30), a rooted lash that strikes hostiles near it for biomass and
 * keeps what they drop. Its kills are credited to the organ by a damage type of its own, so the drops of a creature
 * it killed go into its store before they fall.
 */
public final class DefenseModule {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, InfestusFrontier.MOD_ID);

    /** The sentry's strike: {@code data/infestusfrontier/damage_type/spine.json}. */
    public static final ResourceKey<DamageType> SPINE = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "spine"));

    public static final DeferredBlock<SpineSentryBlock> SPINE_SENTRY = BLOCKS.registerBlock(
            "spine_sentry",
            SpineSentryBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion());
    public static final DeferredItem<OrganItem> SPINE_SENTRY_ITEM =
            ITEMS.register("spine_sentry", () -> new OrganItem(SPINE_SENTRY.get(), new Item.Properties(), "idle"));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpineSentryBlockEntity>> SPINE_SENTRY_ENTITY =
            BLOCK_ENTITIES.register(
                    "spine_sentry",
                    () -> BlockEntityType.Builder.of(SpineSentryBlockEntity::new, SPINE_SENTRY.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<SpineSentryMenu>> SPINE_SENTRY_MENU =
            MENUS.register("spine_sentry", () -> IMenuTypeExtension.create(SpineSentryMenu::new));

    private DefenseModule() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener((RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(BiomassCapabilities.BLOCK, SPINE_SENTRY_ENTITY.get(), (sentry, side) -> sentry);
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    SPINE_SENTRY_ENTITY.get(),
                    (sentry, side) -> BiomassFluids.handler(sentry));
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK, SPINE_SENTRY_ENTITY.get(), (sentry, side) -> sentry.output());
        });
        NeoForge.EVENT_BUS.addListener(DefenseModule::onDrops);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            OrganRenderers.register(
                    modBus,
                    "spine_sentry",
                    SPINE_SENTRY_ENTITY,
                    SPINE_SENTRY_ITEM,
                    new net.minecraft.world.phys.AABB(-0.5, 0, -0.5, 1.5, 2, 1.5));
            modBus.addListener((RegisterMenuScreensEvent event) ->
                    event.register(SPINE_SENTRY_MENU.get(), SpineSentryScreen::new));
        }
    }

    /**
     * A creature killed by a sentry's strike drops into that sentry's store: the strike names the organ's position,
     * the organ keeps each stack as far as it fits, and only what it cannot keep falls.
     */
    private static void onDrops(LivingDropsEvent event) {
        if (!event.getSource().is(SPINE)) return;
        Vec3 at = event.getSource().getSourcePosition();
        if (at == null) return;
        BlockPos pos = BlockPos.containing(at);
        if (!event.getEntity().level().hasChunkAt(pos)
                || !(event.getEntity().level().getBlockEntity(pos) instanceof SpineSentryBlockEntity sentry)) return;
        List<ItemEntity> gone = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            var left = sentry.keep(drop.getItem());
            if (left.isEmpty()) gone.add(drop);
            else drop.setItem(left);
        }
        event.getDrops().removeAll(gone);
    }
}
