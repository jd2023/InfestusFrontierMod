package org.jd.infestusfrontier.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.core.storage.PodKind;

/** The pod of a Mixed Craw: {@value PodKind#CRAW_COMPARTMENTS} compartments of one kind each. */
final class MixedCrawBlockEntity extends ItemCapsuleBlockEntity {
    MixedCrawBlockEntity(BlockPos pos, BlockState state) {
        super(StorageModule.MIXED_CRAW_ENTITY.get(), pos, state, PodKind.CRAW);
    }
}
