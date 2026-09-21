package org.jd.infestusfrontier.construction;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Places shell blocks only when their placement halo is already loaded. */
final class LoadedAreaBlockItem extends BlockItem {
    LoadedAreaBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        if (!MembraneWindowBlock.placementAreaLoaded(context.getLevel(), context.getClickedPos())) return null;
        return super.getPlacementState(context);
    }

    @Override protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        return context.getLevel().setBlock(context.getClickedPos(), state,
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE, 0);
    }
}
