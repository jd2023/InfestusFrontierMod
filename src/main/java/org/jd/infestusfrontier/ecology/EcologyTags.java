package org.jd.infestusfrontier.ecology;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jd.infestusfrontier.InfestusFrontier;

final class EcologyTags {
    static final TagKey<Block> CULTURE_ELIGIBLE = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "ecology/culture_eligible"));

    private EcologyTags() {}
}
