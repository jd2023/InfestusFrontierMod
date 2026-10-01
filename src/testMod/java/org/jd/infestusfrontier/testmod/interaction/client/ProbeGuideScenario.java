package org.jd.infestusfrontier.testmod.interaction.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jd.infestusfrontier.testmod.integration.client.ContentGuideScenario;

public final class ProbeGuideScenario implements ContentGuideScenario {
    private static final ResourceLocation PROBE = ResourceLocation.parse("infestusfrontier:interaction/synaptic_probe");
    @Override public String assertion() { return "infestusfrontier_client:interaction.synaptic_probe.guide"; }
    @Override public boolean tick(Minecraft minecraft) {
        return minecraft.level != null && BuiltInRegistries.ITEM.containsKey(PROBE)
                && minecraft.level.getRecipeManager().byKey(PROBE).isPresent();
    }
}
