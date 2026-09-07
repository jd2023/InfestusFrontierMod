package org.jd.infestusfrontier.world.feature;

import net.minecraft.core.Registry;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.checkerframework.checker.units.qual.C;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.ZgBlocks;

import java.util.List;

public class ZgConfiguredFeatures {
    public static final DeferredRegister<ConfiguredFeature<?, ?>> CONFIGURED_FEATURE =
            DeferredRegister.create(Registry.CONFIGURED_FEATURE_REGISTRY, InfestusFrontier.MODID);

    public static final RegistryObject<ConfiguredFeature<?, ?>> BIOMASS_GEODE = CONFIGURED_FEATURE.register("biomass_geode",
            ()->new ConfiguredFeature<>(Feature.GEODE,
                    new GeodeConfiguration( new GeodeBlockSettings(BlockStateProvider.simple(Blocks.AIR),
                            BlockStateProvider.simple(Blocks.COBBLED_DEEPSLATE),
                            BlockStateProvider.simple(ZgBlocks.BIOMASS_CRYSTAL.get()),
                            BlockStateProvider.simple(ZgBlocks.BIOMASS_CRYSTAL.get()),
                            BlockStateProvider.simple(ZgBlocks.CORRUPT_CHUNK.get()),
                            List.of(ZgBlocks.BIOMASS_CRYSTAL.get().defaultBlockState()),
                            BlockTags.FEATURES_CANNOT_REPLACE, BlockTags.GEODE_INVALID_BLOCKS),
                            new GeodeLayerSettings(2D, 1.5D, 2.8D, 3.8D),
                            new GeodeCrackSettings(0D, 0D, 0), 0.5D, 0.10D,
                            true, UniformInt.of(3, 8),
                            UniformInt.of(2, 6), UniformInt.of(1, 2),
                            -8,8, 0.075D, 1)));


    public static void register(IEventBus eventBus) {
        CONFIGURED_FEATURE.register(eventBus);
    }

}
