package org.jd.infestusfrontier.ecology;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.jd.infestusfrontier.InfestusFrontier;

@EventBusSubscriber(modid = InfestusFrontier.MOD_ID, value = Dist.CLIENT)
final class EcologyClient {
    @SubscribeEvent
    static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        var id = ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, EcologyContent.LIVING_SUBSTRATE);
        var block = BuiltInRegistries.BLOCK.getOptional(id).orElseThrow();
        event.register((state, level, pos, tintIndex) -> LivingSubstrateBlock.tint(state), block);
    }

    @SubscribeEvent
    static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        var id = ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, EcologyContent.LIVING_SUBSTRATE);
        var item = BuiltInRegistries.ITEM.getOptional(id).orElseThrow();
        event.register((stack, tintIndex) -> 0xB85D5D, item);
    }

    private EcologyClient() {}
}
