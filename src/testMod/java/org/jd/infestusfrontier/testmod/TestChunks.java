package org.jd.infestusfrontier.testmod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Forcing chunks for tests that unload part of a build. The GameTest server runs ticks without waiting, so a far chunk
 * a test forces may still be generating on other threads when the test's tick budget runs out. {@link #force} loads it
 * and its ring on the server thread, so it ticks within a few ticks however slow the machine is.
 */
final class TestChunks {
    /** The ring a forced chunk needs loaded around it before it ticks. */
    private static final int RING = 2;

    private TestChunks() {}

    /** Forces {@code chunk} and loads it and its ring now. */
    static void force(ServerLevel level, ChunkPos chunk) {
        level.setChunkForced(chunk.x, chunk.z, true);
        for (int dx = -RING; dx <= RING; dx++)
            for (int dz = -RING; dz <= RING; dz++) level.getChunk(chunk.x + dx, chunk.z + dz);
    }
}
