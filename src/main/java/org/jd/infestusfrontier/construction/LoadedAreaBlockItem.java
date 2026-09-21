package org.jd.infestusfrontier.construction;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.BlockItemStateProperties;
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
        // Apply components before the bounded write so vanilla's later component
        // step sees the final state and cannot start recursive shape updates.
        state = context.getItemInHand().getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).apply(state);
        return context.getLevel().setBlock(context.getClickedPos(), state,
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE, 0);
    }
}
