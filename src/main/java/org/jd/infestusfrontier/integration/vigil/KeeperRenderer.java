package org.jd.infestusfrontier.integration.vigil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Original sac-and-tendril model. Only the client animates its breathing and hanging limbs. */
public final class KeeperRenderer extends MobRenderer<Mob, KeeperRenderer.Model> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("infestusfrontier", "vigil_keeper"), "main");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("infestusfrontier", "textures/entity/vigil_keeper.png");

    public KeeperRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(Mob entity) {
        return TEXTURE;
    }

    public static LayerDefinition layer() {
        MeshDefinition mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-8, -8, -6, 16, 12, 12)
                        .texOffs(0, 26)
                        .addBox(-6, -10, -4, 12, 2, 8),
                PartPose.offset(0, 16, 0));
        for (int i = 0; i < 4; i++)
            root.addOrReplaceChild(
                    "tendril" + i,
                    CubeListBuilder.create().texOffs(48, 24).addBox(-1, 0, -1, 2, 8, 2),
                    PartPose.offset(i % 2 == 0 ? -5 : 5, 19, i / 2 == 0 ? -3 : 3));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static final class Model extends EntityModel<Mob> {
        private final ModelPart root;

        Model(ModelPart root) {
            this.root = root;
        }

        @Override
        public void setupAnim(Mob entity, float swing, float amount, float age, float yaw, float pitch) {
            root.getChild("body").yScale = 1 + (float) Math.sin(age * 0.08) * 0.035F;
            for (int i = 0; i < 4; i++) root.getChild("tendril" + i).xRot = (float) Math.sin(age * 0.09 + i) * 0.2F;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
            root.render(pose, buffer, light, overlay, color);
        }
    }
}
