package org.jd.infestusfrontier.foundation.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * A living armor set drawn the vanilla way with growths on top. The base is the vanilla armor model and its layer
 * texture, so the vanilla armor layer gives it dye, trims and the enchantment glint as it does any armor. The growths
 * are separate parts with their own texture, drawn once per piece through the same render path (so a baby wearer
 * scales them as vanilla scales its armor), with the glint when the piece has one and never with a trim, which only
 * fits the vanilla shape. Dyed growths take the piece's dye; the rest keep their colours. They breathe slowly.
 */
final class LivingArmorModel extends HumanoidModel<LivingEntity> {
    private enum Pass {
        BASE,
        DYED,
        PLAIN
    }

    private static final float BREATH_TICKS = 80;
    private static final int WHITE = 0xFFFFFFFF;

    private final RenderType growthType;
    private final Map<EquipmentSlot, Growths> growths = new EnumMap<>(EquipmentSlot.class);
    private Pass pass = Pass.BASE;
    /** Set when vanilla starts a piece, spent on its first pass that is not a trim. */
    private boolean pending;

    private EquipmentSlot slot = EquipmentSlot.CHEST;
    private ItemStack stack = ItemStack.EMPTY;

    LivingArmorModel(ModelPart base, ArmorGrowths.Set set) {
        super(base);
        growthType = RenderType.armorCutoutNoCull(set.texture());
        for (var entry : set.bySlot().entrySet()) growths.put(entry.getKey(), new Growths(entry.getValue()));
    }

    /** Vanilla is about to draw {@code stack} in {@code slot}; the growths breathe with the wearer's age. */
    void prepare(LivingEntity wearer, ItemStack stack, EquipmentSlot slot, float ageInTicks) {
        this.stack = stack;
        this.slot = slot;
        pending = true;
        Growths set = growths.get(slot);
        if (set != null) set.breathe(ageInTicks, wearer.getId());
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
        super.renderToBuffer(poseStack, buffer, light, overlay, color);
        Growths set = growths.get(slot);
        if (!pending || set == null || buffer instanceof SpriteCoordinateExpander) return;
        pending = false;
        set.follow(this);
        MultiBufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer growth = ItemRenderer.getArmorFoilBuffer(buffers, growthType, stack.hasFoil());
        pass = Pass.DYED;
        super.renderToBuffer(poseStack, growth, light, overlay, DyedItemColor.getOrDefault(stack, WHITE) | 0xFF000000);
        pass = Pass.PLAIN;
        super.renderToBuffer(poseStack, growth, light, overlay, WHITE);
        pass = Pass.BASE;
    }

    @Override
    protected Iterable<ModelPart> headParts() {
        if (pass == Pass.BASE) return super.headParts();
        return List.of(growths.get(slot).root(pass == Pass.DYED, ArmorGrowths.Bone.HEAD));
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        if (pass == Pass.BASE) return super.bodyParts();
        Growths set = growths.get(slot);
        boolean dyed = pass == Pass.DYED;
        return List.of(
                set.root(dyed, ArmorGrowths.Bone.BODY),
                set.root(dyed, ArmorGrowths.Bone.RIGHT_ARM),
                set.root(dyed, ArmorGrowths.Bone.LEFT_ARM),
                set.root(dyed, ArmorGrowths.Bone.RIGHT_LEG),
                set.root(dyed, ArmorGrowths.Bone.LEFT_LEG));
    }

    /** One slot's growths: a root per bone, dyed and plain, each following its base bone's pose. */
    private static final class Growths {
        private final ArmorGrowths.Slot parts;

        Growths(ArmorGrowths.Slot parts) {
            this.parts = parts;
        }

        ModelPart root(boolean dyed, ArmorGrowths.Bone bone) {
            return parts.root(dyed, bone);
        }

        void follow(HumanoidModel<?> base) {
            for (ArmorGrowths.Bone bone : ArmorGrowths.Bone.values()) {
                ModelPart from = bone.of(base);
                for (boolean dyed : new boolean[] {true, false}) {
                    ModelPart root = parts.root(dyed, bone);
                    root.copyFrom(from);
                    root.visible = from.visible;
                }
            }
        }

        /** A slow breath: each breathing part swells about its own pivot, a little out of step with the others. */
        void breathe(float ageInTicks, int wearer) {
            for (ArmorGrowths.Breather breather : parts.breathers()) {
                double phase = (ageInTicks + wearer * 7) / BREATH_TICKS * Math.PI * 2 - breather.lag();
                float scale = 1 + breather.depth() * (float) (0.5 + 0.5 * Math.sin(phase));
                breather.part().xScale = scale;
                breather.part().yScale = scale;
                breather.part().zScale = scale;
            }
        }
    }
}
