package org.jd.infestusfrontier.datagen.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;
import org.jd.infestusfrontier.InfestusFrontier;

import java.util.List;

public class PlacedFeatureRegistry {
    public static final ResourceKey<PlacedFeature> BIOMASS_GEODE = createKey("biomass_geode.json");

    public static void bootstrap(BootstapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> holderGetter = context.lookup(Registries.CONFIGURED_FEATURE);

        Holder.Reference<ConfiguredFeature<?, ?>> holder = holderGetter.getOrThrow(ConfiguredFeatureRegistry.BIOMASS_GEODE);
        register(context, BIOMASS_GEODE, holder, rarityBiomassGeode(), inSquarePlacement(), placementBiomassModifier(), biomeFilter());
    }
    private static RarityFilter rarityBiomassGeode() {
        return RarityFilter.onAverageOnceEvery(1);
    }
    private static InSquarePlacement inSquarePlacement() {
        return InSquarePlacement.spread();
    }
    private static PlacementModifier placementBiomassModifier() {
        return HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(30), VerticalAnchor.absolute(30));
    }
    private static BiomeFilter biomeFilter() {
        return BiomeFilter.biome();
    }
    private static ResourceKey<PlacedFeature> createKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, InfestusFrontier.createResource(name));
    }

    private static void register(BootstapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration, List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }

    private static void register(BootstapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration, PlacementModifier... modifiers) {
        register(context, key, configuration, List.of(modifiers));
    }
    public static void init() {};

}
