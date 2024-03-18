package org.jd.infestusfrontier.world.feature;

import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.InfestusFrontier;

import java.util.List;

public class ZgPlacedFeatures {
    public static final DeferredRegister<PlacedFeature> PLACED_FEATURES =
            DeferredRegister.create(Registry.PLACED_FEATURE_REGISTRY, InfestusFrontier.MODID);
    public static final RegistryObject<PlacedFeature> BIOMASS_GEODE_PLACED = PLACED_FEATURES.register("biomass_geode_placed",
            () -> new PlacedFeature(ZgConfiguredFeatures.BIOMASS_GEODE.getHolder().get(), List.of(
                    RarityFilter.onAverageOnceEvery(50), InSquarePlacement.spread(),
                    HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(6), VerticalAnchor.absolute(50)),
                    BiomeFilter.biome())));
    public static void register(IEventBus eventBus) {
        PLACED_FEATURES.register(eventBus);
    }

}
