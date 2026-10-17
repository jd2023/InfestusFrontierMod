package org.jd.infestusfrontier.foundation.screen;

import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.Meter;
import org.jd.infestusfrontier.ui.Sheet;

/** A low vessel that fills from the left, with its label and short amount written to its right. */
public final class Trough extends Part {
    private static final int TEXT_GAP = 4;
    private final Component label;
    private final IntSupplier value;
    private final IntSupplier max;
    private final Unit unit;

    /** A trough whose top left corner is at panel position {@code x, y}; the text runs to {@code rightX}. */
    public Trough(int x, int y, int rightX, Component label, IntSupplier value, IntSupplier max, Unit unit) {
        super(new Bounds(x, y, rightX - x, Math.max(Sheet.TROUGH_EMPTY.height(), 9)), label);
        this.label = label;
        this.value = value;
        this.max = max;
        this.unit = unit;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int capacity = Math.max(1, max.getAsInt());
        int held = Math.clamp(value.getAsInt(), 0, capacity);
        Draw.sprite(graphics, Sheet.TROUGH_EMPTY, getX(), getY());
        int filled = new Meter(held, capacity).filled(Sheet.TROUGH_FULL.width());
        if (filled > 0)
            Draw.sprite(graphics, Sheet.TROUGH_FULL, getX(), getY(), 0, 0, filled, Sheet.TROUGH_FULL.height());
        Component words = Component.empty().append(label).append(" ").append(unit.shortText(held));
        Draw.ink(graphics, words, getX() + Sheet.TROUGH_EMPTY.width() + TEXT_GAP, getY());
        tip(unit.longText(held, capacity));
    }
}
