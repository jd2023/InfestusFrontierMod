package org.jd.infestusfrontier.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jd.infestusfrontier.InfestusFrontier;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = InfestusFrontier.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        ExistingFileHelper fileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> provider = event.getLookupProvider();

        gen.addProvider(event.includeServer(), new InfestusRecipeProvider(output));
        gen.addProvider(event.includeServer(), InfestusLootTableProvider.create(output));

        gen.addProvider(event.includeClient(), new InfestusBlockStateProvider(output, fileHelper));
        gen.addProvider(event.includeClient(), new InfestusItemModelProvider(output, fileHelper));

        InfestusBlockTagGenerator blockTagGen = gen.addProvider(event.includeServer(),
                new InfestusBlockTagGenerator(output, provider, fileHelper));
        gen.addProvider(event.includeServer(), new InfestusItemTagGenerator(output, provider, blockTagGen.contentsGetter(), fileHelper));




    }
}
