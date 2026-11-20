package org.jd.infestusfrontier.mining;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.TransparentBlock;

/**
 * Ground a Leaching Pod weakened. Glass underneath: translucent, solid, and the faces between two of them are not
 * drawn. It breaks almost at once by hand and leaves nothing. It gives no light: the pod's root lights the pit.
 */
public final class LeachedRockBlock extends TransparentBlock {
    /** Soft: about a fifth of a second by hand. */
    static final float HARDNESS = 0.1F;

    private static final MapCodec<LeachedRockBlock> CODEC = simpleCodec(LeachedRockBlock::new);

    LeachedRockBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<LeachedRockBlock> codec() {
        return CODEC;
    }
}
