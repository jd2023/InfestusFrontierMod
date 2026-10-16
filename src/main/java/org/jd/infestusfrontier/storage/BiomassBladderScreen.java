package org.jd.infestusfrontier.storage;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.Meter;

/** Client view of the Bladder: the balance as a gauge and a number, the service slot and the Fill button. */
final class BiomassBladderScreen extends AbstractContainerScreen<BiomassBladderMenu> {
    private static final int PANEL = 0xFF2B1B22;
    private static final int PANEL_EDGE = 0xFF5A3545;
    private static final int SLOT_WELL = 0xFF17101A;
    private static final int GAUGE_WELL = 0xFF140C12;
    private static final int GAUGE_FLUID = 0xFF9A3B3B;
    private static final int TEXT = 0xFFE8D6DC;
    private static final Bounds GAUGE = new Bounds(80, 17, 16, 52);
    private static final Bounds SERVICE = new Bounds(43, 34, 18, 18);
    private Button fill;

    BiomassBladderScreen(BiomassBladderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        fill = addRenderableWidget(Button.builder(
                        Component.translatable("gui.infestusfrontier.biomass_bladder.fill"),
                        button -> minecraft.gameMode.handleInventoryButtonClick(
                                menu.containerId, BiomassBladderMenu.FILL_BUTTON))
                .bounds(leftPos + 104, topPos + 34, 60, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Refusals.text(
                        new org.jd.infestusfrontier.core.foundation.Refusal("equipment.nothing_to_fill"))))
                .build());
    }

    @Override
    protected void containerTick() {
        fill.active = menu.hasServiceItem();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_EDGE);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, PANEL);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) well(graphics, new Bounds(7 + col * 18, 83 + row * 18, 18, 18));
        for (int col = 0; col < 9; col++) well(graphics, new Bounds(7 + col * 18, 141, 18, 18));
        well(graphics, SERVICE);
        fillBounds(graphics, GAUGE, GAUGE_WELL);
        fillBounds(graphics, new Meter(menu.stored(), menu.capacity()).fillFromBottom(GAUGE), GAUGE_FLUID);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        Component amount = Component.translatable("gui.infestusfrontier.biomass", menu.stored(), menu.capacity());
        graphics.drawString(font, amount, GAUGE.x() + GAUGE.width() + 6, GAUGE.bottom() - font.lineHeight, TEXT, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void well(GuiGraphics graphics, Bounds slot) {
        fillBounds(graphics, slot, SLOT_WELL);
    }

    private void fillBounds(GuiGraphics graphics, Bounds b, int color) {
        graphics.fill(leftPos + b.x(), topPos + b.y(), leftPos + b.right(), topPos + b.bottom(), color);
    }
}
