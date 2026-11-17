package org.jd.infestusfrontier.genetics;

import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
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
import org.jd.infestusfrontier.core.genetics.Genetics;
import org.jd.infestusfrontier.genetics.api.DnaSampleRecipe;
import org.jd.infestusfrontier.genetics.api.GeneticConstructRecipe;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.slf4j.Logger;

/**
 * Owner of DNA research: the Sampling Needle that takes DNA samples from living creatures, the samples, and the DNA
 * Bank that extracts them, keeps each species' genome and produces genetic constructs from complete genomes. Which
 * creature gives which sample, and what extracting it costs and adds, is recipe data: {@link DnaSampleRecipe}; which
 * genomes and materials make which construct is {@link GeneticConstructRecipe}. The constructs belong to the features
 * whose hosts take them.
 */
public final class GeneticsModule {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, InfestusFrontier.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, InfestusFrontier.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, InfestusFrontier.MOD_ID);

    /** Which creature gives which DNA sample and what the bank does with it; data under {@code recipe/dna}. */
    public static final DeferredHolder<RecipeType<?>, RecipeType<DnaSampleRecipe>> DNA_SAMPLE = RECIPE_TYPES.register(
            "dna_sample",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "dna_sample")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DnaSampleRecipe>> DNA_SAMPLE_SERIALIZER =
            RECIPE_SERIALIZERS.register("dna_sample", DnaSampleRecipe.Serializer::new);

    /** Which complete genomes and materials a DNA Bank turns into which genetic construct; data under {@code recipe/construct}. */
    public static final DeferredHolder<RecipeType<?>, RecipeType<GeneticConstructRecipe>> GENETIC_CONSTRUCT =
            RECIPE_TYPES.register(
                    "genetic_construct",
                    () -> RecipeType.simple(
                            ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "genetic_construct")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GeneticConstructRecipe>>
            GENETIC_CONSTRUCT_SERIALIZER =
                    RECIPE_SERIALIZERS.register("genetic_construct", GeneticConstructRecipe.Serializer::new);

    /** A hand tool that takes one DNA sample from a living creature a recipe names. Crafted. */
    public static final DeferredItem<SamplingNeedleItem> SAMPLING_NEEDLE = ITEMS.registerItem(
            "sampling_needle", SamplingNeedleItem::new, new Item.Properties().durability(Genetics.NEEDLE_USES));

    /** A pig's DNA, taken with a Sampling Needle; the pig recipe makes it pig research in a DNA Bank. */
    public static final DeferredItem<Item> PIG_DNA_SAMPLE =
            ITEMS.registerSimpleItem("pig_dna_sample", new Item.Properties().stacksTo(Genetics.MAX_SAMPLES));

    /** An organ that extracts DNA samples and keeps the genomes they research. Crafted from a bud. */
    public static final DeferredBlock<DnaBankBlock> DNA_BANK = BLOCKS.registerBlock(
            "dna_bank",
            DnaBankBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_PURPLE)
                    .strength(Organs.HARDNESS, Organs.BLAST_RESISTANCE)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion());

    public static final DeferredItem<DnaBankItem> DNA_BANK_ITEM =
            ITEMS.registerItem("dna_bank", properties -> new DnaBankItem(DNA_BANK.get(), properties));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DnaBankBlockEntity>> DNA_BANK_ENTITY =
            BLOCK_ENTITIES.register(
                    "dna_bank",
                    () -> BlockEntityType.Builder.of(DnaBankBlockEntity::new, DNA_BANK.get())
                            .build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<DnaBankMenu>> DNA_BANK_MENU =
            MENUS.register("dna_bank", () -> IMenuTypeExtension.create(DnaBankMenu::new));
    /**
     * What a broken bank carries on its item: its samples, its biomass, its research, the extraction under way, its
     * construct materials and the constructs it made.
     */
    static final DeferredHolder<DataComponentType<?>, DataComponentType<DnaBankContents>> DNA_BANK_CONTENTS =
            COMPONENTS.registerComponentType("dna_bank", builder -> builder.persistent(DnaBankContents.CODEC.codec()));

    private GeneticsModule() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        COMPONENTS.register(modBus);
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        modBus.addListener((RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(BiomassCapabilities.BLOCK, DNA_BANK_ENTITY.get(), (bank, side) -> bank);
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    DNA_BANK_ENTITY.get(),
                    (bank, side) -> BiomassFluids.handler(bank));
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK, DNA_BANK_ENTITY.get(), (bank, side) -> bank.intake());
        });
        if (FMLEnvironment.dist == Dist.CLIENT)
            modBus.addListener(
                    (RegisterMenuScreensEvent event) -> event.register(DNA_BANK_MENU.get(), DnaBankScreen::new));
    }
}
