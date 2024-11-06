package org.jd.infestusfrontier.datagen.registry;


import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import org.jd.infestusfrontier.InfestusFrontier;

public class TagRegistry {
    public static class Biomes {
        public static final TagKey<Biome> BLACKLIST_BIOMES
                = biomeTag("blacklist_biomes");
        //Geodes
        public static final TagKey<Biome> HAS_BIOMASS_GEODE
                = biomeTag("has_biomass_geode");
        private static TagKey<Biome> biomeTag(String name) {
            return TagKey.create(Registries.BIOME, new ResourceLocation(InfestusFrontier.MODID, name));
        }

    }

    public static class Blocks {
        public static final TagKey<Block> GEODE_INVALID_BLOCKS
                = blockTag("geode_invalid_blocks");

        public static final TagKey<Block> GEODES_CANNOT_REPLACE
                = blockTag("geodes_cannot_replace");


        private static TagKey<Block> blockTag(String name) {
            return TagKey.create(Registries.BLOCK, new ResourceLocation(InfestusFrontier.MODID, name));
        }
    }
    public static class Items {
        private static TagKey<Item> itemTag(String name) {
            return TagKey.create(Registries.ITEM, new ResourceLocation(InfestusFrontier.MODID, name));
        }
    }
}
