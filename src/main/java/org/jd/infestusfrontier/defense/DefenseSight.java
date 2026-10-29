package org.jd.infestusfrontier.defense;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Ray tracing and projectile movement never ask vanilla to read an unloaded chunk. */
final class DefenseSight {
    private DefenseSight() {}

    static boolean loaded(Level level, Vec3 from, Vec3 to) {
        for (int x = Mth.floor(Math.min(from.x, to.x)) >> 4; x <= Mth.floor(Math.max(from.x, to.x)) >> 4; x++)
            for (int z = Mth.floor(Math.min(from.z, to.z)) >> 4; z <= Mth.floor(Math.max(from.z, to.z)) >> 4; z++)
                if (!level.hasChunk(x, z)) return false;
        return true;
    }
}
