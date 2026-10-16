package org.jd.infestusfrontier.integration.geckolib;

import java.util.function.Supplier;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Client-only wiring of one organ's GeckoLib model to its block entity and its item. Called from a module's
 * {@code register} on the physical client only. Owns no gameplay state.
 */
public final class OrganRenderers {
    private OrganRenderers() {}

    public static <B extends BlockEntity & GeoBlockEntity, I extends Item & GeoItem> void register(
            IEventBus modBus, String name, Supplier<BlockEntityType<B>> blockEntity, Supplier<I> item) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(blockEntity.get(), context -> new BlockRenderer<>(name)));
        modBus.addListener((RegisterClientExtensionsEvent event) -> event.registerItem(
                new IClientItemExtensions() {
                    // Made on first use: the renderer needs the client's resources to be ready.
                    private ItemRenderer<I> renderer;

                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        if (renderer == null) renderer = new ItemRenderer<>(name);
                        return renderer;
                    }
                },
                item.get()));
    }

    private static final class BlockRenderer<B extends BlockEntity & GeoBlockEntity> extends GeoBlockRenderer<B> {
        BlockRenderer(String name) {
            super(new OrganModel<>(name));
            addRenderLayer(new GlowmaskLayer<>(this, ((OrganModel<B>) getGeoModel()).glowmask()));
        }
    }

    private static final class ItemRenderer<I extends Item & GeoItem> extends GeoItemRenderer<I> {
        ItemRenderer(String name) {
            super(new OrganModel<>(name));
            addRenderLayer(new GlowmaskLayer<>(this, ((OrganModel<I>) getGeoModel()).glowmask()));
        }
    }
}
