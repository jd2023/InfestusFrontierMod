package org.jd.infestusfrontier.world;

import com.mojang.serialization.Codec;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.world.biome_modifiers.BiomassGeodeModifier;

public class BiomeModifierRegistry {
    public static final DeferredRegister<Codec<? extends BiomeModifier>> MODIFIER =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, InfestusFrontier.MODID);
    public static RegistryObject<Codec<BiomassGeodeModifier>> BIOMASS_GEODE =
            MODIFIER.register("biomass_geode.json", BiomassGeodeModifier::makeCodec);
    public static void register(IEventBus eventBus) {
        MODIFIER.register(eventBus);
    }
}
