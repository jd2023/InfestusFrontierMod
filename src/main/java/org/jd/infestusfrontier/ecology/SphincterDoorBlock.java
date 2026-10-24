package org.jd.infestusfrontier.ecology;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A two-high doorway mutated from a wall (BLOCK_CATALOG T1-29): a muscular rim whose membrane blades close like an
 * iris. It is a vanilla door, so hands and redstone open it and its two halves stay one doorway; the lower half keeps
 * the block entity that draws and animates the whole iris, the upper half draws nothing of its own. Closed, a membrane
 * four pixels thick fills the doorway; open, nothing stops a walker. Grown, never placed: broken, it dies into Dead
 * Tissue.
 */
public final class SphincterDoorBlock extends DoorBlock implements EntityBlock {
    /** How the doorway sounds and who may open it: any hand, redstone, never an arrow or a wind charge. */
    static final BlockSetType TYPE = new BlockSetType(
            "infestusfrontier:sphincter",
            true,
            false,
            false,
            BlockSetType.PressurePlateSensitivity.EVERYTHING,
            SoundType.SLIME_BLOCK,
            SoundEvents.HONEY_BLOCK_SLIDE,
            SoundEvents.SLIME_SQUISH,
            SoundEvents.HONEY_BLOCK_SLIDE,
            SoundEvents.SLIME_SQUISH,
            SoundEvents.SLIME_BLOCK_STEP,
            SoundEvents.SLIME_BLOCK_STEP,
            SoundEvents.SLIME_BLOCK_STEP,
            SoundEvents.SLIME_BLOCK_STEP);

    public static final MapCodec<SphincterDoorBlock> CODEC = simpleCodec(SphincterDoorBlock::new);
    private static final VoxelShape ACROSS_X = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape ACROSS_Z = Block.box(6, 0, 0, 10, 16, 16);
    private static final VoxelShape JAMBS_X = Shapes.or(Block.box(0, 0, 6, 3, 16, 10), Block.box(13, 0, 6, 16, 16, 10));
    private static final VoxelShape JAMBS_Z = Shapes.or(Block.box(6, 0, 0, 10, 16, 3), Block.box(6, 0, 13, 10, 16, 16));

    SphincterDoorBlock(Properties properties) {
        super(TYPE, properties);
    }

    @Override
    public MapCodec<SphincterDoorBlock> codec() {
        return CODEC;
    }

    /** Closed, the membrane across the doorway; open, only the jambs, so a click passes through the doorway. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean acrossX = state.getValue(FACING).getAxis() == Direction.Axis.Z;
        if (isOpen(state)) return acrossX ? JAMBS_X : JAMBS_Z;
        return acrossX ? ACROSS_X : ACROSS_Z;
    }

    /** Closed, the membrane stops a walker; open, nothing does. */
    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isOpen(state) ? Shapes.empty() : getShape(state, level, pos, context);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new SphincterDoorBlockEntity(pos, state) : null;
    }
}
