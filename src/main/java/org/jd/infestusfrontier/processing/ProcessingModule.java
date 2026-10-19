package org.jd.infestusfrontier.processing;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
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
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.integration.geckolib.OrganItem;
import org.jd.infestusfrontier.integration.geckolib.OrganRenderers;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.processing.api.DigestionRecipe;
import org.jd.infestusfrontier.processing.api.OrganBatchRecipe;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.slf4j.Logger;

/** Owner of reserved batch execution. Ships the Digestive Sac and the digestion recipe type. */
public final class ProcessingModule {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, InfestusFrontier.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, InfestusFrontier.MOD_ID);

    public static final DeferredBlock<Block> DIGESTIVE_SAC = BLOCKS.registerBlock(
            "digestive_sac",
            DigestiveSacBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.0F)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion());
    public static final DeferredItem<OrganItem> DIGESTIVE_SAC_ITEM =
            ITEMS.register("digestive_sac", () -> new OrganItem(DIGESTIVE_SAC.get(), new Item.Properties(), "idle"));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DigestiveSacBlockEntity>> DIGESTIVE_SAC_ENTITY =
            BLOCK_ENTITIES.register(
                    "digestive_sac",
                    () -> BlockEntityType.Builder.of(DigestiveSacBlockEntity::new, DIGESTIVE_SAC.get())
                            .build(null));
    /** The Culture Bowl: nine ingredient slots, water and biomass, on the tall panel. */
    static final BatchOrganKind BOWL = new BatchOrganKind(
            "culture_bowl",
            9,
            2000,
            Biomass.of(2000),
            3,
            Block.box(1, 0, 1, 15, 6, 15),
            () -> ProcessingModule.CULTURE_BOWL.get(),
            () -> ProcessingModule.CULTURE_BOWL_ENTITY.get(),
            () -> ProcessingModule.CULTURE_BOWL_MENU.get());
    /** The Membrane Rack: two ingredient slots and water, no biomass, on a one-strip panel. */
    static final BatchOrganKind RACK = new BatchOrganKind(
            "membrane_rack",
            2,
            1000,
            Biomass.ZERO,
            1,
            Block.box(1, 0, 1, 15, 14, 15),
            () -> ProcessingModule.MEMBRANE_RACK.get(),
            () -> ProcessingModule.MEMBRANE_RACK_ENTITY.get(),
            () -> ProcessingModule.MEMBRANE_RACK_MENU.get());

    public static final DeferredBlock<Block> CULTURE_BOWL = batchOrgan(BOWL, MapColor.TERRACOTTA_PINK, 0.8F);
    public static final DeferredItem<OrganItem> CULTURE_BOWL_ITEM =
            ITEMS.register("culture_bowl", () -> new OrganItem(CULTURE_BOWL.get(), new Item.Properties(), "idle"));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatchOrganBlockEntity>> CULTURE_BOWL_ENTITY =
            batchEntity(BOWL, () -> ProcessingModule.CULTURE_BOWL.get());
    public static final DeferredHolder<MenuType<?>, MenuType<BatchOrganMenu>> CULTURE_BOWL_MENU =
            batchMenu(BOWL, () -> ProcessingModule.CULTURE_BOWL_MENU.get());

    public static final DeferredBlock<Block> MEMBRANE_RACK = batchOrgan(RACK, MapColor.COLOR_PINK, 0.8F);
    public static final DeferredItem<OrganItem> MEMBRANE_RACK_ITEM =
            ITEMS.register("membrane_rack", () -> new OrganItem(MEMBRANE_RACK.get(), new Item.Properties(), "idle"));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatchOrganBlockEntity>> MEMBRANE_RACK_ENTITY =
            batchEntity(RACK, () -> ProcessingModule.MEMBRANE_RACK.get());
    public static final DeferredHolder<MenuType<?>, MenuType<BatchOrganMenu>> MEMBRANE_RACK_MENU =
            batchMenu(RACK, () -> ProcessingModule.MEMBRANE_RACK_MENU.get());
    /** What the Rack makes: the common flexible construction material. */
    public static final DeferredItem<Item> MEMBRANE_SHEET = ITEMS.registerSimpleItem("membrane_sheet");

    public static final DeferredHolder<MenuType<?>, MenuType<DigestiveSacMenu>> DIGESTIVE_SAC_MENU =
            MENUS.register("digestive_sac", () -> IMenuTypeExtension.create(DigestiveSacMenu::new));
    public static final DeferredHolder<RecipeType<?>, RecipeType<DigestionRecipe>> DIGESTION = RECIPE_TYPES.register(
            "digestion",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "digestion")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DigestionRecipe>> DIGESTION_SERIALIZER =
            RECIPE_SERIALIZERS.register("digestion", DigestionRecipe.Serializer::new);
    static final DeferredHolder<DataComponentType<?>, DataComponentType<DigestionBatch>> DIGESTION_BATCH =
            COMPONENTS.registerComponentType(
                    "digestion_batch",
                    builder ->
                            builder.persistent(DigestionBatch.CODEC).networkSynchronized(DigestionBatch.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BatchOrganContents>> BATCH_CONTENTS =
            COMPONENTS.registerComponentType(
                    "batch_organ_contents",
                    builder -> builder.persistent(BatchOrganContents.CODEC)
                            .networkSynchronized(BatchOrganContents.STREAM_CODEC));
    public static final DeferredHolder<RecipeType<?>, RecipeType<OrganBatchRecipe>> ORGAN_BATCH = RECIPE_TYPES.register(
            "organ_batch",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "organ_batch")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<OrganBatchRecipe>> ORGAN_BATCH_SERIALIZER =
            RECIPE_SERIALIZERS.register("organ_batch", OrganBatchRecipe.Serializer::new);

    private ProcessingModule() {}

    private static DeferredBlock<Block> batchOrgan(BatchOrganKind kind, MapColor colour, float strength) {
        return BLOCKS.registerBlock(
                kind.id(),
                properties -> new BatchOrganBlock(kind, properties),
                BlockBehaviour.Properties.of()
                        .mapColor(colour)
                        .strength(strength)
                        .sound(SoundType.SLIME_BLOCK)
                        .noOcclusion());
    }

    private static DeferredHolder<BlockEntityType<?>, BlockEntityType<BatchOrganBlockEntity>> batchEntity(
            BatchOrganKind kind, Supplier<Block> block) {
        return BLOCK_ENTITIES.register(
                kind.id(),
                () -> BlockEntityType.Builder.of(
                                (pos, state) -> new BatchOrganBlockEntity(kind, pos, state), block.get())
                        .build(null));
    }

    private static DeferredHolder<MenuType<?>, MenuType<BatchOrganMenu>> batchMenu(
            BatchOrganKind kind, Supplier<MenuType<BatchOrganMenu>> self) {
        return MENUS.register(
                kind.id(),
                () -> IMenuTypeExtension.create(
                        (id, inventory, extra) -> new BatchOrganMenu(self.get(), kind, id, inventory, extra)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        COMPONENTS.register(modBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            OrganRenderers.register(modBus, "digestive_sac", DIGESTIVE_SAC_ENTITY, DIGESTIVE_SAC_ITEM);
            OrganRenderers.register(modBus, "culture_bowl", CULTURE_BOWL_ENTITY, CULTURE_BOWL_ITEM);
            OrganRenderers.register(modBus, "membrane_rack", MEMBRANE_RACK_ENTITY, MEMBRANE_RACK_ITEM);
            modBus.addListener((RegisterMenuScreensEvent event) -> {
                event.register(DIGESTIVE_SAC_MENU.get(), DigestiveSacScreen::new);
                event.register(CULTURE_BOWL_MENU.get(), CultureBowlScreen::new);
                event.register(MEMBRANE_RACK_MENU.get(), MembraneRackScreen::new);
            });
        }
        modBus.addListener((RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(BiomassCapabilities.BLOCK, DIGESTIVE_SAC_ENTITY.get(), (sac, side) -> sac);
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK, DIGESTIVE_SAC_ENTITY.get(), (sac, side) -> sac.feed());
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    DIGESTIVE_SAC_ENTITY.get(),
                    (sac, side) -> BiomassFluids.handler(sac));
            for (var entity : List.of(CULTURE_BOWL_ENTITY, MEMBRANE_RACK_ENTITY)) {
                event.registerBlockEntity(BiomassCapabilities.BLOCK, entity.get(), (organ, side) -> organ);
                event.registerBlockEntity(
                        Capabilities.FluidHandler.BLOCK,
                        entity.get(),
                        (organ, side) -> BiomassFluids.join(organ.water(), BiomassFluids.handler(organ)));
            }
        });
    }
}
