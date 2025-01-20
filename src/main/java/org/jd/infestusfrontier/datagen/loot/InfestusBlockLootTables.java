package org.jd.infestusfrontier.datagen.loot;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.block.InfestusBlocks;
import org.jd.infestusfrontier.item.InfestusItems;

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
        this.dropSelf(InfestusBlocks.MUTATION_POOL.get());
        this.dropSelf(InfestusBlocks.BIOMASS_RESERVOIR.get());
        this.add(InfestusBlocks.BIOMASS_CRYSTAL_BLOCK.get(),
                block -> createCrystalLikeDrops(InfestusBlocks.BIOMASS_CRYSTAL_BLOCK.get(), InfestusItems.BIOMASS_SHARD.get()));





    }



    @Override
    protected Iterable<Block> getKnownBlocks() {
        return InfestusBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
    }
    protected LootTable.Builder createCrystalLikeDrops(Block pBlock, Item item) {
        return createSilkTouchDispatchTable(pBlock,
                this.applyExplosionDecay(pBlock,
                        LootItem.lootTableItem(item)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 9.0F)))));

    }
}