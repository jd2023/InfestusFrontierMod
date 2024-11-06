package org.jd.infestusfrontier.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.datagen.registry.ConfiguredFeatureRegistry;
import org.jd.infestusfrontier.datagen.registry.PlacedFeatureRegistry;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class InfestusRegistryProvider extends DatapackBuiltinEntriesProvider {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.CONFIGURED_FEATURE, ConfiguredFeatureRegistry::bootstrap)
            .add(Registries.PLACED_FEATURE, PlacedFeatureRegistry::bootstrap);


    public InfestusRegistryProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(InfestusFrontier.MODID));
    }
}
