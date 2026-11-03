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
 * The two-block undergrowth a Tall Blighted Undergrowth was before infection: tall grass or a large fern. It is all
 * the block keeps, because its harvest is that plant's own drop (VEGETATION_INFECTION.md, "Harvest rules").
 */
public enum TallUndergrowthSource implements StringRepresentable {
    TALL_GRASS("tall_grass", Blocks.TALL_GRASS),
    LARGE_FERN("large_fern", Blocks.LARGE_FERN);

    private static final Map<Block, TallUndergrowthSource> BY_PLANT = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(TallUndergrowthSource::plant, Function.identity()));

    private final String name;
    private final Block plant;

    TallUndergrowthSource(String name, Block plant) {
        this.name = name;
        this.plant = plant;
    }

    /** The source of {@code plant}; empty for every block that is not a supported tall undergrowth. */
    public static Optional<TallUndergrowthSource> of(Block plant) {
        return Optional.ofNullable(BY_PLANT.get(plant));
    }

    /** The living plant this source names. */
    public Block plant() {
        return plant;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
