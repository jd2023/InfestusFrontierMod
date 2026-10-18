package org.jd.infestusfrontier.foundation.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.Sheet;

/** The organ's own mark in the panel's left column. An organ may show none. */
public final class Emblem extends Part {
    private final Bounds sprite;

    /** {@code index} names a sheet emblem: {@link Sheet#SAC_EMBLEM}, {@link Sheet#BLADDER_EMBLEM}, ... */
    public Emblem(int index) {
        super(
                new Bounds(
                        Sheet.EMBLEM_X,
                        Sheet.EMBLEM_Y,
                        Sheet.emblem(index).width(),
                        Sheet.emblem(index).height()),
                Component.empty());
        this.sprite = Sheet.emblem(index);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Draw.sprite(graphics, sprite, getX(), getY());
    }
}
