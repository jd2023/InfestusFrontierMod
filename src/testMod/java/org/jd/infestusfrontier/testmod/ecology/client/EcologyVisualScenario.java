package org.jd.infestusfrontier.testmod.ecology.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jd.infestusfrontier.testmod.integration.client.ContentVisualScenario;

/** Validates the real loaded textures and waits for the server-prepared connected scene. */
public final class EcologyVisualScenario implements ContentVisualScenario {
    private static final ResourceLocation SUBSTRATE = ResourceLocation.parse(
            "infestusfrontier:ecology/living_substrate");
    private boolean texturesChecked;
    private int settledTicks;

    @Override
    public boolean ready(Minecraft minecraft) {
        if (!BuiltInRegistries.BLOCK.containsKey(SUBSTRATE)) return true;
        if (!texturesChecked) {
            verifyTextures(minecraft);
            texturesChecked = true;
        }
        if (minecraft.level == null || minecraft.player == null
                || minecraft.player.position().distanceToSqr(new net.minecraft.world.phys.Vec3(0.5, -60, 0.5)) > 0.01) return false;
        boolean ready = true;
        String[] stages = {"basic", "young", "mature"};
        for (int index = 0; index < stages.length; index++) {
            String property = "stage=" + stages[index];
            for (int offset = 0; offset < 3; offset++) {
                ready &= stage(minecraft, new BlockPos(-4 + index, -61, 5 + offset), property);
            }
            for (int offset = 0; offset < 2; offset++) {
                ready &= stage(minecraft, new BlockPos(2 + index, -60 + offset, 7), property);
            }
            for (int offset = 0; offset < 4; offset++) {
                BlockPos underside = new BlockPos(-1 + index, -55, offset);
                ready &= stage(minecraft, underside, property) && stage(minecraft, underside, "pigment=white")
                        && minecraft.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, underside.below()) == 15;
            }
        }
        ready &= stage(minecraft, new BlockPos(5, -60, 7), "pigment=cyan")
                && stage(minecraft, new BlockPos(5, -59, 7), "pigment=cyan");
        settledTicks = ready ? Math.min(6, settledTicks + 1) : 0;
        return settledTicks == 6;
    }

    @Override
    public List<View> detailViews() {
        return BuiltInRegistries.BLOCK.containsKey(SUBSTRATE)
                ? List.of(new View("ecology-underside.png", 0, -65)) : List.of();
    }

    private static boolean stage(Minecraft minecraft, BlockPos pos, String property) {
        var state = minecraft.level.getBlockState(pos);
        return state.is(BuiltInRegistries.BLOCK.get(SUBSTRATE)) && state.toString().contains(property);
    }

    private static void verifyTextures(Minecraft minecraft) {
        List<NativeImage> images = new ArrayList<>();
        try {
            for (String stage : List.of("basic", "young", "mature")) {
                for (int variant = 0; variant < 2; variant++) {
                    var id = ResourceLocation.fromNamespaceAndPath("infestusfrontier",
                            "textures/block/ecology/living_substrate_" + stage + "_" + variant + ".png");
                    var resource = minecraft.getResourceManager().getResource(id)
                            .orElseThrow(() -> new IllegalStateException("Missing substrate texture " + id));
                    NativeImage image = NativeImage.read(resource.open());
                    if (image.getWidth() != 64 || image.getHeight() != 64) {
                        image.close();
                        throw new IllegalStateException("Substrate texture must be original 64px art: " + id);
                    }
                    images.add(image);
                }
            }
            NativeImage reference = images.getFirst();
            for (NativeImage image : images) {
                for (int offset = 0; offset < 4; offset++) {
                    for (int pixel = 0; pixel < 64; pixel++) {
                        requireEqual(reference.getPixelRGBA(pixel, offset), image.getPixelRGBA(pixel, offset));
                        requireEqual(reference.getPixelRGBA(pixel, 63 - offset), image.getPixelRGBA(pixel, 63 - offset));
                        requireEqual(reference.getPixelRGBA(offset, pixel), image.getPixelRGBA(offset, pixel));
                        requireEqual(reference.getPixelRGBA(63 - offset, pixel), image.getPixelRGBA(63 - offset, pixel));
                    }
                }
            }
            if (interiorHash(images.get(0)) == interiorHash(images.get(2))
                    || interiorHash(images.get(2)) == interiorHash(images.get(4))) {
                throw new IllegalStateException("Basic, young and mature texture interiors must remain distinguishable");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot inspect substrate textures", exception);
        } finally {
            images.forEach(NativeImage::close);
        }
    }

    private static long interiorHash(NativeImage image) {
        long hash = 1;
        for (int y = 4; y < 60; y++) {
            for (int x = 4; x < 60; x++) hash = 31 * hash + image.getPixelRGBA(x, y);
        }
        return hash;
    }

    private static void requireEqual(int expected, int actual) {
        if (expected != actual) throw new IllegalStateException("Connected substrate texture borders do not match");
    }
}
