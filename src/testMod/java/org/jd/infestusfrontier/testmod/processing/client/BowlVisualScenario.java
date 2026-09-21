package org.jd.infestusfrontier.testmod.processing.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jd.infestusfrontier.testmod.integration.client.ContentVisualScenario;

/** Observes server-created Bowl models and product stacks before the shared capture. */
public final class BowlVisualScenario implements ContentVisualScenario {
    private static final BlockPos IDLE_FURNACE = new BlockPos(4, -60, 4);
    private static final BlockPos ACTIVE_FURNACE = new BlockPos(6, -60, 4);
    private int settled;
    private boolean started;
    private boolean opening;
    private int openTicks;
    private String cameraView;
    private int cameraTicks;
    @Override public boolean ready(Minecraft game) {
        if (game.level == null || game.player == null) return false;
        var bowl = BuiltInRegistries.BLOCK.get(id("culture_bowl"));
        boolean ready = game.level.getBlockState(new BlockPos(-2, -60, 3)).is(bowl)
                && game.level.getBlockState(new BlockPos(-2, -60, 4)).is(bowl);
        int slot = 1;
        for (String name : List.of("fusion_binder", "lumen_secretion", "char_gland_feed", "skeletal_graft")) {
            var stack = game.player.getInventory().getItem(slot++);
            int expected = name.equals("fusion_binder") ? 4 : name.equals("lumen_secretion") ? 2 : 1;
            ready &= stack.is(BuiltInRegistries.ITEM.get(id(name))) && stack.getCount() == expected;
            if (ready && game.getItemRenderer().getModel(stack, game.level, game.player, 0).getParticleIcon()
                    .contents().name().toString().equals("minecraft:missingno")) {
                throw new IllegalStateException("Missing Bowl product model: " + name);
            }
        }
        for (String name : List.of("membrane_sheet", "bone_plate", "fusion_binder", "lumen_secretion",
                "char_gland_feed", "skeletal_graft")) {
            var stack = new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(id(name)));
            String texture = game.getItemRenderer().getModel(stack, game.level, game.player, 0)
                    .getParticleIcon().contents().name().toString();
            if (texture.equals("minecraft:missingno")) throw new IllegalStateException("Missing preparation item model: " + name);
        }
        var rackState = game.level.getBlockState(new BlockPos(1, -60, 2));
        var loomState = game.level.getBlockState(new BlockPos(3, -60, 2));
        var idleFurnace = game.level.getBlockState(IDLE_FURNACE);
        var activeFurnace = game.level.getBlockState(ACTIVE_FURNACE);
        ready &= rackState.is(BuiltInRegistries.BLOCK.get(id("membrane_rack")))
                && loomState.is(BuiltInRegistries.BLOCK.get(id("bone_loom")));
        var furnace = BuiltInRegistries.BLOCK.get(id("bio_furnace"));
        ready &= idleFurnace.is(furnace) && activeFurnace.is(furnace)
                && idleFurnace.toString().contains("facing=south") && idleFurnace.toString().contains("active=false")
                && activeFurnace.toString().contains("facing=south") && activeFurnace.toString().contains("active=true");
        if (ready) {
            requireFurnaceModel(game, idleFurnace);
            requireFurnaceModel(game, activeFurnace);
        }
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
            new View("processing-preparation.png", -45, 24),
            new View("processing-bio-furnace-front.png", 180, 20),
            new View("processing-bio-furnace-back.png", 0, 20)); }
    @Override public boolean prepareView(Minecraft game, View view) {
        if (view.filename().equals("processing-bowls.png")) return true;
        if (view.filename().equals("processing-preparation.png")) {
            game.player.getInventory().selected = 6;
            game.gui.getChat().clearMessages(false);
            game.gui.setOverlayMessage(net.minecraft.network.chat.Component.empty(), false);
            return true;
        }
        double x = 5.5;
        double z = view.filename().equals("processing-bio-furnace-front.png") ? 7.5 : 0.5;
        if (!view.filename().equals(cameraView)) {
            cameraView = view.filename();
            cameraTicks = 0;
            game.player.connection.sendUnsignedCommand("tp @s " + x + " -59 " + z);
            return false;
        }
        if (++cameraTicks < 8) return false;
        game.gui.getChat().clearMessages(false);
        game.gui.setOverlayMessage(net.minecraft.network.chat.Component.empty(), false);
        return true;
    }
    @Override public void finishView(Minecraft game, View view) {
        if (view.filename().equals("processing-bio-furnace-back.png")) {
            game.player.connection.sendUnsignedCommand("tp @s 0.5 -60 0.5");
        }
    }
    private static void requireFurnaceModel(Minecraft game, net.minecraft.world.level.block.state.BlockState state) {
        String particle = game.getBlockRenderer().getBlockModel(state).getParticleIcon().contents().name().toString();
        if (particle.equals("minecraft:missingno")) {
            throw new IllegalStateException("Missing Bio-Furnace block model");
        }
    }
    private static ResourceLocation id(String name) { return ResourceLocation.parse("infestusfrontier:processing/" + name); }
}
