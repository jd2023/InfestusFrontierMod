package org.jd.infestusfrontier.foundation.render;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.LayerDefinitions;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Client side of a living armor set: vanilla armor whose worn model gains growths (see {@link LivingArmorModel}).
 * A module calls {@link #register} on the client with the set's id and its four pieces; the model is made on first
 * use and dropped when resources reload.
 */
public final class LivingArmor {
    private static final int WHITE = 0xFFFFFF;
    private static final Map<String, LivingArmorModel> MODELS = new HashMap<>();
    private static boolean reloadRegistered;

    private LivingArmor() {}

    /** The set {@code id} reads its growths from {@code assets/<ns>/armor/<path>.json}. */
    @SafeVarargs
    public static void register(IEventBus modBus, ResourceLocation id, Supplier<? extends Item>... pieces) {
        modBus.addListener((RegisterClientExtensionsEvent event) -> event.registerItem(extensions(id), items(pieces)));
        // the icons' layer0 is the dyeable part, tinted as vanilla tints leather; layer1 keeps its colours
        modBus.addListener((RegisterColorHandlersEvent.Item event) -> event.register(
                (stack, tint) -> tint == 0 ? FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, WHITE)) : -1,
                items(pieces)));
        if (!reloadRegistered) {
            reloadRegistered = true;
            modBus.addListener((RegisterClientReloadListenersEvent event) ->
                    event.registerReloadListener((ResourceManagerReloadListener) resources -> MODELS.clear()));
        }
    }

    @SafeVarargs
    private static Item[] items(Supplier<? extends Item>... pieces) {
        return Arrays.stream(pieces).map(Supplier::get).toArray(Item[]::new);
    }

    private static IClientItemExtensions extensions(ResourceLocation id) {
        return new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity wearer, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                return model(id, slot == EquipmentSlot.LEGS);
            }

            @Override
            public void setupModelAnimations(
                    LivingEntity wearer,
                    ItemStack stack,
                    EquipmentSlot slot,
                    Model model,
                    float limbSwing,
                    float limbSwingAmount,
                    float partialTick,
                    float ageInTicks,
                    float netHeadYaw,
                    float headPitch) {
                if (model instanceof LivingArmorModel living) living.prepare(wearer, stack, slot, ageInTicks);
            }

            /** Undyed, the set keeps its own colours instead of vanilla's leather brown. */
            @Override
            public int getDefaultDyeColor(ItemStack stack) {
                return FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, WHITE));
            }
        };
    }

    /** The outer model for helmet, chest and boots, the inner one for leggings, as vanilla splits its armor. */
    private static LivingArmorModel model(ResourceLocation id, boolean inner) {
        return MODELS.computeIfAbsent(id + (inner ? "#inner" : "#outer"), key -> {
            var growths = ArmorGrowths.load(Minecraft.getInstance().getResourceManager(), id);
            var deformation =
                    inner ? LayerDefinitions.INNER_ARMOR_DEFORMATION : LayerDefinitions.OUTER_ARMOR_DEFORMATION;
            var base = LayerDefinition.create(HumanoidArmorModel.createBodyLayer(deformation), 64, 32)
                    .bakeRoot();
            return new LivingArmorModel(base, growths);
        });
    }
}
