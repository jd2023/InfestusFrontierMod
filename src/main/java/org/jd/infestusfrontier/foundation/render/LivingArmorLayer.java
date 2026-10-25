package org.jd.infestusfrontier.foundation.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the growths of living armor over what the vanilla armor layer draws, on any humanoid renderer: players, armor
 * stands and humanoid mobs. It is a render layer of its own, so it receives the buffers the entity is drawn with (the
 * glowing outline included) and copies the wearer's pose as the vanilla armor layer does.
 */
final class LivingArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    LivingArmorLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            T wearer,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = wearer.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot) continue;
            LivingArmorModel model = LivingArmor.model(stack.getItem());
            if (model == null) continue;
            getParentModel().copyPropertiesTo((HumanoidModel<T>) (HumanoidModel<?>) model);
            model.render(poseStack, buffers, light, stack, slot, ageInTicks, wearer.getId());
        }
    }
}
