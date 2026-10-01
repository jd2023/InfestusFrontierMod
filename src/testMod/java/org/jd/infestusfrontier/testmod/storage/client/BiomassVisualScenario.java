package org.jd.infestusfrontier.testmod.storage.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jd.infestusfrontier.testmod.integration.client.ContentVisualScenario;

/** Inspects baked resources and exercises real client hand dispatch in the captured scene. */
public final class BiomassVisualScenario implements ContentVisualScenario {
    private static final BlockPos SAC = new BlockPos(-1, -60, -2);
    private boolean modelsChecked;
    private boolean fed;
    private int settled;
    private int fillSettled;
    private int placementStage;
    private int placementTicks;

    @Override public boolean ready(Minecraft game) {
        if (game.level == null || game.player == null) return false;
        if (!modelsChecked) {
            var state = BuiltInRegistries.BLOCK.get(id("storage/biomass_bladder")).defaultBlockState();
            var model = game.getBlockRenderer().getBlockModel(state);
            if (!model.getRenderTypes(state, RandomSource.create(0), ModelData.EMPTY).contains(RenderType.translucent())) {
                throw new IllegalStateException("Bladder must have a translucent enclosing membrane");
            }
            var bucket = new ItemStack(BuiltInRegistries.ITEM.get(id("storage/biomass_bucket")));
            String texture = game.getItemRenderer().getModel(bucket, game.level, game.player, 0)
                    .getParticleIcon().contents().name().toString();
            if (texture.equals("minecraft:item/water_bucket") || texture.equals("minecraft:missingno")) {
                throw new IllegalStateException("Biomass Bucket must have its own readable biomass model");
            }
            if ((game.getItemColors().getColor(bucket, 0) >>> 24) != 255) {
                throw new IllegalStateException("Bucket biomass tint must retain opaque alpha in the item renderer");
            }
            var bladderItem = new ItemStack(BuiltInRegistries.ITEM.get(id("storage/biomass_bladder")));
            var bladderModel = game.getItemRenderer().getModel(bladderItem, game.level, game.player, 0);
            if (bladderModel.getQuads(null, null, RandomSource.create(0)).stream().noneMatch(quad ->
                    quad.getSprite().contents().name().toString().equals("infestusfrontier:block/organ_bud"))) {
                throw new IllegalStateException("Bladder item must retain its enclosing organ model");
            }
            modelsChecked = true;
        }
        for (int fill = 0; fill <= 4; fill++) {
            var state = game.level.getBlockState(new BlockPos(fill - 2, -60, -4));
            if (!state.is(BuiltInRegistries.BLOCK.get(id("storage/biomass_bladder")))
                    || !state.toString().contains("fill=" + fill)) return false;
        }
        return game.level.getBlockState(SAC).is(BuiltInRegistries.BLOCK.get(id("processing/digestive_sac")))
                && game.player.getInventory().getItem(7).is(Items.ROTTEN_FLESH)
                && game.player.getInventory().getItem(8).is(BuiltInRegistries.ITEM.get(id("storage/biomass_bucket")));
    }

    @Override public List<View> detailViews() {
        return List.of(new View("biomass-bladder-fill-levels.png", 180, 17),
                new View("biomass-sac-active-and-bucket.png", 180, 30),
                new View("biomass-building-interactions.png", -90, 22));
    }

    @Override public boolean prepareView(Minecraft game, View view) {
        if (view.filename().equals("biomass-building-interactions.png")) return verifyBuilding(game);
        if (view.filename().equals("biomass-bladder-fill-levels.png")) {
            game.player.getInventory().selected = 8;
            clearMessages(game);
            return ++fillSettled >= 8;
        }
        if (!fed) {
            game.player.getInventory().selected = 7;
            var result = game.gameMode.useItemOn(game.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(SAC), Direction.UP, SAC, false));
            if (!result.consumesAction()) throw new IllegalStateException("Client refused actual Sac feed");
            fed = true;
            return false;
        }
        if (!game.level.getBlockState(SAC).toString().contains("active=true")) return false;
        game.player.getInventory().selected = 8;
        clearMessages(game);
        return ++settled >= 8;
    }

    private static void clearMessages(Minecraft game) {
        game.gui.getChat().clearMessages(false);
        game.gui.setOverlayMessage(net.minecraft.network.chat.Component.empty(), false);
    }

    private boolean verifyBuilding(Minecraft game) {
        if (placementTicks++ % 8 != 0) return false;
        var sac = new BlockPos(3, -60, 0);
        var bladder = new BlockPos(3, -60, 1);
        switch (placementStage++) {
            case 0 -> {
                // Move the original Bud out of the offhand through the normal inventory protocol.
                game.gameMode.handleInventoryMouseClick(game.player.inventoryMenu.containerId, 45, 0,
                        net.minecraft.world.inventory.ClickType.PICKUP, game.player);
                game.gameMode.handleInventoryMouseClick(game.player.inventoryMenu.containerId, 9, 0,
                        net.minecraft.world.inventory.ClickType.PICKUP, game.player);
                game.player.getInventory().selected = 5;
                place(game, sac, Direction.UP, InteractionHand.MAIN_HAND);
            }
            case 1 -> {
                requirePlaced(game, sac.above());
                place(game, bladder, Direction.UP, InteractionHand.MAIN_HAND);
            }
            case 2 -> {
                requirePlaced(game, bladder.above());
                game.gameMode.handleInventoryMouseClick(game.player.inventoryMenu.containerId, 41, 40,
                        net.minecraft.world.inventory.ClickType.SWAP, game.player);
            }
            case 3 -> place(game, sac, Direction.WEST, InteractionHand.OFF_HAND);
            case 4 -> {
                requirePlaced(game, sac.west());
                place(game, bladder, Direction.WEST, InteractionHand.OFF_HAND);
            }
            default -> {
                requirePlaced(game, bladder.west());
                game.player.getInventory().selected = 8;
                com.mojang.logging.LogUtils.getLogger().info("INFESTUS_BIOMASS_CLIENT_PLACEMENT main=2 offhand=2");
                return true;
            }
        }
        return false;
    }

    private static void place(Minecraft game, BlockPos pos, Direction face, InteractionHand hand) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).relative(face, .5), face, pos, false);
        if (hand == InteractionHand.OFF_HAND) {
            if (!game.player.getMainHandItem().isEmpty() || !game.player.getOffhandItem().is(Items.COBBLESTONE)) {
                throw new IllegalStateException("Offhand placement fixture must have an empty main hand");
            }
            if (game.gameMode.useItemOn(game.player, InteractionHand.MAIN_HAND, hit).consumesAction()) {
                throw new IllegalStateException("Organ intercepted the client's empty main hand before offhand dispatch");
            }
        }
        if (!game.gameMode.useItemOn(game.player, hand, hit).consumesAction()) {
            throw new IllegalStateException("Organ prevented client building with " + hand);
        }
    }

    private static void requirePlaced(Minecraft game, BlockPos pos) {
        if (!game.level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.COBBLESTONE)) {
            throw new IllegalStateException("Client/server organ placement was not retained at " + pos);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("infestusfrontier", path);
    }
}
