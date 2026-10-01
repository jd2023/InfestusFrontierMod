package org.jd.infestusfrontier.processing.digestion;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.jd.infestusfrontier.InfestusFrontier;

/** One biomass palette for the static organ and portable models. */
@EventBusSubscriber(modid = InfestusFrontier.MOD_ID, value = Dist.CLIENT)
final class DigestionClient {
    private static final int BIOMASS = 0xFFE0FF8A;

    @SubscribeEvent static void blocks(RegisterColorHandlersEvent.Block event) {
        for (String path : new String[] {DigestionModule.SAC, DigestionModule.BLADDER}) {
            event.register((state, level, pos, tint) -> BIOMASS,
                    BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, path)));
        }
    }

    @SubscribeEvent static void items(RegisterColorHandlersEvent.Item event) {
        for (String path : new String[] {DigestionModule.SAC, DigestionModule.BLADDER, DigestionModule.BUCKET}) {
            event.register((stack, tint) -> BIOMASS,
                    BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, path)));
        }
    }

    private DigestionClient() {}
}
