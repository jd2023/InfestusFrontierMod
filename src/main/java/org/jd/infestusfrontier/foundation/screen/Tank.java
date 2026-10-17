package org.jd.infestusfrontier.foundation.screen;

import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.Meter;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * A framed tank that fills from the bottom: its label above, right-aligned to the frame; the short amount below; the
 * amount against the capacity in the tooltip.
 */
public final class Tank extends Part {
    private static final int LABEL_ABOVE = Sheet.TANK_Y - Sheet.TANK_LABEL_Y;
    private static final int VALUE_BELOW = Sheet.TANK_VALUE_Y - Sheet.TANK_Y;
    private static final int TEXT_RIGHT = Sheet.RIGHT_EDGE - Sheet.TANK_X;
    private final Component label;
    private final IntSupplier value;
    private final IntSupplier max;
    private final Unit unit;

    /** A tank whose frame's top left corner is at panel position {@code x, y}. */
    public Tank(int x, int y, Component label, IntSupplier value, IntSupplier max, Unit unit) {
        super(new Bounds(x, y - LABEL_ABOVE, Sheet.TANK_WELL.width(), LABEL_ABOVE + VALUE_BELOW + 9), label);
        this.label = label;
        this.value = value;
        this.max = max;
        this.unit = unit;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int capacity = Math.max(1, max.getAsInt());
        int stored = Math.clamp(value.getAsInt(), 0, capacity);
        int frameY = getY() + LABEL_ABOVE;
        Draw.rightAligned(graphics, label, getX() + TEXT_RIGHT, getY());
        Draw.sprite(graphics, Sheet.TANK_WELL, getX(), frameY);
        Bounds column = Sheet.tankColumn(getX(), frameY);
        Bounds fill = new Meter(stored, capacity).fillFromBottom(column);
        if (fill.height() > 0) {
            int skipped = column.height() - fill.height();
            Draw.sprite(graphics, Sheet.TANK_FILL, fill.x(), fill.y(), 0, skipped, fill.width(), fill.height());
            Draw.sprite(graphics, Sheet.TANK_SURFACE, fill.x(), fill.y());
        }
        Draw.rightAligned(graphics, unit.shortText(stored), getX() + TEXT_RIGHT, frameY + VALUE_BELOW);
        tip(unit.longText(stored, capacity));
    }
}
