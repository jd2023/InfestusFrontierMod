package org.jd.infestusfrontier.genetics;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.jd.infestusfrontier.core.genetics.Genetics;

/**
 * A researched species as the player sees it. A species is an entity type id; its name is the creature's, and a
 * species whose creature is no longer registered shows its id. Screens carry it as the entity type's registry number.
 */
final class Species {
    private Species() {}

    static Optional<EntityType<?>> creature(String species) {
        ResourceLocation id = ResourceLocation.tryParse(species);
        return id == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(id);
    }

    static Component name(String species) {
        return creature(species).map(EntityType::getDescription).orElseGet(() -> Component.literal(species));
    }

    /** The species' registry number plus one, for a synced int; 0 when it has no registered creature. */
    static int synced(String species) {
        return creature(species)
                .map(type -> BuiltInRegistries.ENTITY_TYPE.getId(type) + 1)
                .orElse(0);
    }

    /** The creature a synced int names, or empty for 0 or an unknown number. */
    static Optional<EntityType<?>> fromSynced(int synced) {
        return synced <= 0 ? Optional.empty() : Optional.ofNullable(BuiltInRegistries.ENTITY_TYPE.byId(synced - 1));
    }

    /** "Pig genome: 40%", or "Pig genome: complete". */
    static MutableComponent progress(Component name, int points) {
        return points >= Genetics.COMPLETE
                ? Component.translatable("gui.infestusfrontier.dna_bank.complete", name)
                : Component.translatable(
                        "gui.infestusfrontier.dna_bank.genome", name, points * 100 / Genetics.COMPLETE);
    }
}
