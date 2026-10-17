package org.jd.infestusfrontier.foundation.screen;

import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.Meter;
import org.jd.infestusfrontier.ui.Sheet;

/** A low vessel that fills from the left: its label ends just left of it, its short amount starts just right of it. */
public final class Trough extends Part {
    private static final int TEXT_GAP = 4;
    private final int troughX;
    private final Component label;
    private final IntSupplier value;
    private final IntSupplier max;
    private final Unit unit;

    /**
     * A trough whose top left corner is at panel position {@code x, y}; the label may start at {@code leftX} and the
     * amount runs to {@code rightX}.
     */
    public Trough(int x, int y, int leftX, int rightX, Component label, IntSupplier value, IntSupplier max, Unit unit) {
        super(new Bounds(leftX, y, rightX - leftX, Math.max(Sheet.TROUGH_EMPTY.height(), 9)), label);
        this.troughX = x - leftX;
        this.label = label;
        this.value = value;
        this.max = max;
        this.unit = unit;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int capacity = Math.max(1, max.getAsInt());
        int held = Math.clamp(value.getAsInt(), 0, capacity);
        int x = getX() + troughX;
        Draw.sprite(graphics, Sheet.TROUGH_EMPTY, x, getY());
        int filled = new Meter(held, capacity).filled(Sheet.TROUGH_FULL.width());
        if (filled > 0) Draw.sprite(graphics, Sheet.TROUGH_FULL, x, getY(), 0, 0, filled, Sheet.TROUGH_FULL.height());
        Draw.rightAligned(graphics, label, x - TEXT_GAP, getY());
        Draw.ink(graphics, unit.shortText(held), x + Sheet.TROUGH_EMPTY.width() + TEXT_GAP, getY());
        tip(unit.longText(held, capacity));
    }
}
