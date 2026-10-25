package org.jd.infestusfrontier.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Activation Cyst brewing: a soft light (5) from its glowing chambers, swirls rising from the lid of its mixing
 * bubble, 14.5 px up in the middle (art/gen_activation_cyst.py), and now and then a brewing stand's bubbling.
 */
final class CystBrew implements OrganEffects {
    static final CystBrew INSTANCE = new CystBrew();
    /** The light of a working cyst: its chambers glow, dimmer than the cradle's cocoon. */
    static final int LIGHT = 5;

    private CystBrew() {}

    @Override
    public int light(BlockState state) {
        return state.getValue(BatchOrganBlock.WORKING) ? LIGHT : 0;
    }

    @Override
    public void animate(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(BatchOrganBlock.WORKING)) return;
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.15;
        double y = pos.getY() + 14.8 / 16.0;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.15;
        level.addParticle(ParticleTypes.EFFECT, x, y, z, 0.0, 0.0, 0.0);
        if (random.nextDouble() < 0.2)
            level.playLocalSound(
                    x,
                    y,
                    z,
                    SoundEvents.BREWING_STAND_BREW,
                    SoundSource.BLOCKS,
                    0.6F,
                    0.9F + random.nextFloat() * 0.2F,
                    false);
    }
}
