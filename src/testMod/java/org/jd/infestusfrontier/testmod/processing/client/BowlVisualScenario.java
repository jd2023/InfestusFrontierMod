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
    private boolean opening;
    private int openTicks;
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
        for (String name : List.of("membrane_sheet", "bone_plate")) {
            var stack = new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(id(name)));
            String texture = game.getItemRenderer().getModel(stack, game.level, game.player, 0)
                    .getParticleIcon().contents().name().toString();
            if (texture.equals("minecraft:missingno")) throw new IllegalStateException("Missing preparation item model: " + name);
        }
        var rackState = game.level.getBlockState(new BlockPos(-4, -60, 8));
        var loomState = game.level.getBlockState(new BlockPos(-2, -60, 8));
        ready &= rackState.is(BuiltInRegistries.BLOCK.get(id("membrane_rack")))
                && loomState.is(BuiltInRegistries.BLOCK.get(id("bone_loom")));
        ready &= game.player.getInventory().getItem(6).is(BuiltInRegistries.ITEM.get(
                ResourceLocation.parse("infestusfrontier:interaction/synaptic_probe")));
        if (ready && !started) {
            if (!opening || ++openTicks == 10) {
                var pos = new BlockPos(-2, -60, 3);
                game.player.getInventory().selected = 6;
                game.gameMode.useItemOn(game.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                        new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false));
                opening = true;
            }
            if (game.player.containerMenu instanceof org.jd.infestusfrontier.processing.menu.CultureBowlMenu menu) {
                menu.submit(org.jd.infestusfrontier.processing.menu.BowlIntentPayload.Intent.START, -1, "I030");
                game.player.closeContainer();
                game.player.getInventory().selected = 0;
                started = true;
            }
            if (openTicks > 40) throw new IllegalStateException("Probe did not open the prepared Bowl menu");
            return false;
        }
        ready &= started && game.level.getBlockState(new BlockPos(-2, -60, 3)).toString().contains("active=true");
        settled = ready ? Math.min(8, settled + 1) : 0;
        return settled == 8;
    }
    @Override public List<View> detailViews() { return List.of(new View("processing-bowls.png", 32, 24),
            new View("processing-preparation.png", 25, 22)); }
    private static ResourceLocation id(String name) { return ResourceLocation.parse("infestusfrontier:processing/" + name); }
}
