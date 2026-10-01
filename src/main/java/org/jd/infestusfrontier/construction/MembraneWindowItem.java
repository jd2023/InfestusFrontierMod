package org.jd.infestusfrontier.construction;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Installs a passive pane and its six immediate joins without recursive shape updates. */
final class MembraneWindowItem extends BlockItem {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    MembraneWindowItem(MembraneWindowBlock block, Properties properties) {
        super(block, properties);
    }

    @Override protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (!MembraneWindowBlock.placementAreaLoaded(level, pos)) return false;
        // Apply component properties here, so BlockItem's later component step cannot
        // re-enter vanilla's recursive setBlock path after the bounded installation.
        state = context.getItemInHand().getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).apply(state);
        if (!level.setBlock(pos, state, FLAGS, 0)) return false;
        for (var direction : Direction.values()) {
            var neighborPos = pos.relative(direction);
            var neighbor = level.getBlockState(neighborPos);
            if (neighbor.getBlock() instanceof IronBarsBlock || neighbor.getBlock() instanceof WallBlock) {
                var joined = neighbor.updateShape(direction.getOpposite(), state, level, neighborPos, pos);
                if (joined != neighbor) level.setBlock(neighborPos, joined, FLAGS, 0);
            }
        }
        return true;
    }
}
