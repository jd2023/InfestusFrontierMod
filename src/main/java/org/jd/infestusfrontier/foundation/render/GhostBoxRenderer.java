package org.jd.infestusfrontier.foundation.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix4f;

/**
 * Translucent boxes a block entity names, drawn through the ground at full brightness so the player sees a shape that
 * lies inside terrain: vanilla's see-through text background, which tests no depth. The boxes come from the block
 * entity's synced state alone; nothing here reads the world. A module calls {@link #register} on the client only, so
 * no client class is named where a dedicated server loads.
 */
public final class GhostBoxRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    /** One box relative to the block entity's own block, in {@code argb}. */
    public record Ghost(AABB box, int argb) {}

    private final Function<T, List<Ghost>> ghosts;
    private final int viewDistance;

    private GhostBoxRenderer(Function<T, List<Ghost>> ghosts, int viewDistance) {
        this.ghosts = ghosts;
        this.viewDistance = viewDistance;
    }

    /** Draws the ghosts of every block entity of {@code type} within {@code viewDistance}; call it on the client only. */
    public static <T extends BlockEntity> void register(
            IEventBus modBus,
            Supplier<? extends BlockEntityType<T>> type,
            Function<T, List<Ghost>> ghosts,
            int viewDistance) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(
                type.get(), context -> new GhostBoxRenderer<T>(ghosts, viewDistance)));
    }

    @Override
    public void render(
            T entity, float partialTick, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        List<Ghost> shown = ghosts.apply(entity);
        if (shown.isEmpty()) return;
        VertexConsumer out = buffers.getBuffer(RenderType.textBackgroundSeeThrough());
        Matrix4f pose = poses.last().pose();
        for (Ghost ghost : shown) box(out, pose, ghost.box(), ghost.argb());
    }

    /** The six faces of {@code box}, each wound to face outwards. */
    private static void box(VertexConsumer out, Matrix4f pose, AABB box, int argb) {
        float x0 = (float) box.minX;
        float y0 = (float) box.minY;
        float z0 = (float) box.minZ;
        float x1 = (float) box.maxX;
        float y1 = (float) box.maxY;
        float z1 = (float) box.maxZ;
        quad(out, pose, argb, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1); // down
        quad(out, pose, argb, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0); // up
        quad(out, pose, argb, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0); // north
        quad(out, pose, argb, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1); // south
        quad(out, pose, argb, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0); // west
        quad(out, pose, argb, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1); // east
    }

    private static void quad(VertexConsumer out, Matrix4f pose, int argb, float... corners) {
        for (int n = 0; n < corners.length; n += 3)
            out.addVertex(pose, corners[n], corners[n + 1], corners[n + 2])
                    .setColor(argb)
                    .setLight(LightTexture.FULL_BRIGHT);
    }

    /** The whole of the ghosts, so the renderer is kept while any of them is in view. */
    @Override
    public AABB getRenderBoundingBox(T entity) {
        AABB own = new AABB(entity.getBlockPos());
        for (Ghost ghost : ghosts.apply(entity)) own = own.minmax(ghost.box().move(entity.getBlockPos()));
        return own;
    }

    @Override
    public int getViewDistance() {
        return viewDistance;
    }
}
