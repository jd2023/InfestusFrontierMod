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
 * laid out by {@link Sheet}, and every screen shares its grammar: title, a status line under it, labels above the
 * organ's parts, the biomass tank on the right. A subclass draws its parts in {@link #renderContents} and reads its
 * menu; it never decides a transfer. Coordinates are panel-relative.
 */
public abstract class OrganScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    private static final ResourceLocation SHEET =
            ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "textures/gui/organ.png");
    private static final Component BIOMASS = Component.translatable("gui.infestusfrontier.biomass_label");

    protected OrganScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = Sheet.PANEL.width();
        imageHeight = Sheet.PANEL.height();
        inventoryLabelY = imageHeight - 94;
    }

    /** Draws the organ's own parts over the panel. The player inventory wells are already there. */
    protected abstract void renderContents(GuiGraphics graphics, int mouseX, int mouseY);

    /** Which glyph the status line shows: {@link Sheet#RING}, {@link Sheet#ARROW}, {@link Sheet#CROSS} or {@link Sheet#BARS}. */
    protected abstract int statusGlyph();

    /** The words of the status line. */
    protected abstract Component statusText();

    /** The biomass held, drawn in the shared tank. */
    protected abstract Meter biomass();

    /** The amount under the tank, short. */
    protected abstract Component biomassShort();

    /** The amount against the capacity, for the tank's tooltip. */
    protected abstract Component biomassLong();

    @Override
    protected final void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        sprite(graphics, Sheet.PANEL, 0, 0);
        sprite(graphics, Sheet.glyph(statusGlyph()), Sheet.STATUS_GLYPH_X, Sheet.STATUS_GLYPH_Y);
        tank(graphics, Sheet.TANK_X, Sheet.TANK_Y, biomass());
        renderContents(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, Sheet.INK, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, Sheet.INK, false);
        graphics.drawString(font, statusText(), Sheet.STATUS_TEXT_X, Sheet.STATUS_TEXT_Y, Sheet.INK, false);
        rightAligned(graphics, BIOMASS, Sheet.RIGHT_EDGE, Sheet.TANK_LABEL_Y);
        rightAligned(graphics, biomassShort(), Sheet.RIGHT_EDGE, Sheet.TANK_VALUE_Y);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (overTank(Sheet.TANK_X, Sheet.TANK_Y, mouseX, mouseY))
            graphics.renderTooltip(font, biomassLong(), mouseX, mouseY);
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

    /** Text ending at panel column {@code rightX}. */
    protected final void rightAligned(GuiGraphics graphics, Component text, int rightX, int y) {
        graphics.drawString(font, text, rightX - font.width(text), y, Sheet.INK, false);
    }

    /** A label centered above the slot at {@code slotX}. */
    protected final void slotLabel(GuiGraphics graphics, Component text, int slotX) {
        centered(graphics, text, slotX + 8, Sheet.LABEL_Y);
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
