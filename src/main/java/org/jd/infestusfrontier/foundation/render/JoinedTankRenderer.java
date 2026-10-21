package org.jd.infestusfrontier.foundation.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The liquid in one cell of a tank built of joined cells, drawn from the block state alone: {@code fill} steps of
 * height, and a side property per face that is true where the face joins a cell of the same tank. The surface and the
 * walls run to the block edge on every joined face, so a tank's liquid reads as one volume with no seams; walls are
 * drawn only on faces that join nothing, and the surface only where no liquid stands on it. Translucent, culled, lit
 * by the cell's light. Nothing is synced for it. A module calls {@link #register} on the client only, so no client
 * class is named where a dedicated server loads.
 */
public final class JoinedTankRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    /** Where the liquid stops, in block units, inside the cell's own parts. */
    public record Insets(float side, float bottom, float top) {}

    private final IntegerProperty fill;
    private final int steps;
    private final Map<Direction, BooleanProperty> joined;
    private final Supplier<ResourceLocation> still;
    private final Insets insets;

    public JoinedTankRenderer(
            IntegerProperty fill,
            int steps,
            Map<Direction, BooleanProperty> joined,
            Supplier<ResourceLocation> still,
            Insets insets) {
        this.fill = fill;
        this.steps = steps;
        this.joined = joined;
        this.still = still;
        this.insets = insets;
    }

    /** Draws the liquid of every block entity of {@code type}; call it on the client only. */
    public static <T extends BlockEntity> void register(
            IEventBus modBus,
            Supplier<? extends BlockEntityType<T>> type,
            IntegerProperty fill,
            int steps,
            Map<Direction, BooleanProperty> joined,
            Supplier<ResourceLocation> still,
            Insets insets) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(
                type.get(), context -> new JoinedTankRenderer<T>(fill, steps, joined, still, insets)));
    }

    private boolean joined(BlockState state, Direction side) {
        return state.getValue(joined.get(side));
    }

    @Override
    public void render(T cell, float partialTick, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = cell.getBlockState();
        int level = state.getValue(fill);
        if (level == 0) return;
        float bottom = joined(state, Direction.DOWN) ? 0 : insets.bottom();
        float top = joined(state, Direction.UP) ? 1 : 1 - insets.top();
        float surface = bottom + (top - bottom) * level / steps;
        float x0 = joined(state, Direction.WEST) ? 0 : insets.side();
        float x1 = joined(state, Direction.EAST) ? 1 : 1 - insets.side();
        float z0 = joined(state, Direction.NORTH) ? 0 : insets.side();
        float z1 = joined(state, Direction.SOUTH) ? 1 : 1 - insets.side();
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(still.get());
        Face face =
                new Face(buffers.getBuffer(Sheets.translucentCullBlockSheet()), poses.last(), sprite, light, overlay);
        if (!liquidAbove(cell, state, level)) {
            face.vertex(x0, surface, z0, x0, z0, 0, 1, 0);
            face.vertex(x0, surface, z1, x0, z1, 0, 1, 0);
            face.vertex(x1, surface, z1, x1, z1, 0, 1, 0);
            face.vertex(x1, surface, z0, x1, z0, 0, 1, 0);
        }
        if (!joined(state, Direction.NORTH)) {
            face.vertex(x1, surface, z0, 1 - x1, 1 - surface, 0, 0, -1);
            face.vertex(x1, bottom, z0, 1 - x1, 1 - bottom, 0, 0, -1);
            face.vertex(x0, bottom, z0, 1 - x0, 1 - bottom, 0, 0, -1);
            face.vertex(x0, surface, z0, 1 - x0, 1 - surface, 0, 0, -1);
        }
        if (!joined(state, Direction.SOUTH)) {
            face.vertex(x0, surface, z1, x0, 1 - surface, 0, 0, 1);
            face.vertex(x0, bottom, z1, x0, 1 - bottom, 0, 0, 1);
            face.vertex(x1, bottom, z1, x1, 1 - bottom, 0, 0, 1);
            face.vertex(x1, surface, z1, x1, 1 - surface, 0, 0, 1);
        }
        if (!joined(state, Direction.WEST)) {
            face.vertex(x0, surface, z0, z0, 1 - surface, -1, 0, 0);
            face.vertex(x0, bottom, z0, z0, 1 - bottom, -1, 0, 0);
            face.vertex(x0, bottom, z1, z1, 1 - bottom, -1, 0, 0);
            face.vertex(x0, surface, z1, z1, 1 - surface, -1, 0, 0);
        }
        if (!joined(state, Direction.EAST)) {
            face.vertex(x1, surface, z1, 1 - z1, 1 - surface, 1, 0, 0);
            face.vertex(x1, bottom, z1, 1 - z1, 1 - bottom, 1, 0, 0);
            face.vertex(x1, bottom, z0, 1 - z0, 1 - bottom, 1, 0, 0);
            face.vertex(x1, surface, z0, 1 - z0, 1 - surface, 1, 0, 0);
        }
    }

    /** A full cell under a joined cell that holds liquid shows no surface: the liquid runs on up. */
    private boolean liquidAbove(T cell, BlockState state, int level) {
        Level world = cell.getLevel();
        if (world == null || level < steps || !joined(state, Direction.UP)) return false;
        BlockState above = world.getBlockState(cell.getBlockPos().above());
        return above.hasProperty(fill) && above.getValue(fill) > 0;
    }

    private record Face(VertexConsumer out, PoseStack.Pose pose, TextureAtlasSprite sprite, int light, int overlay) {
        void vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
            out.addVertex(pose, x, y, z)
                    .setColor(255, 255, 255, 255)
                    .setUv(sprite.getU(u), sprite.getV(v))
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, nx, ny, nz);
        }
    }
}
