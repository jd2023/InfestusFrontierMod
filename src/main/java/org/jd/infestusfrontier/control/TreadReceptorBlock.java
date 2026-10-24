package org.jd.infestusfrontier.control;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A cell of mature Living Substrate mutated into a pressure plate, flush with the ground. It is vanilla's pressure
 * plate in a full block: a living creature standing on it presses it, as on a stone plate, and it stays pressed while
 * one is still there, checked once a second; items do not press it. A step presses it through the plate's own check.
 *
 * <p>Pressed, it powers every block beside and below it strongly, so a door standing on the cell next to it opens.
 * Grown, never placed: broken, it dies into Dead Tissue.
 */
public final class TreadReceptorBlock extends BasePressurePlateBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final MapCodec<TreadReceptorBlock> CODEC = simpleCodec(TreadReceptorBlock::new);

    /** Who presses it and how it sounds: living creatures, as the stone plate, with a soft squelch. */
    static final BlockSetType TYPE = BlockSetType.register(new BlockSetType(
            "infestusfrontier:tread_receptor",
            false,
            false,
            false,
            BlockSetType.PressurePlateSensitivity.MOBS,
            SoundType.SLIME_BLOCK,
            SoundEvents.SLIME_SQUISH_SMALL,
            SoundEvents.SLIME_SQUISH_SMALL,
            SoundEvents.SLIME_SQUISH_SMALL,
            SoundEvents.SLIME_SQUISH_SMALL,
            SoundEvents.HONEY_BLOCK_STEP,
            SoundEvents.SLIME_SQUISH_SMALL,
            SoundEvents.HONEY_BLOCK_STEP,
            SoundEvents.SLIME_SQUISH_SMALL));

    TreadReceptorBlock(Properties properties) {
        super(properties, TYPE);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected MapCodec<TreadReceptorBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    /** A whole cell of ground, not a thin plate. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    /** Ground needs nothing under it. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isPossibleToRespawnInThis(BlockState state) {
        return false;
    }

    /** Nothing walks inside a full block, so a step on its top presses it through the plate's own check. */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        entityInside(state, level, pos, entity);
        super.stepOn(level, pos, state, entity);
    }

    /** The stone plate's test, one block up: any living creature, not a spectator, standing on the top. */
    @Override
    protected int getSignalStrength(Level level, BlockPos pos) {
        return getEntityCount(level, TOUCH_AABB.move(pos.above()), LivingEntity.class) > 0 ? 15 : 0;
    }

    @Override
    protected int getSignalForState(BlockState state) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected BlockState setSignalForState(BlockState state, int signal) {
        return state.setValue(POWERED, signal > 0);
    }

    /** Strong power into every neighbour but the space above, where the creature stands. */
    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return side == Direction.DOWN ? 0 : getSignalForState(state);
    }

    /** The blocks it powers strongly tell their own neighbours, as a redstone torch does. */
    @Override
    protected void updateNeighbours(Level level, BlockPos pos) {
        level.updateNeighborsAt(pos, this);
        for (Direction direction : Direction.values())
            if (direction != Direction.UP) level.updateNeighborsAt(pos.relative(direction), this);
    }
}
