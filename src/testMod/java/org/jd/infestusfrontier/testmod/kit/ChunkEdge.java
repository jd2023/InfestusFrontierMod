package org.jd.infestusfrontier.testmod.kit;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

public final class ChunkEdge {
    private ChunkEdge() {}

    public static Fixture prepare(GameTestHelper helper) {
        var level = helper.getLevel();
        var source = level.getChunkSource();
        var origin = helper.absolutePos(BlockPos.ZERO);
        int chunkX = (origin.getX() >> 4) + 208;
        int chunkZ = origin.getZ() >> 4;
        level.getChunk(chunkX, chunkZ);
        var edge = new BlockPos(chunkX * 16 + 15, origin.getY() + 4, chunkZ * 16 + 8);
        int loadedBefore = source.getLoadedChunksCount();
        helper.assertTrue(source.getChunkNow(chunkX + 1, chunkZ) == null,
                "Chunk-edge fixture requires its eastern neighbor to remain absent");
        helper.assertTrue(level.getBlockState(edge).isAir(), "Chunk-edge fixture edge must be air");
        return new Fixture(edge, chunkX + 1, chunkZ, loadedBefore);
    }

    public static void assertNothingLoaded(GameTestHelper helper, Fixture fixture) {
        var source = helper.getLevel().getChunkSource();
        helper.assertTrue(source.getChunkNow(fixture.neighborChunkX(), fixture.neighborChunkZ()) == null
                        && source.getLoadedChunksCount() == fixture.loadedBefore(),
                "Chunk-edge fixture loaded its absent eastern neighbor or another chunk");
    }

    public record Fixture(BlockPos edge, int neighborChunkX, int neighborChunkZ, int loadedBefore) {}
}
