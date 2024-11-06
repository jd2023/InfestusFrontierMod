package org.jd.infestusfrontier.datagen.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.block.InfestusBlocks;

import java.util.List;

import static net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider.simple;
public class ConfiguredFeatureRegistry {
    public static final ResourceKey<ConfiguredFeature<?, ?>> BIOMASS_GEODE = registerKey("biomass_geode.json");

    public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context) {
        register(context, BIOMASS_GEODE, FeatureRegistry.GEODE.get(), new GeodeConfiguration(
                new GeodeBlockSettings(simple(Blocks.AIR),
                        simple(InfestusBlocks.BIOMASS_CRYSTAL_BLOCK.get()),
                        simple(InfestusBlocks.BIOMASS_CRYSTAL_BLOCK.get()),
                        simple(InfestusBlocks.FERMENTED_FLESH_BLOCK.get()),
                        simple(Blocks.SCULK),
                        List.of(Blocks.BEDROCK.defaultBlockState()),
                        TagRegistry.Blocks.GEODES_CANNOT_REPLACE, TagRegistry.Blocks.GEODE_INVALID_BLOCKS),
                new GeodeLayerSettings(1.7D, 2.2D, 3.2D, 4.2D), new GeodeCrackSettings(0.95D, 2.0D, 2), 0.35D, 0.083D, true, UniformInt.of(4, 6), UniformInt.of(3, 4), UniformInt.of(1, 2), -16, 16, 0.05D, 1));
    }
    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation(InfestusFrontier.MODID, name));
    }
    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
    public static void init() {};

}
