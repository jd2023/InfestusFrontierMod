package org.jd.infestusfrontier.testmod.construction.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import org.jd.infestusfrontier.testmod.integration.client.ContentGuideScenario;

public final class OrganBudGuideScenario implements ContentGuideScenario {
    private static final ResourceLocation BUD = ResourceLocation.fromNamespaceAndPath(
            "infestusfrontier", "construction/organ_bud");

    @Override
    public String assertion() {
        return "infestusfrontier_client:construction.organ_bud.guide";
    }

    @Override
    public boolean tick(Minecraft minecraft) {
        if (minecraft.level == null || !BuiltInRegistries.ITEM.containsKey(BUD)
                || !BuiltInRegistries.BLOCK.containsKey(BUD)
                || minecraft.level.getRecipeManager().byKey(BUD).isEmpty()) return false;
        var item = BuiltInRegistries.ITEM.getOptional(BUD).orElseThrow();
        var block = BuiltInRegistries.BLOCK.getOptional(BUD).orElseThrow();
        return item instanceof BlockItem blockItem && blockItem.getBlock() == block;
    }
}
