package org.jd.infestusfrontier.construction;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

final class ConstructionTags {
    static final TagKey<Item> SEED_STOCK = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("infestusfrontier", "construction/seed_stock"));
    private ConstructionTags() {}
}
