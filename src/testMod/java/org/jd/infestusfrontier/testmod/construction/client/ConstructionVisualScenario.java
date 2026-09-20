package org.jd.infestusfrontier.testmod.construction.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import org.jd.infestusfrontier.testmod.integration.client.ContentVisualScenario;

/** Waits for actual server inventory/placement packets, never fabricates client world state. */
public final class ConstructionVisualScenario implements ContentVisualScenario {
    private int settledTicks;
    private net.minecraft.world.entity.Entity originalCamera;
    private boolean originalHideGui;

    @Override public List<View> detailViews() {
        return List.of(new View("construction-shell-front.png", 15, 20),
                new View("construction-shell-back.png", 165, 20));
    }

    @Override public boolean prepareView(Minecraft game, View view) {
        originalCamera = game.getCameraEntity();
        originalHideGui = game.options.hideGui;
        game.options.hideGui = true;
        boolean back = view.filename().equals("construction-shell-back.png");
        var camera = new net.minecraft.world.entity.decoration.ArmorStand(game.level, .5, -59, back ? 10.5 : .5);
        camera.moveTo(.5, -59, back ? 10.5 : .5, view.yaw(), view.pitch());
        camera.setYHeadRot(view.yaw());
        camera.yHeadRotO = view.yaw();
        game.setCameraEntity(camera);
        game.gui.getChat().clearMessages(false);
        game.gui.setOverlayMessage(net.minecraft.network.chat.Component.empty(), false);
        return true;
    }

    @Override public void finishView(Minecraft game, View view) {
        game.setCameraEntity(originalCamera);
        game.options.hideGui = originalHideGui;
        originalCamera = null;
    }

    @Override public List<UiView> uiViews() {
        return List.of(new UiView("construction-guide-navigation.png", 3), new UiView("construction-pouch-guide.png", 3));
    }

    @Override public boolean prepareUi(Minecraft game, UiView view) {
        if (view.filename().equals("construction-pouch-guide.png")) {
            var id = ResourceLocation.parse("infestusfrontier:construction/seed_pouch");
            if (game.screen instanceof com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryScreen screen
                    && screen.getEntry().getId().equals(id)) return true;
            com.klikli_dev.modonomicon.client.gui.BookGuiManager.get().openEntry(
                    org.jd.infestusfrontier.discovery.GuideAccess.BOOK_ID, id, 0);
            return false;
        }
        var book = com.klikli_dev.modonomicon.data.BookDataManager.get().getBook(
                org.jd.infestusfrontier.discovery.GuideAccess.BOOK_ID);
        var entry = book.getEntry(ResourceLocation.parse("infestusfrontier:construction/membrane_window"));
        var category = entry.getCategory();
        var occupied = new java.util.HashSet<String>();
        for (var node : category.getEntries().values()) {
            if (!occupied.add(node.getX() + "," + node.getY())) {
                throw new IllegalStateException("Overlapping guide node: " + node.getId());
            }
        }
        if (game.screen instanceof com.klikli_dev.modonomicon.client.gui.book.node.BookParentNodeScreen screen
                && screen.getCurrentCategoryScreen().getCategory() == category) {
            var layout = new com.klikli_dev.modonomicon.bookstate.visual.CategoryVisualState();
            float scale = com.klikli_dev.modonomicon.client.gui.book.node.BookCategoryNodeScreen.ENTRY_GRID_SCALE;
            layout.scrollX = 2 * 5 * scale;
            layout.scrollY = 2 * 5 * scale;
            layout.targetZoom = 1;
            screen.getCurrentCategoryScreen().loadState(layout);
            screen.renderMouseXOverride = game.getWindow().getGuiScaledWidth() + 100;
            screen.renderMouseYOverride = game.getWindow().getGuiScaledHeight() + 100;
            return true;
        }
        com.klikli_dev.modonomicon.client.gui.BookGuiManager.get().openBook(
                com.klikli_dev.modonomicon.client.gui.book.BookAddress.ignoreSaved(
                        org.jd.infestusfrontier.discovery.GuideAccess.BOOK_ID, category.getId(), null, 0));
        return false;
    }
    @Override
    public boolean ready(Minecraft minecraft) {
        var bud = ResourceLocation.parse("infestusfrontier:construction/organ_bud");
        var culture = ResourceLocation.parse("infestusfrontier:construction/spore_culture");
        if (!BuiltInRegistries.BLOCK.containsKey(bud)) return true;
        var player = minecraft.player;
        boolean ready = player != null && minecraft.level != null
                && player.distanceToSqr(0.5, -60, 0.5) < 0.01
                && player.getMainHandItem().is(BuiltInRegistries.ITEM.get(culture))
                && player.getOffhandItem().is(BuiltInRegistries.ITEM.get(bud));
        for (String name : List.of("living_skin", "living_skin_slab", "living_skin_stairs",
                "living_skin_covering", "rib_frame", "membrane_window", "seed_pouch")) {
            var id = ResourceLocation.parse("infestusfrontier:construction/" + name);
            ready &= BuiltInRegistries.BLOCK.containsKey(id) && BuiltInRegistries.ITEM.containsKey(id);
            var stack = new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(id));
            if (ready && minecraft.getItemRenderer().getModel(stack, minecraft.level, player, 0)
                    .getParticleIcon().contents().name().toString().equals("minecraft:missingno")) {
                throw new IllegalStateException("Missing construction model: " + name);
            }
        }
        var middle = minecraft.level.getBlockState(new BlockPos(0, -60, 5));
        var lumen = minecraft.level.getBlockState(new BlockPos(3, -60, 5));
        ready &= middle.toString().contains("east=true") && middle.toString().contains("west=true")
                && lumen.toString().contains("function=lumen") && lumen.getLightEmission() == 12;
        // Observe stable inventory for the bounded vanilla hand-equip animation before capture.
        settledTicks = ready ? Math.min(12, settledTicks + 1) : 0;
        if (settledTicks < 12) return false;
        minecraft.getToasts().clear();
        return true;
    }

}
