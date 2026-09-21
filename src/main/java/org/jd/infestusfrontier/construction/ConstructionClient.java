package org.jd.infestusfrontier.construction;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.jd.infestusfrontier.InfestusFrontier;

@EventBusSubscriber(modid = InfestusFrontier.MOD_ID, value = Dist.CLIENT)
final class ConstructionClient {
    @SubscribeEvent
    static void configureRenderLayers(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(
                BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(
                        InfestusFrontier.MOD_ID, ConstructionContent.MEMBRANE_WINDOW)),
                RenderType.translucent()));
    }

    private ConstructionClient() {}
}
