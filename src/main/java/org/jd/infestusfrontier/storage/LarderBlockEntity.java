package org.jd.infestusfrontier.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.core.storage.PodKind;

/** The pod of a Larder: {@value PodKind#LARDER_COMPARTMENTS} compartments of one kind each, with a label and a reserve. */
final class LarderBlockEntity extends ItemCapsuleBlockEntity {
    LarderBlockEntity(BlockPos pos, BlockState state) {
        super(StorageModule.LARDER_ENTITY.get(), pos, state, PodKind.LARDER);
    }
}
