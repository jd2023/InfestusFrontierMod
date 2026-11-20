package org.jd.infestusfrontier.mining;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.MultifaceSpreader;

/**
 * Scrape marks and a little torn tissue a Structure Grower leaves on one wall of every few steps it builds, where the
 * jaws worked: a flat mark on the wall's face, no collision. It never spreads and never covers ore; when its wall goes it
 * goes too. Grown tissue: it gives nothing back.
 */
public final class CutScarBlock extends MultifaceBlock {
    private static final MapCodec<CutScarBlock> CODEC = simpleCodec(CutScarBlock::new);
    /** Required by the multiface block; a scar never spreads, so it is never asked to. */
    private final MultifaceSpreader spreader = new MultifaceSpreader(this);

    CutScarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CutScarBlock> codec() {
        return CODEC;
    }

    /** Only walls are scarred. */
    @Override
    protected boolean isFaceSupported(Direction face) {
        return face.getAxis().isHorizontal();
    }

    @Override
    public MultifaceSpreader getSpreader() {
        return spreader;
    }
}
