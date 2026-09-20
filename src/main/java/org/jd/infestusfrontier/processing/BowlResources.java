package org.jd.infestusfrontier.processing;

import java.util.Map;
import java.util.LinkedHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jd.infestusfrontier.processing.api.CultureBowlRecipes;

/** Finite registry translation; no arbitrary IDs or item components enter Bowl storage. */
final class BowlResources {
    private static final Map<String, ResourceLocation> IDS = create();
    private static Map<String, ResourceLocation> create() {
        var ids = new LinkedHashMap<String, ResourceLocation>();
        for (var recipe : CultureBowlRecipes.all().values()) {
            recipe.itemInputAlternatives().forEach(inputs -> inputs.keySet().forEach(key -> ids.put(key, id(key))));
            recipe.outputs().keySet().forEach(key -> ids.put(key, id(key)));
            recipe.returnedContainers().keySet().forEach(key -> ids.put(key, id(key)));
        }
        return Map.copyOf(ids);
    }
    private static ResourceLocation id(String key) {
        return ResourceLocation.parse(switch (key) {
            case "spore_culture", "organ_bud" -> "infestusfrontier:construction/" + key;
            case "elastic_gel", "nutrient_mash", "honey_culture", "rooting_gel" -> "infestusfrontier:processing/" + key;
            default -> "minecraft:" + key;
        });
    }
    static String key(Item item) {
        var id = BuiltInRegistries.ITEM.getKey(item);
        return IDS.entrySet().stream().filter(entry -> entry.getValue().equals(id)).map(Map.Entry::getKey).findFirst().orElse("");
    }
    static Item item(String key) {
        var id = IDS.get(key);
        if (id == null) throw new IllegalArgumentException("Unsupported Bowl item: " + key);
        return BuiltInRegistries.ITEM.getOptional(id).orElseThrow();
    }
    private BowlResources() {}
}
