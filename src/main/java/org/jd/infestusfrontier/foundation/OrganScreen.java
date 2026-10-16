package org.jd.infestusfrontier.foundation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.Meter;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * Base of every organ screen: the panel, wells, tanks, veins, glyphs and buttons all come from the one GUI sheet
 * laid out by {@link Sheet}. A subclass draws its parts in {@link #renderContents} and reads its menu; it never
 * decides a transfer. Coordinates are panel-relative.
 */
public abstract class OrganScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    private static final ResourceLocation SHEET =
            ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "textures/gui/organ.png");

    protected OrganScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = Sheet.PANEL.width();
        imageHeight = Sheet.PANEL.height();
        inventoryLabelY = imageHeight - 94;
    }

    /** Draws the organ's own parts over the panel. The player inventory wells are already there. */
    protected abstract void renderContents(GuiGraphics graphics, int mouseX, int mouseY);

    @Override
    protected final void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        sprite(graphics, Sheet.PANEL, 0, 0);
        renderContents(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, Sheet.INK, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, Sheet.INK, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    /** Blits one sheet sprite with its top left corner at panel position {@code x, y}. */
    protected final void sprite(GuiGraphics graphics, Bounds sprite, int x, int y) {
        graphics.blit(SHEET, leftPos + x, topPos + y, sprite.x(), sprite.y(), sprite.width(), sprite.height());
    }

    /** A well around the slot at {@code slotX, slotY}. */
    protected final void well(GuiGraphics graphics, int slotX, int slotY) {
        sprite(graphics, Sheet.SLOT, slotX - 1, slotY - 1);
    }

    /** A tank frame at {@code x, y} with its column filled by {@code meter}. */
    protected final void tank(GuiGraphics graphics, int x, int y, Meter meter) {
        sprite(graphics, Sheet.TANK_WELL, x, y);
        Bounds column = Sheet.tankColumn(x, y);
        Bounds fill = meter.fillFromBottom(column);
        if (fill.height() == 0) return;
        int skipped = column.height() - fill.height();
        graphics.blit(
                SHEET,
                leftPos + fill.x(),
                topPos + fill.y(),
                Sheet.TANK_FILL.x(),
                Sheet.TANK_FILL.y() + skipped,
                fill.width(),
                fill.height());
        sprite(graphics, Sheet.TANK_SURFACE, fill.x(), fill.y());
    }

    /** True while the mouse is over the column of a tank drawn at {@code x, y}. */
    protected final boolean overTank(int x, int y, int mouseX, int mouseY) {
        Bounds column = Sheet.tankColumn(x, y);
        return isHovering(column.x(), column.y(), column.width(), column.height(), mouseX, mouseY);
    }

    /** A progress vein at {@code x, y} filled from the left by {@code meter}. */
    protected final void vein(GuiGraphics graphics, int x, int y, Meter meter) {
        sprite(graphics, Sheet.VEIN_EMPTY, x, y);
        int done = meter.filled(Sheet.VEIN_FULL.width());
        if (done == 0) return;
        graphics.blit(
                SHEET,
                leftPos + x,
                topPos + y,
                Sheet.VEIN_FULL.x(),
                Sheet.VEIN_FULL.y(),
                done,
                Sheet.VEIN_FULL.height());
    }

    /** Text centered on panel column {@code centerX}. */
    protected final void centered(GuiGraphics graphics, Component text, int centerX, int y) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, Sheet.INK, false);
    }

    /** A button drawn from the sheet at panel position {@code x, y}. */
    protected final Button sheetButton(int x, int y, Component text, Button.OnPress onPress) {
        return addRenderableWidget(new SheetButton(leftPos + x, topPos + y, text, onPress));
    }

    /** Hover and keyboard focus show as an ivory rim; a disabled button sinks and greys. */
    private static final class SheetButton extends Button {
        SheetButton(int x, int y, Component message, OnPress onPress) {
            super(x, y, Sheet.BUTTON.width(), Sheet.BUTTON.height(), message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Bounds face = !active ? Sheet.BUTTON_OFF : isHoveredOrFocused() ? Sheet.BUTTON_HOVER : Sheet.BUTTON;
            graphics.blit(SHEET, getX(), getY(), face.x(), face.y(), face.width(), face.height());
            renderString(graphics, Minecraft.getInstance().font, Sheet.IVORY);
        }
    }
}
