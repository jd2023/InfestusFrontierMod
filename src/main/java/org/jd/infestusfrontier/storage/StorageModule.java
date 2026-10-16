package org.jd.infestusfrontier.storage;

import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.storage.api.BiomassBuckets;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;
import org.slf4j.Logger;

/** Owner of item, fluid and biomass balances. Ships the Biomass Bladder, the biomass bucket and hand transfers. */
public final class StorageModule {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, InfestusFrontier.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, InfestusFrontier.MOD_ID);

    public static final DeferredBlock<Block> BIOMASS_BLADDER = BLOCKS.registerBlock(
            "biomass_bladder",
            BiomassBladderBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_MAGENTA)
                    .strength(1.0F)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> BIOMASS_BLADDER_ITEM = ITEMS.registerSimpleBlockItem(BIOMASS_BLADDER);
    public static final DeferredItem<Item> BIOMASS_BUCKET = ITEMS.registerItem(
            "biomass_bucket",
            BiomassBucketItem::new,
            new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET));
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

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        COMPONENTS.register(modBus);
        modBus.addListener((RegisterCapabilitiesEvent event) -> event.registerBlockEntity(
                BiomassCapabilities.BLOCK, BIOMASS_BLADDER_ENTITY.get(), (bladder, side) -> bladder));
        if (FMLEnvironment.dist == Dist.CLIENT)
            modBus.addListener((RegisterMenuScreensEvent event) ->
                    event.register(BIOMASS_BLADDER_MENU.get(), BiomassBladderScreen::new));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) ->
                BiomassBuckets.forget(event.getEntity().getUUID()));
    }
}
