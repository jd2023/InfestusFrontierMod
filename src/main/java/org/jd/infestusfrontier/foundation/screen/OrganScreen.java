package org.jd.infestusfrontier.foundation.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * Every organ screen: the panel, the title, a status line under it, the biomass tank on the right, and the organ's
 * own parts in one row between them. A subclass names its state in {@link #status} and adds its parts in
 * {@link #addParts}; it draws nothing itself and decides nothing. Parts are laid out in panel pixels.
 */
public abstract class OrganScreen<M extends AbstractContainerMenu & BiomassView> extends AbstractContainerScreen<M> {
    private static final Component BIOMASS = Component.translatable("gui.infestusfrontier.biomass_label");

    private final int strips;

    protected OrganScreen(M menu, Inventory inventory, Component title) {
        this(menu, inventory, title, 0);
    }

    /** A panel made taller by {@code strips} body strips under the parts row; the menu's inventory slots sit lower. */
    protected OrganScreen(M menu, Inventory inventory, Component title, int strips) {
        super(menu, inventory, title);
        this.strips = strips;
        imageWidth = Sheet.PANEL.width();
        imageHeight = Sheet.panelHeight(strips);
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
        return button(row.place(Sheet.BUTTON.width()), Sheet.partY(Sheet.BUTTON.height()), text, onPress);
    }

    /** A sheet button at panel position {@code x, y}. */
    protected final Button button(int x, int y, Component text, Button.OnPress onPress) {
        return addRenderableWidget(new SheetButton(leftPos + x, topPos + y, text, onPress));
    }

    @Override
    protected final void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        Bounds panel = Sheet.PANEL;
        Draw.sprite(graphics, panel, leftPos, topPos, 0, 0, panel.width(), Sheet.SPLIT_Y);
        int y = topPos + Sheet.SPLIT_Y;
        for (int n = 0; n < strips; n++, y += Sheet.BODY_STRIP.height())
            Draw.sprite(graphics, Sheet.BODY_STRIP, leftPos, y);
        Draw.sprite(graphics, panel, leftPos, y, 0, Sheet.SPLIT_Y, panel.width(), panel.height() - Sheet.SPLIT_Y);
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
