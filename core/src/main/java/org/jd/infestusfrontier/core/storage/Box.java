package org.jd.infestusfrontier.core.storage;

/** An axis-aligned box of block cells, both corners inclusive. */
public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public Box {
        if (minX > maxX || minY > maxY || minZ > maxZ) throw new IllegalArgumentException("Inverted box");
    }

    /** The box of one cell. */
    public static Box of(int x, int y, int z) {
        return new Box(x, y, z, x, y, z);
    }

    public Box union(Box other) {
        return new Box(
                Math.min(minX, other.minX),
                Math.min(minY, other.minY),
                Math.min(minZ, other.minZ),
                Math.max(maxX, other.maxX),
                Math.max(maxY, other.maxY),
                Math.max(maxZ, other.maxZ));
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean contains(Box other) {
        return contains(other.minX, other.minY, other.minZ) && contains(other.maxX, other.maxY, other.maxZ);
    }

    /** The length of a side, exact for any corners; a side wider than an int throws. */
    private static int side(int min, int max) {
        return Math.toIntExact((long) max - min + 1);
    }

    public int sizeX() {
        return side(minX, maxX);
    }

    public int sizeY() {
        return side(minY, maxY);
    }

    public int sizeZ() {
        return side(minZ, maxZ);
    }

    public int volume() {
        return Math.toIntExact((long) sizeX() * sizeY() * sizeZ());
    }

    /** True when no side is longer than {@code side}. Exact for any corners, so check it before anything else. */
    public boolean fits(int side) {
        return (long) maxX - minX < side && (long) maxY - minY < side && (long) maxZ - minZ < side;
    }

    public boolean single() {
        return minX == maxX && minY == maxY && minZ == maxZ;
    }
}
