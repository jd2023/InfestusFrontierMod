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
 * The species an infected tree was: the one thing Infected Logs and Blighted Leaves keep, so the bark can be told apart
 * and the leaves drop what their species dropped (VEGETATION_INFECTION.md, "Harvest rules"). The tints are the
 * species' muted tones; a log's harvest never depends on it.
 */
public enum TreeSource implements StringRepresentable {
    OAK("oak", Blocks.OAK_LEAVES, 0x9A7C52, 0x9A9250),
    BIRCH("birch", Blocks.BIRCH_LEAVES, 0xCFC6A8, 0xC4BE88),
    SPRUCE("spruce", Blocks.SPRUCE_LEAVES, 0x6F6A58, 0x6A7C66),
    JUNGLE("jungle", Blocks.JUNGLE_LEAVES, 0x8A8A58, 0x7E8C4C),
    ACACIA("acacia", Blocks.ACACIA_LEAVES, 0xB8925E, 0xB0A25A),
    DARK_OAK("dark_oak", Blocks.DARK_OAK_LEAVES, 0x5F5244, 0x5E6048);

    private static final Map<Block, TreeSource> BY_LEAVES =
            Stream.of(values()).collect(Collectors.toUnmodifiableMap(TreeSource::leaves, Function.identity()));

    private final String name;
    private final Block leaves;
    private final int barkTint;
    private final int leafTint;

    TreeSource(String name, Block leaves, int barkTint, int leafTint) {
        this.name = name;
        this.leaves = leaves;
        this.barkTint = barkTint;
        this.leafTint = leafTint;
    }

    /** The source of {@code leaves}; empty for every block that is not one of the six supported leaf blocks. */
    public static Optional<TreeSource> ofLeaves(Block leaves) {
        return Optional.ofNullable(BY_LEAVES.get(leaves));
    }

    /** The living leaf block this source names. */
    public Block leaves() {
        return leaves;
    }

    /** The colour this species' infected bark is drawn in. */
    public int barkTint() {
        return barkTint;
    }

    /** The colour this species' blighted leaves are drawn in. */
    public int leafTint() {
        return leafTint;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
