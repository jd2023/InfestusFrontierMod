package org.jd.infestusfrontier.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Server-observed selection ray with bounded reach and no chunk-loading reads. */
final class SelectedGroundVisibility implements BlockGetter {
    private static final double MAX_REACH = 8;
    private static final int MAX_STATE_READS = 64;
    private final Level level;
    private int reads;
    private boolean unavailable;

    private SelectedGroundVisibility(Level level) {
        this.level = level;
    }

    static boolean visible(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null) return false;
        Vec3 start = player.getEyePosition();
        Vec3 selected = context.getClickLocation();
        Vec3 relative = selected.subtract(Vec3.atCenterOf(context.getClickedPos()));
        Vec3 normal = Vec3.atLowerCornerOf(context.getClickedFace().getNormal());
        double distance = start.distanceToSqr(selected);
        double reach = Math.min(MAX_REACH, player.blockInteractionRange() + 1);
        if (!Double.isFinite(distance) || !(distance > 0 && distance <= reach * reach)
                || Math.abs(relative.x) > 0.5001 || Math.abs(relative.y) > 0.5001
                || Math.abs(relative.z) > 0.5001 || Math.abs(relative.dot(normal) - 0.5) > 0.0001) {
            return false;
        }
        SelectedGroundVisibility view = new SelectedGroundVisibility(context.getLevel());
        BlockPos neighbor = context.getClickedPos().relative(context.getClickedFace());
        if (view.getBlockState(neighbor).isFaceSturdy(view, neighbor,
                context.getClickedFace().getOpposite(), SupportType.FULL) || view.unavailable) return false;
        var hit = view.clip(new ClipContext(start, selected.subtract(normal.scale(0.001)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return !view.unavailable && hit.getType() == HitResult.Type.BLOCK
                && !hit.isInside() && hit.getBlockPos().equals(context.getClickedPos())
                && hit.getDirection() == context.getClickedFace();
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (++reads <= MAX_STATE_READS) {
            var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null) return chunk.getBlockState(pos);
        }
        unavailable = true;
        return Blocks.BEDROCK.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        // Shape callbacks cannot escape into mutable world data through this view.
        unavailable = true;
        return null;
    }

    @Override
    public int getHeight() {
        return level.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return level.getMinBuildHeight();
    }
}
