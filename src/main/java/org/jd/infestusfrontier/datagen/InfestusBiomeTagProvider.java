package org.jd.infestusfrontier.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.datagen.registry.TagRegistry;

import java.util.concurrent.CompletableFuture;

public class InfestusBiomeTagProvider extends BiomeTagsProvider {
    public InfestusBiomeTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper)
    {
        super(output, lookupProvider, InfestusFrontier.MODID, existingFileHelper);
    }

    protected void addTags(HolderLookup.Provider arg) {
        this.tag(TagRegistry.Biomes.HAS_BIOMASS_GEODE).add(Biomes.SWAMP, Biomes.BAMBOO_JUNGLE, Biomes.JUNGLE, Biomes.MANGROVE_SWAMP);

    }
}
