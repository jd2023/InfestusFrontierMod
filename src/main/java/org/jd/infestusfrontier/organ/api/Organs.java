package org.jd.infestusfrontier.organ.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import org.jd.infestusfrontier.InfestusFrontier;

/** Rules every organ block shares. */
public final class Organs {
    /** Blocks an organ may stand on. Living Substrate in every stage. */
    public static final TagKey<Block> FOUNDATIONS = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "organ_foundations"));

    private Organs() {}

    /** True when the block below {@code pos} is a loaded foundation. An organ places and works only then. */
    public static boolean hasFoundation(LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.hasChunkAt(below) && level.getBlockState(below).is(FOUNDATIONS);
    }
}
