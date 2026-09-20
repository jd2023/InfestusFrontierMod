package org.jd.infestusfrontier.ui.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.ui.OrganScreenLayout;
import org.jd.infestusfrontier.ui.UiTheme;

/** Client-only UI foundation. Features bind immutable values and actions, never drawing policy. */
public abstract class OrganScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    protected OrganScreenLayout layout;
    private final Map<AbstractWidget, Component> tooltips = new LinkedHashMap<>();

    protected OrganScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = OrganScreenLayout.WIDTH;
        imageHeight = OrganScreenLayout.HEIGHT;
    }
    @Override protected void init() {
        super.init();
        layout = OrganScreenLayout.centered(width, height);
        tooltips.clear();
    }
    protected final void action(String id, Component label, Component tooltip, Runnable action) {
        var b = layout.control(id).bounds();
        var widget = Button.builder(label, ignored -> action.run()).bounds(b.x(), b.y(), b.width(), b.height()).build();
        addRenderableWidget(widget);
        tooltips.put(widget, tooltip);
    }
    protected final void slot(String id, Component label, Component tooltip, Supplier<ItemStack> item, Runnable action) {
        var widget = new SnapshotSlot(layout.control(id).bounds(), label, item, action);
        addRenderableWidget(widget);
        tooltips.put(widget, tooltip);
    }
    protected final void panel(GuiGraphics graphics) {
        var b = layout.panel();
        graphics.fill(b.x(), b.y(), b.x() + b.width(), b.y() + b.height(), UiTheme.PANEL_EDGE);
        graphics.fill(b.x() + 2, b.y() + 2, b.x() + b.width() - 2, b.y() + b.height() - 2, UiTheme.PANEL_BACKGROUND);
    }
    protected final void bar(GuiGraphics graphics, int y, int value, int capacity, int color) {
        int x = layout.panel().x() + 12;
        int top = layout.panel().y() + y;
        int filled = capacity <= 0 ? 0 : (int) (216L * Math.clamp(value, 0, capacity) / capacity);
        graphics.fill(x, top, x + 216, top + 8, UiTheme.PANEL_INSET);
        graphics.fill(x, top, x + filled, top + 8, color);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        var hovered = tooltips.entrySet().stream().filter(entry -> entry.getKey().isHovered()).findFirst();
        var selected = hovered.or(() -> tooltips.entrySet().stream().filter(entry -> entry.getKey().isFocused()).findFirst());
        selected.ifPresent(entry -> {
            var lines = font.split(entry.getValue(), Math.min(220, width - 12));
            int textWidth = lines.stream().mapToInt(font::width).max().orElse(0);
            int boxHeight = lines.size() * 10 + 8;
            int x = Math.clamp(mouseX + 10, 4, Math.max(4, width - textWidth - 12));
            int y = Math.clamp(mouseY + 14, 4, Math.max(4, height - boxHeight - 4));
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);
            graphics.fill(x, y, x + textWidth + 8, y + boxHeight, UiTheme.TOOLTIP_BACKGROUND);
            for (int i = 0; i < lines.size(); i++) graphics.drawString(font, lines.get(i), x + 4, y + 4 + i * 10, UiTheme.TOOLTIP_TEXT, false);
            graphics.pose().popPose();
        });
    }
    private final class SnapshotSlot extends AbstractButton {
        private final Supplier<ItemStack> item;
        private final Runnable action;
        SnapshotSlot(OrganScreenLayout.Bounds b, Component label, Supplier<ItemStack> item, Runnable action) {
            super(b.x(), b.y(), b.width(), b.height(), label);
            this.item = item;
            this.action = action;
        }
        @Override public void onPress() { action.run(); }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), isFocused() ? UiTheme.FOCUSED_BORDER : UiTheme.PANEL_EDGE);
            graphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, UiTheme.PANEL_INSET);
            var stack = item.get();
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, getX() + 1, getY() + 1);
                graphics.renderItemDecorations(font, stack, getX() + 1, getY() + 1);
            }
        }
        @Override public void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
    }
}
