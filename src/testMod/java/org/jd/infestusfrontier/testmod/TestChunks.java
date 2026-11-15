package org.jd.infestusfrontier.testmod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Forcing chunks for tests that unload part of a build. The GameTest server runs ticks without waiting, so a far chunk
 * a test forces may still be generating on other threads when the test's tick budget runs out. {@link #force} loads it
 * and its ring on the server thread, so it ticks within a few ticks however slow the machine is.
 *
 * <p>A chunk is either forced or not, however many tests asked, and the tests of a batch run side by side along one
 * row, 21 blocks apart in x. So each test that forces chunks takes its own z offset, 64 blocks from the others': a
 * test's x-offset chunks reach those of its neighbours in the row, and one that unforces a chunk another is waiting on
 * unloads it under that test.
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
