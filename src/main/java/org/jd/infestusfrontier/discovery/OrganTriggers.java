package org.jd.infestusfrontier.discovery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import org.jd.infestusfrontier.organ.api.WorkshopActionEvent;

/** The criterion triggers of the contact advancements. Registered by {@link DiscoveryModule}. */
final class OrganTriggers {
    private OrganTriggers() {}

    /**
     * Fires with {@code {organ: id}} when a batch the player started completes. A criterion that also names a
     * {@code recipe} fires only for a batch of that recipe.
     */
    static final class OrganBatchCompleted extends SimpleCriterionTrigger<OrganBatchCompleted.Instance> {
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        void trigger(ServerPlayer player, ResourceLocation organ, Optional<ResourceLocation> recipe) {
            trigger(
                    player,
                    instance -> instance.organ().equals(organ)
                            && instance.recipe()
                                    .map(wanted -> recipe.equals(Optional.of(wanted)))
                                    .orElse(true));
        }

        record Instance(
                Optional<ContextAwarePredicate> player, ResourceLocation organ, Optional<ResourceLocation> recipe)
                implements SimpleCriterionTrigger.SimpleInstance {
            static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                            EntityPredicate.ADVANCEMENT_CODEC
                                    .optionalFieldOf("player")
                                    .forGetter(Instance::player),
                            ResourceLocation.CODEC.fieldOf("organ").forGetter(Instance::organ),
                            ResourceLocation.CODEC.optionalFieldOf("recipe").forGetter(Instance::recipe))
                    .apply(instance, Instance::new));
        }
    }

    /** Fires with {@code {organ: id}} when an organ works on its own for the player who tends it. */
    static final class AutomaticWork extends SimpleCriterionTrigger<AutomaticWork.Instance> {
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

    /** Fires with {@code {action: name}} when a player does a {@link WorkshopActionEvent.Action} at a screen. */
    static final class WorkshopAction extends SimpleCriterionTrigger<WorkshopAction.Instance> {
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        void trigger(ServerPlayer player, WorkshopActionEvent.Action action) {
            trigger(player, instance -> instance.action() == action);
        }

        record Instance(Optional<ContextAwarePredicate> player, WorkshopActionEvent.Action action)
                implements SimpleCriterionTrigger.SimpleInstance {
            static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                            EntityPredicate.ADVANCEMENT_CODEC
                                    .optionalFieldOf("player")
                                    .forGetter(Instance::player),
                            StringRepresentable.fromEnum(WorkshopActionEvent.Action::values)
                                    .fieldOf("action")
                                    .forGetter(Instance::action))
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
