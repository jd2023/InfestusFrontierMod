package org.jd.infestusfrontier.ecology;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The sapling a Withered Sapling was before infection: the one thing it keeps, because its harvest is that sapling's
 * own loot table (VEGETATION_INFECTION.md, "Harvest rules"). The tint is the species' muted tone, so the six can be
 * told apart; the block never grows into a tree.
 */
public enum SaplingSource implements StringRepresentable {
    OAK("oak", Blocks.OAK_SAPLING, 0x8A6F48),
    BIRCH("birch", Blocks.BIRCH_SAPLING, 0xCFC6A8),
    SPRUCE("spruce", Blocks.SPRUCE_SAPLING, 0x5F5A4A),
    JUNGLE("jungle", Blocks.JUNGLE_SAPLING, 0x7B7A4E),
    ACACIA("acacia", Blocks.ACACIA_SAPLING, 0xA8845A),
    DARK_OAK("dark_oak", Blocks.DARK_OAK_SAPLING, 0x4F4338);

    private static final Map<Block, SaplingSource> BY_SAPLING =
            Stream.of(values()).collect(Collectors.toUnmodifiableMap(SaplingSource::sapling, Function.identity()));

    private final String name;
    private final Block sapling;
    private final int tint;

    SaplingSource(String name, Block sapling, int tint) {
        this.name = name;
        this.sapling = sapling;
        this.tint = tint;
    }

    /** The source of {@code sapling}; empty for every block that is not a supported sapling. */
    public static Optional<SaplingSource> of(Block sapling) {
        return Optional.ofNullable(BY_SAPLING.get(sapling));
    }

    /** The living sapling this source names. */
    public Block sapling() {
        return sapling;
    }

    /** The colour this species' withered sapling is drawn in. */
    public int tint() {
        return tint;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
