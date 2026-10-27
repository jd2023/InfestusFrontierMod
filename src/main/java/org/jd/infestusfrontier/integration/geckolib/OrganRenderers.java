package org.jd.infestusfrontier.integration.geckolib;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
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

    /** The space a model fills, relative to its block: one block, unless the registration says otherwise. */
    private static final AABB BLOCK = new AABB(0, 0, 0, 1, 1, 1);

    public static <B extends BlockEntity & GeoBlockEntity, I extends Item & GeoItem> void register(
            IEventBus modBus, String name, Supplier<BlockEntityType<B>> blockEntity, Supplier<I> item) {
        register(modBus, name, blockEntity, item, BLOCK, context -> null);
    }

    /**
     * As {@link #register(IEventBus, String, Supplier, Supplier)} for a model that reaches past its block:
     * {@code extent} is the space it fills in any pose, relative to the block, so it is not culled while only the part
     * outside the block is in view.
     */
    public static <B extends BlockEntity & GeoBlockEntity, I extends Item & GeoItem> void register(
            IEventBus modBus, String name, Supplier<BlockEntityType<B>> blockEntity, Supplier<I> item, AABB extent) {
        register(modBus, name, blockEntity, item, extent, context -> null);
    }

    /**
     * As {@link #register(IEventBus, String, Supplier, Supplier)}, with {@code overlay} drawn after the model each
     * frame: what the organ shows beside its model, such as the stock on a pod's face.
     */
    public static <B extends BlockEntity & GeoBlockEntity, I extends Item & GeoItem> void register(
            IEventBus modBus,
            String name,
            Supplier<BlockEntityType<B>> blockEntity,
            Supplier<I> item,
            Function<BlockEntityRendererProvider.Context, BlockEntityRenderer<B>> overlay) {
        register(modBus, name, blockEntity, item, BLOCK, overlay);
    }

    private static <B extends BlockEntity & GeoBlockEntity, I extends Item & GeoItem> void register(
            IEventBus modBus,
            String name,
            Supplier<BlockEntityType<B>> blockEntity,
            Supplier<I> item,
            AABB extent,
            Function<BlockEntityRendererProvider.Context, BlockEntityRenderer<B>> overlay) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(
                blockEntity.get(), context -> new BlockRenderer<>(name, extent, overlay.apply(context))));
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
        AABB extent = new AABB(0, 0, 0, 1, height, 1);
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(
                blockEntity.get(), context -> new BlockRenderer<>(name, extent, null)));
    }

    private static final class BlockRenderer<B extends BlockEntity & GeoBlockEntity> extends GeoBlockRenderer<B> {
        private final AABB extent;
        private final BlockEntityRenderer<B> overlay;

        BlockRenderer(String name, AABB extent, BlockEntityRenderer<B> overlay) {
            super(new OrganModel<>(name));
            this.extent = extent;
            this.overlay = overlay;
            addRenderLayer(new GlowmaskLayer<>(this, ((OrganModel<B>) getGeoModel()).glowmask()));
        }

        @Override
        public void render(
                B blockEntity,
                float partialTick,
                PoseStack poses,
                MultiBufferSource buffers,
                int packedLight,
                int packedOverlay) {
            super.render(blockEntity, partialTick, poses, buffers, packedLight, packedOverlay);
            if (overlay != null) overlay.render(blockEntity, partialTick, poses, buffers, packedLight, packedOverlay);
        }

        @Override
        public AABB getRenderBoundingBox(B blockEntity) {
            return extent.move(blockEntity.getBlockPos());
        }
    }

    private static final class ItemRenderer<I extends Item & GeoItem> extends GeoItemRenderer<I> {
        ItemRenderer(String name) {
            super(new OrganModel<>(name));
            addRenderLayer(new GlowmaskLayer<>(this, ((OrganModel<I>) getGeoModel()).glowmask()));
        }
    }
}
