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
 * The two-block flower a Tall Withered Flower was before infection: the one thing it keeps, because its harvest is
 * that flower's own drop (VEGETATION_INFECTION.md, "Harvest rules"). Appearance is not kept here; the blockstate and
 * the block colour draw it from the position.
 */
public enum TallFlowerSource implements StringRepresentable {
    SUNFLOWER("sunflower", Blocks.SUNFLOWER),
    LILAC("lilac", Blocks.LILAC),
    ROSE_BUSH("rose_bush", Blocks.ROSE_BUSH),
    PEONY("peony", Blocks.PEONY);

    private static final Map<Block, TallFlowerSource> BY_PLANT =
            Stream.of(values()).collect(Collectors.toUnmodifiableMap(TallFlowerSource::plant, Function.identity()));

    private final String name;
    private final Block plant;

    TallFlowerSource(String name, Block plant) {
        this.name = name;
        this.plant = plant;
    }

    /** The source of {@code plant}; empty for every block that is not a supported tall flower. */
    public static Optional<TallFlowerSource> of(Block plant) {
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
