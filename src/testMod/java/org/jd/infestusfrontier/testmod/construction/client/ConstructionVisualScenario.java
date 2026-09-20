package org.jd.infestusfrontier.testmod.construction.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jd.infestusfrontier.testmod.integration.client.ContentVisualScenario;

/** Waits for actual server inventory/placement packets, never fabricates client world state. */
public final class ConstructionVisualScenario implements ContentVisualScenario {
    private int settledTicks;
    @Override
    public boolean ready(Minecraft minecraft) {
        var bud = ResourceLocation.parse("infestusfrontier:construction/organ_bud");
        var culture = ResourceLocation.parse("infestusfrontier:construction/spore_culture");
        if (!BuiltInRegistries.BLOCK.containsKey(bud)) return true;
        var player = minecraft.player;
        boolean ready = player != null && minecraft.level != null
                && player.distanceToSqr(0.5, -60, 0.5) < 0.01
                && player.getMainHandItem().is(BuiltInRegistries.ITEM.get(culture))
                && player.getOffhandItem().is(BuiltInRegistries.ITEM.get(bud))
                && minecraft.level.getBlockState(new BlockPos(0, -60, 3)).is(BuiltInRegistries.BLOCK.get(bud));
        // Observe stable inventory for the bounded vanilla hand-equip animation before capture.
        settledTicks = ready ? Math.min(12, settledTicks + 1) : 0;
        if (settledTicks < 12) return false;
        minecraft.getToasts().clear();
        return true;
    }
}
