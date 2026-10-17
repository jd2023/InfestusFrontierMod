package org.jd.infestusfrontier.foundation.screen;

import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.Meter;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/** A progress vein on the parts row that fills from the left, with the percent above it. */
public final class Vein extends Part {
    private static final int BAR_Y = Sheet.partY(Sheet.VEIN_EMPTY.height());
    private final IntSupplier percent;

    public Vein(PartRow row, IntSupplier percent) {
        super(
                new Bounds(
                        row.place(Sheet.VEIN_EMPTY.width()),
                        Sheet.LABEL_Y,
                        Sheet.VEIN_EMPTY.width(),
                        BAR_Y + Sheet.VEIN_EMPTY.height() - Sheet.LABEL_Y),
                Component.empty());
        this.percent = percent;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int done = Math.clamp(percent.getAsInt(), 0, 100);
        Component words = Component.translatable("gui.infestusfrontier.percent", done);
        setMessage(words);
        Draw.centered(graphics, words, getX() + getWidth() / 2, getY());
        int barY = getY() + BAR_Y - Sheet.LABEL_Y;
        Draw.sprite(graphics, Sheet.VEIN_EMPTY, getX(), barY);
        int filled = new Meter(done, 100).filled(Sheet.VEIN_FULL.width());
        if (filled > 0) Draw.sprite(graphics, Sheet.VEIN_FULL, getX(), barY, 0, 0, filled, Sheet.VEIN_FULL.height());
    }
}
