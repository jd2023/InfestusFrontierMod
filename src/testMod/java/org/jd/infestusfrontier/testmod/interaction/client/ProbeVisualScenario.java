package org.jd.infestusfrontier.testmod.interaction.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jd.infestusfrontier.processing.client.CultureBowlScreen;
import org.jd.infestusfrontier.processing.menu.BowlRefusal;
import org.jd.infestusfrontier.processing.menu.CultureBowlMenu;
import org.jd.infestusfrontier.processing.menu.CultureBowlMenuSnapshot;
import org.jd.infestusfrontier.testmod.integration.client.ContentVisualScenario;
import org.jd.infestusfrontier.ui.OrganScreenLayout;
import org.lwjgl.glfw.GLFW;

/** Opens the real server menu and captures keyboard focus plus a hovered high-contrast tooltip. */
public final class ProbeVisualScenario implements ContentVisualScenario {
    private static final BlockPos BOWL = new BlockPos(-2, -60, 3);
    private boolean opening;
    private int preparedScale;
    private int settled;
    private boolean refusalRequested;

    @Override public boolean ready(Minecraft game) {
        if (game.level == null || game.player == null) return false;
        var probe = BuiltInRegistries.ITEM.get(ResourceLocation.parse("infestusfrontier:interaction/synaptic_probe"));
        var stack = game.player.getInventory().getItem(6);
        return stack.is(probe) && !game.getItemRenderer().getModel(stack, game.level, game.player, 0)
                .getParticleIcon().contents().name().toString().equals("minecraft:missingno");
    }

    @Override public List<UiView> uiViews() {
        return List.of(new UiView("culture-bowl-gui-scale-2.png", 2),
                new UiView("culture-bowl-gui-scale-3.png", 3),
                new UiView("culture-bowl-gui-scale-4.png", 4));
    }

    @Override public boolean prepareUi(Minecraft game, UiView view) {
        if (!(game.screen instanceof CultureBowlScreen)) {
            if (!opening) {
                game.player.getInventory().selected = 6;
                game.gameMode.useItemOn(game.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(BOWL), Direction.UP, BOWL, false));
                opening = true;
            }
            return false;
        }
        var snapshot = ((CultureBowlMenu) game.player.containerMenu).snapshot();
        if (snapshot.state() != CultureBowlMenuSnapshot.State.WORKING || snapshot.completedWork() == 0)
            throw new IllegalStateException("Refusal capture requires actual incubation progress");
        if (!refusalRequested) {
            var start = OrganScreenLayout.centered(game.screen.width, game.screen.height).control("start").bounds();
            if (!game.screen.mouseClicked(start.centerX(), start.centerY(), 0))
                throw new IllegalStateException("Unclickable Start during incubation");
            game.screen.mouseReleased(start.centerX(), start.centerY(), 0);
            game.screen.setFocused(null);
            refusalRequested = true;
            return false;
        }
        if (snapshot.refusal() != BowlRefusal.ACTIVE_BATCH) return false;
        if (preparedScale != view.guiScale()) {
            preparedScale = view.guiScale();
            settled = 0;
            game.screen.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0);
            if (!(game.screen.getFocused() instanceof net.minecraft.client.gui.components.AbstractWidget focused)
                    || !focused.isFocused() || focused.getMessage().getString().isBlank())
                throw new IllegalStateException("Missing keyboard focus/narration label at scale " + preparedScale);
            var widgets = game.screen.children().stream()
                    .filter(child -> child instanceof net.minecraft.client.gui.components.AbstractWidget)
                    .map(child -> (net.minecraft.client.gui.components.AbstractWidget) child).toList();
            var layout = OrganScreenLayout.centered(
                    game.getWindow().getGuiScaledWidth(), game.getWindow().getGuiScaledHeight());
            if (widgets.size() != layout.focusOrder().size()) throw new IllegalStateException("Missing organ controls");
            for (var widget : widgets) {
                if (widget.getX() < 0 || widget.getY() < 0 || widget.getRight() > game.screen.width
                        || widget.getBottom() > game.screen.height || !widget.isMouseOver(
                        widget.getX() + widget.getWidth() / 2.0, widget.getY() + widget.getHeight() / 2.0))
                    throw new IllegalStateException("Inaccessible control at scale " + preparedScale);
            }
            var hover = layout.control("recipe-0").bounds();
            double x = hover.centerX() * (double) game.getWindow().getScreenWidth() / game.getWindow().getGuiScaledWidth();
            double y = hover.centerY() * (double) game.getWindow().getScreenHeight() / game.getWindow().getGuiScaledHeight();
            GLFW.glfwSetCursorPos(game.getWindow().getWindow(), x, y);
        }
        if (++settled < 3) return false;
        verifyRefusalPixels(game);
        return true;
    }

    /** Inspect the rendered frame, so stale drawing coordinates cannot evade layout tests. */
    private static void verifyRefusalPixels(Minecraft game) {
        var panel = OrganScreenLayout.centered(game.screen.width, game.screen.height).panel();
        int scale = (int) game.getWindow().getGuiScale();
        int refusalPixels = 0;
        int progressPixels = 0;
        try (var frame = Screenshot.takeScreenshot(game.getMainRenderTarget())) {
            for (int y = panel.y() * scale; y < (panel.y() + panel.height()) * scale; y++) {
                boolean refusalRow = false;
                boolean progressRow = false;
                for (int x = panel.x() * scale; x < (panel.x() + panel.width()) * scale; x++) {
                    int rgb = frame.getPixelRGBA(x, y) & 0xFFFFFF; // NativeImage uses ABGR.
                    if (nearColor(rgb, 0xA0A0FF)) {
                        refusalPixels++;
                        refusalRow = true;
                        if (x < (panel.x() + 2) * scale || x >= (panel.x() + panel.width() - 2) * scale
                                || y < (panel.y() + 2) * scale || y >= (panel.y() + panel.height() - 2) * scale)
                            throw new IllegalStateException("Refusal text crosses the panel edge");
                    }
                    if (nearColor(rgb, 0x4BB982)) { progressPixels++; progressRow = true; }
                }
                if (refusalRow && progressRow)
                    throw new IllegalStateException("Refusal text overlaps incubation progress at scale " + scale);
            }
        }
        if (refusalPixels == 0 || progressPixels == 0)
            throw new IllegalStateException("Missing rendered refusal text or incubation progress at scale " + scale
                    + ": refusal=" + refusalPixels + " progress=" + progressPixels);
        com.mojang.logging.LogUtils.getLogger().info("INFESTUS_BOWL_REFUSAL_RENDERED scale={} refusalPixels={} progressPixels={}",
                scale, refusalPixels, progressPixels);
    }

    private static boolean nearColor(int actual, int expected) {
        // The font shader slightly dims RGB; tolerate only its three-level rounding.
        for (int shift = 0; shift <= 16; shift += 8)
            if (Math.abs((actual >>> shift & 255) - (expected >>> shift & 255)) > 3) return false;
        return true;
    }
}
