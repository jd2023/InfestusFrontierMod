package org.jd.infestusfrontier.foundation.screen;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.Sheet;

/** The glyph under the title and the words beside it. Words that do not fit show their first line and a tooltip. */
public final class StatusLine extends Part {
    private static final Bounds PLACE = new Bounds(
            Sheet.STATUS_GLYPH_X,
            Sheet.STATUS_GLYPH_Y,
            Sheet.STATUS_TEXT_X + Sheet.STATUS_TEXT_WIDTH - Sheet.STATUS_GLYPH_X,
            Sheet.glyph(0).height());
    private final Supplier<Status> status;

    public StatusLine(Supplier<Status> status) {
        super(PLACE, Component.empty());
        this.status = status;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Status now = status.get();
        setMessage(now.text());
        Draw.sprite(graphics, Sheet.glyph(now.kind().glyph()), getX(), getY());
        List<FormattedCharSequence> lines = Draw.font().split(now.text(), Sheet.STATUS_TEXT_WIDTH);
        int textX = getX() + Sheet.STATUS_TEXT_X - Sheet.STATUS_GLYPH_X;
        int textY = getY() + Sheet.STATUS_TEXT_Y - Sheet.STATUS_GLYPH_Y;
        if (!lines.isEmpty()) graphics.drawString(Draw.font(), lines.get(0), textX, textY, Sheet.INK, false);
        if (lines.size() > 1) tip(now.text());
        else setTooltip(null);
    }
}
