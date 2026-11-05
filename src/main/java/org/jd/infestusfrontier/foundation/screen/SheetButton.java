package org.jd.infestusfrontier.foundation.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.Sheet;
import org.jetbrains.annotations.Nullable;

/** A button drawn from the sheet. Hover and keyboard focus show as an ivory rim; a disabled button sinks and greys. */
final class SheetButton extends Button {
    private static final int MARK_X = 3;
    /** Pixels between a mark and the text, and between the text and the button's edge. */
    private static final int MARK_GAP = 2;

    private final @Nullable Bounds mark;

    /** {@code mark} is a sprite set into the button's left end, or null. */
    SheetButton(int x, int y, Component message, OnPress onPress, @Nullable Bounds mark) {
        super(x, y, Sheet.BUTTON.width(), Sheet.BUTTON.height(), message, onPress, DEFAULT_NARRATION);
        this.mark = mark;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Bounds face = !active ? Sheet.BUTTON_OFF : isHoveredOrFocused() ? Sheet.BUTTON_HOVER : Sheet.BUTTON;
        Draw.sprite(graphics, face, getX(), getY());
        if (mark != null) Draw.sprite(graphics, mark, getX() + MARK_X, getY() + (getHeight() - mark.height()) / 2);
        if (mark == null) renderString(graphics, Draw.font(), Sheet.IVORY);
        else
            renderScrollingString(
                    graphics,
                    Draw.font(),
                    getMessage(),
                    getX() + MARK_X + mark.width() + MARK_GAP,
                    getY(),
                    getX() + getWidth() - MARK_GAP,
                    getY() + getHeight(),
                    Sheet.IVORY);
    }
}
