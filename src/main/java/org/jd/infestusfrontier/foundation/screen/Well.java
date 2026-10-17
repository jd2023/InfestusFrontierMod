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
        this(row.place(Sheet.SLOT.width()), Sheet.LABEL_Y, Sheet.PARTS_Y, slot, label);
    }

    /** A well off the parts row, for example on {@link Sheet#ROW2_Y}; the slot must sit one pixel inside it. */
    public Well(int x, int labelY, int wellY, Slot slot, Component label) {
        super(place(x, labelY, wellY, slot), label);
        this.label = label;
    }

    private static Bounds place(int x, int labelY, int wellY, Slot slot) {
        if (slot.x != x + 1 || slot.y != wellY + 1)
            throw new IllegalStateException(
                    "Slot at " + slot.x + "," + slot.y + " is off its well at " + x + "," + wellY);
        return new Bounds(x, labelY, Sheet.SLOT.width(), wellY + Sheet.SLOT.height() - labelY);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Draw.centered(graphics, label, getX() + Sheet.SLOT.width() / 2, getY());
        Draw.sprite(graphics, Sheet.SLOT, getX(), getY() + getHeight() - Sheet.SLOT.height());
    }
}
