package org.jd.infestusfrontier.foundation.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * Every organ screen: the panel, the title, a status line under it, the biomass tank on the right, and the organ's
 * own parts in one row between them. A subclass names its state in {@link #status} and adds its parts in
 * {@link #addParts}; it draws nothing itself and decides nothing. Parts are laid out in panel pixels.
 */
public abstract class OrganScreen<M extends AbstractContainerMenu & BiomassView> extends AbstractContainerScreen<M> {
    private static final Component BIOMASS = Component.translatable("gui.infestusfrontier.biomass_label");

    protected OrganScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = Sheet.PANEL.width();
        imageHeight = Sheet.PANEL.height();
        inventoryLabelY = imageHeight - 94;
    }

    /** What the organ is doing now, read from the menu. Called every frame. */
    protected abstract Status status();

    /** Adds the organ's own parts left to right along {@code row}. Called on open and on every resize. */
    protected abstract void addParts(PartRow row);

    @Override
    protected void init() {
        super.init();
        add(new StatusLine(this::status));
        add(new Tank(Sheet.TANK_X, Sheet.TANK_Y, BIOMASS, menu::stored, menu::capacity, Unit.BIOMASS));
        addParts(PartRow.standard());
    }

    /** Puts a part on the panel and shows it. */
    protected final <P extends Part> P add(P part) {
        part.setPosition(leftPos + part.panel().x(), topPos + part.panel().y());
        return addRenderableWidget(part);
    }

    /** A sheet button on the parts row. {@code onPress} sends an intent; the server decides. */
    protected final Button button(PartRow row, Component text, Button.OnPress onPress) {
        int x = row.place(Sheet.BUTTON.width());
        int y = Sheet.partY(Sheet.BUTTON.height());
        return addRenderableWidget(new SheetButton(leftPos + x, topPos + y, text, onPress));
    }

    @Override
    protected final void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        Draw.sprite(graphics, Sheet.PANEL, leftPos, topPos);
    }

    @Override
    protected final void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Draw.ink(graphics, title, titleLabelX, titleLabelY);
        Draw.ink(graphics, playerInventoryTitle, inventoryLabelX, inventoryLabelY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
