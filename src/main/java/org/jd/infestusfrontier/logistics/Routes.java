package org.jd.infestusfrontier.logistics;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The blocks a route runs through: vein cells, Septum Crossings and Vascular Stitches. A crossing and a stitch hold
 * nothing; what enters one face of them leaves by the face its passage leads to.
 */
final class Routes {
    /** What a crossing or a stitch passes, taken from the vein it was made on. */
    static final EnumProperty<Carrier> CARRIER = EnumProperty.create("carrier", Carrier.class);

    private Routes() {}

    static boolean isRoute(BlockState state) {
        Block block = state.getBlock();
        return block instanceof VeinBlock
                || block instanceof SeptumCrossingBlock
                || block instanceof VascularStitchBlock;
    }

    /**
     * Whether {@code state} lets {@code carrier} in or out through its own face {@code face}: a vein through every
     * face, a crossing through the faces of its two passages, a stitch through its two ends.
     */
    static boolean opens(BlockState state, Direction face, Carrier carrier) {
        Block block = state.getBlock();
        if (block instanceof VeinBlock vein) return vein.carrier() == carrier;
        if (block instanceof SeptumCrossingBlock)
            return state.getValue(CARRIER) == carrier && SeptumCrossingBlock.passes(state, face.getAxis());
        if (block instanceof VascularStitchBlock)
            return state.getValue(CARRIER) == carrier
                    && VascularStitchBlock.ends(state).contains(face);
        return false;
    }
}
