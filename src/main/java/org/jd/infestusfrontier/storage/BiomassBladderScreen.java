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

/** Client view of the Bladder: the balance as a tank and a number, the service slot and the Fill button. */
final class BiomassBladderScreen extends OrganScreen<BiomassBladderMenu> {
    private static final Component PIECE = Component.translatable("gui.infestusfrontier.biomass_bladder.piece");
    private static final Component FILL = Component.translatable("gui.infestusfrontier.biomass_bladder.fill");
    private static final Component NOTHING_TO_FILL = Refusals.text(new Refusal("equipment.nothing_to_fill"));
    private static final int SLOT_X = 44;
    private static final int SLOT_Y = 35;
    private static final int LABEL_Y = 24;
    private static final int TANK_X = 78;
    private static final int TANK_Y = 14;
    private static final int RIGHT_X = 104;
    private static final int AMOUNT_Y = 20;
    private static final int BUTTON_Y = 34;
    private Button fill;

    BiomassBladderScreen(BiomassBladderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        fill = sheetButton(
                RIGHT_X,
                BUTTON_Y,
                FILL,
                button -> minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId, BiomassBladderMenu.FILL_BUTTON));
        fill.setTooltip(Tooltip.create(NOTHING_TO_FILL));
    }

    @Override
    protected void containerTick() {
        fill.active = menu.hasServiceItem();
    }

    private Meter balance() {
        return new Meter(Math.min(menu.stored(), menu.capacity()), menu.capacity());
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY) {
        well(graphics, SLOT_X, SLOT_Y);
        tank(graphics, TANK_X, TANK_Y, balance());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        centered(graphics, PIECE, SLOT_X + 8, LABEL_Y);
        graphics.drawString(font, amount(), RIGHT_X, AMOUNT_Y, Sheet.INK, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (overTank(TANK_X, TANK_Y, mouseX, mouseY)) graphics.renderTooltip(font, amount(), mouseX, mouseY);
    }

    private Component amount() {
        return Component.translatable("gui.infestusfrontier.biomass", menu.stored(), menu.capacity());
    }
}
