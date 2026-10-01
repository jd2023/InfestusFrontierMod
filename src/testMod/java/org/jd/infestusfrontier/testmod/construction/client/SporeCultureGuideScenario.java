package org.jd.infestusfrontier.testmod.construction.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jd.infestusfrontier.testmod.integration.client.ContentGuideScenario;

public final class SporeCultureGuideScenario implements ContentGuideScenario {
    private static final ResourceLocation ITEM = ResourceLocation.fromNamespaceAndPath(
            "infestusfrontier", "construction/spore_culture");

    @Override
    public String assertion() {
        return "infestusfrontier_client:construction.spore_culture.guide";
    }

    @Override
    public boolean tick(Minecraft minecraft) {
        return minecraft.level != null
                && BuiltInRegistries.ITEM.containsKey(ITEM)
                && minecraft.level.getRecipeManager().byKey(ITEM).isPresent();
    }
}
