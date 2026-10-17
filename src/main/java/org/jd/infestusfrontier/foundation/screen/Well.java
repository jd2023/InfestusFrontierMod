package org.jd.infestusfrontier.foundation.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/** The well around one of the organ's slots, with its label above. The slot must sit where the row puts it. */
public final class Well extends Part {
    private final Component label;

    public Well(PartRow row, Slot slot, Component label) {
        super(place(row, slot), label);
        this.label = label;
    }

    private static Bounds place(PartRow row, Slot slot) {
        int x = row.place(Sheet.SLOT.width());
        if (slot.x != x + 1 || slot.y != Sheet.SLOT_Y)
            throw new IllegalStateException("Slot at " + slot.x + "," + slot.y + " is off the parts row at " + x);
        return new Bounds(x, Sheet.LABEL_Y, Sheet.SLOT.width(), Sheet.PARTS_Y + Sheet.SLOT.height() - Sheet.LABEL_Y);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Draw.centered(graphics, label, getX() + Sheet.SLOT.width() / 2, getY());
        Draw.sprite(graphics, Sheet.SLOT, getX(), getY() + Sheet.PARTS_Y - Sheet.LABEL_Y);
    }
}
