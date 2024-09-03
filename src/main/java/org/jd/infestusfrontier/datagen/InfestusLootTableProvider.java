package org.jd.infestusfrontier.datagen;


import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.jd.infestusfrontier.datagen.loot.InfestusBlockLootTables;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class InfestusLootTableProvider {
    public static LootTableProvider create(PackOutput output) {
        return new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(InfestusBlockLootTables::new, LootContextParamSets.BLOCK)
        ));
    }

}
