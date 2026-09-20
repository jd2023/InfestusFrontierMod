package org.jd.infestusfrontier.processing;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jd.infestusfrontier.processing.api.PreparationRecipes;

/** Finite registry translation for Rack/Loom storage and integration projections. */
public final class PreparationResources {
    private static final Map<String, ResourceLocation> IDS = create();

    private static Map<String, ResourceLocation> create() {
        var ids = new LinkedHashMap<String, ResourceLocation>();
        for (var recipe : PreparationRecipes.all().values()) {
            recipe.routes().forEach(route -> route.itemInputs().keySet().forEach(key -> ids.put(key, id(key))));
            recipe.outputs().keySet().forEach(key -> ids.put(key, id(key)));
        }
        ids.put("biomass_bucket", ResourceLocation.parse("infestusfrontier:storage/biomass_bucket"));
        return Map.copyOf(ids);
    }

    private static ResourceLocation id(String key) {
        return ResourceLocation.parse(switch (key) {
            case "membrane_sheet", "bone_plate" -> "infestusfrontier:processing/" + key;
            default -> "minecraft:" + key;
        });
    }

    public static String key(Item item) {
        var id = BuiltInRegistries.ITEM.getKey(item);
        return IDS.entrySet().stream().filter(entry -> entry.getValue().equals(id))
                .map(Map.Entry::getKey).findFirst().orElse("");
    }

    public static Item item(String key) {
        var id = IDS.get(key);
        if (id == null) throw new IllegalArgumentException("Unsupported preparation item: " + key);
        return BuiltInRegistries.ITEM.getOptional(id).orElseThrow();
    }

    private PreparationResources() {}
}
