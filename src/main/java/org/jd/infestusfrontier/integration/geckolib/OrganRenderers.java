package org.jd.infestusfrontier.integration.geckolib;

import java.util.function.Supplier;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
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

    /**
     * A block entity model without an item, {@code height} blocks tall from its block up, so it is not culled while only
     * its upper part is in view.
     */
    public static <B extends BlockEntity & GeoBlockEntity> void registerBlock(
            IEventBus modBus, String name, Supplier<BlockEntityType<B>> blockEntity, int height) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(blockEntity.get(), context -> new BlockRenderer<>(name, height)));
    }

    private static final class BlockRenderer<B extends BlockEntity & GeoBlockEntity> extends GeoBlockRenderer<B> {
        private final int height;

        BlockRenderer(String name) {
            this(name, 1);
        }

        BlockRenderer(String name, int height) {
            super(new OrganModel<>(name));
            this.height = height;
            addRenderLayer(new GlowmaskLayer<>(this, ((OrganModel<B>) getGeoModel()).glowmask()));
        }

        @Override
        public AABB getRenderBoundingBox(B blockEntity) {
            return new AABB(blockEntity.getBlockPos()).expandTowards(0, height - 1, 0);
        }
    }

    private static final class ItemRenderer<I extends Item & GeoItem> extends GeoItemRenderer<I> {
        ItemRenderer(String name) {
            super(new OrganModel<>(name));
            addRenderLayer(new GlowmaskLayer<>(this, ((OrganModel<I>) getGeoModel()).glowmask()));
        }
    }
}
