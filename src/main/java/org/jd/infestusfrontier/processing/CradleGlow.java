package org.jd.infestusfrontier.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Awakening Cradle at work: the cocoon glows (light 7), specks of the culture are drawn into it and it beats like
 * a heart. Vanilla's portal particle flies in to where it is spawned, so the specks are spawned at the cocoon's
 * middle, 6.5 px up in art/gen_awakening_cradle.py, with the place they start from as their velocity.
 */
final class CradleGlow implements OrganEffects {
    static final CradleGlow INSTANCE = new CradleGlow();
    /** The light of a working cradle: a glow, dimmer than a furnace's fire. */
    static final int LIGHT = 7;

    private CradleGlow() {}

    @Override
    public int light(BlockState state) {
        return state.getValue(BatchOrganBlock.WORKING) ? LIGHT : 0;
    }

    @Override
    public void animate(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(BatchOrganBlock.WORKING)) return;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 6.5 / 16.0;
        double z = pos.getZ() + 0.5;
        for (int i = 0; i < 2; i++)
            level.addParticle(
                    ParticleTypes.PORTAL,
                    x,
                    y,
                    z,
                    (random.nextDouble() - 0.5) * 1.2,
                    -random.nextDouble() * 0.5,
                    (random.nextDouble() - 0.5) * 1.2);
        if (random.nextDouble() < 0.15)
            level.playLocalSound(x, y, z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 0.5F, 1.3F, false);
    }
}
