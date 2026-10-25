package org.jd.infestusfrontier.foundation.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.LayerDefinitions;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * The growths of one living armor set, drawn over the vanilla armor by {@link LivingArmorLayer}. It is a humanoid
 * model so the wearer's model can copy its pose, age and crouch onto it as vanilla copies them onto armor; only the
 * growth parts are ever drawn, from their own texture, with the glint when the piece has one. Dyed growths take the
 * piece's dye; the rest keep their colours. They breathe slowly.
 */
final class LivingArmorModel extends HumanoidModel<LivingEntity> {
    private enum Pass {
        DYED,
        PLAIN
    }

    private static final float BREATH_TICKS = 80;
    private static final int WHITE = 0xFFFFFFFF;

    private final ArmorGrowths.Set set;
    private final RenderType type;
    private Pass pass = Pass.PLAIN;
    private ArmorGrowths.Slot slot;

    LivingArmorModel(ArmorGrowths.Set set) {
        // the vanilla parts only carry the copied pose; nothing draws them
        super(LayerDefinition.create(
                        HumanoidArmorModel.createBodyLayer(LayerDefinitions.OUTER_ARMOR_DEFORMATION), 64, 32)
                .bakeRoot());
        this.set = set;
        this.type = RenderType.armorCutoutNoCull(set.texture());
    }

    /** Draws {@code stack}'s growths for {@code equipped}, posed as this model's parts now are. */
    void render(
            PoseStack poseStack,
            MultiBufferSource buffers,
            int light,
            ItemStack stack,
            EquipmentSlot equipped,
            float ageInTicks,
            int wearer) {
        slot = set.bySlot().get(equipped);
        if (slot == null || slot.empty()) return;
        follow();
        breathe(ageInTicks, wearer);
        VertexConsumer buffer = ItemRenderer.getArmorFoilBuffer(buffers, type, stack.hasFoil());
        pass = Pass.DYED;
        renderToBuffer(
                poseStack,
                buffer,
                light,
                OverlayTexture.NO_OVERLAY,
                DyedItemColor.getOrDefault(stack, WHITE) | 0xFF000000);
        pass = Pass.PLAIN;
        renderToBuffer(poseStack, buffer, light, OverlayTexture.NO_OVERLAY, WHITE);
    }

    @Override
    protected Iterable<ModelPart> headParts() {
        return List.of(slot.root(pass == Pass.DYED, ArmorGrowths.Bone.HEAD));
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        boolean dyed = pass == Pass.DYED;
        return List.of(
                slot.root(dyed, ArmorGrowths.Bone.BODY),
                slot.root(dyed, ArmorGrowths.Bone.RIGHT_ARM),
                slot.root(dyed, ArmorGrowths.Bone.LEFT_ARM),
                slot.root(dyed, ArmorGrowths.Bone.RIGHT_LEG),
                slot.root(dyed, ArmorGrowths.Bone.LEFT_LEG));
    }

    /** Each growth root takes its bone's copied pose. */
    private void follow() {
        for (ArmorGrowths.Bone bone : ArmorGrowths.Bone.values()) {
            ModelPart from = bone.of(this);
            for (boolean dyed : new boolean[] {true, false})
                slot.root(dyed, bone).copyFrom(from);
        }
    }

    /** A slow breath: each breathing part swells about its own pivot, a little after the one before it. */
    private void breathe(float ageInTicks, int wearer) {
        for (ArmorGrowths.Breather breather : slot.breathers()) {
            double phase = (ageInTicks + wearer * 7) / BREATH_TICKS * Math.PI * 2 - breather.lag();
            float scale = 1 + breather.depth() * (float) (0.5 + 0.5 * Math.sin(phase));
            breather.part().xScale = scale;
            breather.part().yScale = scale;
            breather.part().zScale = scale;
        }
    }
}
