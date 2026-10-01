package org.jd.infestusfrontier.processing;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Registry translation for the dynamic vanilla-smelting input and output pair. */
public final class BioFurnaceResources {
    public static String key(Item item) {
        var id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? "" : id.toString();
    }

    public static Item item(String key) {
        var id = ResourceLocation.tryParse(key);
        if (id == null) throw new IllegalArgumentException("Invalid Bio-Furnace item: " + key);
        return BuiltInRegistries.ITEM.getOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown Bio-Furnace item: " + key));
    }

    private BioFurnaceResources() {}
}
