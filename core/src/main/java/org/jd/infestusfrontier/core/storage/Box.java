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

    public int sizeX() {
        return maxX - minX + 1;
    }

    public int sizeY() {
        return maxY - minY + 1;
    }

    public int sizeZ() {
        return maxZ - minZ + 1;
    }

    public int volume() {
        return sizeX() * sizeY() * sizeZ();
    }

    /** True when no side is longer than {@code side}. */
    public boolean fits(int side) {
        return sizeX() <= side && sizeY() <= side && sizeZ() <= side;
    }

    public boolean single() {
        return volume() == 1;
    }
}
