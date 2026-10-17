package org.jd.infestusfrontier.discovery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** The criterion triggers of the contact advancements. Registered by {@link DiscoveryModule}. */
final class OrganTriggers {
    private OrganTriggers() {}

    /** Fires with {@code {organ: id}} when a batch the player started completes. */
    static final class OrganBatchCompleted extends SimpleCriterionTrigger<OrganBatchCompleted.Instance> {
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        void trigger(ServerPlayer player, ResourceLocation organ) {
            trigger(player, instance -> instance.organ().equals(organ));
        }

        record Instance(Optional<ContextAwarePredicate> player, ResourceLocation organ)
                implements SimpleCriterionTrigger.SimpleInstance {
            static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                            EntityPredicate.ADVANCEMENT_CODEC
                                    .optionalFieldOf("player")
                                    .forGetter(Instance::player),
                            ResourceLocation.CODEC.fieldOf("organ").forGetter(Instance::organ))
                    .apply(instance, Instance::new));
        }
    }

    /** Fires with {@code {block: id, inserted: boolean}} when a bucket moves biomass at an organ block. */
    static final class BiomassBucket extends SimpleCriterionTrigger<BiomassBucket.Instance> {
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        void trigger(ServerPlayer player, ResourceLocation block, boolean inserted) {
            trigger(player, instance -> instance.block().equals(block) && instance.inserted() == inserted);
        }

        record Instance(Optional<ContextAwarePredicate> player, ResourceLocation block, boolean inserted)
                implements SimpleCriterionTrigger.SimpleInstance {
            static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                            EntityPredicate.ADVANCEMENT_CODEC
                                    .optionalFieldOf("player")
                                    .forGetter(Instance::player),
                            ResourceLocation.CODEC.fieldOf("block").forGetter(Instance::block),
                            Codec.BOOL.fieldOf("inserted").forGetter(Instance::inserted))
                    .apply(instance, Instance::new));
        }
    }
}
