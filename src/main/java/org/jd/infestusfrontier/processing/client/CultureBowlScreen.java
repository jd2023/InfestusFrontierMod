package org.jd.infestusfrontier.processing.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.processing.menu.BowlIntentPayload;
import org.jd.infestusfrontier.processing.menu.CultureBowlMenu;
import org.jd.infestusfrontier.ui.OrganScreenLayout;
import org.jd.infestusfrontier.ui.UiTheme;

/** Thin rendering adapter over the internal UI layout and immutable processing snapshot. */
public final class CultureBowlScreen extends AbstractContainerScreen<CultureBowlMenu> {
    private static final List<String> RECIPES = List.of("I000", "I001", "I005", "I007", "I030", "I033");
    private OrganScreenLayout layout;
    private String selectedRecipe;

    public CultureBowlScreen(CultureBowlMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = OrganScreenLayout.WIDTH;
        imageHeight = OrganScreenLayout.HEIGHT;
        selectedRecipe = menu.snapshot().selectedRecipe();
    }

    @Override protected void init() {
        super.init();
        layout = OrganScreenLayout.centered(width, height);
        for (int slot = 0; slot < 9; slot++) {
            var bounds = layout.control("slot-" + slot).bounds();
            addRenderableWidget(new SnapshotSlotButton(slot, bounds));
        }
        for (int index = 0; index < RECIPES.size(); index++) {
            String recipe = RECIPES.get(index);
            var bounds = layout.control("recipe-" + index).bounds();
            addRenderableWidget(Button.builder(Component.translatable("screen.infestusfrontier.bowl.recipe." + recipe),
                            ignored -> selectedRecipe = recipe)
                    .bounds(bounds.x(), bounds.y(), bounds.width(), bounds.height())
                    .tooltip(Tooltip.create(Component.translatable("screen.infestusfrontier.bowl.recipe.tooltip." + recipe)))
                    .build());
        }
        var start = layout.control("start").bounds();
        addRenderableWidget(Button.builder(Component.translatable("screen.infestusfrontier.bowl.start"), ignored ->
                        menu.submit(BowlIntentPayload.Intent.START, -1, selectedRecipe))
                .bounds(start.x(), start.y(), start.width(), start.height())
                .tooltip(Tooltip.create(Component.translatable("screen.infestusfrontier.bowl.start.tooltip"))).build());
        var cancel = layout.control("cancel").bounds();
        addRenderableWidget(Button.builder(Component.translatable("screen.infestusfrontier.bowl.cancel"), ignored ->
                        menu.submit(BowlIntentPayload.Intent.CANCEL, -1, ""))
                .bounds(cancel.x(), cancel.y(), cancel.width(), cancel.height())
                .tooltip(Tooltip.create(Component.translatable("screen.infestusfrontier.bowl.cancel.tooltip"))).build());
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        var panel = layout.panel();
        graphics.fill(panel.x(), panel.y(), panel.x() + panel.width(), panel.y() + panel.height(), UiTheme.PANEL_EDGE);
        graphics.fill(panel.x() + 2, panel.y() + 2, panel.x() + panel.width() - 2, panel.y() + panel.height() - 2,
                UiTheme.PANEL_BACKGROUND);
        int waterWidth = 216 * menu.snapshot().water() / Math.max(1, menu.snapshot().waterCapacity());
        graphics.fill(panel.x() + 12, panel.y() + 130, panel.x() + 228, panel.y() + 138, UiTheme.PANEL_INSET);
        graphics.fill(panel.x() + 12, panel.y() + 130, panel.x() + 12 + waterWidth, panel.y() + 138, 0xFF3A9FC4);
        int progressWidth = menu.snapshot().requiredWork() == 0 ? 0
                : 216 * menu.snapshot().completedWork() / menu.snapshot().requiredWork();
        graphics.fill(panel.x() + 12, panel.y() + 143, panel.x() + 228, panel.y() + 151, UiTheme.PANEL_INSET);
        graphics.fill(panel.x() + 12, panel.y() + 143, panel.x() + 12 + progressWidth, panel.y() + 151, 0xFF82B94B);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 12, 9, UiTheme.TEXT, false);
        graphics.drawString(font, Component.translatable("screen.infestusfrontier.bowl.water",
                menu.snapshot().water(), menu.snapshot().waterCapacity()), 12, 120, UiTheme.MUTED_TEXT, false);
        graphics.drawString(font, Component.translatable("screen.infestusfrontier.bowl.state." +
                menu.snapshot().state().name().toLowerCase(java.util.Locale.ROOT)), 12, 91, UiTheme.TEXT, false);
        graphics.drawString(font, Component.translatable("screen.infestusfrontier.bowl.completed",
                menu.snapshot().completedBatches()), 12, 102, UiTheme.MUTED_TEXT, false);
        if (menu.snapshot().refusal() != org.jd.infestusfrontier.processing.menu.BowlRefusal.NONE) {
            graphics.drawString(font, Component.translatable("screen.infestusfrontier.bowl.refusal." +
                    menu.snapshot().refusal().name().toLowerCase(java.util.Locale.ROOT)), 12, 155, 0xFFFFA0A0, false);
        }
    }

    private final class SnapshotSlotButton extends AbstractButton {
        private final int slot;
        SnapshotSlotButton(int slot, OrganScreenLayout.Bounds bounds) {
            super(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    Component.translatable("screen.infestusfrontier.bowl.slot", slot + 1));
            this.slot = slot;
            setTooltip(Tooltip.create(Component.translatable("screen.infestusfrontier.bowl.slot.tooltip", slot + 1)));
        }
        @Override public void onPress() { menu.submit(BowlIntentPayload.Intent.EXTRACT_SLOT, slot, ""); }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int border = isFocused() ? UiTheme.FOCUSED_BORDER : UiTheme.PANEL_EDGE;
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), border);
            graphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, UiTheme.PANEL_INSET);
            var stored = menu.snapshot().slots().get(slot);
            if (!stored.resource().isEmpty()) {
                var stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(stored.resource())), stored.count());
                graphics.renderItem(stack, getX() + 1, getY() + 1);
                graphics.renderItemDecorations(font, stack, getX() + 1, getY() + 1);
            }
        }
        @Override public void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
    }
}
