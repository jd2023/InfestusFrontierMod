package org.jd.infestusfrontier.ecology;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

final class LivingSubstrateItem extends BlockItem {
    LivingSubstrateItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (context.getLevel() instanceof ServerLevel level
                && context.getPlayer() != null
                && !SubstrateOwnership.get(level).canClaim(context.getClickedPos())) {
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }
}
