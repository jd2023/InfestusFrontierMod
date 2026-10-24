package org.jd.infestusfrontier.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * A treatment that opens a living wall: a window graft turns the wall cell it is used on into a Membrane Window; a door
 * graft turns that cell and the wall cell above or below it into a Sphincter Door facing the player. Walls of any tier
 * take them; anything else ignores them.
 */
final class OpeningGraftItem extends Item {
    private static final int PARTICLES = 10;
    private final boolean door;

    OpeningGraftItem(Properties properties, boolean door) {
        super(properties);
        this.door = door;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos target = context.getClickedPos();
        if (!(level.getBlockState(target).getBlock() instanceof LivingWallBlock)) return InteractionResult.PASS;
        boolean done = door ? door(context, level, target) : window(level, target);
        if (!done) return InteractionResult.PASS;
        Player player = context.getPlayer();
        if (!level.isClientSide && (player == null || !player.getAbilities().instabuild))
            context.getItemInHand().shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean window(Level level, BlockPos target) {
        if (level.isClientSide) return true;
        BlockState window = EcologyModule.MEMBRANE_WINDOW.get().defaultBlockState();
        level.setBlock(target, Block.updateFromNeighbourShapes(window, level, target), Block.UPDATE_ALL);
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target, PARTICLES);
        return true;
    }

    /**
     * The doorway is the clicked wall cell and the wall cell above it, or below it when there is none above, and it
     * must stand where a vanilla door could. It faces the clicked side, or the player when a top or bottom is clicked.
     */
    private static boolean door(UseOnContext context, Level level, BlockPos target) {
        BlockPos lower;
        if (isWall(level, target.above())) lower = target;
        else if (isWall(level, target.below())) lower = target.below();
        else return false;
        Direction clicked = context.getClickedFace();
        Direction facing = clicked.getAxis().isHorizontal()
                ? clicked
                : context.getHorizontalDirection().getOpposite();
        BlockState bottom = EcologyModule.SPHINCTER_DOOR
                .get()
                .defaultBlockState()
                .setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        if (!bottom.canSurvive(level, lower)) return false;
        if (level.isClientSide) return true;
        level.setBlock(lower, bottom, Block.UPDATE_ALL);
        level.setBlock(lower.above(), bottom.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, lower, PARTICLES);
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, lower.above(), PARTICLES);
        return true;
    }

    private static boolean isWall(Level level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() instanceof LivingWallBlock;
    }
}
