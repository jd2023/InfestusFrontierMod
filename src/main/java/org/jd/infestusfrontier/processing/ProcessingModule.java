package org.jd.infestusfrontier.processing;

import com.mojang.logging.LogUtils;
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
import org.jd.infestusfrontier.integration.geckolib.OrganItem;
import org.jd.infestusfrontier.integration.geckolib.OrganRenderers;
import org.jd.infestusfrontier.processing.api.CultureBowlContents;
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
    public static final DeferredBlock<Block> CULTURE_BOWL = BLOCKS.registerBlock(
            "culture_bowl",
            CultureBowlBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_PINK)
                    .strength(0.8F)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion());
    public static final DeferredItem<OrganItem> CULTURE_BOWL_ITEM =
            ITEMS.register("culture_bowl", () -> new OrganItem(CULTURE_BOWL.get(), new Item.Properties(), "idle"));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CultureBowlBlockEntity>> CULTURE_BOWL_ENTITY =
            BLOCK_ENTITIES.register(
                    "culture_bowl",
                    () -> BlockEntityType.Builder.of(CultureBowlBlockEntity::new, CULTURE_BOWL.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<DigestiveSacMenu>> DIGESTIVE_SAC_MENU =
            MENUS.register("digestive_sac", () -> IMenuTypeExtension.create(DigestiveSacMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<CultureBowlMenu>> CULTURE_BOWL_MENU =
            MENUS.register("culture_bowl", () -> IMenuTypeExtension.create(CultureBowlMenu::new));
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
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CultureBowlContents>> BOWL_CONTENTS =
            COMPONENTS.registerComponentType(
                    "culture_bowl_contents",
                    builder -> builder.persistent(CultureBowlContents.CODEC)
                            .networkSynchronized(CultureBowlContents.STREAM_CODEC));
    public static final DeferredHolder<RecipeType<?>, RecipeType<OrganBatchRecipe>> ORGAN_BATCH = RECIPE_TYPES.register(
            "organ_batch",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "organ_batch")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<OrganBatchRecipe>> ORGAN_BATCH_SERIALIZER =
            RECIPE_SERIALIZERS.register("organ_batch", OrganBatchRecipe.Serializer::new);

    private ProcessingModule() {}

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
            modBus.addListener((RegisterMenuScreensEvent event) -> {
                event.register(DIGESTIVE_SAC_MENU.get(), DigestiveSacScreen::new);
                event.register(CULTURE_BOWL_MENU.get(), CultureBowlScreen::new);
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
            event.registerBlockEntity(BiomassCapabilities.BLOCK, CULTURE_BOWL_ENTITY.get(), (bowl, side) -> bowl);
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    CULTURE_BOWL_ENTITY.get(),
                    (bowl, side) -> BiomassFluids.join(bowl.water(), BiomassFluids.handler(bowl)));
        });
    }
}
