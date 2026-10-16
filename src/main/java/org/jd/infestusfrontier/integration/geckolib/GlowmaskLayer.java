package org.jd.infestusfrontier.integration.geckolib;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Draws a model's glowmask over it, unlit by the world. The mask holds the glowing texels in their own colours and
 * alpha. GeckoLib's own glowing layer is not used: it cannot load a mask without a single texel.
 */
public final class GlowmaskLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
    private final RenderType glow;

    public GlowmaskLayer(GeoRenderer<T> renderer, ResourceLocation glowmask) {
        super(renderer);
        this.glow = RenderType.entityTranslucentEmissive(glowmask);
    }

    @Override
    public void render(
            PoseStack poseStack,
            T animatable,
            BakedGeoModel bakedModel,
            @Nullable RenderType renderType,
            MultiBufferSource bufferSource,
            @Nullable VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay) {
        getRenderer()
                .reRender(
                        bakedModel,
                        poseStack,
                        bufferSource,
                        animatable,
                        glow,
                        bufferSource.getBuffer(glow),
                        partialTick,
                        LightTexture.FULL_BRIGHT,
                        packedOverlay,
                        getRenderer()
                                .getRenderColor(animatable, partialTick, packedLight)
                                .argbInt());
    }
}
