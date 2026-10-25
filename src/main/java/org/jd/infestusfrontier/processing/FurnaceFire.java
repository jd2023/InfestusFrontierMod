package org.jd.infestusfrontier.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Bio-Furnace's fire, as vanilla's furnace shows it: light 13 while it works, flames and smoke in its mouth and
 * the crackle, and smoke from its chimneys. The places match art/gen_bio_furnace.py: the mouth faces north, the
 * chimney's lip is centred 1.5 px behind the middle and the second chimney (stage 2) stands to one side.
 */
final class FurnaceFire {
    /** The light a working furnace gives, a vanilla furnace's. */
    static final int LIGHT = 13;

    private FurnaceFire() {}

    static int light(BlockState state) {
        return state.getValue(BatchOrganBlock.WORKING) ? LIGHT : 0;
    }

    static void animate(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(BatchOrganBlock.WORKING)) return;
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        if (random.nextDouble() < 0.1)
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        double across = (random.nextDouble() - 0.5) * 6.0 / 16.0;
        double up = (3.0 + random.nextDouble() * 4.0) / 16.0;
        level.addParticle(ParticleTypes.FLAME, x + across, y + up, z - 5.5 / 16.0, 0.0, 0.0, 0.0);
        if (random.nextBoolean())
            level.addParticle(ParticleTypes.SMOKE, x + across, y + 7.0 / 16.0, z - 7.5 / 16.0, 0.0, 0.0, 0.0);
        chimney(level, random, x, y + 15.6 / 16.0, z + 1.5 / 16.0);
        if (state.hasProperty(BatchOrganBlock.STAGE) && state.getValue(BatchOrganBlock.STAGE) >= 2)
            chimney(level, random, x - 4.5 / 16.0, y + 15.6 / 16.0, z + 2.5 / 16.0);
    }

    private static void chimney(Level level, RandomSource random, double x, double y, double z) {
        if (random.nextDouble() < 0.6) level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.04, 0.0);
        if (random.nextDouble() < 0.15) level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0.0, 0.02, 0.0);
    }
}
