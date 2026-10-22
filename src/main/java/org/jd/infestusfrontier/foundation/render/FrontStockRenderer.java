package org.jd.infestusfrontier.foundation.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The stock a block holds, drawn on its front as inventory icons in a grid of pockets with each count under its icon,
 * read from what the block entity syncs. Lit by the block in front. Nothing is decided here. A module calls
 * {@link #register} on the client only, so no client class is named where a dedicated server loads.
 */
public final class FrontStockRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    /**
     * Where the pockets are on a front, in block pixels seen from in front: {@code columns} across, the first
     * pocket's centre at {@code firstX} from the left and {@code firstY} from the bottom, {@code pitch} between
     * centres, icons {@code icon} wide, drawn on the plane {@code depth} pixels behind the block's front face.
     */
    public record Pockets(int columns, float firstX, float firstY, float pitch, float icon, float depth) {}

    /** Text height in block pixels. */
    private static final float TEXT = 1.5F;

    private static final int TEXT_COLOUR = 0xFFF4E6D8;

    private final DirectionProperty facing;
    private final Function<T, List<ItemStack>> shown;
    private final Pockets pockets;

    private FrontStockRenderer(DirectionProperty facing, Function<T, List<ItemStack>> shown, Pockets pockets) {
        this.facing = facing;
        this.shown = shown;
        this.pockets = pockets;
    }

    /** Draws the stock of every block entity of {@code type}; call it on the client only. */
    public static <T extends BlockEntity> void register(
            IEventBus modBus,
            Supplier<? extends BlockEntityType<T>> type,
            DirectionProperty facing,
            Function<T, List<ItemStack>> shown,
            Pockets pockets) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(
                type.get(), context -> new FrontStockRenderer<T>(facing, shown, pockets)));
    }

    @Override
    public void render(T block, float partialTick, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        List<ItemStack> stock = shown.apply(block);
        if (stock.stream().allMatch(ItemStack::isEmpty)) return;
        Direction front = block.getBlockState().getValue(facing);
        Level level = block.getLevel();
        int lit = level == null
                ? light
                : LevelRenderer.getLightColor(level, block.getBlockPos().relative(front));
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        poses.pushPose();
        poses.translate(0.5, 0.5, 0.5);
        poses.mulPose(Axis.YP.rotationDegrees(-front.toYRot())); // the front now faces +z, the viewer's right is +x
        for (int i = 0; i < stock.size(); i++) {
            ItemStack stack = stock.get(i);
            if (stack.isEmpty()) continue;
            float x = pockets.firstX() + (i % pockets.columns()) * pockets.pitch();
            float y = pockets.firstY() - (i / pockets.columns()) * pockets.pitch();
            float z = 8 - pockets.depth();
            poses.pushPose();
            poses.translate((x - 8) / 16, (y - 8 + TEXT * 0.4F) / 16, z / 16);
            poses.scale(pockets.icon() / 16, pockets.icon() / 16, 0.001F);
            minecraft
                    .getItemRenderer()
                    .renderStatic(
                            stack, ItemDisplayContext.GUI, lit, OverlayTexture.NO_OVERLAY, poses, buffers, level, 0);
            poses.popPose();
            String count = Integer.toString(stack.getCount());
            float scale = TEXT / 16 / font.lineHeight;
            poses.pushPose();
            poses.translate((x - 8) / 16, (y - 8 - pockets.icon() / 2 + TEXT * 0.4F) / 16, (z + 0.05F) / 16);
            poses.scale(scale, -scale, scale);
            font.drawInBatch(
                    count,
                    -font.width(count) / 2.0F,
                    0,
                    TEXT_COLOUR,
                    false,
                    poses.last().pose(),
                    buffers,
                    Font.DisplayMode.NORMAL,
                    0,
                    lit);
            poses.popPose();
        }
        poses.popPose();
    }
}
