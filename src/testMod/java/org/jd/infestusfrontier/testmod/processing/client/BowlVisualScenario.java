package org.jd.infestusfrontier.testmod.processing.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jd.infestusfrontier.testmod.integration.client.ContentVisualScenario;

/** Observes server-created Bowl models and product stacks before the shared capture. */
public final class BowlVisualScenario implements ContentVisualScenario {
    private int settled;
    private boolean started;
    @Override public boolean ready(Minecraft game) {
        if (game.level == null || game.player == null) return false;
        var bowl = BuiltInRegistries.BLOCK.get(id("culture_bowl"));
        boolean ready = game.level.getBlockState(new BlockPos(-2, -60, 3)).is(bowl)
                && game.level.getBlockState(new BlockPos(-2, -60, 4)).is(bowl);
        int slot = 1;
        for (String name : List.of("elastic_gel", "nutrient_mash", "honey_culture", "rooting_gel")) {
            var stack = game.player.getInventory().getItem(slot++);
            ready &= stack.is(BuiltInRegistries.ITEM.get(id(name))) && stack.getCount() == 2;
            if (ready && game.getItemRenderer().getModel(stack, game.level, game.player, 0).getParticleIcon()
                    .contents().name().toString().equals("minecraft:missingno")) {
                throw new IllegalStateException("Missing Bowl product model: " + name);
            }
        }
        if (ready && !started) {
            var pos = new BlockPos(-2, -60, 3);
            game.player.getInventory().selected = 5;
            game.gameMode.useItemOn(game.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false));
            game.player.getInventory().selected = 0;
            started = true;
            return false;
        }
        ready &= started && game.level.getBlockState(new BlockPos(-2, -60, 3)).toString().contains("active=true");
        settled = ready ? Math.min(8, settled + 1) : 0;
        return settled == 8;
    }
    @Override public List<View> detailViews() { return List.of(new View("processing-bowls.png", 32, 24)); }
    private static ResourceLocation id(String name) { return ResourceLocation.parse("infestusfrontier:processing/" + name); }
}
