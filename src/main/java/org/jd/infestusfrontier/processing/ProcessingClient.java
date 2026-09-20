package org.jd.infestusfrontier.processing;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = "infestusfrontier", value = Dist.CLIENT)
final class ProcessingClient {
    @SubscribeEvent static void colors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> 0xFF92D3BF, BowlResources.item("elastic_gel"));
        event.register((stack, tint) -> 0xFFD8B277, BowlResources.item("nutrient_mash"));
        event.register((stack, tint) -> 0xFFFFC34F, BowlResources.item("honey_culture"));
        event.register((stack, tint) -> 0xFF94BC62, BowlResources.item("rooting_gel"));
    }
    private ProcessingClient() {}
}
