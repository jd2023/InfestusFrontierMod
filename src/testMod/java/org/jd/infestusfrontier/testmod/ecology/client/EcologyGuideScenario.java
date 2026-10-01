package org.jd.infestusfrontier.testmod.ecology.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import org.jd.infestusfrontier.testmod.integration.client.ContentGuideScenario;

public final class EcologyGuideScenario implements ContentGuideScenario {
    private static final ResourceLocation SUBSTRATE = ResourceLocation.fromNamespaceAndPath(
            "infestusfrontier", "ecology/living_substrate");

    @Override
    public String assertion() {
        return "infestusfrontier_client:ecology.living_substrate.guide";
    }

    @Override
    public boolean tick(Minecraft minecraft) {
        if (minecraft.level == null || !BuiltInRegistries.ITEM.containsKey(SUBSTRATE)
                || !BuiltInRegistries.BLOCK.containsKey(SUBSTRATE)
                || minecraft.level.getRecipeManager().byKey(SUBSTRATE).isEmpty()) return false;
        var item = BuiltInRegistries.ITEM.getOptional(SUBSTRATE).orElseThrow();
        var block = BuiltInRegistries.BLOCK.getOptional(SUBSTRATE).orElseThrow();
        return item instanceof BlockItem blockItem && blockItem.getBlock() == block;
    }
}
