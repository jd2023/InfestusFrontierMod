package org.jd.infestusfrontier.foundation.screen;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jd.infestusfrontier.ui.Bounds;

/**
 * A short paragraph of ink on the panel, read from a supplier every frame and wrapped to its width over at most a few
 * lines: what a single line would cut off or hide in a tooltip, such as the full reason an organ stopped.
 */
public final class Note extends Part {
    private static final int LINE = 10;
    private final Supplier<Component> text;
    private final int lines;

    /** A paragraph whose top left is at panel position {@code x, y}, {@code width} wide and at most {@code lines} long. */
    public Note(int x, int y, int width, int lines, Supplier<Component> text) {
        super(new Bounds(x, y, width, lines * LINE), Component.empty());
        this.text = text;
        this.lines = lines;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Component words = text.get();
        setMessage(words);
        List<FormattedCharSequence> wrapped = Draw.font().split(words, getWidth());
        for (int line = 0; line < Math.min(lines, wrapped.size()); line++)
            Draw.ink(graphics, wrapped.get(line), getX(), getY() + line * LINE);
    }
}
