package org.jd.infestusfrontier.datagen.loot;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.block.InfestusBlocks;

import java.util.Set;

public class InfestusBlockLootTables extends BlockLootSubProvider {
    public InfestusBlockLootTables() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        this.dropSelf(InfestusBlocks.ROTTEN_FLESH_BLOCK.get());

        this.dropSelf(InfestusBlocks.FERMENTED_FLESH_BLOCK.get());

        this.dropSelf(InfestusBlocks.CORRUPTION_CORE.get());



    }



    @Override
    protected Iterable<Block> getKnownBlocks() {
        return InfestusBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
    }
}