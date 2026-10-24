package org.jd.infestusfrontier.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * A living wall of one hardness tier. Walls are grown, never placed: sneaking with bone meal on a mature substrate
 * cell starts one ({@link #start}), and bone meal on a wall's top face grows a new Living Wall on it, one block a
 * bone meal. Grafts harden a wall one tier at a time. Broken, a wall dies into Dead Tissue; no piston moves it.
 */
public final class LivingWallBlock extends Block {
    private static final int GROWTH_PARTICLES = 8;

    LivingWallBlock(Properties properties) {
        super(properties);
    }

    /** Bone meal on the top face grows the wall one block up; every other face and item builds as usual. */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!stack.is(Items.BONE_MEAL) || hit.getDirection() != Direction.UP)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!grow(level, pos.above(), player, stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Sneaking with bone meal on the top of a mature Living Substrate cell starts a wall on it. True when it did; the
     * caller then stops the click there, so the bone meal does not also feed the floor.
     */
    static boolean start(Level level, BlockPos cell, Direction face, Player player, ItemStack stack) {
        if (!stack.is(Items.BONE_MEAL) || face != Direction.UP || !player.isSecondaryUseActive()) return false;
        BlockState state = level.getBlockState(cell);
        if (!state.is(EcologyModule.LIVING_SUBSTRATE.get())
                || state.getValue(LivingSubstrateBlock.STAGE) != SubstrateStage.MATURE) return false;
        return grow(level, cell.above(), player, stack);
    }

    /**
     * Grows a Living Wall at {@code at} for a player who may build (not a spectator or adventurer) when it is loaded,
     * inside the world, empty and clear of anyone standing there, as vanilla placement requires; spends one bone meal.
     */
    private static boolean grow(Level level, BlockPos at, Player player, ItemStack stack) {
        BlockState wall = EcologyModule.LIVING_WALL.get().defaultBlockState();
        if (!player.mayBuild()
                || !level.isInWorldBounds(at)
                || !level.hasChunkAt(at)
                || !level.getBlockState(at).canBeReplaced()
                || !level.isUnobstructed(wall, at, CollisionContext.empty())) return false;
        if (level.isClientSide) return true;
        level.setBlock(at, wall, Block.UPDATE_ALL);
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, at, GROWTH_PARTICLES);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return true;
    }
}
