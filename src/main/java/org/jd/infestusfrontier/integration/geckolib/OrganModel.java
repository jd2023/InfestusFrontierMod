package org.jd.infestusfrontier.integration.geckolib;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jd.infestusfrontier.InfestusFrontier;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

/**
 * One organ's GeckoLib model, for its block entity and its item alike: {@code geo/block/<name>.geo.json},
 * {@code animations/block/<name>.animation.json}, {@code textures/block_entity/<name>.png} and the glowmask
 * {@code <name>_glowmask.png}. Drawn translucent, so the texture's alpha decides what is membrane.
 */
public final class OrganModel<T extends GeoAnimatable> extends GeoModel<T> {
    private final ResourceLocation model;
    private final ResourceLocation animations;
    private final ResourceLocation texture;
    private final ResourceLocation glowmask;

    public OrganModel(String name) {
        model = ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "geo/block/" + name + ".geo.json");
        animations = ResourceLocation.fromNamespaceAndPath(
                InfestusFrontier.MOD_ID, "animations/block/" + name + ".animation.json");
        texture = ResourceLocation.fromNamespaceAndPath(
                InfestusFrontier.MOD_ID, "textures/block_entity/" + name + ".png");
        glowmask = ResourceLocation.fromNamespaceAndPath(
                InfestusFrontier.MOD_ID, "textures/block_entity/" + name + "_glowmask.png");
    }

    public ResourceLocation glowmask() {
        return glowmask;
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return animations;
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }
}
