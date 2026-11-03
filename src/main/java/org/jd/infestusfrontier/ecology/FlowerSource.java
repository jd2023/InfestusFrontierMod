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
 * The flower a Withered Flower was before infection: the one thing it keeps, because its harvest is that flower's own
 * loot table (VEGETATION_INFECTION.md, "Harvest rules"). Appearance is not kept here; the blockstate draws it from the
 * position.
 */
public enum FlowerSource implements StringRepresentable {
    DANDELION("dandelion", Blocks.DANDELION),
    POPPY("poppy", Blocks.POPPY),
    BLUE_ORCHID("blue_orchid", Blocks.BLUE_ORCHID),
    ALLIUM("allium", Blocks.ALLIUM),
    AZURE_BLUET("azure_bluet", Blocks.AZURE_BLUET),
    RED_TULIP("red_tulip", Blocks.RED_TULIP),
    ORANGE_TULIP("orange_tulip", Blocks.ORANGE_TULIP),
    WHITE_TULIP("white_tulip", Blocks.WHITE_TULIP),
    PINK_TULIP("pink_tulip", Blocks.PINK_TULIP),
    OXEYE_DAISY("oxeye_daisy", Blocks.OXEYE_DAISY),
    CORNFLOWER("cornflower", Blocks.CORNFLOWER),
    LILY_OF_THE_VALLEY("lily_of_the_valley", Blocks.LILY_OF_THE_VALLEY);

    private static final Map<Block, FlowerSource> BY_FLOWER =
            Stream.of(values()).collect(Collectors.toUnmodifiableMap(FlowerSource::flower, Function.identity()));

    private final String name;
    private final Block flower;

    FlowerSource(String name, Block flower) {
        this.name = name;
        this.flower = flower;
    }

    /** The source of {@code flower}; empty for every block that is not a supported flower. */
    public static Optional<FlowerSource> of(Block flower) {
        return Optional.ofNullable(BY_FLOWER.get(flower));
    }

    /** The living flower this source names. */
    public Block flower() {
        return flower;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
