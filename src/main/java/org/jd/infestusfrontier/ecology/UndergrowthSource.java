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
 * The one-block undergrowth a Blighted Undergrowth was before infection: short grass or a fern. It is all the block
 * keeps, because its harvest is that plant's own drop (VEGETATION_INFECTION.md, "Harvest rules").
 */
public enum UndergrowthSource implements StringRepresentable {
    SHORT_GRASS("short_grass", Blocks.SHORT_GRASS),
    FERN("fern", Blocks.FERN);

    private static final Map<Block, UndergrowthSource> BY_PLANT =
            Stream.of(values()).collect(Collectors.toUnmodifiableMap(UndergrowthSource::plant, Function.identity()));

    private final String name;
    private final Block plant;

    UndergrowthSource(String name, Block plant) {
        this.name = name;
        this.plant = plant;
    }

    /** The source of {@code plant}; empty for every block that is not a supported undergrowth. */
    public static Optional<UndergrowthSource> of(Block plant) {
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
