package org.jd.infestusfrontier.foundation.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import org.slf4j.Logger;

/**
 * The growths of one armor set, read from {@code assets/<ns>/armor/<set>.json}, which {@code art/gen_armor.py}
 * writes with the textures: parts on the six humanoid bones, each worn by one slot, dyed or not, some breathing.
 * Coordinates are vanilla model units relative to the bone's pivot, the texture's own box-UV layout.
 */
final class ArmorGrowths {
    enum Bone {
        HEAD(model -> model.head),
        BODY(model -> model.body),
        RIGHT_ARM(model -> model.rightArm),
        LEFT_ARM(model -> model.leftArm),
        RIGHT_LEG(model -> model.rightLeg),
        LEFT_LEG(model -> model.leftLeg);

        private final Function<HumanoidModel<?>, ModelPart> part;

        Bone(Function<HumanoidModel<?>, ModelPart> part) {
            this.part = part;
        }

        ModelPart of(HumanoidModel<?> model) {
            return part.apply(model);
        }

        String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    record Breather(ModelPart part, float depth, float lag) {}

    /** One slot's baked roots, dyed and plain, and the parts that breathe. */
    record Slot(Map<Bone, ModelPart> dyed, Map<Bone, ModelPart> plain, List<Breather> breathers, boolean empty) {
        ModelPart root(boolean isDyed, Bone bone) {
            return (isDyed ? dyed : plain).get(bone);
        }
    }

    record Set(ResourceLocation texture, Map<EquipmentSlot, Slot> bySlot) {}

    private static final Logger LOGGER = LogUtils.getLogger();

    private ArmorGrowths() {}

    /**
     * The set's growths, or none when its file is missing or broken (a resource pack may replace it): the armor then
     * draws as plain vanilla armor and the problem is logged, never a crash on the render thread.
     */
    static Set load(ResourceManager resources, ResourceLocation id) {
        ResourceLocation file = id.withPath(path -> "armor/" + path + ".json");
        try (Reader reader = resources.openAsReader(file)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Armor growths {} could not be read; the armor draws without them", file, e);
            return new Set(file, Map.of());
        }
    }

    private static Set parse(JsonObject json) {
        JsonArray size = json.getAsJsonArray("size");
        int width = size.get(0).getAsInt();
        int height = size.get(1).getAsInt();
        Map<EquipmentSlot, Slot> bySlot = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot :
                List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET))
            bySlot.put(slot, slot(json.getAsJsonArray("parts"), slot, width, height));
        return new Set(ResourceLocation.parse(json.get("texture").getAsString()), bySlot);
    }

    private static Slot slot(JsonArray parts, EquipmentSlot slot, int width, int height) {
        List<Breather> breathers = new ArrayList<>();
        Map<Bone, ModelPart> dyed = bake(parts, slot, true, width, height, breathers);
        Map<Bone, ModelPart> plain = bake(parts, slot, false, width, height, breathers);
        boolean empty = true;
        for (JsonElement element : parts)
            if (element.getAsJsonObject().get("slot").getAsString().equals(slot.getName())) empty = false;
        return new Slot(dyed, plain, List.copyOf(breathers), empty);
    }

    private static Map<Bone, ModelPart> bake(
            JsonArray parts, EquipmentSlot slot, boolean dyed, int width, int height, List<Breather> breathers) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        Map<Bone, PartDefinition> bones = new EnumMap<>(Bone.class);
        for (Bone bone : Bone.values())
            bones.put(bone, root.addOrReplaceChild(bone.key(), CubeListBuilder.create(), PartPose.ZERO));
        List<String[]> breathing = new ArrayList<>();
        List<float[]> breaths = new ArrayList<>();
        int index = 0;
        for (JsonElement element : parts) {
            JsonObject part = element.getAsJsonObject();
            index++;
            if (!part.get("slot").getAsString().equals(slot.getName())) continue;
            if (part.get("dyed").getAsBoolean() != dyed) continue;
            Bone bone = Bone.valueOf(part.get("bone").getAsString().toUpperCase(Locale.ROOT));
            CubeListBuilder cubes = CubeListBuilder.create();
            for (JsonElement c : part.getAsJsonArray("cubes")) {
                JsonObject cube = c.getAsJsonObject();
                float[] origin = floats(cube.getAsJsonArray("origin"));
                float[] dims = floats(cube.getAsJsonArray("size"));
                float[] uv = floats(cube.getAsJsonArray("uv"));
                cubes.texOffs((int) uv[0], (int) uv[1]);
                if (cube.has("mirror") && cube.get("mirror").getAsBoolean()) cubes.mirror();
                cubes.addBox(
                        origin[0],
                        origin[1],
                        origin[2],
                        dims[0],
                        dims[1],
                        dims[2],
                        new CubeDeformation(
                                cube.has("inflate") ? cube.get("inflate").getAsFloat() : 0));
                cubes.mirror(false);
            }
            float[] pivot = floats(part.getAsJsonArray("pivot"));
            float[] rotation = part.has("rotation") ? floats(part.getAsJsonArray("rotation")) : new float[3];
            String name = "part_" + index;
            bones.get(bone)
                    .addOrReplaceChild(
                            name,
                            cubes,
                            PartPose.offsetAndRotation(
                                    pivot[0],
                                    pivot[1],
                                    pivot[2],
                                    rotation[0] * Mth.DEG_TO_RAD,
                                    rotation[1] * Mth.DEG_TO_RAD,
                                    rotation[2] * Mth.DEG_TO_RAD));
            if (part.has("breath")) {
                breathing.add(new String[] {bone.key(), name});
                breaths.add(new float[] {
                    part.get("breath").getAsFloat(),
                    part.has("lag") ? part.get("lag").getAsFloat() : 0
                });
            }
        }
        ModelPart baked = LayerDefinition.create(mesh, width, height).bakeRoot();
        Map<Bone, ModelPart> out = new EnumMap<>(Bone.class);
        for (Bone bone : Bone.values()) out.put(bone, baked.getChild(bone.key()));
        for (int i = 0; i < breathing.size(); i++)
            breathers.add(new Breather(
                    baked.getChild(breathing.get(i)[0]).getChild(breathing.get(i)[1]),
                    breaths.get(i)[0],
                    breaths.get(i)[1]));
        return out;
    }

    private static float[] floats(JsonArray array) {
        float[] out = new float[array.size()];
        for (int i = 0; i < out.length; i++) out[i] = array.get(i).getAsFloat();
        return out;
    }
}
