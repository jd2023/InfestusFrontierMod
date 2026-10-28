package org.jd.infestusfrontier.foundation.screen;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * A groove with a chitin thumb beside a scrolled grid of wells. It shows the row it is given against the last row and
 * asks for the row the player wants: a click or a drag in the groove, or the wheel over it; the menu decides. A
 * container screen keeps drags from its widgets, so the screen passes them on while {@link #dragging}, and sends the
 * wheel over the grid to {@link #scrollBy}.
 */
public final class Scrollbar extends Part {
    private final IntSupplier row;
    private final IntSupplier lastRow;
    private final IntConsumer scrollTo;
    private final Supplier<Component> story;
    private boolean dragging;

    /** A scrollbar whose groove's top left corner is at panel position {@code x, y}. */
    public Scrollbar(
            int x, int y, IntSupplier row, IntSupplier lastRow, IntConsumer scrollTo, Supplier<Component> story) {
        super(new Bounds(x, y, Sheet.SCROLL_TRACK.width(), Sheet.SCROLL_TRACK.height()), Component.empty());
        this.row = row;
        this.lastRow = lastRow;
        this.scrollTo = scrollTo;
        this.story = story;
        active = true;
    }

    /** How far the thumb's top travels down the groove. */
    private int travel() {
        return getHeight() - Sheet.SCROLL_THUMB.height();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int last = lastRow.getAsInt();
        Draw.sprite(graphics, Sheet.SCROLL_TRACK, getX(), getY());
        int at = last <= 0 ? 0 : travel() * Math.clamp(row.getAsInt(), 0, last) / last;
        Draw.sprite(graphics, last <= 0 ? Sheet.SCROLL_THUMB_OFF : Sheet.SCROLL_THUMB, getX(), getY() + at);
        Component words = story.get();
        setMessage(words);
        tip(words);
    }

    /** Asks for the view {@code rows} rows further down; negative for up. */
    public void scrollBy(int rows) {
        scrollTo.accept(row.getAsInt() + rows);
    }

    /** True while the thumb is held. */
    public boolean dragging() {
        return dragging;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (lastRow.getAsInt() <= 0) return;
        dragging = true;
        jump(mouseY);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (dragging) jump(mouseY);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        dragging = false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) return false;
        scrollBy(scrollY > 0 ? -1 : 1);
        return true;
    }

    /** A thumb makes no button sound. */
    @Override
    public void playDownSound(SoundManager manager) {}

    /** Asks for the row whose thumb would centre nearest {@code mouseY}. */
    private void jump(double mouseY) {
        double along = (mouseY - getY() - Sheet.SCROLL_THUMB.height() / 2.0) / travel();
        scrollTo.accept((int) Math.round(Math.clamp(along, 0.0, 1.0) * lastRow.getAsInt()));
    }
}
