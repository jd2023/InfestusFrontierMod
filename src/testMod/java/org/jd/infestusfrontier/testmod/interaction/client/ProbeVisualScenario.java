package org.jd.infestusfrontier.testmod.interaction.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jd.infestusfrontier.processing.client.CultureBowlScreen;
import org.jd.infestusfrontier.testmod.integration.client.ContentVisualScenario;
import org.jd.infestusfrontier.ui.OrganScreenLayout;
import org.lwjgl.glfw.GLFW;

/** Opens the real server menu and captures keyboard focus plus a hovered high-contrast tooltip. */
public final class ProbeVisualScenario implements ContentVisualScenario {
    private static final BlockPos BOWL = new BlockPos(-2, -60, 3);
    private boolean opening;
    private int preparedScale;
    private int settled;

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
        return ++settled >= 3;
    }
}
