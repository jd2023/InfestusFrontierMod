package org.jd.infestusfrontier.foundation.screen;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;
import org.jd.infestusfrontier.ui.SlotGrid;

/** A grid of wells around the organ's slots, one label above. Every slot must sit one pixel inside its cell. */
public final class Wells extends Part {
    private final SlotGrid grid;
    private final Component label;

    public Wells(PartRow row, SlotGrid grid, List<Slot> slots, Component label) {
        super(place(row, grid, slots), label);
        this.grid = grid;
        this.label = label;
    }

    private static Bounds place(PartRow row, SlotGrid grid, List<Slot> slots) {
        Bounds all = grid.bounds();
        int x = row.place(all.width());
        if (all.x() != x) throw new IllegalStateException("Grid at " + all.x() + " is off the parts row at " + x);
        if (slots.size() != grid.count())
            throw new IllegalStateException("Grid of " + grid.count() + " wells, " + slots.size() + " slots");
        for (int i = 0; i < slots.size(); i++) {
            Bounds cell = grid.cell(i);
            Slot slot = slots.get(i);
            if (slot.x != cell.x() + 1 || slot.y != cell.y() + 1)
                throw new IllegalStateException(
                        "Slot " + i + " at " + slot.x + "," + slot.y + " is off its well " + cell);
        }
        return new Bounds(all.x(), Sheet.LABEL_Y, all.width(), all.bottom() - Sheet.LABEL_Y);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Draw.centered(graphics, label, getX() + getWidth() / 2, getY());
        int dy = getY() - Sheet.LABEL_Y;
        int dx = getX() - grid.origin().x();
        for (int i = 0; i < grid.count(); i++) {
            Bounds cell = grid.cell(i);
            Draw.sprite(graphics, Sheet.SLOT, cell.x() + dx, cell.y() + dy);
        }
    }
}
