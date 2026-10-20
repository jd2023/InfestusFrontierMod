package org.jd.infestusfrontier.foundation.screen;

import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jd.infestusfrontier.ui.Bounds;

/** One line of ink on the panel, read from a supplier every frame, with a longer story in its tooltip. */
public final class Tally extends Part {
    private static final int LINE = 9;
    private final Supplier<Component> text;
    private final Supplier<Component> story;

    /** A line whose left end is at panel position {@code x, y} and that may run {@code width} pixels. */
    public Tally(int x, int y, int width, Supplier<Component> text, Supplier<Component> story) {
        super(new Bounds(x, y, width, LINE), Component.empty());
        this.text = text;
        this.story = story;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Component words = text.get();
        setMessage(words);
        Draw.ink(graphics, words, getX(), getY());
        tip(story.get());
    }
}
