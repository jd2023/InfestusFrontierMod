package org.jd.infestusfrontier.storage;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.foundation.OrganScreen;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.ui.Meter;
import org.jd.infestusfrontier.ui.Sheet;

/** Client view of the Bladder: empty, holding or full, the service slot, the Fill button and the tank. */
final class BiomassBladderScreen extends OrganScreen<BiomassBladderMenu> {
    private static final Component PIECE = Component.translatable("gui.infestusfrontier.biomass_bladder.piece");
    private static final Component FILL = Component.translatable("gui.infestusfrontier.biomass_bladder.fill");
    private static final Component EMPTY = Component.translatable("gui.infestusfrontier.biomass_bladder.empty");
    private static final Component HOLDING = Component.translatable("gui.infestusfrontier.biomass_bladder.holding");
    private static final Component FULL = Component.translatable("gui.infestusfrontier.biomass_bladder.full");
    private static final Component NOTHING_TO_FILL = Refusals.text(new Refusal("equipment.nothing_to_fill"));
    static final int SLOT_X = Sheet.FIRST_PART_X;
    static final int SLOT_Y = Sheet.PARTS_Y;
    private static final int BUTTON_X = SLOT_X + 26;
    private Button fill;

    BiomassBladderScreen(BiomassBladderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        fill = sheetButton(
                BUTTON_X,
                SLOT_Y,
                FILL,
                button -> minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId, BiomassBladderMenu.FILL_BUTTON));
        fill.setTooltip(Tooltip.create(NOTHING_TO_FILL));
    }

    @Override
    protected void containerTick() {
        fill.active = menu.hasServiceItem();
    }

    @Override
    protected int statusGlyph() {
        return menu.stored() == 0 ? Sheet.RING : menu.stored() >= menu.capacity() ? Sheet.BARS : Sheet.ARROW;
    }

    @Override
    protected Component statusText() {
        return menu.stored() == 0 ? EMPTY : menu.stored() >= menu.capacity() ? FULL : HOLDING;
    }

    @Override
    protected Meter biomass() {
        return new Meter(Math.min(menu.stored(), menu.capacity()), menu.capacity());
    }

    @Override
    protected Component biomassShort() {
        return Component.translatable("gui.infestusfrontier.biomass_short", menu.stored());
    }

    @Override
    protected Component biomassLong() {
        return Component.translatable("gui.infestusfrontier.biomass", menu.stored(), menu.capacity());
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY) {
        well(graphics, SLOT_X, SLOT_Y);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        slotLabel(graphics, PIECE, SLOT_X);
    }
}
