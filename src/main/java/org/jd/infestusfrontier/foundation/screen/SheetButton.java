package org.jd.infestusfrontier.foundation.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.Sheet;

/** A button drawn from the sheet. Hover and keyboard focus show as an ivory rim; a disabled button sinks and greys. */
final class SheetButton extends Button {
    SheetButton(int x, int y, Component message, OnPress onPress) {
        super(x, y, Sheet.BUTTON.width(), Sheet.BUTTON.height(), message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Bounds face = !active ? Sheet.BUTTON_OFF : isHoveredOrFocused() ? Sheet.BUTTON_HOVER : Sheet.BUTTON;
        Draw.sprite(graphics, face, getX(), getY());
        renderString(graphics, Draw.font(), Sheet.IVORY);
    }
}
